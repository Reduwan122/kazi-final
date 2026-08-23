package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RolePermissionConfig
import com.example.ui.viewmodel.PoultryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RolePermissionEditorScreen(
    viewModel: PoultryViewModel,
    onBack: () -> Unit,
    initialRole: String = "MANAGER"
) {
    val context = LocalContext.current
    val rolePermissionsMap by viewModel.rolePermissions.collectAsState()

    var selectedRoleKey by remember { mutableStateOf(initialRole.uppercase()) }
    var showRoleDropdown by remember { mutableStateOf(false) }

    // Current editable state
    var currentConfig by remember {
        mutableStateOf(
            rolePermissionsMap[selectedRoleKey]
                ?: RolePermissionConfig.getDefaultPermissionsForRole(selectedRoleKey)
        )
    }

    // Keep in sync when switching roles or when repository updates
    LaunchedEffect(selectedRoleKey, rolePermissionsMap) {
        val updated = rolePermissionsMap[selectedRoleKey]
            ?: RolePermissionConfig.getDefaultPermissionsForRole(selectedRoleKey)
        currentConfig = updated
    }

    val allRoles = remember { RolePermissionConfig.getAllRoles() }
    val primaryGreen = Color(0xFF0D631B)
    val cardBackground = Color(0xFFFFFFFF)
    val headerBackground = Color(0xFFF5F3F3)
    val cardBorder = Color(0xFFE3E2E2)
    val textPrimary = Color(0xFF1B1C1C)
    val textSecondary = Color(0xFF40493D)

    // Calculate enabled permissions count
    val totalPermissions = 7
    val activePermissionsCount = listOf(
        currentConfig.dailyReportView,
        currentConfig.dailyReportAdd,
        currentConfig.userManagementView,
        currentConfig.expenseView,
        currentConfig.expenseAdd,
        currentConfig.expenseDelete,
        currentConfig.reportAnalyticsView,
        currentConfig.reportAnalyticsDownload
    ).count { it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "রোল পারমিশন এডিটর",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF5F3F3))
                            .testTag("btn_back_role_permission")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "পিছনে যান",
                            tint = primaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            viewModel.updateRolePermissions(currentConfig) {
                                Toast.makeText(
                                    context,
                                    "${currentConfig.roleDisplayName} পারমিশন সফলভাবে সংরক্ষণ করা হয়েছে!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("btn_save_role_permissions")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সংরক্ষণ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFBF9F9))
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("screen_role_permission_editor")
        ) {
            // -------------------------------------------------------------
            // ROLE HEADER CARD (Clickable to switch roles)
            // -------------------------------------------------------------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .clickable { showRoleDropdown = true }
                    .testTag("card_role_header"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "নির্বাচিত রোল",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 12.5.sp,
                                        color = textSecondary
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFFCBFFC2))
                                        .padding(horizontal = 7.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "$activePermissionsCount টি পারমিশন সক্রিয়",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryGreen
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentConfig.roleDisplayName,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryGreen
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Change Role",
                                    tint = primaryGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2E7D32)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (selectedRoleKey) {
                                    "ADMIN" -> Icons.Default.AdminPanelSettings
                                    "MANAGER" -> Icons.Default.ManageAccounts
                                    "SUPERVISOR" -> Icons.Default.Security
                                    else -> Icons.Default.Badge
                                },
                                contentDescription = "Role Badge",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Role Selection Dropdown
                    DropdownMenu(
                        expanded = showRoleDropdown,
                        onDismissRequest = { showRoleDropdown = false },
                        modifier = Modifier.testTag("menu_role_selection")
                    ) {
                        allRoles.forEach { (roleKey, roleName) ->
                            val isSelected = selectedRoleKey == roleKey
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = roleName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) primaryGreen else textPrimary
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = primaryGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedRoleKey = roleKey
                                    showRoleDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Batch Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        currentConfig = currentConfig.copy(
                            dailyReportView = true,
                            dailyReportAdd = true,
                            userManagementView = true,
                            expenseView = true,
                            expenseAdd = true,
                            expenseDelete = true,
                            reportAnalyticsView = true,
                            reportAnalyticsDownload = true
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("সব চালু করুন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = {
                        currentConfig = RolePermissionConfig.getDefaultPermissionsForRole(selectedRoleKey)
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ডিফল্ট রিসেট", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // MODULE 1: দৈনিক রিপোর্ট (Daily Report)
            // -------------------------------------------------------------
            PermissionModuleCard(
                icon = Icons.Default.Description,
                title = "দৈনিক রিপোর্ট",
                headerBackground = headerBackground,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                primaryColor = primaryGreen
            ) {
                PermissionToggleItem(
                    title = "রিপোর্ট দেখা (View)",
                    subtitle = "দৈনিক হিসাব ও ফার্মের সার্বিক রিপোর্ট দেখতে পারবে",
                    isChecked = currentConfig.dailyReportView,
                    onCheckedChange = { checked ->
                        currentConfig = currentConfig.copy(dailyReportView = checked)
                    },
                    primaryColor = primaryGreen,
                    testTag = "toggle_daily_view",
                    showDivider = true
                )

                PermissionToggleItem(
                    title = "রিপোর্ট এন্ট্রি করা (Add)",
                    subtitle = "নতুন দৈনিক ডিম উৎপাদন ও খাদ্য রিপোর্ট যোগ করতে পারবে",
                    isChecked = currentConfig.dailyReportAdd,
                    onCheckedChange = { checked ->
                        currentConfig = currentConfig.copy(dailyReportAdd = checked)
                    },
                    primaryColor = primaryGreen,
                    testTag = "toggle_daily_add",
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // MODULE 2: ইউজার ম্যানেজমেন্ট (User Management)
            // -------------------------------------------------------------
            PermissionModuleCard(
                icon = Icons.Default.Group,
                title = "ইউজার ম্যানেজমেন্ট",
                headerBackground = headerBackground,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                primaryColor = primaryGreen
            ) {
                PermissionToggleItem(
                    title = "ইউজার নিয়ন্ত্রণ (User Access)",
                    subtitle = "ব্যবহারকারী তালিকা দেখা, অনুমোদন এবং ভূমিকা পরিবর্তনের অ্যাক্সেস",
                    isChecked = currentConfig.userManagementView,
                    onCheckedChange = { checked ->
                        currentConfig = currentConfig.copy(userManagementView = checked)
                    },
                    primaryColor = primaryGreen,
                    testTag = "toggle_user_view",
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // MODULE 3: খরচ ম্যানেজমেন্ট (Expense Management)
            // -------------------------------------------------------------
            PermissionModuleCard(
                icon = Icons.Default.Payments,
                title = "খরচ ও হিসাব ম্যানেজমেন্ট",
                headerBackground = headerBackground,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                primaryColor = primaryGreen
            ) {
                PermissionToggleItem(
                    title = "খরচ তালিকা দেখা (View)",
                    subtitle = "খামারের সকল ব্যয়ের হিসাব ও তালিকা দেখতে পারবে",
                    isChecked = currentConfig.expenseView,
                    onCheckedChange = { checked ->
                        currentConfig = currentConfig.copy(expenseView = checked)
                    },
                    primaryColor = primaryGreen,
                    testTag = "toggle_expense_view",
                    showDivider = true
                )

                PermissionToggleItem(
                    title = "নতুন খরচ যোগ করা (Add)",
                    subtitle = "নতুন ব্যয়ের ভাউচার বা ক্যাটাগরি অনুসারে এন্ট্রি যুক্ত করতে পারবে",
                    isChecked = currentConfig.expenseAdd,
                    onCheckedChange = { checked ->
                        currentConfig = currentConfig.copy(expenseAdd = checked)
                    },
                    primaryColor = primaryGreen,
                    testTag = "toggle_expense_add",
                    showDivider = true
                )

                PermissionToggleItem(
                    title = "খরচ মুছে ফেলা (Delete)",
                    subtitle = "ভুল এন্ট্রি সংশোধন বা ডিলিট করার ক্ষমতা",
                    isChecked = currentConfig.expenseDelete,
                    onCheckedChange = { checked ->
                        currentConfig = currentConfig.copy(expenseDelete = checked)
                    },
                    primaryColor = primaryGreen,
                    testTag = "toggle_expense_delete",
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // MODULE 4: রিপোর্ট ও অ্যানালিটিক্স (Reports & Analytics)
            // -------------------------------------------------------------
            PermissionModuleCard(
                icon = Icons.Default.Analytics,
                title = "রিপোর্ট ও অ্যানালিটিক্স",
                headerBackground = headerBackground,
                cardBackground = cardBackground,
                cardBorder = cardBorder,
                primaryColor = primaryGreen
            ) {
                PermissionToggleItem(
                    title = "গ্রাফ ও পরিসংখ্যান দেখা (View Analytics)",
                    subtitle = "মাসিক আয়-ব্যয় এবং ডিম উৎপাদনের গ্রাফ দেখতে পারবে",
                    isChecked = currentConfig.reportAnalyticsView,
                    onCheckedChange = { checked ->
                        currentConfig = currentConfig.copy(reportAnalyticsView = checked)
                    },
                    primaryColor = primaryGreen,
                    testTag = "toggle_report_view",
                    showDivider = true
                )

                PermissionToggleItem(
                    title = "পিডিএফ ও এক্সেল ডাউনলোড (Download)",
                    subtitle = "মাসিক ও বাৎসরিক রিপোর্ট পিডিএফ আকারে এক্সপোর্ট করতে পারবে",
                    isChecked = currentConfig.reportAnalyticsDownload,
                    onCheckedChange = { checked ->
                        currentConfig = currentConfig.copy(reportAnalyticsDownload = checked)
                    },
                    primaryColor = primaryGreen,
                    testTag = "toggle_report_download",
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun PermissionModuleCard(
    icon: ImageVector,
    title: String,
    headerBackground: Color,
    cardBackground: Color,
    cardBorder: Color,
    primaryColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Module Header Bar
            Surface(
                color = headerBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBFFC2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF1B1C1C)
                        )
                    )
                }
            }

            HorizontalDivider(color = cardBorder, thickness = 1.dp)

            // Module Body Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun PermissionToggleItem(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    primaryColor: Color,
    testTag: String,
    showDivider: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color(0xFF1B1C1C)
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        color = Color(0xFF40493D)
                    )
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = primaryColor,
                    checkedBorderColor = primaryColor,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE3E2E2),
                    uncheckedBorderColor = Color(0xFFBFCABA)
                ),
                thumbContent = if (isChecked) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = primaryColor
                        )
                    }
                } else null,
                modifier = Modifier.testTag(testTag)
            )
        }

        if (showDivider) {
            HorizontalDivider(
                color = Color(0xFFF0EFEF),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}
