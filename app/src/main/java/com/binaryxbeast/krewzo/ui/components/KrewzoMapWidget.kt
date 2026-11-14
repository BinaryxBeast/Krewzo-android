package com.binaryxbeast.krewzo.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.binaryxbeast.krewzo.core.util.GeocoderHelper
import com.binaryxbeast.krewzo.core.util.PunePresetLocation
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Official Google Maps Jetpack Compose client address picker widget.
 *
 * Features:
 * - Pure Google Maps SDK integration via maps-compose
 * - Center crosshair pin placement (no draggable Marker — floating overlay only)
 * - Real-time reverse geocoding via Android Geocoder to address, city, & pincode
 * - One-tap GPS "Locate Me" button with runtime permissions
 * - Location search bar with forward geocoding (small map & fullscreen)
 * - Instant Pune commercial & industrial hub preset chips
 * - Full-screen precision map dialog with search bar
 * - Dark mode theme styling matching Krewzo UI
 */
@Composable
fun KrewzoMapWidget(
    modifier: Modifier = Modifier,
    latitude: Double = 18.5204,
    longitude: Double = 73.8567,
    addressLabel: String = "",
    onLocationSelected: (address: String, city: String, pincode: String, lat: Double, lng: Double) -> Unit,
    onCoordinatesChanged: (lat: Double, lng: Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()

    var currentLat by remember(latitude) { mutableDoubleStateOf(if (latitude != 0.0) latitude else 18.5204) }
    var currentLng by remember(longitude) { mutableDoubleStateOf(if (longitude != 0.0) longitude else 73.8567) }
    var resolvedAddress by rememberSaveable { mutableStateOf(addressLabel) }
    var isGeocoding by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isFullScreen by remember { mutableStateOf(false) }

    // Google Maps Camera Position State (small map)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(currentLat, currentLng), 15f)
    }

    // Sync camera when coordinates change externally (e.g. preset chip tap)
    LaunchedEffect(currentLat, currentLng) {
        val target = LatLng(currentLat, currentLng)
        if (!cameraPositionState.isMoving) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                target,
                cameraPositionState.position.zoom.coerceAtLeast(14f)
            )
        }
    }

    // Trigger reverse-geocoding when user stops dragging the small map
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            if (Math.abs(center.latitude - currentLat) > 0.0001 ||
                Math.abs(center.longitude - currentLng) > 0.0001
            ) {
                currentLat = center.latitude
                currentLng = center.longitude
                onCoordinatesChanged(center.latitude, center.longitude)

                isGeocoding = true
                val details = GeocoderHelper.reverseGeocode(context, center.latitude, center.longitude)
                isGeocoding = false
                if (details != null) {
                    resolvedAddress = details.formattedAddress
                    onLocationSelected(
                        details.addressLine,
                        details.city,
                        details.pincode,
                        center.latitude,
                        center.longitude
                    )
                }
            }
        }
    }

    // Animate camera to target coordinates and reverse-geocode
    fun animateCameraTo(lat: Double, lng: Double, zoom: Float = 16f) {
        currentLat = lat
        currentLng = lng
        val target = LatLng(lat, lng)
        onCoordinatesChanged(lat, lng)

        scope.launch {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, zoom))
            isGeocoding = true
            val details = GeocoderHelper.reverseGeocode(context, lat, lng)
            isGeocoding = false
            if (details != null) {
                resolvedAddress = details.formattedAddress
                onLocationSelected(
                    details.addressLine,
                    details.city,
                    details.pincode,
                    lat,
                    lng
                )
            }
        }
    }

    // Device GPS fetcher
    fun fetchDeviceLocation() {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val hasFine = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasFine || hasCoarse) {
                    val gpsLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    val netLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    val bestLoc: Location? = when {
                        gpsLoc != null && netLoc != null -> if (gpsLoc.time > netLoc.time) gpsLoc else netLoc
                        gpsLoc != null -> gpsLoc
                        else -> netLoc
                    }

                    if (bestLoc != null) {
                        animateCameraTo(bestLoc.latitude, bestLoc.longitude, 17f)
                    } else {
                        @SuppressLint("MissingPermission")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            locationManager.getCurrentLocation(
                                LocationManager.NETWORK_PROVIDER,
                                null,
                                ContextCompat.getMainExecutor(context)
                            ) { loc -> if (loc != null) animateCameraTo(loc.latitude, loc.longitude, 17f) }
                        } else {
                            @Suppress("DEPRECATION")
                            locationManager.requestSingleUpdate(
                                LocationManager.NETWORK_PROVIDER,
                                { loc -> animateCameraTo(loc.latitude, loc.longitude, 17f) },
                                null
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // Permission launcher for GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchDeviceLocation()
    }

    // Google Maps UI & Property Settings
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = true,
            rotationGesturesEnabled = true,
            scrollGesturesEnabled = true,
            tiltGesturesEnabled = false,
            zoomGesturesEnabled = true
        )
    }

    val mapProperties = remember(isDark) {
        MapProperties(
            isMyLocationEnabled = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED,
            mapStyleOptions = if (isDark) MapStyleOptions(GOOGLE_MAPS_DARK_STYLE) else null
        )
    }

    // ─────────────────────────── SMALL MAP UI ───────────────────────────────

    Column(modifier = modifier.fillMaxWidth()) {
        // Search & Geocode Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "Search landmark, area or MIDC...",
                        fontSize = 13.sp,
                        color = KrewzoTheme.colors.textTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = KrewzoTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = KrewzoTheme.colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        if (searchQuery.isNotBlank()) {
                            scope.launch {
                                isGeocoding = true
                                val result = GeocoderHelper.forwardGeocode(context, searchQuery)
                                isGeocoding = false
                                if (result != null) {
                                    animateCameraTo(result.latitude, result.longitude, 16f)
                                    searchQuery = ""
                                }
                            }
                        }
                    }
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = KrewzoTheme.colors.surfaceSecondary,
                    unfocusedContainerColor = KrewzoTheme.colors.surfaceSecondary,
                    focusedTextColor = KrewzoTheme.colors.textPrimary,
                    unfocusedTextColor = KrewzoTheme.colors.textPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(KrewzoDimens.RadiusMd),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Locate Me Button
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        val fineGranted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted) fetchDeviceLocation()
                        else locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Current Location",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Quick Preset Chips for Pune Hubs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GeocoderHelper.PUNE_HUBS.forEach { hub: PunePresetLocation ->
                val isSelected = Math.abs(currentLat - hub.latitude) < 0.002 &&
                        Math.abs(currentLng - hub.longitude) < 0.002
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else KrewzoTheme.colors.surfaceSecondary
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else KrewzoTheme.colors.border,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            animateCameraTo(hub.latitude, hub.longitude, 16f)
                            onLocationSelected(hub.addressLine, hub.city, hub.pincode, hub.latitude, hub.longitude)
                            resolvedAddress = hub.addressLine
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else KrewzoTheme.colors.textTertiary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = hub.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else KrewzoTheme.colors.textSecondary
                        )
                    }
                }
            }
        }

        // Google Map Frame (small)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(KrewzoDimens.RadiusLg))
                .border(1.dp, KrewzoTheme.colors.border, RoundedCornerShape(KrewzoDimens.RadiusLg))
        ) {
            // Map — no Marker composable; we use a floating center pin overlay instead
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = mapProperties,
                uiSettings = uiSettings,
                onMapClick = { latLng ->
                    animateCameraTo(latLng.latitude, latLng.longitude, cameraPositionState.position.zoom)
                }
            )

            // ── Floating Center Pin (crosshair-style, no Marker composable) ──
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing circle halo
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6).copy(alpha = 0.30f))
                )
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Pin",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier
                        .size(38.dp)
                        .shadow(elevation = 6.dp, shape = CircleShape)
                )
            }

            // Top-Right: Fullscreen Expand
            IconButton(
                onClick = { isFullScreen = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .background(Color.Black.copy(alpha = 0.65f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Full Screen Map",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Bottom-Left: Coordinates Badge
            Surface(
                color = Color.Black.copy(alpha = 0.72f),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, "%.5f, %.5f", currentLat, currentLng),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            // Top-Left: Geocoding Spinner
            if (isGeocoding) {
                Surface(
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExpressLoadingIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            size = 12.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Locating address...", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // Resolved address preview (below small map)
        if (resolvedAddress.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 2.dp, end = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = resolvedAddress,
                    fontSize = 12.sp,
                    color = KrewzoTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
            }
        }

        // Tap instructions hint
        Text(
            text = "Drag map or tap a point to set precise client coordinates.",
            style = MaterialTheme.typography.bodySmall,
            color = KrewzoTheme.colors.textTertiary,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 4.dp, start = 2.dp)
        )
    }

    // ─────────────────────── FULL SCREEN MAP DIALOG ─────────────────────────

    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val fullScreenCameraState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(currentLat, currentLng), 17f)
            }
            var fsSearchQuery by remember { mutableStateOf("") }
            var fsFocusManager = LocalFocusManager.current
            var isFsGeocoding by remember { mutableStateOf(false) }

            // Reverse-geocode when fullscreen camera stops moving
            LaunchedEffect(fullScreenCameraState.isMoving) {
                if (!fullScreenCameraState.isMoving) {
                    val center = fullScreenCameraState.position.target
                    currentLat = center.latitude
                    currentLng = center.longitude
                    onCoordinatesChanged(center.latitude, center.longitude)

                    isFsGeocoding = true
                    val details = GeocoderHelper.reverseGeocode(context, center.latitude, center.longitude)
                    isFsGeocoding = false
                    if (details != null) {
                        resolvedAddress = details.formattedAddress
                        onLocationSelected(
                            details.addressLine,
                            details.city,
                            details.pincode,
                            center.latitude,
                            center.longitude
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    // Full-screen Map — no Marker composable
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = fullScreenCameraState,
                        properties = mapProperties,
                        uiSettings = uiSettings,
                        onMapClick = { latLng ->
                            scope.launch {
                                fullScreenCameraState.animate(CameraUpdateFactory.newLatLng(latLng))
                            }
                        }
                    )

                    // ── Floating Center Crosshair Pin ──
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(bottom = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6).copy(alpha = 0.35f))
                        )
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Pinpoint",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(elevation = 10.dp, shape = CircleShape)
                        )
                    }

                    // ── Top Bar: Exit + Search + GPS ──
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: Back button + GPS button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Exit Fullscreen
                                Surface(
                                    color = Color.Black.copy(alpha = 0.78f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    IconButton(onClick = { isFullScreen = false }) {
                                        Icon(
                                            imageVector = Icons.Default.FullscreenExit,
                                            contentDescription = "Exit Fullscreen",
                                            tint = Color.White
                                        )
                                    }
                                }

                                // Geocoding indicator (center)
                                if (isFsGeocoding) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.72f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ExpressLoadingIndicator(
                                                color = MaterialTheme.colorScheme.primary,
                                                size = 12.dp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Locating...",
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                } else {
                                    // Coordinates pill
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.72f),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = String.format(Locale.US, "%.5f, %.5f", currentLat, currentLng),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // GPS / My Location
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    IconButton(onClick = { fetchDeviceLocation() }) {
                                        Icon(
                                            imageVector = Icons.Default.MyLocation,
                                            contentDescription = "Locate Me",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Row 2: Search bar
                            TextField(
                                value = fsSearchQuery,
                                onValueChange = { fsSearchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Search location, landmark or area...",
                                        fontSize = 13.sp,
                                        color = KrewzoTheme.colors.textTertiary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = KrewzoTheme.colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (fsSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { fsSearchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear",
                                                tint = KrewzoTheme.colors.textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        fsFocusManager.clearFocus()
                                        if (fsSearchQuery.isNotBlank()) {
                                            scope.launch {
                                                isFsGeocoding = true
                                                val result = GeocoderHelper.forwardGeocode(context, fsSearchQuery)
                                                isFsGeocoding = false
                                                if (result != null) {
                                                    fullScreenCameraState.animate(
                                                        CameraUpdateFactory.newLatLngZoom(
                                                            LatLng(result.latitude, result.longitude), 17f
                                                        )
                                                    )
                                                    currentLat = result.latitude
                                                    currentLng = result.longitude
                                                    fsSearchQuery = ""
                                                }
                                            }
                                        }
                                    }
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                    focusedTextColor = KrewzoTheme.colors.textPrimary,
                                    unfocusedTextColor = KrewzoTheme.colors.textPrimary,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(KrewzoDimens.RadiusMd),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(KrewzoDimens.RadiusMd))
                            )
                        }
                    }

                    // ── Bottom Address Confirmation Card ──
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(KrewzoDimens.RadiusLg),
                        shadowElevation = 12.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECTED LOCATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    color = KrewzoTheme.colors.textTertiary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = String.format(Locale.US, "%.5f, %.5f", currentLat, currentLng),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(top = 1.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = resolvedAddress.ifBlank { "Move map to pinpoint location…" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (resolvedAddress.isBlank()) KrewzoTheme.colors.textTertiary
                                    else KrewzoTheme.colors.textPrimary,
                                    maxLines = 2
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            KrewzoButton(
                                text = "Confirm Location",
                                onClick = { isFullScreen = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard JSON Map styling for Google Maps dark theme.
 */
private const val GOOGLE_MAPS_DARK_STYLE = """
[
  { "elementType": "geometry", "stylers": [{ "color": "#1e293b" }] },
  { "elementType": "labels.text.stroke", "stylers": [{ "color": "#1e293b" }] },
  { "elementType": "labels.text.fill", "stylers": [{ "color": "#94a3b8" }] },
  { "featureType": "administrative.locality", "elementType": "labels.text.fill", "stylers": [{ "color": "#f8fafc" }] },
  { "featureType": "poi", "elementType": "labels.text.fill", "stylers": [{ "color": "#64748b" }] },
  { "featureType": "poi.park", "elementType": "geometry", "stylers": [{ "color": "#0f172a" }] },
  { "featureType": "road", "elementType": "geometry", "stylers": [{ "color": "#334155" }] },
  { "featureType": "road", "elementType": "geometry.stroke", "stylers": [{ "color": "#1e293b" }] },
  { "featureType": "road", "elementType": "labels.text.fill", "stylers": [{ "color": "#cbd5e1" }] },
  { "featureType": "road.highway", "elementType": "geometry", "stylers": [{ "color": "#475569" }] },
  { "featureType": "road.highway", "elementType": "labels.text.fill", "stylers": [{ "color": "#f1f5f9" }] },
  { "featureType": "transit", "elementType": "geometry", "stylers": [{ "color": "#1e293b" }] },
  { "featureType": "water", "elementType": "geometry", "stylers": [{ "color": "#0f172a" }] },
  { "featureType": "water", "elementType": "labels.text.fill", "stylers": [{ "color": "#38bdf8" }] }
]
"""
