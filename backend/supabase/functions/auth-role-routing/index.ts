import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { adminClient, getUserClient, corsHeaders } from "../_shared/supabaseClient.ts";
import { geocodeAddress } from "../_shared/geocoding.ts";

serve(async (req) => {
  // Handle CORS preflight
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
        JSON.stringify({ error: "Unauthorized user token" }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const body = await req.json().catch(() => ({}));
    const action = body.action || "route_session";

    if (action === "route_session") {
      const phone = user.phone || body.phone;
      if (!phone) {
        return new Response(
          JSON.stringify({ error: "Phone number required" }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      // Call our atomic routing RPC
      const { data: routeData, error: rpcErr } = await adminClient.rpc(
        "get_or_route_user_session",
        {
          p_phone: phone,
          p_auth_user_id: user.id,
        }
      );

      if (rpcErr) {
        throw rpcErr;
      }

      return new Response(JSON.stringify(routeData), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    if (action === "complete_onboarding") {
      const {
        business_name,
        employee_count,
        address_line,
        city = "Pune",
        state = "Maharashtra",
        pincode = "411001",
        latitude,
        longitude,
        logo_url,
      } = body;

      if (!business_name || !address_line) {
        return new Response(
          JSON.stringify({ error: "business_name and address_line are required" }),
          { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
        );
      }

      let lat = latitude;
      let lng = longitude;
      if (!lat || !lng) {
        const geo = await geocodeAddress(`${address_line}, ${city}, ${pincode}`);
        lat = geo.latitude;
        lng = geo.longitude;
      }

      const { data: onboardData, error: onboardErr } = await adminClient.rpc(
        "complete_client_onboarding",
        {
          p_user_id: user.id,
          p_business_name: business_name,
          p_employee_count: employee_count || 10,
          p_address_line: address_line,
          p_city: city,
          p_state: state,
          p_pincode: pincode,
          p_latitude: lat,
          p_longitude: lng,
          p_logo_url: logo_url || null,
        }
      );

      if (onboardErr) {
        throw onboardErr;
      }

      return new Response(JSON.stringify(onboardData), {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      });
    }

    return new Response(
      JSON.stringify({ error: `Unknown action '${action}'` }),
      { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: unknown) {
    const errorMsg = err instanceof Error ? err.message : String(err);
    console.error("Error in auth-role-routing:", errorMsg);
    return new Response(
      JSON.stringify({ error: errorMsg }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
