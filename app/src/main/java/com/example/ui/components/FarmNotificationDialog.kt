package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.PoultryViewModel

@Composable
fun FarmNotificationDialog(
    viewModel: PoultryViewModel,
    onDismiss: () -> Unit,
    onNavigateToAddDailyReport: () -> Unit = {},
    onNavigateToDailyReportList: () -> Unit = {}
) {
    val dailyReports by viewModel.dailyReports.collectAsState()
    val stats by viewModel.dashboardStats.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val pendingUsers = remember(allUsers) { allUsers.filter { !it.isApproved && !it.isAdmin() } }
    val currentUser by viewModel.currentUser.collectAsState()
    val rolePermissionsMap by viewModel.rolePermissions.collectAsState()

    val userPerms = currentUser?.let { rolePermissionsMap[it.role.uppercase()] }
    val canViewReport = currentUser?.canViewReport(userPerms) ?: false
    val canAddReport = currentUser?.canAddReport(userPerms) ?: false
    val canManageUsers = currentUser?.canManageUsers(userPerms) ?: false || currentUser?.isAdmin() == true

    val todayDateFormatted = remember { BanglaNumberFormatter.getCurrentDateFormatted() }
    val todayDateBangla = remember { BanglaNumberFormatter.formatBanglaDate(todayDateFormatted) }

    val todayReport = remember(dailyReports, todayDateFormatted) {
        dailyReports.firstOrNull { it.date == todayDateFormatted }
    }
    val latestReport = remember(dailyReports) { dailyReports.firstOrNull() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("farm_notification_dialog")
        ) {
            Column {
                // ── Header ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "খামার আপডেট ও নোটিফিকেশন",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = todayDateBangla,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .testTag("btn_close_notification_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // ── Notification Items ──
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 6.dp)
                ) {
                    // 1. Role-based Daily Report Status
                    if (canViewReport) {
                        if (todayReport != null) {
                            NotifItem(
                                icon = Icons.Default.CheckCircle,
                                dotColor = Color(0xFF2E7D32),
                                title = "আজকের রিপোর্ট সম্পন্ন",
                                subtitle = "ডিম: ${BanglaNumberFormatter.formatNumber(todayReport.eggProduction)} টি • বিক্রি: ${BanglaNumberFormatter.formatNumber(todayReport.eggSold)} টি",
                                actionHint = "রিপোর্ট দেখুন",
                                onClick = {
                                    onDismiss()
                                    onNavigateToDailyReportList()
                                }
                            )
                        } else {
                            if (canAddReport) {
                                // User has permission to enter daily data
                                NotifItem(
                                    icon = Icons.Default.Edit,
                                    dotColor = Color(0xFFE65100),
                                    title = "আজকের রিপোর্ট এন্ট্রি করুন",
                                    subtitle = "দৈনিক ডিম সংগ্রহ ও খাদ্য ব্যবহারের তথ্য এখনো যুক্ত করা হয়নি। চাপ দিয়ে এন্ট্রি করুন।",
                                    actionHint = "এন্ট্রি করুন ➔",
                                    onClick = {
                                        onDismiss()
                                        onNavigateToAddDailyReport()
                                    }
                                )
                            } else {
                                // User can view but cannot add data (e.g. Viewer or restricted role)
                                NotifItem(
                                    icon = Icons.Default.Visibility,
                                    dotColor = Color(0xFFF57C00),
                                    title = "আজকের রিপোর্ট অপেক্ষমাণ",
                                    subtitle = "আজকের দৈনিক রিপোর্ট এখনো সুপারভাইজার কর্তৃক এন্ট্রি করা হয়নি।",
                                    actionHint = "পূর্বের রিপোর্ট দেখুন",
                                    onClick = {
                                        onDismiss()
                                        onNavigateToDailyReportList()
                                    }
                                )
                            }
                        }
                    }

                    // 2. Health & Mortality Alert (visible if user can view report)
                    if (canViewReport) {
                        val activeMortality = todayReport?.deadBirds ?: (latestReport?.deadBirds ?: 0)
                        if (activeMortality > 0) {
                            NotifItem(
                                icon = Icons.Default.Warning,
                                dotColor = Color(0xFFD32F2F),
                                title = "মর্টালিটি সতর্কতা",
                                subtitle = "${BanglaNumberFormatter.formatNumber(activeMortality)} টি মুরগি মারা গেছে • খামারে বিশেষ যত্ন ও পর্যবেক্ষণ প্রয়োজন।",
                                actionHint = null,
                                onClick = null
                            )
                        } else {
                            NotifItem(
                                icon = Icons.Default.Pets,
                                dotColor = Color(0xFF2E7D32),
                                title = "মুরগির স্বাস্থ্য স্বাভাবিক",
                                subtitle = "বর্তমানে সক্রিয় মুরগি: ${BanglaNumberFormatter.formatNumber(stats.currentBirds)} টি",
                                actionHint = null,
                                onClick = null
                            )
                        }
                    }

                    // 3. User Approvals (visible only to Admin or users with User Management permission)
                    if (canManageUsers && pendingUsers.isNotEmpty()) {
                        NotifItem(
                            icon = Icons.Default.PersonAdd,
                            dotColor = Color(0xFF1976D2),
                            title = "ব্যবহারকারী অনুমোদন অপেক্ষমাণ",
                            subtitle = "${BanglaNumberFormatter.formatNumber(pendingUsers.size)} জন নতুন রেজিস্ট্রেশনকারী অনুমোদনের অপেক্ষায় আছেন।",
                            actionHint = "সেটিংস থেকে অনুমোদন দিন",
                            onClick = null
                        )
                    }

                    // 4. Role info notice if user has restricted access
                    if (!canViewReport && !canAddReport) {
                        NotifItem(
                            icon = Icons.Default.Info,
                            dotColor = Color(0xFF0288D1),
                            title = "অ্যাকাউন্ট রোল: ${currentUser?.roleNameBengali() ?: ""}",
                            subtitle = "আপনার বর্তমান রোলে দৈনিক রিপোর্ট অ্যাক্সেস সীমিত রয়েছে। প্রয়োজনে অ্যাডমিনের সাথে যোগাযোগ করুন।",
                            actionHint = null,
                            onClick = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun NotifItem(
    icon: ImageVector,
    dotColor: Color,
    title: String,
    subtitle: String,
    actionHint: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable { onClick() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                }
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Colored dot indicator
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )

        // Icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(dotColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = dotColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionHint != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = actionHint,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = dotColor,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
