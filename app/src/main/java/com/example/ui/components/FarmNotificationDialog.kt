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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Pets
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
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("farm_notification_dialog")
        ) {
            Column {
                // ── Header ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "নোটিফিকেশন",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = todayDateBangla,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        .padding(vertical = 4.dp)
                ) {
                    // 1. Today's report status
                    if (todayReport != null) {
                        NotifItem(
                            icon = Icons.Default.CheckCircle,
                            dotColor = Color(0xFF4CAF50),
                            title = "আজকের রিপোর্ট সম্পন্ন",
                            subtitle = "ডিম: ${BanglaNumberFormatter.formatNumber(todayReport.eggProduction)} টি • বিক্রি: ${BanglaNumberFormatter.formatNumber(todayReport.eggSold)} টি",
                            onClick = {
                                onDismiss()
                                onNavigateToDailyReportList()
                            }
                        )
                    } else {
                        NotifItem(
                            icon = Icons.Default.Edit,
                            dotColor = Color(0xFFF59E0B),
                            title = "আজকের রিপোর্ট এন্ট্রি করুন",
                            subtitle = "দৈনিক ডিম সংগ্রহ ও খাদ্য ব্যবহারের তথ্য যোগ করুন",
                            onClick = {
                                onDismiss()
                                onNavigateToAddDailyReport()
                            }
                        )
                    }

                    // 2. Mortality alert
                    val activeMortality = todayReport?.deadBirds ?: (latestReport?.deadBirds ?: 0)
                    if (activeMortality > 0) {
                        NotifItem(
                            icon = Icons.Default.Warning,
                            dotColor = Color(0xFFEF4444),
                            title = "মর্টালিটি সতর্কতা",
                            subtitle = "${BanglaNumberFormatter.formatNumber(activeMortality)} টি মুরগি মারা গেছে • পর্যবেক্ষণ করুন",
                            onClick = null
                        )
                    } else {
                        NotifItem(
                            icon = Icons.Default.Pets,
                            dotColor = Color(0xFF4CAF50),
                            title = "স্বাস্থ্য স্ট্যাটাস স্বাভাবিক",
                            subtitle = "সক্রিয় মুরগি: ${BanglaNumberFormatter.formatNumber(stats.currentBirds)} টি",
                            onClick = null
                        )
                    }

                    // 3. Pending user approvals (admin only)
                    if (currentUser?.isAdmin() == true && pendingUsers.isNotEmpty()) {
                        NotifItem(
                            icon = Icons.Default.PersonAdd,
                            dotColor = Color(0xFF3B82F6),
                            title = "${BanglaNumberFormatter.formatNumber(pendingUsers.size)} জন নতুন ইউজার পেন্ডিং",
                            subtitle = "অনুমোদনের জন্য অপেক্ষায় আছেন",
                            onClick = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
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
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )

        // Text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
