package com.binaryxbeast.krewzo.feature.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.Client
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoMapWidget
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme
import java.util.UUID

@Composable
fun AddEditClientScreen(
    clientId: String?,
    viewModel: AdminViewModel,
    onSaveComplete: () -> Unit,
    onBack: () -> Unit
) {
    val clients by viewModel.clients.collectAsState()
    val existingClient = clients.find { it.id == clientId }
    val isEditing = clientId != null

    // Form fields preserved across screen rotation / configuration changes
    var businessName by rememberSaveable { mutableStateOf(existingClient?.businessName ?: "") }
    var contactPerson by rememberSaveable { mutableStateOf(existingClient?.contactPerson ?: "") }
    var phone by rememberSaveable { mutableStateOf(existingClient?.phone ?: "") }
    var email by rememberSaveable { mutableStateOf(existingClient?.email ?: "") }
    var employeeCountText by rememberSaveable { mutableStateOf(existingClient?.employeeCount?.toString() ?: "10") }
    var address by rememberSaveable { mutableStateOf(existingClient?.address ?: "") }
    var city by rememberSaveable { mutableStateOf(existingClient?.city ?: "Pune") }
    var pincode by rememberSaveable { mutableStateOf(existingClient?.pincode ?: "411001") }
    var latitude by rememberSaveable { mutableStateOf(existingClient?.latitude ?: 18.5204) }
    var longitude by rememberSaveable { mutableStateOf(existingClient?.longitude ?: 73.8567) }
    var gstin by rememberSaveable { mutableStateOf(existingClient?.gstin ?: "") }
    var isVerified by rememberSaveable { mutableStateOf(existingClient?.isVerified ?: true) }
    var isActive by rememberSaveable { mutableStateOf(existingClient?.isActive ?: true) }

    // If existingClient finishes loading after initial composition, hydrate fields if untouched
    LaunchedEffect(existingClient) {
        if (existingClient != null && businessName.isEmpty() && phone.isEmpty()) {
            businessName = existingClient.businessName
            contactPerson = existingClient.contactPerson
            phone = existingClient.phone
            email = existingClient.email ?: ""
            employeeCountText = existingClient.employeeCount.toString()
            address = existingClient.address
            city = existingClient.city
            pincode = existingClient.pincode
            latitude = existingClient.latitude ?: 18.5204
            longitude = existingClient.longitude ?: 73.8567
            gstin = existingClient.gstin ?: ""
            isVerified = existingClient.isVerified
            isActive = existingClient.isActive
        }
    }

    val saveState by viewModel.saveClientState.collectAsState()
    val isLoading = saveState is Resource.Loading

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = if (isEditing) "Edit Client Profile" else "Register New Client",
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Icon Header
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = "Business Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Business Information
            com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = 0) {
                KrewzoCard(padding = 18.dp) {
                Text(
                    text = "COMPANY DETAILS",
                    style = MaterialTheme.typography.labelSmall,
                    color = KrewzoTheme.colors.textTertiary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                KrewzoTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = "Business / Company Name *"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    KrewzoTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = "Contact Person *",
                        modifier = Modifier.weight(1.3f)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    KrewzoTextField(
                        value = employeeCountText,
                        onValueChange = { employeeCountText = it },
                        label = "Crew Size",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                KrewzoTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "Mobile Phone Number *",
                    placeholder = "+91 98230 ...",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(14.dp))

                KrewzoTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Business Email",
                    placeholder = "ops@company.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
        }

            Spacer(modifier = Modifier.height(16.dp))

            // Location & Tax Information
            com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = 1) {
                KrewzoCard(padding = 18.dp) {
                Text(
                    text = "LOCATION & TAX",
                    style = MaterialTheme.typography.labelSmall,
                    color = KrewzoTheme.colors.textTertiary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Map Widget with GPS, search and Pune presets
                KrewzoMapWidget(
                    latitude = latitude,
                    longitude = longitude,
                    addressLabel = address,
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
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                KrewzoTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "Operating Address / Warehouse Location *"
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    KrewzoTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = "City",
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                    KrewzoTextField(
                        value = pincode,
                        onValueChange = { pincode = it },
                        label = "Pincode",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                KrewzoTextField(
                    value = gstin,
                    onValueChange = { gstin = it },
                    label = "GSTIN / Tax Registration Number",
                    placeholder = "27AAAAA0000A1Z5"
                )
            }
        }

            Spacer(modifier = Modifier.height(16.dp))

            // Verification & Active Status
            com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = 2) {
                KrewzoCard(padding = 16.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isVerified) "KYC Verification: Approved" else "KYC Verification: Pending",
                            style = MaterialTheme.typography.titleSmall,
                            color = KrewzoTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isVerified) "Client can create and broadcast bookings" else "Booking requests require admin review",
                            style = MaterialTheme.typography.bodySmall,
                            color = KrewzoTheme.colors.textSecondary
                        )
                    }

                    Switch(
                        checked = isVerified,
                        onCheckedChange = { isVerified = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = KrewzoTheme.colors.success
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isActive) "Client Status: Active" else "Client Status: Suspended",
                            style = MaterialTheme.typography.titleSmall,
                            color = KrewzoTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isActive) "Account active for operations" else "Account access disabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = KrewzoTheme.colors.textSecondary
                        )
                    }

                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = KrewzoTheme.colors.success
                        )
                    )
                }
            }
        }

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

            KrewzoButton(
                text = if (isEditing) "Update Client Profile" else "Register Client",
                onClick = {
                    val targetId = if (!clientId.isNullOrBlank()) clientId else (existingClient?.id ?: UUID.randomUUID().toString())
                    val updatedClient = Client(
                        id = targetId,
                        businessName = businessName.trim(),
                        contactPerson = contactPerson.trim(),
                        phone = phone.trim(),
                        email = email.trim().ifEmpty { null },
                        employeeCount = employeeCountText.toIntOrNull() ?: 10,
                        address = address.trim(),
                        city = city.trim(),
                        pincode = pincode.trim(),
                        latitude = latitude,
                        longitude = longitude,
                        gstin = gstin.trim().ifEmpty { null },
                        isVerified = isVerified,
                        isActive = isActive,
                        totalBookings = existingClient?.totalBookings ?: 0
                    )
                    viewModel.saveClient(
                        client = updatedClient,
                        onSuccess = {
                            viewModel.resetSaveClientState()
                            onSaveComplete()
                        }
                    )
                },
                isLoading = isLoading,
                enabled = businessName.isNotBlank() && phone.isNotBlank() && contactPerson.isNotBlank() && !isLoading
            )

            if (isEditing && existingClient != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.deleteClient(existingClient.id)
                        onSaveComplete()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(KrewzoDimens.RadiusLg),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDC2626)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f))
                ) {
                    Text("Delete Client", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
