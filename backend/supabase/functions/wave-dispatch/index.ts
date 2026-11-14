import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { adminClient, corsHeaders } from "../_shared/supabaseClient.ts";
import { sendFcmNotification } from "../_shared/fcm.ts";
import { sendMsg91Sms } from "../_shared/msg91.ts";

/**
 * Wave Escalation Worker:
 * Scans for bookings in 'searching' or 'partially_filled' status where
 * the current wave has expired, and automatically widens the search radius
 * to dispatch the next wave of offers.
 */
serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const nowIso = new Date().toISOString();

    // 1. Mark expired offers
    const { data: expiredOffers } = await adminClient
      .from("booking_offers")
      .update({ status: "expired", updated_at: nowIso })
      .eq("status", "pending")
      .lt("expires_at", nowIso)
      .select("id, booking_id");

    console.log(`Marked ${expiredOffers?.length ?? 0} offers as expired`);

    // 2. Find unfilled bookings that need next wave
    const { data: activeBookings } = await adminClient
      .from("bookings")
      .select("id, workers_needed, workers_accepted, current_wave, pay_per_worker, work_date, work_address, status")
      .in("status", ["searching", "partially_filled"])
      .lt("offer_expires_at", nowIso);

    const escalatedBookings: Array<{ bookingId: string; nextWave: number; workersNotified: number }> = [];

    if (activeBookings && activeBookings.length > 0) {
      for (const b of activeBookings) {
        const remainingNeeded = b.workers_needed - b.workers_accepted;
        if (remainingNeeded <= 0) continue;

        const nextWave = (b.current_wave || 1) + 1;

        // Wave radius progression: Wave 1 = 15km, Wave 2 = 30km, Wave 3+ = 50km
        const radiusMeters = nextWave === 2 ? 30000 : 50000;

        if (nextWave > 4 && b.workers_accepted === 0) {
          // Max waves exceeded without any acceptances: mark booking expired
          await adminClient
            .from("bookings")
            .update({ status: "expired", updated_at: nowIso })
            .eq("id", b.id);
          continue;
        }

        // Find next wave of eligible workers
        const batchSize = Math.max(4, remainingNeeded * 2);
        const { data: eligibleWorkers } = await adminClient.rpc(
          "find_eligible_workers",
          {
            p_booking_id: b.id,
            p_limit: batchSize,
            p_max_radius_meters: radiusMeters,
          }
        );

        const workerList: Array<{
          worker_id: string;
          user_id: string;
          full_name: string;
          phone: string;
          distance_meters: number;
          fcm_token: string | null;
        }> = eligibleWorkers || [];

        if (workerList.length > 0) {
          const workerIds = workerList.map((w) => w.worker_id);

          const { data: createdOffers } = await adminClient.rpc(
            "create_booking_wave_offers",
            {
              p_booking_id: b.id,
              p_wave_number: nextWave,
              p_worker_ids: workerIds,
              p_timeout_minutes: 10,
            }
          );

          const offerMap = new Map<string, string>();
          (createdOffers || []).forEach((o: { worker_id: string; offer_id: string }) => {
            offerMap.set(o.worker_id, o.offer_id);
          });

          // Dispatch notifications
          await Promise.allSettled(
            workerList.map(async (worker) => {
              const offerId = offerMap.get(worker.worker_id);
              const distanceKm = (worker.distance_meters / 1000).toFixed(1);
              const title = `Job Offer (Wave ${nextWave})`;
              const bodyText = `₹${b.pay_per_worker}/worker · ${distanceKm} km away. Date: ${b.work_date}. Tap to accept.`;

              let pushSuccess = false;

              if (worker.fcm_token) {
                const pushRes = await sendFcmNotification({
                  fcmToken: worker.fcm_token,
                  userId: worker.user_id,
                  bookingId: b.id,
                  offerId: offerId,
                  title: title,
                  body: bodyText,
                  data: {
                    type: "NEW_JOB_OFFER",
                    booking_id: b.id,
                    offer_id: offerId || "",
                    wave: String(nextWave),
                  },
                });
                pushSuccess = pushRes.success;
              }

              // SMS Fallback
              if (!pushSuccess && worker.phone) {
                await sendMsg91Sms({
                  recipientPhone: worker.phone,
                  userId: worker.user_id,
                  bookingId: b.id,
                  offerId: offerId,
                  messageText: `KREWZO: Wave ${nextWave} job alert! ₹${b.pay_per_worker} on ${b.work_date}. Open app to claim: https://krewzo.in/j/${b.id}`,
                });
              }
            })
          );

          escalatedBookings.push({
            bookingId: b.id,
            nextWave: nextWave,
            workersNotified: workerList.length,
          });
        }
      }
    }

    return new Response(
      JSON.stringify({
        success: true,
        escalated_bookings: escalatedBookings,
        timestamp: nowIso,
      }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    console.error("Error in wave-dispatch worker:", errorMsg);
    return new Response(
      JSON.stringify({ error: errorMsg }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
