package com.binaryxbeast.krewzo.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.ui.components.BottomNavItem
import com.binaryxbeast.krewzo.ui.components.KrewzoBottomNav
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme
import kotlinx.coroutines.launch

@Composable
fun AdminProfileScreen(
    viewModel: AdminViewModel,
    onNavigateDashboard: () -> Unit,
    onNavigateWorkers: () -> Unit,
    onNavigateClients: () -> Unit,
    onNavigateBookings: () -> Unit,
    onSignOut: () -> Unit
) {
    val adminProfile by viewModel.adminProfile.collectAsState()
    val saveProfileState by viewModel.saveAdminProfileState.collectAsState()

    var fullName by rememberSaveable { mutableStateOf(adminProfile.fullName) }
    var phone by rememberSaveable { mutableStateOf(adminProfile.phone) }
    var email by rememberSaveable { mutableStateOf(adminProfile.email) }
    var avatarUrl by rememberSaveable { mutableStateOf(adminProfile.avatarUrl ?: "") }

    var showSignOutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(adminProfile) {
        if (fullName.isEmpty()) fullName = adminProfile.fullName
        if (phone.isEmpty()) phone = adminProfile.phone
        if (email.isEmpty()) email = adminProfile.email
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
                    text = "Are you sure you want to sign out of the Administrator account?",
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
                title = "Admin Profile",
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
                    BottomNavItem.AdminDashboard,
                    BottomNavItem.AdminWorkers,
                    BottomNavItem.AdminClients,
                    BottomNavItem.AdminBookings,
                    BottomNavItem.AdminProfile
                ),
                currentRoute = BottomNavItem.AdminProfile.route,
                onItemClick = { item ->
                    when (item) {
                        BottomNavItem.AdminDashboard -> onNavigateDashboard()
                        BottomNavItem.AdminWorkers -> onNavigateWorkers()
                        BottomNavItem.AdminClients -> onNavigateClients()
                        BottomNavItem.AdminBookings -> onNavigateBookings()
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
            // Header Card with Avatar and Role Badge
            KrewzoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 18.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fullName.ifBlank { "Administrator" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KrewzoTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = adminProfile.roleTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = KrewzoTheme.colors.textTertiary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Pune HQ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Editable Profile Fields Card
            Text(
                text = "Personal & Contact Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            KrewzoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp
            ) {
                // Full Name
                KrewzoTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = "Full Name",
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Phone Number
                KrewzoTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Mobile Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Email Address
                KrewzoTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Email Address",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar URL / Photo Link (Optional)
                KrewzoTextField(
                    value = avatarUrl,
                    onValueChange = { avatarUrl = it },
                    label = "Avatar Image URL (Optional)",
                    placeholder = "https://...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // System Security & Access Scope
            Text(
                text = "Privileges & Access Scope",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            KrewzoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp
            ) {
                val permissions = listOf(
                    "Worker Verification & Dispatch Management",
                    "Client Onboarding & Compliance Review",
                    "Live Booking Orchestration & Wave Overrides",
                    "Role Assignment & Security Administration"
                )

                permissions.forEach { perm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = perm,
                            style = MaterialTheme.typography.bodySmall,
                            color = KrewzoTheme.colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            val isSaving = saveProfileState is Resource.Loading
            KrewzoButton(
                text = if (isSaving) "Saving Changes..." else "Save Admin Profile",
                onClick = {
                    viewModel.updateAdminProfile(
                        fullName = fullName,
                        phone = phone,
                        email = email,
                        avatarUrl = avatarUrl.ifBlank { null },
                        onSuccess = {
                            scope.launch {
                                snackbarHostState.showSnackbar("Admin profile updated successfully!")
                            }
                        }
                    )
                },
                enabled = !isSaving && fullName.isNotBlank() && phone.isNotBlank(),
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
                    text = "Sign Out of Admin Console",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
