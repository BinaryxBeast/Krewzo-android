package com.binaryxbeast.krewzo.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.binaryxbeast.krewzo.core.model.Worker
import com.binaryxbeast.krewzo.ui.components.BottomNavItem
import com.binaryxbeast.krewzo.ui.components.KrewzoBottomNav
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

@Composable
fun AdminWorkersScreen(
    viewModel: AdminViewModel,
    onAddWorkerClick: () -> Unit,
    onEditWorkerClick: (String) -> Unit,
    onNavigateDashboard: () -> Unit = {},
    onNavigateClients: () -> Unit = {},
    onNavigateBookings: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val workers by viewModel.workers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredWorkers = workers.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) ||
        it.phone.contains(searchQuery) ||
        it.skills.any { s -> s.displayName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "Manage Workers"
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
                currentRoute = BottomNavItem.AdminWorkers.route,
                onItemClick = { item ->
                    when (item) {
                        BottomNavItem.AdminDashboard -> onNavigateDashboard()
                        BottomNavItem.AdminClients -> onNavigateClients()
                        BottomNavItem.AdminBookings -> onNavigateBookings()
                        BottomNavItem.AdminProfile -> onNavigateProfile()
                        else -> {}
                    }
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddWorkerClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Worker")
            }
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

            KrewzoTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search by name, phone, or skill...",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = KrewzoTheme.colors.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "REGISTERED CREW (${filteredWorkers.size})",
                style = MaterialTheme.typography.labelSmall,
                color = KrewzoTheme.colors.textTertiary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredWorkers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(KrewzoTheme.colors.surfaceSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = KrewzoTheme.colors.textTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No workers registered yet" else "No matching workers found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KrewzoTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Tap '+' below to add your first worker" else "Try adjusting your search criteria",
                            style = MaterialTheme.typography.bodySmall,
                            color = KrewzoTheme.colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(18.dp))
                            KrewzoButton(
                                text = "+ Add Worker",
                                onClick = onAddWorkerClick,
                                modifier = Modifier.width(180.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredWorkers.size) { index ->
                        val worker = filteredWorkers[index]
                        com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = index) {
                            AdminWorkerItemCard(
                                worker = worker,
                                onClick = { onEditWorkerClick(worker.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                    }
                }
            }
        }
    }
}

@Composable
fun AdminWorkerItemCard(
    worker: Worker,
    onClick: () -> Unit
) {
    val isAadhaarVerified = !worker.adminNotes.isNullOrBlank() && (
        worker.adminNotes.contains("Aadhaar", ignoreCase = true) ||
        worker.adminNotes.contains("FRONT", ignoreCase = true) ||
        worker.adminNotes.contains("NUM", ignoreCase = true)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar with availability dot
                Box(contentAlignment = Alignment.BottomEnd) {
                    if (!worker.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = worker.photoUrl,
                            contentDescription = worker.fullName,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = worker.fullName.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Availability Indicator Dot
                    Box(
                        modifier = Modifier
                            .size(13.dp)
                            .clip(CircleShape)
                            .background(if (worker.isAvailable) Color(0xFF10B981) else Color(0xFF9CA3AF))
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = worker.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        color = KrewzoTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val ageDisplay = worker.effectiveAge()?.let { "$it yrs" } ?: (worker.age?.let { "$it yrs" } ?: "Age N/A")
                        val dobDisplay = if (!worker.dateOfBirth.isNullOrBlank()) " • ${worker.dateOfBirth}" else ""
                        Text(
                            text = "$ageDisplay$dobDisplay",
                            style = MaterialTheme.typography.bodySmall,
                            color = KrewzoTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        com.binaryxbeast.krewzo.ui.components.RatingBadge(
                            rating = worker.rating ?: 5.0
                        )

                        if (worker.totalJobsCompleted > 0) {
                            Text(
                                text = " • ${worker.totalJobsCompleted} jobs",
                                fontSize = 11.sp,
                                color = KrewzoTheme.colors.textTertiary
                            )
                        }
                    }
                }
            }

            // Status chip: Active / Inactive + Availability
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(KrewzoDimens.RadiusSm))
                        .background(if (worker.isActive) KrewzoTheme.colors.successContainer else Color(0xFFFEF2F2))
                        .border(
                            0.5.dp,
                            if (worker.isActive) KrewzoTheme.colors.success.copy(alpha = 0.3f) else Color(0xFFDC2626).copy(alpha = 0.3f),
                            RoundedCornerShape(KrewzoDimens.RadiusSm)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (worker.isActive) "Active" else "Deactivated",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (worker.isActive) KrewzoTheme.colors.success else Color(0xFFDC2626)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (worker.isAvailable) {
                        com.binaryxbeast.krewzo.ui.components.PulsingStatusDot(
                            color = Color(0xFF10B981),
                            size = 6.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (worker.isAvailable) "Available" else "Offline",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (worker.isAvailable) Color(0xFF10B981) else KrewzoTheme.colors.textTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = KrewzoTheme.colors.textTertiary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = worker.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = KrewzoTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "📍 ${worker.homeAddress ?: "Pune"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = KrewzoTheme.colors.textTertiary,
                    maxLines = 1
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(KrewzoDimens.RadiusSm))
                    .background(
                        if (isAadhaarVerified) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else Color(0xFFFEF3C7)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isAadhaarVerified) "✓ Aadhaar" else "KYC Pending",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isAadhaarVerified) MaterialTheme.colorScheme.primary else Color(0xFFD97706)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Skills tags
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            worker.skills.take(3).forEach { skill ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(KrewzoDimens.RadiusSm))
                        .background(KrewzoTheme.colors.surfaceSecondary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = skill.displayName,
                        fontSize = 11.sp,
                        color = KrewzoTheme.colors.textSecondary
                    )
                }
            }

            if (worker.skills.size > 3) {
                Text(
                    text = "+${worker.skills.size - 3} more",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = KrewzoTheme.colors.textTertiary
                )
            }
        }
    }
}
