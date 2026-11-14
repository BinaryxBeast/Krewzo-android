package com.binaryxbeast.krewzo.feature.client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.model.AssignedWorker
import com.binaryxbeast.krewzo.core.model.BookingStatus
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoButtonVariant
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoStatusBadge
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.components.LiveOrderTimeline
import com.binaryxbeast.krewzo.ui.components.PulsingStatusDot
import com.binaryxbeast.krewzo.ui.components.RadarPulseAnimation
import com.binaryxbeast.krewzo.ui.components.RatingBadge
import com.binaryxbeast.krewzo.ui.components.StepperStep
import com.binaryxbeast.krewzo.ui.components.shimmerEffect
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

@Composable
fun BookingDetailLiveScreen(
    bookingId: String,
    viewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val bookings by viewModel.bookings.collectAsState()
    val booking = bookings.find { it.id == bookingId } ?: bookings.firstOrNull()

    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "Live Order Tracking",
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (booking == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Booking not found", color = KrewzoTheme.colors.textSecondary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))

                // ─────────────────────────────────────────────
                // 1. HERO LIVE RADAR CARD
                // ─────────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    PulsingStatusDot(
                                        color = if (booking.isFilled) KrewzoTheme.colors.ratingGreen else MaterialTheme.colorScheme.primary,
                                        size = 9.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (booking.isFilled) "CREW CONFIRMED" else "LIVE MATCHING ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (booking.isFilled) KrewzoTheme.colors.ratingGreen else MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Order #${booking.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrewzoTheme.colors.textTertiary
                                )
                            }

                            KrewzoStatusBadge(status = booking.status)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Center Radar or Check Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (booking.isFilled) KrewzoTheme.colors.ratingGreenContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (booking.isFilled) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(KrewzoTheme.colors.ratingGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "All ${booking.workersNeeded} Workers Confirmed!",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = KrewzoTheme.colors.ratingGreen
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    RadarPulseAnimation(
                                        modifier = Modifier.size(64.dp),
                                        pulseColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Radar,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Broadcasting Wave ${booking.currentWave} in Pune...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Connecting nearest verified blue-collar workers",
                                        fontSize = 11.sp,
                                        color = KrewzoTheme.colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Progress fill bar
                        val liveProgress by androidx.compose.animation.core.animateFloatAsState(
                            targetValue = booking.fillProgress,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "liveProgressFill"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${booking.workersAccepted} of ${booking.workersNeeded} Crew Confirmed",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = KrewzoTheme.colors.textPrimary
                            )
                            Text(
                                text = "${(booking.fillProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { liveProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(KrewzoDimens.RadiusFull)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 2. LIVE STEPPER TIMELINE
            // ─────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Live Order Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KrewzoTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val isSearching = booking.status == BookingStatus.SEARCHING
                        val isPartially = booking.status == BookingStatus.PARTIALLY_FILLED
                        val isConfirmed = booking.status == BookingStatus.CONFIRMED || booking.isFilled
                        val isInProgress = booking.status == BookingStatus.IN_PROGRESS
                        val isCompleted = booking.status == BookingStatus.COMPLETED

                        val steps = listOf(
                            StepperStep(
                                title = "Booking Broadcasted",
                                subtitle = "Dispatched Wave ${booking.currentWave} to workers nearby",
                                isCompleted = true,
                                isCurrent = false
                            ),
                            StepperStep(
                                title = "Worker Matching",
                                subtitle = if (isSearching) "Awaiting worker responses via Push & SMS..." else "Workers responded & accepted slots",
                                isCompleted = isPartially || isConfirmed || isInProgress || isCompleted,
                                isCurrent = isSearching
                            ),
                            StepperStep(
                                title = "Crew Confirmed",
                                subtitle = "${booking.workersAccepted} of ${booking.workersNeeded} workers confirmed for duty",
                                isCompleted = isConfirmed || isInProgress || isCompleted,
                                isCurrent = isPartially
                            ),
                            StepperStep(
                                title = "Shift Execution",
                                subtitle = "Work begins at ${booking.workStartTime} (${booking.workDate})",
                                isCompleted = isCompleted,
                                isCurrent = isInProgress
                            )
                        )

                        LiveOrderTimeline(steps = steps)
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 3. CONFIRMED CREW WORKER CARDS
            // ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Confirmed Crew Members (${booking.assignedWorkers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KrewzoTheme.colors.textPrimary
                    )

                    if (booking.assignedWorkers.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = KrewzoTheme.colors.ratingGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Verified",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KrewzoTheme.colors.ratingGreen
                            )
                        }
                    }
                }
            }

            if (booking.assignedWorkers.isEmpty()) {
                item {
                    KrewzoCard(padding = 20.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Skeleton shimmer cards indicating waiting state
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .shimmerEffect(12.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(20.dp)
                                    .shimmerEffect(8.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Waiting for nearest workers to accept...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = KrewzoTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Push notifications & SMS sent to workers within 20 km",
                                style = MaterialTheme.typography.bodySmall,
                                color = KrewzoTheme.colors.textTertiary
                            )
                        }
                    }
                }
            } else {
                items(booking.assignedWorkers) { worker ->
                    WorkerAssignmentCard(worker = worker)
                }
            }

            // ─────────────────────────────────────────────
            // 4. BOOKING SPECIFICATIONS
            // ─────────────────────────────────────────────
            item {
                Text(
                    text = "Booking Specifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = KrewzoTheme.colors.textPrimary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        DetailItemRow(
                            icon = Icons.Default.LocationOn,
                            label = "Work Address",
                            value = booking.workAddress
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        DetailItemRow(
                            icon = Icons.Default.CalendarToday,
                            label = "Work Schedule",
                            value = "${booking.workDate} · ${booking.workStartTime}"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        DetailItemRow(
                            icon = Icons.AutoMirrored.Filled.Notes,
                            label = "Job Instructions",
                            value = booking.jobNotes ?: "Standard labor requirements"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(KrewzoTheme.colors.divider)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Billing Amount",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                                Text(
                                    text = "₹${booking.payPerWorker.toInt()} × ${booking.workersNeeded} workers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrewzoTheme.colors.textTertiary
                                )
                            }
                            Text(
                                text = "₹${booking.totalCost.toInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = KrewzoTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 5. HELPLINE & CANCEL ACTIONS
            // ─────────────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { /* Helpline click */ },
                    shape = RoundedCornerShape(14.dp),
                    color = KrewzoTheme.colors.surfaceSecondary.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "24/7 Operations Support",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = KrewzoTheme.colors.textPrimary
                                )
                                Text(
                                    text = "Need help with crew arrival or replacements?",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                            }
                        }
                        Text(
                            text = "Call",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (booking.status in listOf(BookingStatus.SEARCHING, BookingStatus.PARTIALLY_FILLED, BookingStatus.CONFIRMED)) {
                item {
                    KrewzoButton(
                        text = "Cancel Booking",
                        onClick = { showCancelDialog = true },
                        variant = KrewzoButtonVariant.DANGER
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (showCancelDialog && booking != null) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = {
                Text(
                    text = "Cancel this booking?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Pending offers sent to nearby workers will be immediately revoked.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelBooking(booking.id)
                        showCancelDialog = false
                    }
                ) {
                    Text("Yes, Cancel", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Booking", color = KrewzoTheme.colors.textSecondary)
                }
            },
            shape = RoundedCornerShape(KrewzoDimens.RadiusXl)
        )
    }
}

@Composable
fun WorkerAssignmentCard(worker: AssignedWorker) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "workerCardScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar with verified badge
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = worker.fullName.take(2).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 15.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(KrewzoTheme.colors.ratingGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Verified",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = worker.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        color = KrewzoTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RatingBadge(rating = worker.rating ?: 4.8)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "· ${worker.assignedAt}",
                            fontSize = 11.sp,
                            color = KrewzoTheme.colors.textTertiary
                        )
                    }
                }
            }

            // Direct Call Phone Action Button
            IconButton(
                onClick = { /* Android Call Intent */ },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(KrewzoTheme.colors.ratingGreenContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call Worker",
                    tint = KrewzoTheme.colors.ratingGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailItemRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KrewzoTheme.colors.surfaceSecondary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = KrewzoTheme.colors.textTertiary,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = KrewzoTheme.colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
