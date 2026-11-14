package com.binaryxbeast.krewzo.feature.admin

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.model.BookingStatus
import com.binaryxbeast.krewzo.core.model.Worker
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoStatusBadge
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.components.LiveOrderTimeline
import com.binaryxbeast.krewzo.ui.components.PulsingStatusDot
import com.binaryxbeast.krewzo.ui.components.RadarPulseAnimation
import com.binaryxbeast.krewzo.ui.components.RatingBadge
import com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem
import com.binaryxbeast.krewzo.ui.components.bouncyClickable
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

@Composable
fun AdminBookingDetailScreen(
    bookingId: String,
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val bookings by viewModel.bookings.collectAsState()
    val workers by viewModel.workers.collectAsState()
    val booking = bookings.find { it.id == bookingId } ?: bookings.firstOrNull()

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "Booking #${booking?.id ?: ""}",
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (booking == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Booking not found", color = KrewzoTheme.colors.textSecondary)
            }
            return@Scaffold
        }

        val animatedProgress by animateFloatAsState(
            targetValue = booking.fillProgress,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "fillProgress"
        )

        val activeWorkers = workers.filter { it.isActive }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Booking Card
            item {
                Spacer(modifier = Modifier.height(4.dp))
                StaggeredAnimatedItem(index = 0) {
                    KrewzoCard(padding = 20.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = booking.clientName ?: "Client",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = KrewzoTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = KrewzoTheme.colors.textTertiary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${booking.workDate} · ${booking.workStartTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KrewzoTheme.colors.textSecondary
                                    )
                                }
                            }
                            KrewzoStatusBadge(status = booking.status)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Fill Progress Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Fill Status: ${booking.workersAccepted} / ${booking.workersNeeded} workers confirmed",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(booking.fillProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(KrewzoDimens.RadiusFull)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primaryContainer
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Details Breakdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Pay Rate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KrewzoTheme.colors.textTertiary
                                )
                                Text(
                                    text = "₹${booking.payPerWorker.toInt()} / worker",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KrewzoTheme.colors.textPrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Contract",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KrewzoTheme.colors.textTertiary
                                )
                                Text(
                                    text = "₹${booking.totalCost.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (!booking.workAddress.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = booking.workAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Live Order / Dispatch Timeline
            item {
                StaggeredAnimatedItem(index = 1) {
                    KrewzoCard(padding = 18.dp) {
                        Text(
                            text = "DISPATCH & EXECUTION TIMELINE",
                            style = MaterialTheme.typography.labelSmall,
                            color = KrewzoTheme.colors.textTertiary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LiveOrderTimeline(currentStatus = booking.status)
                    }
                }
            }

            // Radar broadcast scanner if active
            if (booking.status == BookingStatus.SEARCHING) {
                item {
                    StaggeredAnimatedItem(index = 2) {
                        KrewzoCard(padding = 20.dp) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                RadarPulseAnimation(
                                    size = 90.dp,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Live Wave ${booking.currentWave} Broadcast",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = KrewzoTheme.colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Broadcasting flash gig offers to verified crew across Pune radius...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrewzoTheme.colors.textSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANUALLY ASSIGN WORKERS (${activeWorkers.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = KrewzoTheme.colors.textTertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Available Workers who can be assigned manually
            itemsIndexed(activeWorkers) { index, worker ->
                StaggeredAnimatedItem(index = index + 3) {
                    ManualAssignWorkerRow(
                        worker = worker,
                        onAssign = {
                            viewModel.manuallyAssignWorker(booking.id, worker.id)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ManualAssignWorkerRow(
    worker: Worker,
    onAssign: () -> Unit
) {
    KrewzoCard(padding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = worker.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            color = KrewzoTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RatingBadge(rating = worker.rating ?: 5.0)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${worker.homeAddress} · ${worker.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = KrewzoTheme.colors.textTertiary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAssign,
                shape = RoundedCornerShape(KrewzoDimens.RadiusMd),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.bouncyClickable(onClick = onAssign)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Assign", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

