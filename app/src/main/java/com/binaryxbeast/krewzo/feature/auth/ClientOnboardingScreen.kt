package com.binaryxbeast.krewzo.feature.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoMapWidget
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.components.bouncyClickable
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

@Composable
fun ClientOnboardingScreen(
    phone: String,
    viewModel: AuthViewModel,
    onOnboardingComplete: () -> Unit,
    onSignOut: () -> Unit = {}
) {
    var businessName by remember { mutableStateOf("") }
    var selectedEmployeeTier by remember { mutableIntStateOf(25) }
    var addressLine by remember { mutableStateOf("Plot 14, Hinjawadi Phase 1") }
    var pincode by remember { mutableStateOf("411057") }
    var latitude by remember { mutableDoubleStateOf(18.5913) }
    var longitude by remember { mutableDoubleStateOf(73.7389) }

    val onboardingResult by viewModel.onboardingResult.collectAsState()
    val isLoading = onboardingResult is Resource.Loading
    val errorMessage = (onboardingResult as? Resource.Error)?.message

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "Business Profile",
                onBackClick = onSignOut
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
            Spacer(modifier = Modifier.height(16.dp))

            com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = 0) {
                Column {
                    Text(
                        text = "Welcome to Krewzo!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KrewzoTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Complete your company profile to start hiring verified on-demand crews across Pune.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KrewzoTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = 1) {
                KrewzoCard(padding = 20.dp) {
                KrewzoTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = "Company / Business Name *",
                    placeholder = "e.g. Pune Logistics & Warehousing"
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Total Employees",
                    style = MaterialTheme.typography.bodySmall,
                    color = KrewzoTheme.colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Employee Tier Chips
                val tiers = listOf(10, 25, 50, 150)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tiers.forEach { count ->
                        val isSelected = selectedEmployeeTier == count
                        val tierBg by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else KrewzoTheme.colors.surfaceSecondary,
                            animationSpec = androidx.compose.animation.core.tween(150),
                            label = "tierBg"
                        )
                        val tierBorder by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.border,
                            animationSpec = androidx.compose.animation.core.tween(150),
                            label = "tierBorder"
                        )
                        val tierTextColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.textSecondary,
                            animationSpec = androidx.compose.animation.core.tween(150),
                            label = "tierText"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                                .background(tierBg)
                                .border(
                                    width = if (isSelected) 1.5.dp else KrewzoDimens.BorderWidth,
                                    color = tierBorder,
                                    shape = RoundedCornerShape(KrewzoDimens.RadiusMd)
                                )
                                .bouncyClickable { selectedEmployeeTier = count },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count+",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = tierTextColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Map Widget
                KrewzoMapWidget(
                    latitude = latitude,
                    longitude = longitude,
                    addressLabel = addressLine,
                    onLocationSelected = { addr, _, pin, lat, lng ->
                        addressLine = addr
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
                    value = addressLine,
                    onValueChange = { addressLine = it },
                    label = "Primary Work Site / Warehouse Address *",
                    placeholder = "e.g. Plot 24, Bhosari MIDC"
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    KrewzoTextField(
                        value = "Pune, Maharashtra",
                        onValueChange = {},
                        label = "City",
                        enabled = false,
                        modifier = Modifier.weight(1.2f)
                    )

                    Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                    KrewzoTextField(
                        value = pincode,
                        onValueChange = { pincode = it },
                        label = "Pincode",
                        placeholder = "411001",
                        modifier = Modifier.weight(0.8f)
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                KrewzoButton(
                    text = "Save & Continue",
                    onClick = {
                        viewModel.completeOnboarding(
                            businessName = businessName,
                            employeeCount = selectedEmployeeTier,
                            address = addressLine,
                            pincode = pincode,
                            latitude = latitude,
                            longitude = longitude,
                            onSuccess = onOnboardingComplete
                        )
                    },
                    isLoading = isLoading,
                    enabled = businessName.isNotBlank() && addressLine.isNotBlank()
                )
            }
        }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
