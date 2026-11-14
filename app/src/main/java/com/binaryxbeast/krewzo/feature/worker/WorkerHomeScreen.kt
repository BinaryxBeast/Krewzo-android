package com.binaryxbeast.krewzo.feature.worker

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.model.JobOffer
import com.binaryxbeast.krewzo.core.model.OfferStatus
import com.binaryxbeast.krewzo.ui.components.BottomNavItem
import com.binaryxbeast.krewzo.ui.components.KrewzoBottomNav
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.PulsingStatusDot
import com.binaryxbeast.krewzo.ui.components.RatingBadge
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

@Composable
fun WorkerHomeScreen(
    viewModel: WorkerViewModel,
    onOfferClick: (String) -> Unit,
    onNavigateProfile: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val isAvailable by viewModel.isAvailable.collectAsState()
    val offers by viewModel.incomingOffers.collectAsState()
    val workerProfile by viewModel.workerProfile.collectAsState()
    val displayName = workerProfile?.fullName?.ifBlank { "Krewzo Partner" } ?: "Krewzo Partner"
    val initials = remember(workerProfile) {
        val name = workerProfile?.fullName ?: "KP"
        val parts = name.trim().split(" ")
        if (parts.size >= 2) "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
        else name.take(2).uppercase()
    }
    val pendingOffers = offers.filter { it.status == OfferStatus.PENDING }
    var showSignOutDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to go offline and stop receiving job offers?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KrewzoTheme.colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        onSignOut()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(KrewzoDimens.RadiusMd)
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
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
        bottomBar = {
            KrewzoBottomNav(
                items = listOf(
                    BottomNavItem.WorkerHome,
                    BottomNavItem.WorkerSchedule,
                    BottomNavItem.WorkerEarnings,
                    BottomNavItem.WorkerProfile
                ),
                currentRoute = BottomNavItem.WorkerHome.route,
                onItemClick = { item ->
                    if (item == BottomNavItem.WorkerProfile) {
                        onNavigateProfile()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))

                // ─────────────────────────────────────────────
                // 1. TOP HEADER
                // ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            Color(0xFF7582FF)
                                        )
                                    )
                                )
                                .clickable { onNavigateProfile() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Krewzo Partner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                color = KrewzoTheme.colors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RatingBadge(rating = 4.9, reviewCount = 84)

                        IconButton(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(KrewzoTheme.colors.surfaceSecondary)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = KrewzoTheme.colors.textTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 2. HERO ONLINE / OFFLINE AVAILABILITY CARD
            // ─────────────────────────────────────────────
            item {
                val bannerBg by animateColorAsState(
                    targetValue = if (isAvailable) KrewzoTheme.colors.ratingGreenContainer.copy(alpha = 0.8f) else KrewzoTheme.colors.surfaceSecondary,
                    animationSpec = tween(300),
                    label = "workerStatusBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isAvailable) KrewzoTheme.colors.ratingGreen.copy(alpha = 0.5f) else KrewzoTheme.colors.border,
                    animationSpec = tween(300),
                    label = "workerBorderColor"
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = bannerBg),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isAvailable) 4.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PulsingStatusDot(
                                color = if (isAvailable) KrewzoTheme.colors.ratingGreen else KrewzoTheme.colors.textTertiary,
                                size = 10.dp
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = if (isAvailable) "You Are Online" else "You Are Offline",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isAvailable) KrewzoTheme.colors.ratingGreen else KrewzoTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isAvailable) "Receiving gig offers in Pune" else "Go online to receive live offers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isAvailable,
                            onCheckedChange = { viewModel.toggleAvailability(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = KrewzoTheme.colors.ratingGreen,
                                uncheckedThumbColor = KrewzoTheme.colors.textSecondary,
                                uncheckedTrackColor = KrewzoTheme.colors.border
                            )
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 3. DAILY PERFORMANCE METRIC STRIP
            // ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkerStatCard(
                        title = "Today's Payout",
                        value = "₹1,700",
                        subtitle = "2 gigs done",
                        modifier = Modifier.weight(1f)
                    )
                    WorkerStatCard(
                        title = "Acceptance",
                        value = "96%",
                        subtitle = "High demand",
                        modifier = Modifier.weight(1f)
                    )
                    WorkerStatCard(
                        title = "Jobs Done",
                        value = "142",
                        subtitle = "Lifetime",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ─────────────────────────────────────────────
            // 4. INCOMING FLASH GIG OFFERS
            // ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Incoming Job Offers",
                            style = MaterialTheme.typography.titleMedium,
                            color = KrewzoTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${pendingOffers.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (pendingOffers.isEmpty()) {
                item {
                    KrewzoCard(padding = 28.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (isAvailable) {
                                com.binaryxbeast.krewzo.ui.components.RadarPulseAnimation(
                                    size = 72.dp,
                                    tint = KrewzoTheme.colors.ratingGreen
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Scanning Pune market for active gigs...",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KrewzoTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(KrewzoTheme.colors.surfaceSecondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkHistory,
                                        contentDescription = null,
                                        tint = KrewzoTheme.colors.textTertiary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "You are currently Offline",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KrewzoTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isAvailable)
                                    "Your profile is active in the live dispatch queue. When nearby businesses broadcast requests, offers appear here immediately."
                                else
                                    "Turn availability switch ON above to receive priority booking notifications across Pune.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KrewzoTheme.colors.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(pendingOffers.size) { index ->
                    val offer = pendingOffers[index]
                    com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = index) {
                        FlashJobOfferCard(
                            offer = offer,
                            onAccept = { viewModel.acceptOffer(offer.id) },
                            onDecline = { viewModel.declineOffer(offer.id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/**
 * Metric card for worker daily summary.
 */
@Composable
private fun WorkerStatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = KrewzoTheme.colors.textTertiary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = KrewzoTheme.colors.ratingGreen,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * Flash urgent incoming gig offer card with pulsing ruby border and quick response buttons.
 */
@Composable
fun FlashJobOfferCard(
    offer: JobOffer,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "flashCardPulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashBorderAlpha"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Price & Distance Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "₹${offer.payAmount.toInt()}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(KrewzoTheme.colors.ratingGreenContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "INSTANT PAY",
                                color = KrewzoTheme.colors.ratingGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Full Day Shift Compensation",
                        style = MaterialTheme.typography.bodySmall,
                        color = KrewzoTheme.colors.textTertiary,
                        fontSize = 11.sp
                    )
                }

                // Proximity Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = KrewzoTheme.colors.surfaceSecondary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${offer.distanceKm} km away",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = KrewzoTheme.colors.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Location
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = KrewzoTheme.colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = offer.workAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KrewzoTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = KrewzoTheme.colors.textTertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${offer.workDate} · ${offer.workStartTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KrewzoTheme.colors.textSecondary
                )
            }

            if (offer.jobNotes != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = offer.jobNotes,
                    style = MaterialTheme.typography.bodySmall,
                    color = KrewzoTheme.colors.textTertiary,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Expiry countdown alert bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = KrewzoTheme.colors.warningContainer.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.warning.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱ Auto-expires soon: Respond within 10 minutes",
                        fontSize = 12.sp,
                        color = KrewzoTheme.colors.warning,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Decline & Accept
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDecline,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KrewzoTheme.colors.textSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KrewzoTheme.colors.border)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Decline", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Decline", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KrewzoTheme.colors.ratingGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Accept Job ✓", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
