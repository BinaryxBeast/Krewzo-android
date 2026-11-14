package com.binaryxbeast.krewzo.feature.client

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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
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
import androidx.compose.runtime.mutableDoubleStateOf
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
import com.binaryxbeast.krewzo.core.model.Client
import com.binaryxbeast.krewzo.ui.components.BottomNavItem
import com.binaryxbeast.krewzo.ui.components.KrewzoBottomNav
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoMapWidget
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme
import kotlinx.coroutines.launch

@Composable
fun ClientProfileScreen(
    viewModel: ClientViewModel,
    onNavigateHome: () -> Unit,
    onNavigateCreateBooking: () -> Unit,
    onNavigateHistory: () -> Unit,
    onSignOut: () -> Unit
) {
    val clientProfile by viewModel.clientProfile.collectAsState()
    val saveProfileState by viewModel.saveProfileState.collectAsState()

    var businessName by rememberSaveable { mutableStateOf(clientProfile?.businessName ?: "") }
    var contactPerson by rememberSaveable { mutableStateOf(clientProfile?.contactPerson ?: "") }
    var contactPersonRole by rememberSaveable { mutableStateOf(clientProfile?.contactPersonRole ?: "Operations Manager") }
    var phone by rememberSaveable { mutableStateOf(clientProfile?.phone ?: "") }
    var email by rememberSaveable { mutableStateOf(clientProfile?.email ?: "") }
    var address by rememberSaveable { mutableStateOf(clientProfile?.address ?: "") }
    var city by rememberSaveable { mutableStateOf(clientProfile?.city ?: "Pune") }
    var pincode by rememberSaveable { mutableStateOf(clientProfile?.pincode ?: "411057") }
    var latitude by rememberSaveable { mutableDoubleStateOf(clientProfile?.latitude ?: 18.5913) }
    var longitude by rememberSaveable { mutableDoubleStateOf(clientProfile?.longitude ?: 73.7389) }
    var gstin by rememberSaveable { mutableStateOf(clientProfile?.gstin ?: "") }
    var employeeCountText by rememberSaveable { mutableStateOf(clientProfile?.employeeCount?.toString() ?: "25") }
    var logoUrl by rememberSaveable { mutableStateOf(clientProfile?.logoUrl ?: "") }

    var showSignOutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(clientProfile) {
        val cp = clientProfile
        if (cp != null && businessName.isEmpty()) {
            businessName = cp.businessName
            contactPerson = cp.contactPerson
            contactPersonRole = cp.contactPersonRole ?: "Operations Manager"
            phone = cp.phone
            email = cp.email ?: ""
            address = cp.address
            city = cp.city
            pincode = cp.pincode
            latitude = cp.latitude ?: 18.5913
            longitude = cp.longitude ?: 73.7389
            gstin = cp.gstin ?: ""
            employeeCountText = cp.employeeCount.toString()
            logoUrl = cp.logoUrl ?: ""
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
                    text = "Are you sure you want to sign out of your business account?",
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
                title = "Business Profile",
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
                    BottomNavItem.ClientHome,
                    BottomNavItem.CreateBooking,
                    BottomNavItem.ClientHistory,
                    BottomNavItem.ClientProfile
                ),
                currentRoute = BottomNavItem.ClientProfile.route,
                onItemClick = { item ->
                    when (item) {
                        BottomNavItem.ClientHome -> onNavigateHome()
                        BottomNavItem.CreateBooking -> onNavigateCreateBooking()
                        BottomNavItem.ClientHistory -> onNavigateHistory()
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
            // Header Business Card
            KrewzoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 18.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(KrewzoDimens.RadiusMd)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = businessName.ifBlank { "Client Business" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KrewzoTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = KrewzoTheme.colors.ratingGreenContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = KrewzoTheme.colors.ratingGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Verified Business",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KrewzoTheme.colors.ratingGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 1: Business & Contact Details
            Text(
                text = "Company & Contact Information",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            KrewzoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp
            ) {
                // Business Name
                KrewzoTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = "Company / Business Name",
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Contact Person Name
                KrewzoTextField(
                    value = contactPerson,
                    onValueChange = { contactPerson = it },
                    label = "Contact Person Name",
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Contact Person Role
                KrewzoTextField(
                    value = contactPersonRole,
                    onValueChange = { contactPersonRole = it },
                    label = "Contact Person Role / Designation",
                    placeholder = "e.g. Operations Manager, Store Owner",
                    leadingIcon = {
                        Icon(Icons.Default.Badge, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Phone Number
                KrewzoTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Contact Mobile Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Business Email
                KrewzoTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Business Email",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: Business Address & Google Map Widget
            Text(
                text = "Business Work Location & Coordinates",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            KrewzoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp
            ) {
                // Address Line
                KrewzoTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Worksite / Billing Address",
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    KrewzoTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = "City",
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    KrewzoTextField(
                        value = pincode,
                        onValueChange = { pincode = it },
                        label = "Pincode",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Embedded Google Map Widget
                Text(
                    text = "Location Preview (Pune Coordinates)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = KrewzoTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                KrewzoMapWidget(
                    latitude = latitude,
                    longitude = longitude,
                    addressLabel = address.ifBlank { businessName.ifBlank { "Work Location" } },
                    onLocationSelected = { addr, cty, pin, lat, lng ->
                        address = addr
                        city = cty
                        pincode = pin
                        latitude = lat
                        longitude = lng
                    },
                    onCoordinatesChanged = { lat, lng ->
                        latitude = lat
                        longitude = lng
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 3: Optional Tax & Organization Details
            Text(
                text = "Tax & Organization Details (Optional)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            KrewzoCard(
                modifier = Modifier.fillMaxWidth(),
                padding = 16.dp
            ) {
                // GSTIN (Optional)
                KrewzoTextField(
                    value = gstin,
                    onValueChange = { gstin = it },
                    label = "GSTIN Number (Optional)",
                    placeholder = "27ABCDE1234F1Z5",
                    leadingIcon = {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Employee Count (Optional)
                KrewzoTextField(
                    value = employeeCountText,
                    onValueChange = { employeeCountText = it },
                    label = "Approx. Employee Count (Optional)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = KrewzoTheme.colors.textTertiary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Logo URL (Optional)
                KrewzoTextField(
                    value = logoUrl,
                    onValueChange = { logoUrl = it },
                    label = "Company Logo URL (Optional)",
                    placeholder = "https://...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Profile Button
            val isSaving = saveProfileState is Resource.Loading
            KrewzoButton(
                text = if (isSaving) "Saving Profile..." else "Save Business Profile",
                onClick = {
                    val updatedClient = (clientProfile ?: Client(
                        id = "cli_${System.currentTimeMillis()}",
                        businessName = businessName,
                        contactPerson = contactPerson,
                        phone = phone,
                        address = address
                    )).copy(
                        businessName = businessName,
                        contactPerson = contactPerson,
                        contactPersonRole = contactPersonRole,
                        phone = phone,
                        email = email.ifBlank { null },
                        address = address,
                        city = city,
                        pincode = pincode,
                        latitude = latitude,
                        longitude = longitude,
                        gstin = gstin.ifBlank { null },
                        employeeCount = employeeCountText.toIntOrNull() ?: 10,
                        logoUrl = logoUrl.ifBlank { null }
                    )

                    viewModel.updateClientProfile(updatedClient) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Business profile updated successfully!")
                        }
                    }
                },
                enabled = !isSaving && businessName.isNotBlank() && contactPerson.isNotBlank() && phone.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sign Out Button
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
