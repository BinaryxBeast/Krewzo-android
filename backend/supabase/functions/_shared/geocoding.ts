interface GeocodeResult {
  latitude: number;
  longitude: number;
  formattedAddress: string;
}

/**
 * Geocodes an address string using Google Maps Geocoding API.
 * Biased towards Pune, Maharashtra, India.
 * If API key is not provided, defaults to central Pune coordinates (18.5204, 73.8567)
 * to avoid blocking testing/pilot development.
 */
export async function geocodeAddress(address: string): Promise<GeocodeResult> {
  const apiKey = Deno.env.get("GOOGLE_MAPS_API_KEY");

  // Fallback default coordinates: Central Pune
  const fallback: GeocodeResult = {
    latitude: 18.5204303,
    longitude: 73.8567437,
    formattedAddress: address,
  };

  if (!apiKey) {
    console.warn("GOOGLE_MAPS_API_KEY is not set. Using Pune center fallback coordinates.");
    return fallback;
  }

  try {
    const encodedAddress = encodeURIComponent(
      address.toLowerCase().includes("pune") ? address : `${address}, Pune, Maharashtra, India`
    );
    const url = `https://maps.googleapis.com/maps/api/geocode/json?address=${encodedAddress}&key=${apiKey}&region=in`;

    const res = await fetch(url);
    const data = await res.json();

    if (data.status === "OK" && data.results && data.results.length > 0) {
      const topResult = data.results[0];
      return {
        latitude: topResult.geometry.location.lat,
        longitude: topResult.geometry.location.lng,
        formattedAddress: topResult.formatted_address,
      };
    } else {
      console.warn(`Geocoding failed with status: ${data.status}. Using Pune fallback.`);
      return fallback;
    }
  } catch (err) {
    console.error("Geocoding fetch error:", err);
    return fallback;
  }
}
