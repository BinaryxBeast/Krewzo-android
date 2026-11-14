package com.binaryxbeast.krewzo.feature.admin

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.model.Client
import com.binaryxbeast.krewzo.ui.components.BottomNavItem
import com.binaryxbeast.krewzo.ui.components.KrewzoBottomNav
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

private enum class ClientFilter(val label: String) {
    ALL("All"),
    VERIFIED("Verified"),
    PENDING("Pending KYC"),
    ACTIVE("Active"),
    INACTIVE("Inactive")
}

@Composable
fun AdminClientsScreen(
    viewModel: AdminViewModel,
    onAddClientClick: () -> Unit,
    onEditClientClick: (String) -> Unit,
    onNavigateDashboard: () -> Unit = {},
    onNavigateWorkers: () -> Unit = {},
    onNavigateBookings: () -> Unit = {},
    onNavigateProfile: () -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    val clients by viewModel.clients.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ClientFilter.ALL) }

    val filteredClients = clients.filter { client ->
        val matchesSearch = client.businessName.contains(searchQuery, ignoreCase = true) ||
            client.contactPerson.contains(searchQuery, ignoreCase = true) ||
            client.phone.contains(searchQuery) ||
            client.address.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            ClientFilter.ALL -> true
            ClientFilter.VERIFIED -> client.isVerified
            ClientFilter.PENDING -> !client.isVerified
            ClientFilter.ACTIVE -> client.isActive
            ClientFilter.INACTIVE -> !client.isActive
        }

        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "Manage Clients"
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
                currentRoute = BottomNavItem.AdminClients.route,
                onItemClick = { item ->
                    when (item) {
                        BottomNavItem.AdminDashboard -> onNavigateDashboard()
                        BottomNavItem.AdminWorkers -> onNavigateWorkers()
                        BottomNavItem.AdminBookings -> onNavigateBookings()
                        BottomNavItem.AdminProfile -> onNavigateProfile()
                        else -> {}
                    }
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClientClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Client")
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
                placeholder = "Search by business name, contact, phone...",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = KrewzoTheme.colors.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ClientFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    val count = when (filter) {
                        ClientFilter.ALL -> clients.size
                        ClientFilter.VERIFIED -> clients.count { it.isVerified }
                        ClientFilter.PENDING -> clients.count { !it.isVerified }
                        ClientFilter.ACTIVE -> clients.count { it.isActive }
                        ClientFilter.INACTIVE -> clients.count { !it.isActive }
                    }
                    val chipBg by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else KrewzoTheme.colors.surfaceSecondary,
                        animationSpec = androidx.compose.animation.core.tween(200),
                        label = "clientChipBg"
                    )
                    val chipTextColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else KrewzoTheme.colors.textSecondary,
                        animationSpec = androidx.compose.animation.core.tween(200),
                        label = "clientChipTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(KrewzoDimens.RadiusFull))
                            .background(chipBg)
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "${filter.label} ($count)",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = chipTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BUSINESS CLIENTS (${filteredClients.size})",
                style = MaterialTheme.typography.labelSmall,
                color = KrewzoTheme.colors.textTertiary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredClients.isEmpty()) {
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
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = KrewzoTheme.colors.textTertiary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isBlank() && selectedFilter == ClientFilter.ALL) "No clients registered yet" else "No matching clients found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KrewzoTheme.colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isBlank() && selectedFilter == ClientFilter.ALL) "Register client accounts to enable booking crew and dispatch" else "Try clearing your search or filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = KrewzoTheme.colors.textSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (searchQuery.isBlank() && selectedFilter == ClientFilter.ALL) {
                            Spacer(modifier = Modifier.height(18.dp))
                            KrewzoButton(
                                text = "+ Add Client",
                                onClick = onAddClientClick,
                                modifier = Modifier.width(180.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredClients.size) { index ->
                        val client = filteredClients[index]
                        com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = index) {
                            AdminClientItemCard(
                                client = client,
                                onClick = { onEditClientClick(client.id) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AdminClientItemCard(
    client: Client,
    onClick: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = client.businessName.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = client.businessName,
                        style = MaterialTheme.typography.titleMedium,
                        color = KrewzoTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Contact: ${client.contactPerson} · ${client.employeeCount} workers",
                        style = MaterialTheme.typography.bodySmall,
                        color = KrewzoTheme.colors.textSecondary
                    )
                }
            }

            // Status chip: Active / Deactivated
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(KrewzoDimens.RadiusSm))
                    .background(if (client.isActive) KrewzoTheme.colors.successContainer else Color(0xFFFEF2F2))
                    .border(
                        0.5.dp,
                        if (client.isActive) KrewzoTheme.colors.success.copy(alpha = 0.3f) else Color(0xFFDC2626).copy(alpha = 0.3f),
                        RoundedCornerShape(KrewzoDimens.RadiusSm)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (client.isActive) "Active" else "Inactive",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (client.isActive) KrewzoTheme.colors.success else Color(0xFFDC2626)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = KrewzoTheme.colors.textTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = client.phone,
                style = MaterialTheme.typography.bodySmall,
                color = KrewzoTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.width(14.dp))
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = KrewzoTheme.colors.textTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${client.address}, ${client.city}",
                style = MaterialTheme.typography.bodySmall,
                color = KrewzoTheme.colors.textTertiary,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // KYC status chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(KrewzoDimens.RadiusSm))
                        .background(if (client.isVerified) KrewzoTheme.colors.infoContainer else KrewzoTheme.colors.warningContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (client.isVerified) "KYC Verified ✓" else "KYC Pending ⏱",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (client.isVerified) KrewzoTheme.colors.info else KrewzoTheme.colors.warning
                    )
                }

                if (client.gstin != null && client.gstin.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(KrewzoDimens.RadiusSm))
                            .background(KrewzoTheme.colors.surfaceSecondary)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "GSTIN: ${client.gstin}",
                            fontSize = 11.sp,
                            color = KrewzoTheme.colors.textSecondary
                        )
                    }
                }
            }

            Text(
                text = "${client.totalBookings} bookings",
                style = MaterialTheme.typography.bodySmall,
                color = KrewzoTheme.colors.textTertiary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
