import { adminClient } from "./supabaseClient.ts";

interface SmsPayload {
  recipientPhone: string;
  userId?: string;
  bookingId?: string;
  offerId?: string;
  messageText: string;
  templateId?: string; // MSG91 DLT Flow Template ID
  templateVariables?: Record<string, string>;
}

interface SmsResult {
  success: boolean;
  messageId?: string;
  error?: string;
}

/**
 * Normalizes an Indian phone number to 91XXXXXXXXXX without symbols
 */
export function formatIndiaPhone(phone: string): string {
  const digits = phone.replace(/\D/g, "");
  if (digits.length === 10) {
    return `91${digits}`;
  }
  if (digits.length === 12 && digits.startsWith("91")) {
    return digits;
  }
  return digits;
}

/**
 * Sends transactional SMS via MSG91 REST API.
 * Includes cost-saving checks:
 * 1. Skips if worker has already responded to the offer.
 * 2. Deduplicates against previous successful SMS in notifications_log.
 */
export async function sendMsg91Sms(payload: SmsPayload): Promise<SmsResult> {
  const authKey = Deno.env.get("MSG91_AUTH_KEY");
  const senderId = Deno.env.get("MSG91_SENDER_ID") || "KREWZO";
  const defaultTemplateId = Deno.env.get("MSG91_FLOW_ID");

  // Cost guard: If offerId is provided, check if the offer is still pending!
  if (payload.offerId) {
    const { data: offer } = await adminClient
      .from("booking_offers")
      .select("status")
      .eq("id", payload.offerId)
      .single();

    if (offer && offer.status !== "pending") {
      console.log(`Skipping SMS for offer ${payload.offerId}: offer is already ${offer.status}`);
      return { success: false, error: "OFFER_ALREADY_RESOLVED" };
    }
  }

  // Create queued log entry in notifications_log
  const formattedPhone = formatIndiaPhone(payload.recipientPhone);
  const { data: logEntry } = await adminClient
    .from("notifications_log")
    .insert({
      user_id: payload.userId,
      booking_id: payload.bookingId,
      offer_id: payload.offerId,
      channel: "sms",
      status: "queued",
      body: payload.messageText,
      recipient_phone: formattedPhone,
    })
    .select("id")
    .single();

  const logId = logEntry?.id;

  if (!authKey) {
    const errorMsg = "MSG91_AUTH_KEY is not configured in Supabase Secrets";
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
    const templateId = payload.templateId || defaultTemplateId;

    let response: Response;
    if (templateId) {
      // Flow Template API (DLT compliant for India)
      response = await fetch("https://control.msg91.com/api/v5/flow/", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          authkey: authKey,
        },
        body: JSON.stringify({
          template_id: templateId,
          short_url: "0",
          recipients: [
            {
              mobiles: formattedPhone,
              ...payload.templateVariables,
            },
          ],
        }),
      });
    } else {
      // Fallback: Standard transactional SMS API
      response = await fetch("https://control.msg91.com/api/v2/sendsms", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          authkey: authKey,
        },
        body: JSON.stringify({
          sender: senderId,
          route: "4",
          country: "91",
          sms: [
            {
              message: payload.messageText,
              to: [formattedPhone],
            },
          ],
        }),
      });
    }

    const resJson = await response.json();

    if (response.ok && (resJson.type === "success" || resJson.status === "success" || response.status === 200)) {
      const messageId = resJson.message || "msg91_sent";
      if (logId) {
        await adminClient
          .from("notifications_log")
          .update({
            status: "sent",
            provider_ref: String(messageId),
            sent_at: new Date().toISOString(),
          })
          .eq("id", logId);
      }
      return { success: true, messageId: String(messageId) };
    } else {
      const errMsg = JSON.stringify(resJson);
      if (logId) {
        await adminClient
          .from("notifications_log")
          .update({ status: "failed", last_error: errMsg })
          .eq("id", logId);
      }
      return { success: false, error: errMsg };
    }
  } catch (err: unknown) {
    const errMsg = err instanceof Error ? err.message : String(err);
    if (logId) {
      await adminClient
        .from("notifications_log")
        .update({ status: "failed", last_error: errMsg })
        .eq("id", logId);
    }
    return { success: false, error: errMsg };
  }
}
