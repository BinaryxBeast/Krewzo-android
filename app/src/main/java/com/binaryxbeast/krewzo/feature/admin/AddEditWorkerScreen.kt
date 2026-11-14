package com.binaryxbeast.krewzo.feature.admin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.SkillCategory
import com.binaryxbeast.krewzo.core.model.Worker
import com.binaryxbeast.krewzo.core.util.GeocoderHelper
import com.binaryxbeast.krewzo.core.util.ImageCompressor
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.components.bouncyClickable
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditWorkerScreen(
    workerId: String?,
    viewModel: AdminViewModel,
    onSaveComplete: () -> Unit,
    onBack: () -> Unit
) {
    if (workerId == null) {
        WorkerOnboardingScreen(
            viewModel = viewModel,
            onSaveComplete = onSaveComplete,
            onBack = onBack
        )
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val workers by viewModel.workers.collectAsState()
    val existingWorker = workers.find { it.id == workerId }

    // ── Form State ───────────────────────────────────────────────────────────
    var fullName by rememberSaveable { mutableStateOf(existingWorker?.fullName ?: "") }
    var phone by rememberSaveable { mutableStateOf(existingWorker?.phone ?: "") }
    var dateOfBirth by rememberSaveable { mutableStateOf(existingWorker?.dateOfBirth ?: "") }
    var ageText by rememberSaveable {
        mutableStateOf(
            existingWorker?.age?.toString()
                ?: existingWorker?.let { Worker.calculateAgeFromDob(it.dateOfBirth)?.toString() }
                ?: ""
        )
    }
    var homeAddress by rememberSaveable { mutableStateOf(existingWorker?.homeAddress ?: "") }
    var emergencyContact by rememberSaveable { mutableStateOf(existingWorker?.emergencyContact ?: "") }
    var adminNotes by rememberSaveable { mutableStateOf(existingWorker?.adminNotes ?: "") }
    var isActive by rememberSaveable { mutableStateOf(existingWorker?.isActive ?: true) }
    var isAvailable by rememberSaveable { mutableStateOf(existingWorker?.isAvailable ?: false) }
    var photoUrl by rememberSaveable { mutableStateOf(existingWorker?.photoUrl) }
    var capturedPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSkillNames by rememberSaveable {
        mutableStateOf(ArrayList(existingWorker?.skills?.map { it.name } ?: emptyList()))
    }

    // Aadhaar Document State
    var aadhaarNumber by rememberSaveable {
        mutableStateOf(adminNotes.extractAadhaarValue("NUM") ?: "")
    }
    var existingAadhaarFrontUrl by rememberSaveable {
        mutableStateOf(adminNotes.extractAadhaarValue("FRONT"))
    }
    var existingAadhaarBackUrl by rememberSaveable {
        mutableStateOf(adminNotes.extractAadhaarValue("BACK"))
    }

    // Captured Camera States for profile & Aadhaar
    var pendingPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingAadhaarFrontBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingAadhaarBackBytes by remember { mutableStateOf<ByteArray?>(null) }

    var isCompressing by remember { mutableStateOf(false) }
    var cameraTarget by remember { mutableStateOf<String?>(null) } // "PHOTO", "FRONT", "BACK"
    var tempCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var compressionError by remember { mutableStateOf<String?>(null) }

    // Fullscreen Document / Photo Preview Dialog
    var previewImageUrl by remember { mutableStateOf<Any?>(null) }
    var previewImageTitle by remember { mutableStateOf("") }

    // Delete Confirmation Dialog
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Hydrate fields once the existing worker is loaded
    LaunchedEffect(existingWorker) {
        if (existingWorker != null && fullName.isEmpty() && phone.isEmpty()) {
            fullName = existingWorker.fullName
            phone = existingWorker.phone
            dateOfBirth = existingWorker.dateOfBirth ?: ""
            ageText = existingWorker.age?.toString()
                ?: (Worker.calculateAgeFromDob(existingWorker.dateOfBirth)?.toString() ?: "")
            homeAddress = existingWorker.homeAddress ?: ""
            emergencyContact = existingWorker.emergencyContact ?: ""
            adminNotes = existingWorker.adminNotes ?: ""
            isActive = existingWorker.isActive
            isAvailable = existingWorker.isAvailable
            if (photoUrl == null) photoUrl = existingWorker.photoUrl
            if (selectedSkillNames.isEmpty()) {
                selectedSkillNames = ArrayList(existingWorker.skills.map { it.name })
            }
            if (aadhaarNumber.isEmpty()) {
                aadhaarNumber = existingWorker.adminNotes.extractAadhaarValue("NUM") ?: ""
            }
            if (existingAadhaarFrontUrl == null) {
                existingAadhaarFrontUrl = existingWorker.adminNotes.extractAadhaarValue("FRONT")
            }
            if (existingAadhaarBackUrl == null) {
                existingAadhaarBackUrl = existingWorker.adminNotes.extractAadhaarValue("BACK")
            }
        }
    }

    // Camera launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraPath != null) {
            coroutineScope.launch {
                try {
                    isCompressing = true
                    compressionError = null
                    val file = File(tempCameraPath!!)
                    if (file.exists() && file.length() > 0) {
                        val compressed = ImageCompressor.compressFile(file)
                        when (cameraTarget) {
                            "PHOTO" -> {
                                capturedPhotoPath = tempCameraPath
                                pendingPhotoBytes = compressed
                            }
                            "FRONT" -> {
                                pendingAadhaarFrontBytes = compressed
                            }
                            "BACK" -> {
                                pendingAadhaarBackBytes = compressed
                            }
                        }
                    }
                } catch (e: Exception) {
                    compressionError = "Image processing failed: ${e.localizedMessage}"
                } finally {
                    isCompressing = false
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraTarget?.let { target ->
                launchCameraCapture(context, target, takePictureLauncher) { path ->
                    tempCameraPath = path
                }
            }
        } else {
            compressionError = "Camera permission is required to capture photos."
        }
    }

    fun openCameraFor(target: String) {
        compressionError = null
        cameraTarget = target
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            launchCameraCapture(context, target, takePictureLauncher) { path ->
                tempCameraPath = path
            }
        }
    }

    fun openDatePickerDialog() {
        val cal = Calendar.getInstance()
        val parts = dateOfBirth.trim().split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) cal.set(y, m - 1, d)
        } else {
            cal.add(Calendar.YEAR, -24)
        }

        android.app.DatePickerDialog(
            context,
            { _, year, monthOfYear, dayOfMonth ->
                val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth)
                dateOfBirth = formatted
                Worker.calculateAgeFromDob(formatted)?.let { ageText = it.toString() }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    val saveState by viewModel.saveWorkerState.collectAsState()
    val isLoading = saveState is Resource.Loading
    val calculatedAge = ageText.toIntOrNull() ?: Worker.calculateAgeFromDob(dateOfBirth)

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "Worker Profile",
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // ── PROFILE HEADER CARD ──────────────────────────────────────────
            KrewzoCard(padding = 0.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo Avatar
                    Box(modifier = Modifier.size(108.dp), contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(104.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    2.dp,
                                    if (isActive) KrewzoTheme.colors.success.copy(alpha = 0.6f)
                                    else KrewzoTheme.colors.border,
                                    CircleShape
                                )
                                .clickable {
                                    val currentImg: Any? = pendingPhotoBytes ?: capturedPhotoPath?.let { File(it) } ?: photoUrl
                                    if (currentImg != null) {
                                        previewImageUrl = currentImg
                                        previewImageTitle = "${fullName.ifBlank { "Worker" }}'s Photo"
                                    } else {
                                        openCameraFor("PHOTO")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isCompressing && cameraTarget == "PHOTO" -> {
                                    com.binaryxbeast.krewzo.ui.components.ExpressLoadingIndicator(
                                        size = 34.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                pendingPhotoBytes != null -> {
                                    AsyncImage(
                                        model = pendingPhotoBytes,
                                        contentDescription = "New Worker Photograph",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                capturedPhotoPath != null && File(capturedPhotoPath!!).exists() -> {
                                    AsyncImage(
                                        model = File(capturedPhotoPath!!),
                                        contentDescription = "Worker Photograph",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                !photoUrl.isNullOrBlank() -> {
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = "Worker Photograph",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(52.dp)
                                    )
                                }
                            }
                        }

                        // Camera overlay badge to retake/take photo
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                .clickable { openCameraFor("PHOTO") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Take Photo",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    if (compressionError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = compressionError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = fullName.ifBlank { existingWorker?.fullName ?: "Worker Profile" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = KrewzoTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = phone.ifBlank { existingWorker?.phone ?: "" },
                        fontSize = 13.sp,
                        color = KrewzoTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Badges row: Active/Deactivated + Availability + KYC Status
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Active / Inactive Badge
                        Surface(
                            color = if (isActive) KrewzoTheme.colors.successContainer else Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isActive) "● Active Account" else "○ Deactivated",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) KrewzoTheme.colors.success else Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Availability Badge
                        Surface(
                            color = if (isAvailable) Color(0xFFECFDF5) else KrewzoTheme.colors.surfaceSecondary,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isAvailable) "Ready for Jobs" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAvailable) Color(0xFF059669) else KrewzoTheme.colors.textTertiary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Aadhaar Verified Badge
                        val isAadhaarDocPresent = existingAadhaarFrontUrl != null || pendingAadhaarFrontBytes != null || aadhaarNumber.isNotBlank()
                        Surface(
                            color = if (isAadhaarDocPresent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isAadhaarDocPresent) "✓ Aadhaar Linked" else "KYC Pending",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAadhaarDocPresent) MaterialTheme.colorScheme.primary else Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = KrewzoTheme.colors.divider)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Performance & Job Statistics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatChip(
                            icon = Icons.Default.Work,
                            label = "Jobs Completed",
                            value = "${existingWorker?.totalJobsCompleted ?: 0}"
                        )
                        StatChip(
                            icon = Icons.Default.Star,
                            label = "Rating",
                            value = String.format(Locale.US, "%.1f", existingWorker?.rating ?: 5.0)
                        )
                        StatChip(
                            icon = Icons.Default.CheckCircle,
                            label = "Earnings",
                            value = "₹${(existingWorker?.totalEarnings ?: 0.0).toInt()}"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── STATUS & AVAILABILITY SWITCHES ───────────────────────────────
            SectionHeader("Account Status & Availability")
            KrewzoCard(padding = 16.dp) {
                StatusToggleRow(
                    label = "Account Active",
                    description = if (isActive) "Worker is visible in directory and eligible for job matching waves" else "Worker is deactivated and will not receive any job requests",
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    activeColor = KrewzoTheme.colors.success
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = KrewzoTheme.colors.divider)
                Spacer(modifier = Modifier.height(10.dp))

                StatusToggleRow(
                    label = "Real-Time Availability",
                    description = if (isAvailable) "Worker is on duty and ready to accept immediate jobs" else "Worker is currently off-duty or on a break",
                    checked = isAvailable,
                    onCheckedChange = { isAvailable = it },
                    activeColor = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── PERSONAL & CONTACT INFO ──────────────────────────────────────
            SectionHeader("Personal Information")
            KrewzoCard(padding = 18.dp) {
                KrewzoTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full Name *",
                    placeholder = "Ramesh Shankar Patil",
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                KrewzoTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Mobile Phone (Used for Worker OTP Login) *",
                    placeholder = "+91 98230 11223",
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Date of Birth & Age Row
                Row(modifier = Modifier.fillMaxWidth()) {
                    KrewzoTextField(
                        value = dateOfBirth,
                        onValueChange = { input ->
                            dateOfBirth = input
                            Worker.calculateAgeFromDob(input)?.let { ageText = it.toString() }
                        },
                        label = "Date of Birth",
                        placeholder = "YYYY-MM-DD",
                        leadingIcon = { Icon(Icons.Default.Cake, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { openDatePickerDialog() }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick Date of Birth",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier.weight(1.6f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    KrewzoTextField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        label = "Age (Years)",
                        placeholder = "28",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (calculatedAge != null && calculatedAge < 18) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "⚠ Legal notice: Worker is under 18 years of age",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                KrewzoTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = "Emergency Phone Contact",
                    placeholder = "+91 98230 99887",
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── WORK & HOME LOCATION ─────────────────────────────────────────
            SectionHeader("Work & Home Address")
            KrewzoCard(padding = 18.dp) {
                KrewzoTextField(
                    value = homeAddress,
                    onValueChange = { homeAddress = it },
                    label = "Work / Residential Location in Pune *",
                    placeholder = "Area, Landmark, Pincode",
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Quick Select Pune Hub:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KrewzoTheme.colors.textTertiary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Preset Pune Locations for fast address alignment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GeocoderHelper.PUNE_HUBS.forEach { hub ->
                        Surface(
                            color = KrewzoTheme.colors.surfaceSecondary,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.clickable {
                                homeAddress = "${hub.name}, ${hub.addressLine}, Pune ${hub.pincode}"
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = hub.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KrewzoTheme.colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── AADHAAR CARD & IDENTITY VERIFICATION ─────────────────────────
            SectionHeader("Aadhaar Card & KYC Documents")
            KrewzoCard(padding = 18.dp) {
                KrewzoTextField(
                    value = aadhaarNumber,
                    onValueChange = { input ->
                        aadhaarNumber = input.filter { it.isDigit() }.take(12)
                    },
                    label = "Aadhaar Card Number (12 Digits)",
                    placeholder = "XXXX XXXX XXXX",
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aadhaar Images (Front & Back):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KrewzoTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Aadhaar Front & Back Document Inspection & Retake Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Front Side
                    val frontModel: Any? = pendingAadhaarFrontBytes ?: existingAadhaarFrontUrl
                    AadhaarCardSide(
                        title = "Front Side",
                        imageModel = frontModel,
                        onCapture = { openCameraFor("FRONT") },
                        onPreview = {
                            if (frontModel != null) {
                                previewImageUrl = frontModel
                                previewImageTitle = "Aadhaar Front Side"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // Back Side
                    val backModel: Any? = pendingAadhaarBackBytes ?: existingAadhaarBackUrl
                    AadhaarCardSide(
                        title = "Back Side",
                        imageModel = backModel,
                        onCapture = { openCameraFor("BACK") },
                        onPreview = {
                            if (backModel != null) {
                                previewImageUrl = backModel
                                previewImageTitle = "Aadhaar Back Side"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── SKILLS & EXPERTISE ───────────────────────────────────────────
            SectionHeader("Skill Categories")
            KrewzoCard(padding = 18.dp) {
                Text(
                    text = "Select all tasks this worker is qualified to handle:",
                    fontSize = 12.sp,
                    color = KrewzoTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkillCategory.entries.forEach { skill ->
                        val isSelected = selectedSkillNames.contains(skill.name)
                        val chipBg by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else KrewzoTheme.colors.surfaceSecondary,
                            animationSpec = androidx.compose.animation.core.tween(150),
                            label = "chipBg"
                        )
                        val chipBorder by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            animationSpec = androidx.compose.animation.core.tween(150),
                            label = "chipBorder"
                        )
                        val chipText by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.textSecondary,
                            animationSpec = androidx.compose.animation.core.tween(150),
                            label = "chipText"
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                                .background(chipBg)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = chipBorder,
                                    shape = RoundedCornerShape(KrewzoDimens.RadiusMd)
                                )
                                .bouncyClickable {
                                    val current = ArrayList(selectedSkillNames)
                                    if (isSelected) current.remove(skill.name) else current.add(skill.name)
                                    selectedSkillNames = current
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = skill.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = chipText
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── INTERNAL ADMIN NOTES ─────────────────────────────────────────
            SectionHeader("Internal Admin Notes")
            KrewzoCard(padding = 18.dp) {
                KrewzoTextField(
                    value = adminNotes,
                    onValueChange = { adminNotes = it },
                    label = "Admin Notes (Private, not visible to worker)",
                    placeholder = "Background verification notes, police record check, internal comments…",
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    singleLine = false,
                    maxLines = 4
                )
            }

            // ── SAVE ERROR NOTICE ────────────────────────────────────────────
            if (saveState is Resource.Error) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = (saveState as Resource.Error).message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── UPDATE WORKER BUTTON ─────────────────────────────────────────
            KrewzoButton(
                text = "Save Worker Profile",
                onClick = {
                    val cleanDob = dateOfBirth.trim().ifBlank { null }
                    val updatedWorker = Worker(
                        id = workerId,
                        fullName = fullName.trim(),
                        phone = phone.trim(),
                        age = calculatedAge,
                        dateOfBirth = cleanDob,
                        homeAddress = homeAddress.trim().ifBlank { null },
                        emergencyContact = emergencyContact.trim().ifBlank { null },
                        adminNotes = adminNotes.trim().ifBlank { null },
                        isActive = isActive,
                        isAvailable = isAvailable,
                        skills = selectedSkillNames.map { SkillCategory.fromString(it) },
                        photoUrl = photoUrl,
                        userId = existingWorker?.userId,
                        totalJobsCompleted = existingWorker?.totalJobsCompleted ?: 0,
                        totalEarnings = existingWorker?.totalEarnings ?: 0.0,
                        rating = existingWorker?.rating ?: 5.0
                    )

                    viewModel.saveWorker(
                        worker = updatedWorker,
                        pendingPhotoBytes = pendingPhotoBytes,
                        pendingAadhaarFrontBytes = pendingAadhaarFrontBytes,
                        pendingAadhaarBackBytes = pendingAadhaarBackBytes,
                        aadhaarNumber = aadhaarNumber.ifBlank { null },
                        onSuccess = {
                            viewModel.resetSaveWorkerState()
                            onSaveComplete()
                        }
                    )
                },
                isLoading = isLoading || isCompressing,
                enabled = fullName.isNotBlank() && phone.isNotBlank() && !isLoading && !isCompressing
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Delete Worker Button with Confirmation Dialog
            if (existingWorker != null) {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(KrewzoDimens.RadiusLg),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Worker", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // ── DELETE CONFIRMATION DIALOG ───────────────────────────────────────────
    if (showDeleteConfirmDialog && existingWorker != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete Worker Profile?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete ${existingWorker.fullName}? This will revoke their role and access.",
                    fontSize = 14.sp,
                    color = KrewzoTheme.colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteWorker(existingWorker.id)
                        onSaveComplete()
                    }
                ) {
                    Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── DOCUMENT PREVIEW DIALOG ──────────────────────────────────────────────
    if (previewImageUrl != null) {
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = previewImageTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = KrewzoTheme.colors.textPrimary
                        )
                        IconButton(onClick = { previewImageUrl = null }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = KrewzoTheme.colors.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = previewImageUrl,
                            contentDescription = previewImageTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { previewImageUrl = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Robust extractor for tagged Aadhaar details from adminNotes string.
 */
private fun String?.extractAadhaarValue(key: String): String? {
    if (this.isNullOrBlank()) return null
    val prefixes = when (key.uppercase()) {
        "FRONT" -> listOf("Aadhaar Front:", "FRONT:")
        "BACK" -> listOf("Aadhaar Back:", "BACK:")
        "NUM" -> listOf("Aadhaar No:", "Aadhaar Number:", "NUM:")
        else -> listOf("$key:")
    }
    for (part in split("|").map { it.trim() }) {
        for (prefix in prefixes) {
            if (part.startsWith(prefix, ignoreCase = true)) {
                return part.substring(prefix.length).trim().ifBlank { null }
            }
        }
    }
    return null
}

/** Opens the camera via FileProvider with safe error trapping. */
private fun launchCameraCapture(
    context: Context,
    target: String,
    launcher: androidx.activity.result.ActivityResultLauncher<android.net.Uri>,
    onPathReady: (String) -> Unit
) {
    try {
        val dir = File(context.cacheDir, "camera_photos")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "worker_${target.lowercase()}_${System.currentTimeMillis()}.jpg")
        if (!file.exists()) file.createNewFile()
        onPathReady(file.absolutePath)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        launcher.launch(uri)
    } catch (_: Exception) {}
}

// ── UI Components ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = KrewzoTheme.colors.textTertiary,
        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
    )
}

@Composable
private fun StatChip(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            shape = CircleShape,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KrewzoTheme.colors.textPrimary)
        Text(label, fontSize = 10.sp, color = KrewzoTheme.colors.textTertiary)
    }
}

@Composable
private fun StatusToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = KrewzoTheme.colors.textPrimary)
            Text(description, fontSize = 12.sp, color = KrewzoTheme.colors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = activeColor
            )
        )
    }
}

@Composable
private fun AadhaarCardSide(
    title: String,
    imageModel: Any?,
    onCapture: () -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = KrewzoTheme.colors.textTertiary,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                .background(KrewzoTheme.colors.surfaceSecondary)
                .border(1.dp, KrewzoTheme.colors.border, RoundedCornerShape(KrewzoDimens.RadiusMd))
                .clickable {
                    if (imageModel != null) onPreview() else onCapture()
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(KrewzoDimens.RadiusMd)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture $title",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Capture $title",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (imageModel != null) "Retake" else "Capture",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onCapture() }
            )

            if (imageModel != null) {
                Text(
                    text = "View",
                    fontSize = 11.sp,
                    color = KrewzoTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onPreview() }
                )
            }
        }
    }
}
