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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.UserRole
import com.binaryxbeast.krewzo.data.repository.RoleAssignment
import com.binaryxbeast.krewzo.data.repository.UserRoleRegistry
import com.binaryxbeast.krewzo.ui.components.KrewzoButton
import com.binaryxbeast.krewzo.ui.components.KrewzoCard
import com.binaryxbeast.krewzo.ui.components.KrewzoTextField
import com.binaryxbeast.krewzo.ui.components.KrewzoTopBar
import com.binaryxbeast.krewzo.ui.components.bouncyClickable
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme

@Composable
fun AdminUserRolesScreen(
    viewModel: AdminViewModel,
    onBack: () -> Unit
) {
    val roleAssignments by viewModel.roleAssignments.collectAsState()
    val saveRoleState by viewModel.saveRoleState.collectAsState()

    var inputPhone by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.ADMIN) }
    var roleFilter by remember { mutableStateOf<UserRole?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val filteredAssignments = remember(roleAssignments, roleFilter) {
        if (roleFilter == null) {
            roleAssignments
        } else {
            roleAssignments.filter { it.userRole == roleFilter }
        }
    }

    Scaffold(
        topBar = {
            KrewzoTopBar(
                title = "User Roles & Permissions",
                onBackClick = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))

                // Assign/Modify Role Card
                KrewzoCard(padding = 20.dp) {
                    Text(
                        text = "Assign or Change Role by Mobile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KrewzoTheme.colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Assign a mobile number to Admin, Worker, or Client perspective.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KrewzoTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mobile Number Input with Indian Prefix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .height(KrewzoDimens.InputHeight)
                                .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                                .background(KrewzoTheme.colors.surfaceSecondary)
                                .border(KrewzoDimens.BorderWidth, KrewzoTheme.colors.border, RoundedCornerShape(KrewzoDimens.RadiusMd))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🇮🇳 +91",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = KrewzoTheme.colors.textPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        KrewzoTextField(
                            value = inputPhone,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(10)
                                inputPhone = digits

                                // Check if this number already has an assignment and auto-fill
                                if (digits.length == 10) {
                                    val existing = UserRoleRegistry.getAssignment(digits)
                                    if (existing != null) {
                                        selectedRole = existing.userRole
                                        if (inputName.isBlank() && !existing.name.isNullOrBlank()) {
                                            inputName = existing.name
                                        }
                                    }
                                }
                            },
                            placeholder = "10-digit number (e.g. 8275976150)",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Phone",
                                    tint = KrewzoTheme.colors.textTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Optional Name / Label
                    KrewzoTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        placeholder = "User / Business Name (Optional)",
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Target Perspective:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = KrewzoTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role Selection Segment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RoleSelectionChip(
                            title = "Admin",
                            icon = Icons.Default.Security,
                            isSelected = selectedRole == UserRole.ADMIN,
                            selectedColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedRole = UserRole.ADMIN }
                        )

                        RoleSelectionChip(
                            title = "Worker",
                            icon = Icons.Default.People,
                            isSelected = selectedRole == UserRole.WORKER,
                            selectedColor = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f),
                            onClick = { selectedRole = UserRole.WORKER }
                        )

                        RoleSelectionChip(
                            title = "Client",
                            icon = Icons.Default.Business,
                            isSelected = selectedRole == UserRole.CLIENT,
                            selectedColor = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            onClick = { selectedRole = UserRole.CLIENT }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    KrewzoButton(
                        text = "Save Role Assignment",
                        onClick = {
                            viewModel.saveRoleAssignment(
                                phone = inputPhone,
                                role = selectedRole,
                                name = inputName.takeIf { it.isNotBlank() }
                            ) {
                                statusMessage = "Updated +91 $inputPhone to ${selectedRole.name}"
                                inputPhone = ""
                                inputName = ""
                            }
                        },
                        isLoading = saveRoleState is Resource.Loading,
                        enabled = inputPhone.length == 10
                    )

                    if (statusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Role Filter Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONFIGURED ROLES (${roleAssignments.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = KrewzoTheme.colors.textTertiary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = roleFilter == null,
                            onClick = { roleFilter = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = roleFilter == UserRole.ADMIN,
                            onClick = { roleFilter = if (roleFilter == UserRole.ADMIN) null else UserRole.ADMIN },
                            label = { Text("Admin") }
                        )
                        FilterChip(
                            selected = roleFilter == UserRole.WORKER,
                            onClick = { roleFilter = if (roleFilter == UserRole.WORKER) null else UserRole.WORKER },
                            label = { Text("Worker") }
                        )
                        FilterChip(
                            selected = roleFilter == UserRole.CLIENT,
                            onClick = { roleFilter = if (roleFilter == UserRole.CLIENT) null else UserRole.CLIENT },
                            label = { Text("Client") }
                        )
                    }
                }
            }

            // List of Role Assignments
            items(filteredAssignments.size) { index ->
                val assignment = filteredAssignments[index]
                com.binaryxbeast.krewzo.ui.components.StaggeredAnimatedItem(index = index) {
                    RoleAssignmentCard(
                        assignment = assignment,
                        onEditClick = {
                            inputPhone = assignment.phone
                            inputName = assignment.name.orEmpty()
                            selectedRole = assignment.userRole
                        },
                        onDeleteClick = {
                            viewModel.removeRoleAssignment(assignment.phone)
                        }
                    )
                }
            }

            // Notice regarding default client perspective
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dynamic Role Rule",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "• 8275976150 is the designated Root Super Admin.\n" +
                                    "• Numbers set as Worker will open the Worker Perspective.\n" +
                                    "• Any number NOT in the Worker or Admin list (or a brand-new number) will automatically open in the Client Perspective (triggering onboarding for new clients).",
                            style = MaterialTheme.typography.bodySmall,
                            color = KrewzoTheme.colors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun RoleSelectionChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) selectedColor.copy(alpha = 0.15f) else KrewzoTheme.colors.surfaceSecondary,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "roleChipBg"
    )
    val borderCol by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) selectedColor else KrewzoTheme.colors.border,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "roleChipBorder"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(KrewzoDimens.RadiusMd))
            .background(bg)
            .border(if (isSelected) 1.5.dp else 1.dp, borderCol, RoundedCornerShape(KrewzoDimens.RadiusMd))
            .bouncyClickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) selectedColor else KrewzoTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedColor else KrewzoTheme.colors.textPrimary
            )
        }
    }
}

@Composable
private fun RoleAssignmentCard(
    assignment: RoleAssignment,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val roleColor = when (assignment.userRole) {
        UserRole.ADMIN -> MaterialTheme.colorScheme.primary
        UserRole.WORKER -> Color(0xFFF59E0B)
        UserRole.CLIENT -> Color(0xFF10B981)
    }

    val roleBg = roleColor.copy(alpha = 0.12f)

    KrewzoCard(padding = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Role Icon Box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(roleBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (assignment.userRole) {
                            UserRole.ADMIN -> Icons.Default.Security
                            UserRole.WORKER -> Icons.Default.People
                            UserRole.CLIENT -> Icons.Default.Business
                        },
                        contentDescription = null,
                        tint = roleColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "+91 ${assignment.phone}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = KrewzoTheme.colors.textPrimary
                        )

                        if (assignment.isPrimaryAdmin) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SUPER ADMIN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = assignment.name ?: if (assignment.userRole == UserRole.ADMIN) "Administrator" else if (assignment.userRole == UserRole.WORKER) "Registered Worker" else "Client Account",
                        style = MaterialTheme.typography.bodySmall,
                        color = KrewzoTheme.colors.textSecondary
                    )
                }
            }

            // Actions & Badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(KrewzoDimens.RadiusSm))
                        .background(roleBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = assignment.role,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = roleColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = KrewzoTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (!assignment.isPrimaryAdmin) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
