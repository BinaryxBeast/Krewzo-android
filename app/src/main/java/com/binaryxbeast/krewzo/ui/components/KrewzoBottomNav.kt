package com.binaryxbeast.krewzo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.navigation.Screen
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
) {
    // Client tabs
    data object ClientHome : BottomNavItem(Screen.ClientHome.route, "Home", Icons.Outlined.Home, Icons.Filled.Home)
    data object CreateBooking : BottomNavItem(Screen.CreateBooking.route, "Book", Icons.Outlined.AddCircle, Icons.Filled.AddCircle)
    data object ClientHistory : BottomNavItem(Screen.ClientHistory.route, "History", Icons.Outlined.History, Icons.Filled.History)
    data object ClientProfile : BottomNavItem(Screen.ClientProfile.route, "Profile", Icons.Outlined.Person, Icons.Filled.Person)

    // Worker tabs
    data object WorkerHome : BottomNavItem(Screen.WorkerHome.route, "Offers", Icons.Outlined.Work, Icons.Filled.Work)
    data object WorkerSchedule : BottomNavItem(Screen.WorkerSchedule.route, "Schedule", Icons.Outlined.CalendarToday, Icons.Filled.CalendarToday)
    data object WorkerEarnings : BottomNavItem(Screen.WorkerEarnings.route, "Earnings", Icons.Outlined.CurrencyRupee, Icons.Filled.CurrencyRupee)
    data object WorkerProfile : BottomNavItem(Screen.WorkerProfile.route, "Profile", Icons.Outlined.Person, Icons.Filled.Person)

    // Admin tabs
    data object AdminDashboard : BottomNavItem(Screen.AdminDashboard.route, "Dashboard", Icons.Outlined.Dashboard, Icons.Filled.Dashboard)
    data object AdminWorkers : BottomNavItem(Screen.AdminWorkers.route, "Workers", Icons.Outlined.People, Icons.Filled.People)
    data object AdminClients : BottomNavItem(Screen.AdminClients.route, "Clients", Icons.Outlined.Business, Icons.Filled.Business)
    data object AdminBookings : BottomNavItem(Screen.AdminBookings.route, "Bookings", Icons.Outlined.Work, Icons.Filled.Work)
    data object AdminProfile : BottomNavItem(Screen.AdminProfile.route, "Profile", Icons.Outlined.Person, Icons.Filled.Person)
}


/**
 * Modern floating pill bottom bar with glassmorphic depth, spring physics,
 * tactile haptic feedback, and fluid selection indicator transitions.
 */
@Composable
fun KrewzoBottomNav(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(34.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.18f),
                    ambientColor = Color.Black.copy(alpha = if (isDark) 0.5f else 0.08f)
                ),
            shape = RoundedCornerShape(34.dp),
            color = if (isDark) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            } else {
                Color(0xFFFFFFFF).copy(alpha = 0.97f)
            },
            border = BorderStroke(
                width = 1.dp,
                color = KrewzoTheme.colors.border
            ),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    KrewzoBottomNavItemView(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onItemClick(item)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun KrewzoBottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "itemScale"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.14f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    val pillWidth by animateDpAsState(
        targetValue = if (isSelected) 46.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "pillWidth"
    )

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 200),
        label = "pillColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            KrewzoTheme.colors.textTertiary
        },
        animationSpec = tween(durationMillis = 200),
        label = "contentColor"
    )

    val dotWidth by animateDpAsState(
        targetValue = if (isSelected) 14.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 400f),
        label = "dotWidth"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, radius = 28.dp, color = MaterialTheme.colorScheme.primary),
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pill wrapping the active icon
        Box(
            modifier = Modifier
                .height(28.dp)
                .width(if (pillWidth > 0.dp) pillWidth else 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.icon,
                contentDescription = item.title,
                tint = contentColor,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Glowing active indicator bar
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(dotWidth)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
