import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { adminClient, getUserClient, corsHeaders } from "../_shared/supabaseClient.ts";
import { geocodeAddress } from "../_shared/geocoding.ts";
import { sendFcmNotification } from "../_shared/fcm.ts";
import { sendMsg91Sms } from "../_shared/msg91.ts";

interface CreateBookingRequest {
  workers_needed: number;
  work_date: string; // YYYY-MM-DD
  work_start_time: string; // HH:MM
  work_end_time?: string;
  work_address: string;
  latitude?: number;
  longitude?: number;
  pay_per_worker: number;
  job_notes?: string;
  location_id?: string;
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: "Missing Authorization header" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const userClient = getUserClient(authHeader);
    const {
      data: { user },
      error: userError,
    } = await userClient.auth.getUser();

    if (userError || !user) {
      return new Response(
        JSON.stringify({ error: "Unauthorized user" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Lookup client profile for this user
    const { data: client, error: clientErr } = await adminClient
      .from("clients")
      .select("id, business_name")
      .eq("user_id", user.id)
      .single();

    if (clientErr || !client) {
      return new Response(
        JSON.stringify({ error: "User is not registered as a client" }),
        { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const body: CreateBookingRequest = await req.json();

    if (!body.workers_needed || body.workers_needed <= 0) {
      return new Response(
        JSON.stringify({ error: "workers_needed must be greater than 0" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }
    if (!body.work_date || !body.work_start_time || !body.pay_per_worker) {
      return new Response(
        JSON.stringify({ error: "Missing required booking fields" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Geocode address if latitude / longitude are missing
    let lat = body.latitude;
    let lng = body.longitude;
    let cleanAddress = body.work_address;

    if (!lat || !lng) {
      const geo = await geocodeAddress(body.work_address);
      lat = geo.latitude;
      lng = geo.longitude;
      cleanAddress = geo.formattedAddress || body.work_address;
    }

    // 1. Create the booking row in PostGIS format
    const { data: booking, error: insertBookingErr } = await adminClient
      .from("bookings")
      .insert({
        client_id: client.id,
        location_id: body.location_id || null,
        workers_needed: body.workers_needed,
        pay_per_worker: body.pay_per_worker,
        work_address: cleanAddress,
        work_coordinates: `POINT(${lng} ${lat})`,
        work_date: body.work_date,
        work_start_time: body.work_start_time,
        work_end_time: body.work_end_time || null,
        job_notes: body.job_notes || null,
        status: "searching",
        current_wave: 1,
      })
      .select()
      .single();

    if (insertBookingErr || !booking) {
      throw insertBookingErr || new Error("Failed to create booking");
    }

    // 2. Wave 1 matching: Find nearest available workers (batch size: workers_needed * 2, min 4)
    const waveBatchSize = Math.max(4, body.workers_needed * 2);
    const wave1RadiusMeters = 20000; // 20 km for Wave 1

    const { data: eligibleWorkers, error: matchErr } = await adminClient.rpc(
      "find_eligible_workers",
      {
        p_booking_id: booking.id,
        p_limit: waveBatchSize,
        p_max_radius_meters: wave1RadiusMeters,
      }
    );

    if (matchErr) {
      console.error("Worker matching query failed:", matchErr);
    }

    const workerList: Array<{
      worker_id: string;
      user_id: string;
      full_name: string;
      phone: string;
      distance_meters: number;
      fcm_token: string | null;
    }> = eligibleWorkers || [];

    let dispatchedOffersCount = 0;

    if (workerList.length > 0) {
      const workerIds = workerList.map((w) => w.worker_id);

      // Create offers in database
      const { data: createdOffers, error: offerErr } = await adminClient.rpc(
        "create_booking_wave_offers",
        {
          p_booking_id: booking.id,
          p_wave_number: 1,
          p_worker_ids: workerIds,
          p_timeout_minutes: 10,
        }
      );

      if (offerErr) {
        console.error("Failed to insert wave offers:", offerErr);
      }

      const offerMap = new Map<string, string>();
      (createdOffers || []).forEach((o: { worker_id: string; offer_id: string }) => {
        offerMap.set(o.worker_id, o.offer_id);
      });

      // Dispatch notifications in parallel
      await Promise.allSettled(
        workerList.map(async (worker) => {
          const offerId = offerMap.get(worker.worker_id);
          const distanceKm = (worker.distance_meters / 1000).toFixed(1);
          const title = "New Job Offer Available!";
          const bodyText = `₹${body.pay_per_worker}/worker · ${distanceKm} km away in Pune. Date: ${body.work_date}. Tap to accept.`;

          let pushSuccess = false;

          // 1. Send push if worker has registered FCM token
          if (worker.fcm_token) {
            const pushRes = await sendFcmNotification({
              fcmToken: worker.fcm_token,
              userId: worker.user_id,
              bookingId: booking.id,
              offerId: offerId,
              title: title,
              body: bodyText,
              data: {
                type: "NEW_JOB_OFFER",
                booking_id: booking.id,
                offer_id: offerId || "",
                pay: String(body.pay_per_worker),
                date: body.work_date,
              },
            });

            pushSuccess = pushRes.success;
            if (pushSuccess && offerId) {
              await adminClient
                .from("booking_offers")
                .update({ push_sent: true })
                .eq("id", offerId);
            }
          }

          // 2. Cost-optimized fallback: Only send SMS if worker has NO fcm token OR push failed
          if (!pushSuccess && worker.phone) {
            const smsText = `KREWZO: New work offer! ₹${body.pay_per_worker} on ${body.work_date} in Pune. Open app to accept: https://krewzo.in/j/${booking.id}`;
            const smsRes = await sendMsg91Sms({
              recipientPhone: worker.phone,
              userId: worker.user_id,
              bookingId: booking.id,
              offerId: offerId,
              messageText: smsText,
            });

            if (smsRes.success && offerId) {
              await adminClient
                .from("booking_offers")
                .update({ sms_sent: true })
                .eq("id", offerId);
            }
          }
        })
      );

      dispatchedOffersCount = workerList.length;
    }

    return new Response(
      JSON.stringify({
        success: true,
        booking: {
          id: booking.id,
          status: booking.status,
          workers_needed: booking.workers_needed,
          workers_accepted: 0,
          work_address: booking.work_address,
          work_date: booking.work_date,
          work_start_time: booking.work_start_time,
          pay_per_worker: booking.pay_per_worker,
          current_wave: 1,
        },
        wave1_dispatched_workers: dispatchedOffersCount,
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
        status: 201,
      }
    );
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    console.error("Error creating booking:", errorMsg);
    return new Response(
      JSON.stringify({ error: errorMsg }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
