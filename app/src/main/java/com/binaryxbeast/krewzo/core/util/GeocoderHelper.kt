package com.binaryxbeast.krewzo.core.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class AddressDetails(
    val formattedAddress: String,
    val addressLine: String,
    val city: String,
    val state: String,
    val pincode: String,
    val latitude: Double,
    val longitude: Double
)

data class PunePresetLocation(
    val name: String,
    val subtitle: String,
    val addressLine: String,
    val city: String = "Pune",
    val state: String = "Maharashtra",
    val pincode: String,
    val latitude: Double,
    val longitude: Double
)

object GeocoderHelper {

    private const val TAG = "GeocoderHelper"

    /**
     * Curated major Pune commercial, industrial, and logistics hubs
     * for instant one-tap selection.
     */
    val PUNE_HUBS = listOf(
        PunePresetLocation(
            name = "Hinjawadi IT Park",
            subtitle = "Phase 1 & 2 Logistics Hub",
            addressLine = "Plot 14, Rajiv Gandhi Infotech Park, Hinjawadi Phase 1",
            pincode = "411057",
            latitude = 18.5913,
            longitude = 73.7389
        ),
        PunePresetLocation(
            name = "Bhosari MIDC",
            subtitle = "Industrial Area & Warehouses",
            addressLine = "G-Block, Bhosari MIDC, Pimpri-Chinchwad",
            pincode = "411026",
            latitude = 18.6298,
            longitude = 73.8443
        ),
        PunePresetLocation(
            name = "Chakan Industrial Area",
            subtitle = "Automobile & Heavy Engineering",
            addressLine = "Phase 2, Chakan MIDC, Chakan",
            pincode = "410501",
            latitude = 18.7606,
            longitude = 73.8567
        ),
        PunePresetLocation(
            name = "Hadapsar Magarpatta",
            subtitle = "Tech City & Commercial Warehouses",
            addressLine = "Cybercity, Magarpatta, Hadapsar",
            pincode = "411028",
            latitude = 18.5133,
            longitude = 73.9249
        ),
        PunePresetLocation(
            name = "Talawade IT Park",
            subtitle = "Software & Logistics Zone",
            addressLine = "Software Technology Park, Talawade",
            pincode = "411062",
            latitude = 18.6833,
            longitude = 73.7833
        ),
        PunePresetLocation(
            name = "Viman Nagar",
            subtitle = "Air Cargo & Commerce Zone",
            addressLine = "Viman Nagar Road, Near Airport",
            pincode = "411014",
            latitude = 18.5679,
            longitude = 73.9143
        ),
        PunePresetLocation(
            name = "Swargate Central",
            subtitle = "Central Distribution Point",
            addressLine = "Near Swargate Junction, Swargate",
            pincode = "411042",
            latitude = 18.5018,
            longitude = 73.8636
        ),
        PunePresetLocation(
            name = "Wagholi Industrial",
            subtitle = "East Pune Warehousing Corridor",
            addressLine = "Pune-Nagar Highway, Wagholi",
            pincode = "412207",
            latitude = 18.5804,
            longitude = 73.9806
        )
    )

    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): AddressDetails? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            Log.w(TAG, "Geocoder service is not present on this device")
            return@withContext null
        }

        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val addr = addresses.firstOrNull()
                            if (addr != null) {
                                continuation.resume(addressToDetails(addr, latitude, longitude))
                            } else {
                                continuation.resume(null)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            Log.w(TAG, "Reverse geocoding error: $errorMessage")
                            continuation.resume(null)
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val addr = addresses?.firstOrNull()
                addr?.let { addressToDetails(it, latitude, longitude) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reverse geocoding for ($latitude, $longitude)", e)
            null
        }
    }

    suspend fun forwardGeocode(
        context: Context,
        query: String
    ): AddressDetails? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent() || query.isBlank()) {
            return@withContext null
        }

        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val searchQuery = if (query.contains("pune", ignoreCase = true)) query else "$query, Pune, Maharashtra"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocationName(searchQuery, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val addr = addresses.firstOrNull()
                            if (addr != null) {
                                continuation.resume(addressToDetails(addr, addr.latitude, addr.longitude))
                            } else {
                                continuation.resume(null)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            Log.w(TAG, "Forward geocoding error: $errorMessage")
                            continuation.resume(null)
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(searchQuery, 1)
                val addr = addresses?.firstOrNull()
                addr?.let { addressToDetails(it, it.latitude, it.longitude) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed forward geocoding for: $query", e)
            null
        }
    }

    private fun addressToDetails(addr: Address, lat: Double, lng: Double): AddressDetails {
        val full = (0..addr.maxAddressLineIndex).mapNotNull { addr.getAddressLine(it) }.joinToString(", ")
        val thoroughfare = addr.thoroughfare ?: addr.subLocality ?: addr.featureName ?: ""
        val locality = addr.locality ?: "Pune"
        val state = addr.adminArea ?: "Maharashtra"
        val postal = addr.postalCode ?: "411001"

        val streetLine = if (thoroughfare.isNotBlank()) {
            if (addr.subLocality != null && addr.subLocality != thoroughfare) {
                "$thoroughfare, ${addr.subLocality}"
            } else {
                thoroughfare
            }
        } else {
            full.split(",").take(2).joinToString(",").trim()
        }

        return AddressDetails(
            formattedAddress = full.ifBlank { "$streetLine, $locality $postal" },
            addressLine = streetLine.ifBlank { full },
            city = locality,
            state = state,
            pincode = postal,
            latitude = lat,
            longitude = lng
        )
    }
}
