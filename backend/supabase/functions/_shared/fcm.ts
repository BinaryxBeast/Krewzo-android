import { adminClient } from "./supabaseClient.ts";

interface FcmNotificationPayload {
  fcmToken: string;
  userId?: string;
  bookingId?: string;
  offerId?: string;
  title: string;
  body: string;
  data?: Record<string, string>;
}

interface FcmResult {
  success: boolean;
  messageId?: string;
  error?: string;
}

/**
 * Sends a high-priority push notification to an Android device using FCM
 * Logs every attempt to `public.notifications_log`.
 */
export async function sendFcmNotification(
  payload: FcmNotificationPayload
): Promise<FcmResult> {
  const fcmServerKey = Deno.env.get("FCM_SERVER_KEY");

  // Create queued log entry
  const { data: logEntry, error: logErr } = await adminClient
    .from("notifications_log")
    .insert({
      user_id: payload.userId,
      booking_id: payload.bookingId,
      offer_id: payload.offerId,
      channel: "push",
      status: "queued",
      title: payload.title,
      body: payload.body,
      fcm_token: payload.fcmToken,
    })
    .select("id")
    .single();

  const logId = logEntry?.id;

  if (!fcmServerKey) {
    const errorMsg = "FCM_SERVER_KEY is not configured in Supabase Secrets";
    if (logId) {
      await adminClient
        .from("notifications_log")
        .update({ status: "failed", last_error: errorMsg })
        .eq("id", logId);
    }
    console.warn(errorMsg);
    return { success: false, error: errorMsg };
  }

  try {
    // FCM Legacy / Direct Endpoint for Android
    const response = await fetch("https://fcm.googleapis.com/fcm/send", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `key=${fcmServerKey}`,
      },
      body: JSON.stringify({
        to: payload.fcmToken,
        priority: "high",
        notification: {
          title: payload.title,
          body: payload.body,
          sound: "default",
          android_channel_id: "krewzo_jobs_channel",
        },
        data: {
          ...payload.data,
          title: payload.title,
          body: payload.body,
          click_action: "FLUTTER_NOTIFICATION_CLICK",
        },
      }),
    });

    const resJson = await response.json();

    if (response.ok && resJson.success === 1) {
      const messageId = resJson.results?.[0]?.message_id || "ok";
      if (logId) {
        await adminClient
          .from("notifications_log")
          .update({
            status: "delivered",
            provider_ref: messageId,
            sent_at: new Date().toISOString(),
            delivered_at: new Date().toISOString(),
          })
          .eq("id", logId);
      }
      return { success: true, messageId };
    } else {
      const errMsg = JSON.stringify(resJson.results?.[0]?.error || resJson);
      if (logId) {
        await adminClient
          .from("notifications_log")
          .update({
            status: "failed",
            last_error: errMsg,
            retry_count: 1,
          })
          .eq("id", logId);
      }
      return { success: false, error: errMsg };
    }
  } catch (err: unknown) {
    const errMsg = err instanceof Error ? err.message : String(err);
    if (logId) {
      await adminClient
        .from("notifications_log")
        .update({
          status: "failed",
          last_error: errMsg,
        })
        .eq("id", logId);
    }
    return { success: false, error: errMsg };
  }
}
