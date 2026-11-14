package com.binaryxbeast.krewzo.feature.client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TwoWheeler
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
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.model.Booking
import com.binaryxbeast.krewzo.core.model.BookingStatus
import com.binaryxbeast.krewzo.ui.components.BottomNavItem
import com.binaryxbeast.krewzo.ui.components.KrewzoBottomNav
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoStatusBadge
import com.binaryxbeast.krewzo.ui.components.PulsingStatusDot
import com.binaryxbeast.krewzo.ui.components.QuickCategoryCard
import com.binaryxbeast.krewzo.ui.components.RadarPulseAnimation
import com.binaryxbeast.krewzo.ui.components.RatingBadge
import com.binaryxbeast.krewzo.ui.components.SearchTicker
import com.binaryxbeast.krewzo.ui.theme.ElectricIndigo
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

private data class LaborCategory(
    val title: String,
    val icon: ImageVector,
    val containerColor: Color,
    val iconTint: Color
)

@Composable
fun ClientHomeScreen(
    viewModel: ClientViewModel,
    onCreateBookingClick: () -> Unit,
    onBookingClick: (String) -> Unit,
    onNavigateProfile: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val bookings by viewModel.bookings.collectAsState()
    val clientProfile by viewModel.clientProfile.collectAsState()
    val initials = remember(clientProfile) {
        val name = clientProfile?.contactPerson?.ifBlank { clientProfile?.businessName } ?: "KZ"
        val parts = name.trim().split(" ")
        if (parts.size >= 2) "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
        else name.take(2).uppercase()
    }
    val activeBooking = bookings.find {
        it.status in listOf(
            BookingStatus.SEARCHING,
            BookingStatus.PARTIALLY_FILLED,
            BookingStatus.CONFIRMED,
            BookingStatus.IN_PROGRESS
        )
    }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var selectedFilterTab by remember { mutableIntStateOf(0) }
    var showSignOutDialog by remember { mutableStateOf(false) }

    val categories = remember {
        listOf(
            LaborCategory("All Crew", Icons.Default.Groups, Color(0xFFEEECFF), ElectricIndigo),
            LaborCategory("Loading", Icons.Default.LocalShipping, Color(0xFFEEECFF), ElectricIndigo),
            LaborCategory("Warehouse", Icons.Default.Inventory2, Color(0xFFEFF6FF), Color(0xFF2563EB)),
            LaborCategory("Cleaning", Icons.Default.CleaningServices, Color(0xFFF0FDF4), Color(0xFF16A34A)),
            LaborCategory("Security", Icons.Default.Security, Color(0xFFFFFBEB), Color(0xFFD97706)),
            LaborCategory("Hospitality", Icons.Default.Groups, Color(0xFFFAF5FF), Color(0xFF9333EA)),
            LaborCategory("Delivery", Icons.Default.TwoWheeler, Color(0xFFFFF1F2), Color(0xFFE11D48))
        )
    }

    val searchHints = remember {
        listOf(
            "Search 'loaders in Hinjawadi'...",
            "Search 'warehouse inventory staff'...",
            "Search 'night shift security guards'...",
            "Search 'cleaning & sanitizing crew'...",
            "Search 'event hospitality team'...",
            "Search 'urgent packaging workers'..."
        )
    }

    val displayedBookings = remember(bookings, activeBooking, selectedFilterTab) {
        val nonActive = bookings.filter { it.id != activeBooking?.id }
        when (selectedFilterTab) {
            1 -> nonActive.filter { it.status in listOf(BookingStatus.SEARCHING, BookingStatus.PARTIALLY_FILLED, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS) }
            2 -> nonActive.filter { it.status == BookingStatus.COMPLETED }
            else -> nonActive
        }
    }

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
                    text = "Are you sure you want to sign out from your Krewzo account?",
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
                    BottomNavItem.ClientHome,
                    BottomNavItem.CreateBooking,
                    BottomNavItem.ClientHistory,
                    BottomNavItem.ClientProfile
                ),
                currentRoute = BottomNavItem.ClientHome.route,
                onItemClick = { item ->
                    when (item) {
                        BottomNavItem.CreateBooking -> onCreateBookingClick()
                        BottomNavItem.ClientProfile -> onNavigateProfile()
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
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─────────────────────────────────────────────
            // 1. TOP HEADER & LOCATION BAR
            // ─────────────────────────────────────────────
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Location Header with drop down
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { /* location picker modal */ }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Workplace",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = clientProfile?.businessName?.ifBlank { "Workplace" } ?: "Workplace",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = KrewzoTheme.colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Select Location",
                                            tint = KrewzoTheme.colors.textPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = clientProfile?.address?.ifBlank { "Plot 14, Hinjawadi Phase 1, Pune" } ?: "Plot 14, Hinjawadi Phase 1, Pune",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KrewzoTheme.colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Profile & Actions
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Notification button with active dot
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(KrewzoTheme.colors.surfaceSecondary)
                                        .clickable { },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = KrewzoTheme.colors.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 8.dp, end = 8.dp)
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }

                                // Avatar badge
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
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
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }

                                // Sign out
                                IconButton(
                                    onClick = { showSignOutDialog = true },
                                    modifier = Modifier.size(34.dp)
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // ─────────────────────────────────────────────
                        // ANIMATED SEARCH & DISCOVERY BAR
                        // ─────────────────────────────────────────────
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(2.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                KrewzoTheme.colors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onCreateBookingClick() }
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                SearchTicker(
                                    hints = searchHints,
                                    modifier = Modifier.weight(1f)
                                )

                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(20.dp)
                                        .background(KrewzoTheme.colors.divider)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 2. QUICK CATEGORIES CAROUSEL
            // ─────────────────────────────────────────────
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hire Crew By Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KrewzoTheme.colors.textPrimary
                        )
                        Text(
                            text = "See All",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onCreateBookingClick() }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(categories.size) { index ->
                            val cat = categories[index]
                            QuickCategoryCard(
                                title = cat.title,
                                icon = cat.icon,
                                containerColor = cat.containerColor,
                                iconTint = cat.iconTint,
                                isSelected = selectedCategoryIndex == index,
                                onClick = {
                                    selectedCategoryIndex = index
                                    onCreateBookingClick()
                                }
                            )
                        }
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 3. FEATURE HIGHLIGHTS & VALUE PROPS CAROUSEL
            // ─────────────────────────────────────────────
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Instant Fill
                    item {
                        PromoHeroCard(
                            tag = "⚡ 15-MIN FILL",
                            title = "Instant Blue-Collar Crew",
                            subtitle = "Broadcast to nearest verified workers in Pune",
                            gradient = Brush.horizontalGradient(
                                listOf(
                                    ElectricIndigo,
                                    Color(0xFF6B78FF),
                                    Color(0xFF8B96FF)
                                )
                            ),
                            actionText = "Post Booking",
                            onClick = onCreateBookingClick
                        )
                    }

                    // Card 2: 100% Verified
                    item {
                        PromoHeroCard(
                            tag = "🛡️ POLICE & AADHAAR VERIFIED",
                            title = "Trusted Workforce",
                            subtitle = "Background-checked & skill-tested verified crew",
                            gradient = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1E2230),
                                    Color(0xFF2C3246),
                                    Color(0xFF3E4660)
                                )
                            ),
                            actionText = "Learn More",
                            onClick = onCreateBookingClick
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 4. ACTIVE LIVE BOOKING RADAR CARD (HERO)
            // ─────────────────────────────────────────────
            if (activeBooking != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PulsingStatusDot(color = MaterialTheme.colorScheme.primary, size = 9.dp)
                            Text(
                                text = "LIVE CREW BROADCAST",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        ActiveBookingLiveHeroCard(
                            booking = activeBooking,
                            onClick = { onBookingClick(activeBooking.id) }
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────
            // 5. QUICK VALUE PROPOSITION PILLS
            // ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ValuePill(icon = "⚡", title = "15m Match", subtitle = "Avg fill time")
                    ValuePill(icon = "⭐", title = "4.8 / 5", subtitle = "Crew rating")
                    ValuePill(icon = "🛡️", title = "Zero Advance", subtitle = "Pay on finish")
                    ValuePill(icon = "📍", title = "Pune", subtitle = "Citywide pool")
                }
            }

            // ─────────────────────────────────────────────
            // 6. BOOKING HISTORY & RECENT ORDERS
            // ─────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Bookings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KrewzoTheme.colors.textPrimary
                        )

                        // Filter Pill Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterTabChip("All", selected = selectedFilterTab == 0) { selectedFilterTab = 0 }
                            FilterTabChip("Active", selected = selectedFilterTab == 1) { selectedFilterTab = 1 }
                            FilterTabChip("Completed", selected = selectedFilterTab == 2) { selectedFilterTab = 2 }
                        }
                    }
                }
            }

            if (displayedBookings.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No bookings found in this view",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KrewzoTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onCreateBookingClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Create Your First Booking", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(displayedBookings.size) { index ->
                    val booking = displayedBookings[index]
                    com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(
                        index = index,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        BookingExpressCard(
                            booking = booking,
                            onClick = { onBookingClick(booking.id) },
                            onRebook = { onCreateBookingClick() }
                        )
                    }
                }
            }
        }
    }
}

/**
 * High-impact hero card for promotional banners.
 */
@Composable
private fun PromoHeroCard(
    tag: String,
    title: String,
    subtitle: String,
    gradient: Brush,
    actionText: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "promoHeroScale"
    )

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(140.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = Color.White),
                onClick = onClick
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tag,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = actionText,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Live Active Booking Card with embedded radar pulse, live worker count, and tracking CTA.
 */
@Composable
private fun ActiveBookingLiveHeroCard(
    booking: Booking,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "activeCardHeroScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadarPulseAnimation(
                        modifier = Modifier.size(34.dp),
                        pulseColor = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "Wave ${booking.currentWave} Live Dispatch",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Finding crew near Hinjawadi",
                            style = MaterialTheme.typography.bodySmall,
                            color = KrewzoTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                KrewzoStatusBadge(status = booking.status)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = booking.fillProgress,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "activeHeroProgress"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${booking.workersAccepted} of ${booking.workersNeeded} Workers Confirmed",
                    style = MaterialTheme.typography.bodySmall,
                    color = KrewzoTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold
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
                    .height(7.dp)
                    .clip(RoundedCornerShape(KrewzoDimens.RadiusFull)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Scheduled Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = KrewzoTheme.colors.textTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${booking.workDate} · ${booking.workStartTime}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = KrewzoTheme.colors.textPrimary
                    )
                }

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Track Live", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modern Express Card for Booking History.
 */
@Composable
private fun BookingExpressCard(
    booking: Booking,
    onClick: () -> Unit,
    onRebook: () -> Unit
) {
    KrewzoCard(
        onClick = onClick,
        padding = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(KrewzoTheme.colors.surfaceSecondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${booking.workersNeeded} Blue-Collar Crew",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = KrewzoTheme.colors.textPrimary
                    )
                    Text(
                        text = "${booking.workDate} · ${booking.workStartTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = KrewzoTheme.colors.textTertiary,
                        fontSize = 12.sp
                    )
                }
            }

            KrewzoStatusBadge(status = booking.status)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = KrewzoTheme.colors.textTertiary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = booking.workAddress,
                style = MaterialTheme.typography.bodySmall,
                color = KrewzoTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(KrewzoTheme.colors.divider)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RatingBadge(rating = 4.8, reviewCount = 28)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Total ₹${booking.totalCost.toInt()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = KrewzoTheme.colors.textPrimary
                )
            }

            OutlinedButton(
                onClick = onRebook,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Rebook", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Filter Chip with animated active pill.
 */
@Composable
private fun FilterTabChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.surfaceSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "filterChipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else KrewzoTheme.colors.textSecondary,
        animationSpec = tween(durationMillis = 200),
        label = "filterChipText"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Tiny value proposition pill.
 */
@Composable
private fun ValuePill(
    icon: String,
    title: String,
    subtitle: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KrewzoTheme.colors.surfaceSecondary.copy(alpha = 0.7f))
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .width(76.dp)
    ) {
        Text(text = icon, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = KrewzoTheme.colors.textPrimary,
            maxLines = 1
        )
        Text(
            text = subtitle,
            fontSize = 9.sp,
            color = KrewzoTheme.colors.textTertiary,
            maxLines = 1
        )
    }
}
