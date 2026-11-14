package com.binaryxbeast.krewzo.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.model.Booking
import com.binaryxbeast.krewzo.core.model.BookingStatus
import com.binaryxbeast.krewzo.ui.components.BottomNavItem
import com.binaryxbeast.krewzo.ui.components.KrewzoBottomNav
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoStatusBadge
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.components.bouncyClickable
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

@Composable
fun AdminBookingsScreen(
    viewModel: AdminViewModel,
    onBookingClick: (String) -> Unit,
    onNavigateDashboard: () -> Unit = {},
    onNavigateWorkers: () -> Unit = {},
    onNavigateClients: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val bookings by viewModel.bookings.collectAsState()
    var selectedFilter by remember { mutableStateOf<BookingStatus?>(null) }

    val filteredBookings = if (selectedFilter == null) {
        bookings
    } else {
        bookings.filter { it.status == selectedFilter }
    }

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "Bookings Management"
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
                currentRoute = BottomNavItem.AdminBookings.route,
                onItemClick = { item ->
                    when (item) {
                        BottomNavItem.AdminDashboard -> onNavigateDashboard()
                        BottomNavItem.AdminWorkers -> onNavigateWorkers()
                        BottomNavItem.AdminClients -> onNavigateClients()
                        BottomNavItem.AdminProfile -> onNavigateProfile()
                        else -> {}
                    }
                }
            )
        },

        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        label = "All (${bookings.size})",
                        isSelected = selectedFilter == null,
                        onClick = { selectedFilter = null }
                    )
                }
                items(listOf(BookingStatus.SEARCHING, BookingStatus.PARTIALLY_FILLED, BookingStatus.CONFIRMED, BookingStatus.COMPLETED)) { status ->
                    FilterChip(
                        label = "${status.label} (${bookings.count { it.status == status }})",
                        isSelected = selectedFilter == status,
                        onClick = { selectedFilter = status }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BOOKINGS (${filteredBookings.size})",
                style = MaterialTheme.typography.labelSmall,
                color = KrewzoTheme.colors.textTertiary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredBookings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedFilter == null) "No bookings yet" else "No ${selectedFilter?.label} bookings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KrewzoTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "New bookings created by clients will appear here for crew dispatch",
                            style = MaterialTheme.typography.bodySmall,
                            color = KrewzoTheme.colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredBookings.size) { index ->
                        val booking = filteredBookings[index]
                        com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = index) {
                            AdminBookingCard(
                                booking = booking,
                                onClick = { onBookingClick(booking.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AdminBookingCard(
    booking: Booking,
    onClick: () -> Unit
) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = booking.fillProgress,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "adminBookingProgress"
    )

    KrewzoCard(
        onClick = onClick,
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = booking.clientName ?: "Client Booking",
                    style = MaterialTheme.typography.titleMedium,
                    color = KrewzoTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: #${booking.id} · Wave ${booking.currentWave} Broadcast",
                    style = MaterialTheme.typography.bodySmall,
                    color = KrewzoTheme.colors.textTertiary
                )
            }

            KrewzoStatusBadge(status = booking.status)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${booking.workersAccepted} of ${booking.workersNeeded} workers confirmed",
                style = MaterialTheme.typography.bodySmall,
                color = KrewzoTheme.colors.textSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(booking.fillProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        androidx.compose.material3.LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(KrewzoDimens.RadiusFull)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📍 ${booking.workAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = KrewzoTheme.colors.textSecondary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Total ₹${booking.totalCost.toInt()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = KrewzoTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipBg by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.surfaceSecondary,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "chipBg"
    )
    val chipTextColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else KrewzoTheme.colors.textSecondary,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "chipTextColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(KrewzoDimens.RadiusFull))
            .background(chipBg)
            .bouncyClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = chipTextColor
        )
    }
}
