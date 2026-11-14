package com.binaryxbeast.krewzo.feature.client

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.components.QuickCategoryCard
import com.binaryxbeast.krewzo.ui.components.bouncyClickable
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

private data class CategoryOption(
    val title: String,
    val icon: ImageVector,
    val defaultNotes: String
)

@Composable
fun CreateBookingScreen(
    viewModel: ClientViewModel,
    onBookingCreated: (String) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }

    val categories = remember {
        listOf(
            CategoryOption("Loading", Icons.Default.LocalShipping, "Loading & unloading truck inventory. Heavy lifting required."),
            CategoryOption("Warehouse", Icons.Default.Inventory2, "Warehouse sorting, picking, scanning and pallet movement."),
            CategoryOption("Cleaning", Icons.Default.CleaningServices, "Deep cleaning and floor sanitization for workplace."),
            CategoryOption("Security", Icons.Default.Security, "Gate entry security and visitor surveillance shift."),
            CategoryOption("Hospitality", Icons.Default.Groups, "Event guest management, table service, and buffet assistance."),
            CategoryOption("Delivery", Icons.Default.TwoWheeler, "Intra-city package pickup and delivery tasks.")
        )
    }

    var workersNeeded by remember { mutableIntStateOf(4) }
    var workDate by remember { mutableStateOf("2026-11-06") }
    var workStartTime by remember { mutableStateOf("09:00 AM") }
    var workAddress by remember { mutableStateOf("Plot 14, Hinjawadi Phase 1, Pune") }
    var payPerWorkerText by remember { mutableStateOf("850") }
    var jobNotes by remember { mutableStateOf(categories[0].defaultNotes) }

    val createBookingState by viewModel.createBookingState.collectAsState()
    val isLoading = createBookingState is Resource.Loading
    val errorMessage = (createBookingState as? Resource.Error)?.message

    val payPerWorker = payPerWorkerText.toDoubleOrNull() ?: 0.0
    val totalCost = workersNeeded * payPerWorker

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "Create Crew Booking",
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
            Spacer(modifier = Modifier.height(12.dp))

            // ─────────────────────────────────────────────
            // 1. CATEGORY SELECTOR CHIPS
            // ─────────────────────────────────────────────
            Text(
                text = "Select Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories.size) { index ->
                    val cat = categories[index]
                    QuickCategoryCard(
                        title = cat.title,
                        icon = cat.icon,
                        containerColor = KrewzoTheme.colors.surfaceSecondary,
                        iconTint = MaterialTheme.colorScheme.primary,
                        isSelected = selectedCategoryIndex == index,
                        onClick = {
                            selectedCategoryIndex = index
                            jobNotes = cat.defaultNotes
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─────────────────────────────────────────────
            // 2. WORKERS COUNT STEPPER CARD
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Number of Workers Needed",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KrewzoTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AnimatedContent(
                            targetState = workersNeeded,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> -height } + fadeOut())
                                } else {
                                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> height } + fadeOut())
                                }
                            },
                            label = "workerCountStepperAnim"
                        ) { count ->
                            Text(
                                text = "$count Verified Workers",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Decrement
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(KrewzoTheme.colors.surfaceSecondary)
                                    .bouncyClickable { if (workersNeeded > 1) workersNeeded-- },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = KrewzoTheme.colors.textPrimary, modifier = Modifier.size(18.dp))
                            }

                            // Increment
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .bouncyClickable { if (workersNeeded < 100) workersNeeded++ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─────────────────────────────────────────────
            // 3. SCHEDULE & LOCATION DETAILS
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Schedule & Workplace",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KrewzoTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        KrewzoTextField(
                            value = workDate,
                            onValueChange = { workDate = it },
                            label = "Work Date",
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        KrewzoTextField(
                            value = workStartTime,
                            onValueChange = { workStartTime = it },
                            label = "Reporting Time",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    KrewzoTextField(
                        value = workAddress,
                        onValueChange = { workAddress = it },
                        label = "Work Address (Pune)",
                        placeholder = "e.g., Plot 14, Hinjawadi Phase 1, Pune"
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─────────────────────────────────────────────
            // 4. COMPENSATION & BILL BREAKDOWN
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Worker Compensation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KrewzoTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    KrewzoTextField(
                        value = payPerWorkerText,
                        onValueChange = { payPerWorkerText = it },
                        label = "Pay per Worker (₹ / day)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Quick Rate Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("750", "850", "1000", "1200").forEach { rate ->
                            val isSelected = payPerWorkerText == rate
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else KrewzoTheme.colors.surfaceSecondary)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { payPerWorkerText = rate }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "₹$rate",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Delivery App-style Bill Details Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = KrewzoTheme.colors.surfaceSecondary.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Estimated Bill Breakdown",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = KrewzoTheme.colors.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Crew Pay ($workersNeeded × ₹${payPerWorker.toInt()})",
                                    fontSize = 12.sp,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                                Text(
                                    text = "₹${totalCost.toInt()}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = KrewzoTheme.colors.textPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Platform Matching Fee",
                                    fontSize = 12.sp,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                                Text(
                                    text = "FREE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KrewzoTheme.colors.ratingGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(KrewzoTheme.colors.border)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "To Pay On Completion",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = KrewzoTheme.colors.textPrimary
                                    )
                                    Text(
                                        text = "Zero advance required",
                                        fontSize = 10.sp,
                                        color = KrewzoTheme.colors.ratingGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = "₹${totalCost.toInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─────────────────────────────────────────────
            // 5. JOB INSTRUCTIONS
            // ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    KrewzoTextField(
                        value = jobNotes,
                        onValueChange = { jobNotes = it },
                        label = "Job Instructions / Special Requirements",
                        placeholder = "e.g., Safety boots required, inventory loading shift",
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary CTA
            KrewzoButton(
                text = "Broadcast Live Wave 1",
                onClick = {
                    viewModel.createBooking(
                        workersNeeded = workersNeeded,
                        workDate = workDate,
                        workStartTime = workStartTime,
                        workEndTime = null,
                        workAddress = workAddress,
                        payPerWorker = payPerWorker,
                        jobNotes = jobNotes,
                        onSuccess = onBookingCreated
                    )
                },
                isLoading = isLoading,
                enabled = payPerWorker > 0 && workAddress.isNotBlank()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
