import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { adminClient, getUserClient, corsHeaders } from "../_shared/supabaseClient.ts";
import { sendFcmNotification } from "../_shared/fcm.ts";

interface RespondOfferRequest {
  offer_id: string;
  decision: "accept" | "decline";
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

    const body: RespondOfferRequest = await req.json();
    if (!body.offer_id || !body.decision) {
      return new Response(
        JSON.stringify({ error: "offer_id and decision ('accept' | 'decline') are required" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    if (body.decision === "accept") {
      // Execute concurrency-safe atomic accept RPC with row locking
      const { data: acceptResult, error: acceptErr } = await adminClient.rpc(
        "accept_booking_offer",
        {
          p_offer_id: body.offer_id,
          p_worker_user_id: user.id,
        }
      );

      if (acceptErr) {
        throw acceptErr;
      }

      if (!acceptResult.success) {
        return new Response(JSON.stringify(acceptResult), {
          status: 409, // Conflict (e.g. slot full, expired)
          headers: { ...corsHeaders, "Content-Type": "application/json" },
        });
      }

      // If this acceptance fulfilled and confirmed the booking, notify the client!
      if (acceptResult.is_confirmed && acceptResult.booking_id) {
        // Fetch client information to send FCM push notification
        const { data: bookingData } = await adminClient
          .from("bookings")
          .select("client_id, workers_needed, work_date, work_start_time, clients(user_id, business_name)")
          .eq("id", acceptResult.booking_id)
          .single();

        const clientUserId = (bookingData?.clients as any)?.user_id;

        if (clientUserId) {
          const { data: fcmRow } = await adminClient
            .from("fcm_tokens")
            .select("token")
            .eq("user_id", clientUserId)
            .order("updated_at", { ascending: false })
            .limit(1)
            .single();

          if (fcmRow?.token) {
            await sendFcmNotification({
              fcmToken: fcmRow.token,
              userId: clientUserId,
              bookingId: acceptResult.booking_id,
              title: "🎉 Booking Confirmed!",
              body: `All ${acceptResult.workers_needed} workers have accepted your booking for ${bookingData?.work_date}. Tap to view crew details.`,
              data: {
                type: "BOOKING_CONFIRMED",
                booking_id: acceptResult.booking_id,
              },
            });
          }
        }
      }

      return new Response(JSON.stringify(acceptResult), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (body.decision === "decline") {
      const { data: declineResult, error: declineErr } = await adminClient.rpc(
        "decline_booking_offer",
        {
          p_offer_id: body.offer_id,
          p_worker_user_id: user.id,
        }
      );

      if (declineErr) {
        throw declineErr;
      }

      // Proactive backfill: Check if booking needs more workers notified immediately
      const bookingId = declineResult.booking_id;
      if (bookingId) {
        const { data: b } = await adminClient
          .from("bookings")
          .select("workers_needed, workers_accepted, status")
          .eq("id", bookingId)
          .single();

        if (b && (b.status === "searching" || b.status === "partially_filled")) {
          const remainingSlots = b.workers_needed - b.workers_accepted;
          const { count: pendingOffers } = await adminClient
            .from("booking_offers")
            .select("*", { count: "exact", head: true })
            .eq("booking_id", bookingId)
            .eq("status", "pending");

          // If pending offers are less than slots needed, replenish immediately
          if ((pendingOffers ?? 0) < remainingSlots) {
            // Trigger replacement worker match
            const { data: replacementWorkers } = await adminClient.rpc(
              "find_eligible_workers",
              {
                p_booking_id: bookingId,
                p_limit: 1,
                p_max_radius_meters: 25000,
              }
            );

            if (replacementWorkers && replacementWorkers.length > 0) {
              const rw = replacementWorkers[0];
              await adminClient.rpc("create_booking_wave_offers", {
                p_booking_id: bookingId,
                p_wave_number: 1,
                p_worker_ids: [rw.worker_id],
                p_timeout_minutes: 10,
              });

              if (rw.fcm_token) {
                await sendFcmNotification({
                  fcmToken: rw.fcm_token,
                  userId: rw.user_id,
                  bookingId: bookingId,
                  title: "New Job Opportunity Available!",
                  body: `Immediate slot opened up in Pune. Tap to accept before it fills.`,
                  data: {
                    type: "NEW_JOB_OFFER",
                    booking_id: bookingId,
                  },
                });
              }
            }
          }
        }
      }

      return new Response(JSON.stringify(declineResult), {
        status: 200,
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    return new Response(
      JSON.stringify({ error: "Invalid decision. Must be 'accept' or 'decline'" }),
      { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    console.error("Error in respond-offer:", errorMsg);
    return new Response(
      JSON.stringify({ error: errorMsg }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
