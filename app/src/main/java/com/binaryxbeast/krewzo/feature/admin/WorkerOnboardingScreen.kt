package com.binaryxbeast.krewzo.feature.admin

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.SkillCategory
import com.binaryxbeast.krewzo.core.model.Worker
import com.binaryxbeast.krewzo.core.util.GeocoderHelper
import com.binaryxbeast.krewzo.core.util.ImageCompressor
import com.binaryxbeast.krewzo.core.util.PunePresetLocation
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.components.bouncyClickable
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
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private const val GOOGLE_MAPS_DARK_STYLE = """
[
  {"elementType": "geometry", "stylers": [{"color": "#1e222a"}]},
  {"elementType": "labels.text.stroke", "stylers": [{"color": "#1e222a"}]},
  {"elementType": "labels.text.fill", "stylers": [{"color": "#8a9ba8"}]},
  {"featureType": "road", "elementType": "geometry", "stylers": [{"color": "#2c313c"}]},
  {"featureType": "road", "elementType": "geometry.stroke", "stylers": [{"color": "#21252b"}]},
  {"featureType": "water", "elementType": "geometry", "stylers": [{"color": "#12151b"}]}
]
"""

/**
 * Step-by-step single-screen Worker Onboarding Wizard for Admin Dashboard.
 *
 * Sequence of Steps:
 * 1. Full Name
 * 2. Mobile Number (validates duplicate check against Supabase database)
 * 3. In-app Camera Worker Photograph
 * 4. In-app Camera Aadhaar Card (Both Front & Back)
 * 5. Date of Birth (Age auto-calculation and >= 18 verification)
 * 6. Work & Home Address with full-screen Google Maps API integration
 * 7. Skill Categories (Default set to Loading & Unloading, multi-selectable)
 * 8. Success State with complete Supabase database update
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkerOnboardingScreen(
    viewModel: AdminViewModel,
    onSaveComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val isDark = isSystemInDarkTheme()

    // Wizard Navigation State
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    val totalSteps = 7

    // Form fields
    var fullName by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var phoneCheckError by rememberSaveable { mutableStateOf<String?>(null) }
    var isCheckingPhone by remember { mutableStateOf(false) }

    // Worker Photograph
    var workerPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var workerPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Aadhaar Card
    var aadhaarNumber by rememberSaveable { mutableStateOf("") }
    var aadhaarFrontPath by rememberSaveable { mutableStateOf<String?>(null) }
    var aadhaarFrontBytes by remember { mutableStateOf<ByteArray?>(null) }
    var aadhaarBackPath by rememberSaveable { mutableStateOf<String?>(null) }
    var aadhaarBackBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Date of Birth & Age
    var dateOfBirth by rememberSaveable { mutableStateOf("") }
    var calculatedAge by rememberSaveable { mutableStateOf<Int?>(null) }
    var dobError by rememberSaveable { mutableStateOf<String?>(null) }

    // Work Address & Google Maps Coordinates (Full-Screen Step 6)
    var currentLat by rememberSaveable { mutableDoubleStateOf(18.5204) }
    var currentLng by rememberSaveable { mutableDoubleStateOf(73.8567) }
    var resolvedAddress by rememberSaveable { mutableStateOf("Pune, Maharashtra") }
    var buildingDetail by rememberSaveable { mutableStateOf("") }
    var isGeocoding by remember { mutableStateOf(false) }
    var mapSearchQuery by remember { mutableStateOf("") }

    // Skill Categories (Default: LOADING_UNLOADING)
    val selectedSkills = remember { mutableStateListOf(SkillCategory.LOADING_UNLOADING) }

    // Submission & Storage Sync
    val onboardState by viewModel.onboardWorkerState.collectAsState()
    var isSubmitted by rememberSaveable { mutableStateOf(false) }

    // Temporary storage for active camera capture type: "PHOTO", "AADHAAR_FRONT", "AADHAAR_BACK"
    var activeCameraTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var tempCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraProcessingError by remember { mutableStateOf<String?>(null) }

    // Direct Camera Launcher using FileProvider
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraPath != null) {
            scope.launch {
                try {
                    val file = File(tempCameraPath!!)
                    if (file.exists() && file.length() > 0) {
                        val compressedBytes = ImageCompressor.compressFile(file)
                        when (activeCameraTarget) {
                            "PHOTO" -> {
                                workerPhotoPath = tempCameraPath
                                workerPhotoBytes = compressedBytes
                            }
                            "AADHAAR_FRONT" -> {
                                aadhaarFrontPath = tempCameraPath
                                aadhaarFrontBytes = compressedBytes
                            }
                            "AADHAAR_BACK" -> {
                                aadhaarBackPath = tempCameraPath
                                aadhaarBackBytes = compressedBytes
                            }
                        }
                    }
                } catch (e: Exception) {
                    cameraProcessingError = "Photo processing failed: ${e.localizedMessage}"
                }
            }
        }
    }

    // Runtime camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission just granted — retry the camera for the pending target
            activeCameraTarget?.let { target ->
                try {
                    cameraProcessingError = null
                    val photoDir = File(context.cacheDir, "camera_photos")
                    if (!photoDir.exists()) photoDir.mkdirs()
                    val photoFile = File(photoDir, "worker_${target.lowercase()}_${System.currentTimeMillis()}.jpg")
                    if (!photoFile.exists()) photoFile.createNewFile()
                    tempCameraPath = photoFile.absolutePath
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        photoFile
                    )
                    takePictureLauncher.launch(uri)
                } catch (e: Exception) {
                    cameraProcessingError = "Unable to start camera: ${e.localizedMessage}"
                }
            }
        } else {
            cameraProcessingError = "Camera permission is required to capture photos. Please grant it in Settings."
        }
    }

    // Camera launcher helper — requests permission first if not yet granted
    fun launchCameraFor(target: String) {
        cameraProcessingError = null
        activeCameraTarget = target
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }

        try {
            val photoDir = File(context.cacheDir, "camera_photos")
            if (!photoDir.exists()) photoDir.mkdirs()
            val photoFile = File(photoDir, "worker_${target.lowercase()}_${System.currentTimeMillis()}.jpg")
            if (!photoFile.exists()) photoFile.createNewFile()
            tempCameraPath = photoFile.absolutePath
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            cameraProcessingError = "Unable to start camera: ${e.localizedMessage}"
        }
    }

    // Google Maps Camera State for Fullscreen Step 6
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(currentLat, currentLng), 15f)
    }

    // Reverse-geocode when map camera stops moving in Step 6
    LaunchedEffect(cameraPositionState.isMoving, currentStep) {
        if (currentStep == 6 && !cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            if (Math.abs(center.latitude - currentLat) > 0.0001 ||
                Math.abs(center.longitude - currentLng) > 0.0001
            ) {
                currentLat = center.latitude
                currentLng = center.longitude
                isGeocoding = true
                val details = GeocoderHelper.reverseGeocode(context, center.latitude, center.longitude)
                isGeocoding = false
                if (details != null) {
                    resolvedAddress = details.formattedAddress
                }
            }
        }
    }

    // Device GPS fetcher for Step 6
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
                        currentLat = bestLoc.latitude
                        currentLng = bestLoc.longitude
                        scope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(bestLoc.latitude, bestLoc.longitude), 16.5f
                                )
                            )
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        locationManager.getCurrentLocation(
                            LocationManager.NETWORK_PROVIDER,
                            null,
                            ContextCompat.getMainExecutor(context)
                        ) { loc ->
                            if (loc != null) {
                                currentLat = loc.latitude
                                currentLng = loc.longitude
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(loc.latitude, loc.longitude), 16.5f
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchDeviceLocation()
    }

    // Date Picker Dialog
    fun openDatePickerDialog() {
        val cal = Calendar.getInstance()
        val parts = dateOfBirth.trim().split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                cal.set(y, m - 1, d)
            }
        } else {
            cal.add(Calendar.YEAR, -24)
        }

        val dpd = DatePickerDialog(
            context,
            { _, year, monthOfYear, dayOfMonth ->
                val formatted = String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    year,
                    monthOfYear + 1,
                    dayOfMonth
                )
                dateOfBirth = formatted
                val computed = Worker.calculateAgeFromDob(formatted)
                calculatedAge = computed
                if (computed != null && computed < 18) {
                    dobError = "Worker must be at least 18 years old to be legally registered"
                } else {
                    dobError = null
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dpd.datePicker.maxDate = System.currentTimeMillis()
        dpd.show()
    }

    // Submit and save to Supabase
    fun submitWorkerOnboarding() {
        isSubmitted = true
        val fullAddressText = if (buildingDetail.isNotBlank()) {
            "$buildingDetail, $resolvedAddress"
        } else {
            resolvedAddress
        }

        val cleanPhone = if (phone.startsWith("+91")) phone else "+91$phone"

        val newWorker = Worker(
            id = UUID.randomUUID().toString(),
            phone = cleanPhone,
            fullName = fullName.trim(),
            dateOfBirth = dateOfBirth.ifBlank { null },
            age = calculatedAge,
            homeAddress = fullAddressText,
            skills = selectedSkills.toList(),
            isAvailable = true,
            isActive = true
        )

        viewModel.onboardWorker(
            worker = newWorker,
            photoBytes = workerPhotoBytes,
            aadhaarFrontBytes = aadhaarFrontBytes,
            aadhaarBackBytes = aadhaarBackBytes,
            aadhaarNumber = aadhaarNumber.ifBlank { null },
            onSuccess = {
                // Stay on success screen to show summary
            }
        )
    }

    Scaffold(
        topBar = {
            if (!isSubmitted) {
                Column {
                    KrewzoTopBar(
                        title = "Add New Worker",
                        onBackClick = {
                            if (currentStep > 1) {
                                currentStep--
                            } else {
                                onBack()
                            }
                        },
                        actions = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancel",
                                    tint = KrewzoTheme.colors.textSecondary
                                )
                            }
                        }
                    )
                    // Visual Step Progress Bar
                    LinearProgressIndicator(
                        progress = { currentStep / totalSteps.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = KrewzoTheme.colors.border
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(color =     MaterialTheme.colorScheme.background)
        ) {
            if (isSubmitted) {
                // ── SUCCESS / SUBMISSION STATE SCREEN ──
                when (onboardState) {
                    is Resource.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            KrewzoCard(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    com.binaryxbeast.krewzo.ui.components.ExpressLoadingIndicator(
                                        size = 48.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(18.dp))
                                    Text(
                                        text = "Registering Worker...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = KrewzoTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Uploading documents & updating Supabase database securely",
                                        fontSize = 13.sp,
                                        color = KrewzoTheme.colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        val errorMsg = (onboardState as Resource.Error).message
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Registration Failed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = KrewzoTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMsg,
                                fontSize = 14.sp,
                                color = KrewzoTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            KrewzoButton(
                                text = "Retry",
                                onClick = { submitWorkerOnboarding() }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    isSubmitted = false
                                    viewModel.resetOnboardWorkerState()
                                },
                                shape = RoundedCornerShape(KrewzoDimens.RadiusMd)
                            ) {
                                Text("Edit Form")
                            }
                        }
                    }
                    is Resource.Success -> {
                        val savedWorker = (onboardState as Resource.Success<Worker>).data
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            // Animated Success Icon
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(54.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Worker Successfully Added!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = KrewzoTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Profile, Aadhaar documents, and coordinates saved to Supabase",
                                fontSize = 13.sp,
                                color = KrewzoTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(24.dp))

                            // Summary Card
                            KrewzoCard(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (workerPhotoBytes != null) {
                                            AsyncImage(
                                                model = workerPhotoBytes,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = savedWorker.fullName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = KrewzoTheme.colors.textPrimary
                                            )
                                            Text(
                                                text = savedWorker.phone,
                                                fontSize = 13.sp,
                                                color = KrewzoTheme.colors.textSecondary
                                            )
                                            if (savedWorker.age != null) {
                                                Text(
                                                    text = "Age: ${savedWorker.age} years",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "📍 ${savedWorker.homeAddress ?: "Pune"}",
                                        fontSize = 13.sp,
                                        color = KrewzoTheme.colors.textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        savedWorker.skills.forEach { skill ->
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text(
                                                    text = skill.displayName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            KrewzoButton(
                                text = "View in Workers List",
                                onClick = {
                                    viewModel.resetOnboardWorkerState()
                                    onSaveComplete()
                                }
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    // Reset wizard to add another worker
                                    fullName = ""
                                    phone = ""
                                    workerPhotoBytes = null
                                    workerPhotoPath = null
                                    aadhaarFrontBytes = null
                                    aadhaarBackBytes = null
                                    aadhaarNumber = ""
                                    dateOfBirth = ""
                                    calculatedAge = null
                                    buildingDetail = ""
                                    selectedSkills.clear()
                                    selectedSkills.add(SkillCategory.LOADING_UNLOADING)
                                    currentStep = 1
                                    isSubmitted = false
                                    viewModel.resetOnboardWorkerState()
                                },
                                shape = RoundedCornerShape(KrewzoDimens.RadiusMd),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add Another Worker", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> {}
                }
            } else {
                // ── STEP-BY-STEP ONBOARDING WIZARD ──
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut()
                            )
                        }
                    },
                    label = "WorkerOnboardingSteps"
                ) { step ->
                    when (step) {
                        1 -> StepFullName(
                            fullName = fullName,
                            onFullNameChange = { fullName = it },
                            onNext = { currentStep = 2 }
                        )
                        2 -> StepMobileNumber(
                            phone = phone,
                            onPhoneChange = {
                                phone = it
                                phoneCheckError = null
                            },
                            errorMessage = phoneCheckError,
                            isChecking = isCheckingPhone,
                            onCheckAndNext = {
                                val clean = phone.filter { it.isDigit() }.takeLast(10)
                                if (clean.length != 10) {
                                    phoneCheckError = "Please enter a valid 10-digit mobile number"
                                    return@StepMobileNumber
                                }
                                isCheckingPhone = true
                                phoneCheckError = null
                                viewModel.checkPhoneRegistered(clean) { isRegistered, error ->
                                    isCheckingPhone = false
                                    if (error != null) {
                                        phoneCheckError = "Failed to verify number: $error"
                                    } else if (isRegistered) {
                                        phoneCheckError = "Mobile number +91 $clean is already registered. Please enter a different number."
                                    } else {
                                        phone = clean
                                        phoneCheckError = null
                                        currentStep = 3
                                    }
                                }
                            }
                        )
                        3 -> StepWorkerPhoto(
                            photoBytes = workerPhotoBytes,
                            processingError = cameraProcessingError,
                            onLaunchCamera = { launchCameraFor("PHOTO") },
                            onNext = { currentStep = 4 }
                        )
                        4 -> StepAadhaarCard(
                            aadhaarNumber = aadhaarNumber,
                            onAadhaarNumberChange = { aadhaarNumber = it },
                            frontBytes = aadhaarFrontBytes,
                            backBytes = aadhaarBackBytes,
                            processingError = cameraProcessingError,
                            onCaptureFront = { launchCameraFor("AADHAAR_FRONT") },
                            onCaptureBack = { launchCameraFor("AADHAAR_BACK") },
                            onNext = { currentStep = 5 }
                        )
                        5 -> StepDateOfBirth(
                            dateOfBirth = dateOfBirth,
                            calculatedAge = calculatedAge,
                            errorMessage = dobError,
                            onOpenDatePicker = { openDatePickerDialog() },
                            onNext = { currentStep = 6 }
                        )
                        6 -> StepWorkAddressFullScreen(
                            cameraPositionState = cameraPositionState,
                            resolvedAddress = resolvedAddress,
                            buildingDetail = buildingDetail,
                            onBuildingDetailChange = { buildingDetail = it },
                            latitude = currentLat,
                            longitude = currentLng,
                            isGeocoding = isGeocoding,
                            searchQuery = mapSearchQuery,
                            onSearchQueryChange = { mapSearchQuery = it },
                            onSearch = {
                                focusManager.clearFocus()
                                if (mapSearchQuery.isNotBlank()) {
                                    scope.launch {
                                        isGeocoding = true
                                        val result = GeocoderHelper.forwardGeocode(context, mapSearchQuery)
                                        isGeocoding = false
                                        if (result != null) {
                                            currentLat = result.latitude
                                            currentLng = result.longitude
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(
                                                    LatLng(result.latitude, result.longitude), 16f
                                                )
                                            )
                                            mapSearchQuery = ""
                                        }
                                    }
                                }
                            },
                            onPresetSelected = { hub ->
                                currentLat = hub.latitude
                                currentLng = hub.longitude
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(hub.latitude, hub.longitude), 16f
                                        )
                                    )
                                    isGeocoding = true
                                    val details = GeocoderHelper.reverseGeocode(context, hub.latitude, hub.longitude)
                                    isGeocoding = false
                                    if (details != null) {
                                        resolvedAddress = details.formattedAddress
                                    }
                                }
                            },
                            onLocateMe = {
                                val fineGranted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                if (fineGranted) {
                                    fetchDeviceLocation()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            isDark = isDark,
                            onNext = { currentStep = 7 }
                        )
                        7 -> StepSkillCategories(
                            selectedSkills = selectedSkills,
                            onToggleSkill = { skill ->
                                if (selectedSkills.contains(skill)) {
                                    if (selectedSkills.size > 1) {
                                        selectedSkills.remove(skill)
                                    }
                                } else {
                                    selectedSkills.add(skill)
                                }
                            },
                            fullName = fullName,
                            phone = phone,
                            onComplete = { submitWorkerOnboarding() }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 1: FULL NAME
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepFullName(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    onNext: () -> Unit
) {
    val canProceed = fullName.trim().length >= 2

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "STEP 1 OF 7 • PERSONAL DETAILS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "What is the worker's full name?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary,
                lineHeight = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter the legal first and last name as shown on their official ID proof.",
                fontSize = 14.sp,
                color = KrewzoTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            KrewzoTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = "Full Name",
                placeholder = "e.g. Ramesh Shankar Patil",
                leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { if (canProceed) onNext() }
                )
            )
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            KrewzoButton(
                text = "Continue to Mobile Number",
                onClick = onNext,
                enabled = canProceed
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 2: MOBILE NUMBER (WITH REAL-TIME DUPLICATE CHECK)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepMobileNumber(
    phone: String,
    onPhoneChange: (String) -> Unit,
    errorMessage: String?,
    isChecking: Boolean,
    onCheckAndNext: () -> Unit
) {
    val cleanDigits = phone.filter { it.isDigit() }
    val canCheck = cleanDigits.length == 10 && !isChecking

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "STEP 2 OF 7 • CONTACT & AUTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Enter Mobile Number",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Used for sending booking offers via SMS/Push and worker OTP login.",
                fontSize = 14.sp,
                color = KrewzoTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            KrewzoTextField(
                value = cleanDigits.take(10),
                onValueChange = { input ->
                    val digits = input.filter { it.isDigit() }.take(10)
                    onPhoneChange(digits)
                },
                label = "Mobile Number (10 Digits)",
                placeholder = "98765 43210",
                leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                singleLine = true,
                errorMessage = errorMessage,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (canCheck) onCheckAndNext() }
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            KrewzoButton(
                text = if (isChecking) "Checking Database..." else "Verify & Continue",
                onClick = onCheckAndNext,
                enabled = canCheck,
                isLoading = isChecking
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 3: WORKER PHOTOGRAPH (IN-APP CAMERA)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepWorkerPhoto(
    photoBytes: ByteArray?,
    processingError: String?,
    onLaunchCamera: () -> Unit,
    onNext: () -> Unit
) {
    val hasPhoto = photoBytes != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.Start)
            ) {
                Text(
                    text = "STEP 3 OF 7 • IDENTITY PHOTO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Capture Worker Photograph",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open camera to capture a clear front portrait photo of the worker.",
                fontSize = 14.sp,
                color = KrewzoTheme.colors.textSecondary,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Camera Viewfinder Box
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(KrewzoTheme.colors.surfaceSecondary)
                    .border(
                        width = 3.dp,
                        color = if (hasPhoto) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clickable { onLaunchCamera() },
                contentAlignment = Alignment.Center
            ) {
                if (hasPhoto) {
                    AsyncImage(
                        model = photoBytes,
                        contentDescription = "Worker Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Open Camera",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap to Open Camera",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (hasPhoto) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Photo Captured Successfully",
                        color = Color(0xFF10B981),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onLaunchCamera,
                    shape = RoundedCornerShape(KrewzoDimens.RadiusMd)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Retake Photo")
                }
            } else {
                KrewzoButton(
                    text = "Open Camera to Capture",
                    onClick = onLaunchCamera,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }

            if (processingError != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = processingError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            KrewzoButton(
                text = "Continue to Aadhaar Card",
                onClick = onNext,
                enabled = hasPhoto
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 4: AADHAAR CARD (BOTH SIDES IN-APP CAMERA)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepAadhaarCard(
    aadhaarNumber: String,
    onAadhaarNumberChange: (String) -> Unit,
    frontBytes: ByteArray?,
    backBytes: ByteArray?,
    processingError: String?,
    onCaptureFront: () -> Unit,
    onCaptureBack: () -> Unit,
    onNext: () -> Unit
) {
    val hasBothSides = frontBytes != null && backBytes != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "STEP 4 OF 7 • AADHAAR VERIFICATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Capture Aadhaar Card",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Take photos of both the Front and Back sides of the worker's Aadhaar card.",
                fontSize = 14.sp,
                color = KrewzoTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            KrewzoTextField(
                value = aadhaarNumber.filter { it.isDigit() }.take(12),
                onValueChange = { onAadhaarNumberChange(it.filter { ch -> ch.isDigit() }.take(12)) },
                label = "Aadhaar Card Number (12 Digits)",
                placeholder = "XXXX XXXX XXXX",
                leadingIcon = { Icon(imageVector = Icons.Default.Badge, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Two Document Cards: Front & Back
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Front Side Card
                AadhaarSideCard(
                    title = "Front Side",
                    imageBytes = frontBytes,
                    onCapture = onCaptureFront,
                    modifier = Modifier.weight(1f)
                )

                // Back Side Card
                AadhaarSideCard(
                    title = "Back Side",
                    imageBytes = backBytes,
                    onCapture = onCaptureBack,
                    modifier = Modifier.weight(1f)
                )
            }

            if (processingError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = processingError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            KrewzoButton(
                text = if (hasBothSides) "Continue to Date of Birth" else "Capture Both Sides to Continue",
                onClick = onNext,
                enabled = hasBothSides
            )
        }
    }
}

@Composable
private fun AadhaarSideCard(
    title: String,
    imageBytes: ByteArray?,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCaptured = imageBytes != null

    KrewzoCard(
        modifier = modifier
            .height(170.dp)
            .clickable { onCapture() }
    ) {
        if (isCaptured) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = imageBytes,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = title, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = KrewzoTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to capture",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 5: DATE OF BIRTH & AGE VALIDATION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepDateOfBirth(
    dateOfBirth: String,
    calculatedAge: Int?,
    errorMessage: String?,
    onOpenDatePicker: () -> Unit,
    onNext: () -> Unit
) {
    val isEligible = calculatedAge != null && calculatedAge >= 18

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "STEP 5 OF 7 • DATE OF BIRTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Worker's Date of Birth",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Workers must be 18 years or older to be legally registered.",
                fontSize = 14.sp,
                color = KrewzoTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Clickable DOB Card
            KrewzoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDatePicker() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Date of Birth",
                            fontSize = 12.sp,
                            color = KrewzoTheme.colors.textTertiary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (dateOfBirth.isNotBlank()) dateOfBirth else "Tap to Select Date",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dateOfBirth.isNotBlank()) KrewzoTheme.colors.textPrimary else KrewzoTheme.colors.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = KrewzoTheme.colors.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (calculatedAge != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                        .background(
                            if (isEligible) Color(0xFF10B981).copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isEligible) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (isEligible) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Calculated Age: $calculatedAge years old",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isEligible) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = if (isEligible) "Eligible for employment (18+)" else "Under legal working age (18+ required)",
                            fontSize = 12.sp,
                            color = KrewzoTheme.colors.textSecondary
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }

        Column(modifier = Modifier.padding(top = 24.dp)) {
            KrewzoButton(
                text = "Continue to Work Address",
                onClick = onNext,
                enabled = isEligible
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 6: WORK ADDRESS WITH FULL-SCREEN GOOGLE MAPS API
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepWorkAddressFullScreen(
    cameraPositionState: com.google.maps.android.compose.CameraPositionState,
    resolvedAddress: String,
    buildingDetail: String,
    onBuildingDetailChange: (String) -> Unit,
    latitude: Double,
    longitude: Double,
    isGeocoding: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPresetSelected: (PunePresetLocation) -> Unit,
    onLocateMe: () -> Unit,
    isDark: Boolean,
    onNext: () -> Unit
) {
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
            mapStyleOptions = if (isDark) MapStyleOptions(GOOGLE_MAPS_DARK_STYLE) else null
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // FULL SCREEN GOOGLE MAP
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        )

        // Center Floating Pin Overlay
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
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
                    .size(42.dp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
            )
        }

        // TOP CONTROLS OVERLAY
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Search Bar & GPS Locate Me
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Search Pune area, street or MIDC...",
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
                            IconButton(onClick = { onSearchQueryChange("") }) {
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
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
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
                        .weight(1f)
                        .height(48.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(KrewzoDimens.RadiusMd))
                )

                Spacer(modifier = Modifier.width(8.dp))

                // GPS Locate Button
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(onClick = onLocateMe) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Locate Me",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Preset Chips (Pune Hubs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                GeocoderHelper.PUNE_HUBS.forEach { hub ->
                    Surface(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { onPresetSelected(hub) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NearMe,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = hub.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // BOTTOM CONFIRMATION CARD
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
                        text = "WORK / HOME LOCATION",
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
                            text = String.format(Locale.US, "%.4f, %.4f", latitude, longitude),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isGeocoding) "Resolving address..." else resolvedAddress,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = KrewzoTheme.colors.textPrimary,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = buildingDetail,
                    onValueChange = onBuildingDetailChange,
                    placeholder = {
                        Text(
                            text = "Room/Flat, Building name (Optional)",
                            fontSize = 12.sp,
                            color = KrewzoTheme.colors.textTertiary
                        )
                    },
                    singleLine = true,
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
                        .fillMaxWidth()
                        .height(44.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                KrewzoButton(
                    text = "Confirm Address & Continue",
                    onClick = onNext,
                    enabled = !isGeocoding && resolvedAddress.isNotBlank()
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 7: SKILL CATEGORIES (DEFAULT SET TO LOADING/UNLOADING)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepSkillCategories(
    selectedSkills: List<SkillCategory>,
    onToggleSkill: (SkillCategory) -> Unit,
    fullName: String,
    phone: String,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "STEP 7 OF 7 • TRADE & SKILLS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Assign Skill Categories",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "By default set to Loading & Unloading. Select all categories this worker is qualified for.",
                fontSize = 14.sp,
                color = KrewzoTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Skills Grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkillCategory.entries.forEach { skill ->
                    val isSelected = selectedSkills.contains(skill)
                    val skillBg by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.surfaceSecondary,
                        animationSpec = androidx.compose.animation.core.tween(150),
                        label = "skillBg"
                    )
                    val skillBorder by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.border,
                        animationSpec = androidx.compose.animation.core.tween(150),
                        label = "skillBorder"
                    )
                    val skillText by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) Color.White else KrewzoTheme.colors.textPrimary,
                        animationSpec = androidx.compose.animation.core.tween(150),
                        label = "skillText"
                    )

                    Surface(
                        color = skillBg,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .bouncyClickable { onToggleSkill(skill) }
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = skillBorder,
                                shape = RoundedCornerShape(20.dp)
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = skill.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = skillText
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(top = 28.dp)) {
            KrewzoButton(
                text = "Complete Registration & Save Worker",
                onClick = onComplete,
                enabled = selectedSkills.isNotEmpty()
            )
        }
    }
}
