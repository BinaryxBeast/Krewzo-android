package com.binaryxbeast.krewzo.feature.worker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.SkillCategory
import com.binaryxbeast.krewzo.ui.components.BottomNavItem
import com.binaryxbeast.krewzo.ui.components.KrewzoBottomNav
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkerProfileScreen(
    viewModel: WorkerViewModel,
    onNavigateHome: () -> Unit,
    onNavigateSchedule: () -> Unit = {},
    onNavigateEarnings: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val workerProfile by viewModel.workerProfile.collectAsState()
    val saveResult by viewModel.saveProfileResult.collectAsState()

    // Editable fields by Worker
    var homeAddress by rememberSaveable { mutableStateOf(workerProfile?.homeAddress ?: "") }
    var emergencyContact by rememberSaveable { mutableStateOf(workerProfile?.emergencyContact ?: "") }
    var dateOfBirth by rememberSaveable { mutableStateOf(workerProfile?.dateOfBirth ?: "") }

    var showSignOutDialog by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(workerProfile) {
        val wp = workerProfile
        if (wp != null && homeAddress.isEmpty() && emergencyContact.isEmpty()) {
            homeAddress = wp.homeAddress ?: ""
            emergencyContact = wp.emergencyContact ?: ""
            dateOfBirth = wp.dateOfBirth ?: ""
        }
    }

    LaunchedEffect(saveResult) {
        if (saveResult is Resource.Error) {
            validationError = (saveResult as Resource.Error).message
        } else if (saveResult is Resource.Success) {
            validationError = null
            snackbarHostState.showSnackbar("Worker contact and DOB details updated successfully!")
            viewModel.resetSaveProfileResult()
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = KrewzoTheme.colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out of your worker account?",
                    color = KrewzoTheme.colors.textSecondary
                )
            },
            confirmButton = {
                KrewzoButton(
                    text = "Sign Out",
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    modifier = Modifier.width(110.dp)
                )
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showSignOutDialog = false },
                    shape = RoundedCornerShape(KrewzoDimens.RadiusMd)
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(KrewzoDimens.RadiusXl)
        )
    }

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "Worker Profile",
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(KrewzoTheme.colors.surfaceSecondary)
                            .clickable { showSignOutDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            KrewzoBottomNav(
                items = listOf(
                    BottomNavItem.WorkerHome,
                    BottomNavItem.WorkerSchedule,
                    BottomNavItem.WorkerEarnings,
                    BottomNavItem.WorkerProfile
                ),
                currentRoute = BottomNavItem.WorkerProfile.route,
                onItemClick = { item ->
                    when (item) {
                        BottomNavItem.WorkerHome -> onNavigateHome()
                        BottomNavItem.WorkerSchedule -> onNavigateSchedule()
                        BottomNavItem.WorkerEarnings -> onNavigateEarnings()
                        else -> {}
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .imePadding()
        ) {
            val worker = workerProfile

            // Header Banner: Explaining verification policy
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(KrewzoDimens.RadiusMd),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Official credentials (Name, Phone, and Assigned Skills) are verified and managed by Krewzo Administration. You can edit your home address, emergency contact, and Date of Birth below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KrewzoTheme.colors.textPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Profile Photo & Name Card (Read-Only)
            KrewzoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Photo (Avatar preview, read-only)
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = worker?.fullName ?: "Verified Worker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = KrewzoTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked credential",
                                tint = KrewzoTheme.colors.textTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = KrewzoTheme.colors.textSecondary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+91 ${worker?.phone ?: "—"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KrewzoTheme.colors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = KrewzoTheme.colors.ratingGreenContainer
                        ) {
                            Text(
                                text = "ID & KYC Verified",
                                style = MaterialTheme.typography.labelSmall,
                                color = KrewzoTheme.colors.ratingGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Performance Metrics Card (Read-Only)
            Text(
                text = "Performance Metrics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Rating
                KrewzoCard(
                    modifier = Modifier.weight(1f),
                    padding = 12.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        com.binaryxbeast.krewzo.ui.components.RatingBadge(
                            rating = worker?.rating ?: 5.0
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Rating",
                            style = MaterialTheme.typography.labelSmall,
                            color = KrewzoTheme.colors.textSecondary
                        )
                    }
                }

                // Jobs Completed
                KrewzoCard(
                    modifier = Modifier.weight(1f),
                    padding = 12.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${worker?.totalJobsCompleted ?: 0}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = KrewzoTheme.colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.labelSmall,
                            color = KrewzoTheme.colors.textSecondary
                        )
                    }
                }

                // Earnings
                KrewzoCard(
                    modifier = Modifier.weight(1f),
                    padding = 12.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CurrencyRupee,
                                contentDescription = null,
                                tint = KrewzoTheme.colors.ratingGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${(worker?.totalEarnings ?: 24800.0).toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = KrewzoTheme.colors.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total Payout",
                            style = MaterialTheme.typography.labelSmall,
                            color = KrewzoTheme.colors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Assigned Skills (Read-Only)
            Text(
                text = "Assigned Skills (Admin Managed)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            KrewzoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 14.dp
            ) {
                val skills = worker?.skills ?: listOf(SkillCategory.LOADING_UNLOADING, SkillCategory.PACKAGING)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    skills.forEach { skill ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = skill.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Worker Editable Details Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Editable Worker Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KrewzoTheme.colors.textPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Editable by you",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Validation Error Banner if Denied (e.g. age < 18)
            if (validationError != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(KrewzoDimens.RadiusMd),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = validationError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            KrewzoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp
            ) {
                // Home Address (Editable)
                KrewzoTextField(
                    value = homeAddress,
                    onValueChange = {
                        homeAddress = it
                        validationError = null
                    },
                    label = "Home Address / Pune Locality",
                    placeholder = "e.g. Dhayari Phata, Sinhagad Road, Pune",
                    leadingIcon = {
                        Icon(Icons.Default.Home, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Emergency Contact Number (Editable)
                KrewzoTextField(
                    value = emergencyContact,
                    onValueChange = {
                        emergencyContact = it
                        validationError = null
                    },
                    label = "Emergency Contact Mobile",
                    placeholder = "10-digit mobile number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Date of Birth (Editable with age >= 18 strict check)
                KrewzoTextField(
                    value = dateOfBirth,
                    onValueChange = {
                        dateOfBirth = it
                        validationError = null
                    },
                    label = "Date of Birth (YYYY-MM-DD)",
                    placeholder = "e.g. 2000-05-15",
                    leadingIcon = {
                        Icon(Icons.Default.Cake, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = KrewzoTheme.colors.textTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Must be at least 18 years old. Current recorded age: ${worker?.age ?: 26} yrs",
                        style = MaterialTheme.typography.labelSmall,
                        color = KrewzoTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            val isSaving = saveResult is Resource.Loading
            KrewzoButton(
                text = if (isSaving) "Verifying & Saving..." else "Save My Details",
                onClick = {
                    validationError = null
                    viewModel.updateWorkerDetails(
                        homeAddress = homeAddress,
                        emergencyContact = emergencyContact,
                        dobString = dateOfBirth
                    )
                },
                enabled = !isSaving && homeAddress.isNotBlank() && emergencyContact.isNotBlank() && dateOfBirth.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sign Out Option
            OutlinedButton(
                onClick = { showSignOutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(KrewzoDimens.RadiusMd),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
