package com.binaryxbeast.krewzo.feature.admin

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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.ui.components.BottomNavItem
import com.binaryxbeast.krewzo.ui.components.KrewzoBottomNav
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onNavigateWorkers: () -> Unit,
    onNavigateAddWorker: () -> Unit,
    onNavigateClients: () -> Unit,
    onNavigateAddClient: () -> Unit,
    onNavigateBookings: () -> Unit,
    onNavigateUserRoles: () -> Unit,
    onNavigateProfile: () -> Unit = {},
    onBookingClick: (String) -> Unit = {},
    onSignOut: () -> Unit = {}
) {

    val metrics by viewModel.dashboardMetrics.collectAsState()
    val bookings by viewModel.bookings.collectAsState()
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
                    text = "Are you sure you want to sign out of the Admin Console?",
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
                    BottomNavItem.AdminDashboard,
                    BottomNavItem.AdminWorkers,
                    BottomNavItem.AdminClients,
                    BottomNavItem.AdminBookings,
                    BottomNavItem.AdminProfile
                ),
                currentRoute = BottomNavItem.AdminDashboard.route,
                onItemClick = { item ->
                    when (item) {
                        BottomNavItem.AdminWorkers -> onNavigateWorkers()
                        BottomNavItem.AdminClients -> onNavigateClients()
                        BottomNavItem.AdminBookings -> onNavigateBookings()
                        BottomNavItem.AdminProfile -> onNavigateProfile()
                        else -> {}
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Admin Console",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Operations Hub",
                            style = MaterialTheme.typography.headlineSmall,
                            color = KrewzoTheme.colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(KrewzoDimens.RadiusSm))
                                .background(KrewzoTheme.colors.infoContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                com.binaryxbeast.krewzo.ui.components.PulsingStatusDot(
                                    color = KrewzoTheme.colors.info,
                                    size = 8.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pune Market",
                                    color = KrewzoTheme.colors.info,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSignOutDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Sign Out",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Quick Create Options (+ Add Worker & + Add Client)
            item {
                val addWorkerSource = remember { MutableInteractionSource() }
                val isAddWorkerPressed by addWorkerSource.collectIsPressedAsState()
                val addWorkerScale by animateFloatAsState(
                    targetValue = if (isAddWorkerPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "addWorkerScale"
                )

                val addClientSource = remember { MutableInteractionSource() }
                val isAddClientPressed by addClientSource.collectIsPressedAsState()
                val addClientScale by animateFloatAsState(
                    targetValue = if (isAddClientPressed) 0.96f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "addClientScale"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateAddWorker,
                        interactionSource = addWorkerSource,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .graphicsLayer {
                                scaleX = addWorkerScale
                                scaleY = addWorkerScale
                            },
                        shape = RoundedCornerShape(KrewzoDimens.RadiusLg),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Worker", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onNavigateAddClient,
                        interactionSource = addClientSource,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .graphicsLayer {
                                scaleX = addClientScale
                                scaleY = addClientScale
                            },
                        shape = RoundedCornerShape(KrewzoDimens.RadiusLg),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Client", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Real-Time Dynamic Metrics Tiles
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        title = "Total Workers",
                        value = "${metrics.totalWorkers}",
                        subtitle = "${metrics.activeWorkers} active",
                        icon = Icons.Default.People,
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        title = "Total Clients",
                        value = "${metrics.totalClients}",
                        subtitle = "${metrics.verifiedClients} verified",
                        icon = Icons.Default.Business,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        title = "Bookings",
                        value = "${metrics.bookingsToday}",
                        subtitle = "${metrics.searchingBookingsCount} searching",
                        icon = Icons.AutoMirrored.Filled.Assignment,
                        modifier = Modifier.weight(1f)
                    )

                    MetricTile(
                        title = "Gross Value",
                        value = "₹${metrics.totalVolumeRupees.toInt()}",
                        subtitle = "Fill: ${metrics.fillRatePercent}%",
                        icon = Icons.Default.CurrencyRupee,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MANAGEMENT WORKFLOWS",
                    style = MaterialTheme.typography.labelSmall,
                    color = KrewzoTheme.colors.textTertiary,
                    fontWeight = FontWeight.Bold
                )
            }

            // Option 1: Manage Workers
            item {
                KrewzoCard(
                    onClick = onNavigateWorkers,
                    padding = 18.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Manage Workers (${metrics.totalWorkers})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KrewzoTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "View directory, verify skills, activate or deactivate crew",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = KrewzoTheme.colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Option 2: Manage Clients
            item {
                KrewzoCard(
                    onClick = onNavigateClients,
                    padding = 18.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(KrewzoTheme.colors.infoContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = KrewzoTheme.colors.info
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Manage Clients (${metrics.totalClients})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KrewzoTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Manage business accounts, approve KYC, update addresses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = KrewzoTheme.colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Option 3: Manage Bookings
            item {
                KrewzoCard(
                    onClick = onNavigateBookings,
                    padding = 18.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(KrewzoTheme.colors.surfaceSecondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Manage Bookings (${metrics.bookingsToday})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KrewzoTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Monitor fill rates, assign or replace crew in real time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = KrewzoTheme.colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Option 4: User Roles & Access Management
            item {
                KrewzoCard(
                    onClick = onNavigateUserRoles,
                    padding = 18.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "User Roles & Permissions",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = KrewzoTheme.colors.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Assign or modify roles (Admin, Worker, Client) by mobile number",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = KrewzoTheme.colors.textSecondary
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = KrewzoTheme.colors.textTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Live Dispatch Queue or Empty Notice
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE DISPATCH QUEUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = KrewzoTheme.colors.textTertiary,
                        fontWeight = FontWeight.Bold
                    )
                    if (bookings.isNotEmpty()) {
                        Text(
                            text = "View All (${bookings.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigateBookings() }
                        )
                    }
                }
            }

            if (bookings.isEmpty()) {
                item {
                    KrewzoCard(padding = 24.dp) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Dispatch Queue is Clear",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = KrewzoTheme.colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "When registered clients place booking requests, they will show up here for live worker dispatch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KrewzoTheme.colors.textSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(bookings.size) { index ->
                    val booking = bookings[index]
                    com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = index) {
                        AdminBookingCard(
                            booking = booking,
                            onClick = { onBookingClick(booking.id) }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    KrewzoCard(
        modifier = modifier,
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = KrewzoTheme.colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = KrewzoTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = KrewzoTheme.colors.textTertiary,
            fontWeight = FontWeight.Medium
        )
    }
}
