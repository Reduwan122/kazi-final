package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.DailyReportEntity
import com.example.data.local.UserEntity
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
    val farmProfile by viewModel.farmProfile.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val pendingUsers = remember(allUsers) { allUsers.filter { !it.isApproved && !it.isAdmin() } }
    val currentUser by viewModel.currentUser.collectAsState()

    val todayDateFormatted = remember { BanglaNumberFormatter.getCurrentDateFormatted() }
    val todayDateBangla = remember { BanglaNumberFormatter.formatBanglaDate(todayDateFormatted) }

    val todayReport = remember(dailyReports, todayDateFormatted) {
        dailyReports.firstOrNull { it.date == todayDateFormatted }
    }
    val latestReport = remember(dailyReports) {
        dailyReports.firstOrNull()
    }

    val primaryGreen = Color(0xFF0D631B)
    val cardBorder = Color(0xFFE3E2E2)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("farm_notification_dialog")
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // -------------------------------------------------------------
                // HEADER
                // -------------------------------------------------------------
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(primaryGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Farm Notifications",
                                    tint = primaryGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "খামার আপডেট ও নোটিফিকেশন",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = todayDateBangla,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = primaryGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .testTag("btn_close_notification_dialog")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = cardBorder)

                // -------------------------------------------------------------
                // SCROLLABLE CONTENT BODY
                // -------------------------------------------------------------
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. TODAY'S ENTRY STATUS ALERT
                    if (todayReport != null) {
                        NotificationAlertCard(
                            icon = Icons.Default.CheckCircle,
                            iconTint = primaryGreen,
                            backgroundColor = Color(0xFFE8F5E9),
                            borderColor = primaryGreen.copy(alpha = 0.3f),
                            title = "আজকের রিপোর্ট সম্পন্ন হয়েছে",
                            description = "আজকের ডিম সংগ্রহ: ${BanglaNumberFormatter.formatNumber(todayReport.eggProduction)} টি | বিক্রি: ${BanglaNumberFormatter.formatNumber(todayReport.eggSold)} টি",
                            actionLabel = null,
                            onAction = null
                        )
                    } else {
                        NotificationAlertCard(
                            icon = Icons.Default.Warning,
                            iconTint = Color(0xFFD97706),
                            backgroundColor = Color(0xFFFFFBEB),
                            borderColor = Color(0xFFFDE68A),
                            title = "আজকের রিপোর্ট এখনো এন্ট্রি করা হয়নি!",
                            description = "খামারের আজকের ডিম সংগ্রহ ও খাদ্য ব্যবহারের দৈনিক তথ্য যোগ করুন।",
                            actionLabel = "এন্ট্রি করুন",
                            onAction = {
                                onDismiss()
                                onNavigateToAddDailyReport()
                            }
                        )
                    }

                    // 2. MORTALITY ALERT
                    val activeMortality = todayReport?.deadBirds ?: (latestReport?.deadBirds ?: 0)
                    if (activeMortality > 0) {
                        NotificationAlertCard(
                            icon = Icons.Default.PriorityHigh,
                            iconTint = Color(0xFFBA1A1A),
                            backgroundColor = Color(0xFFFFEBEE),
                            borderColor = Color(0xFFFFCDD2),
                            title = "মর্টালিটি সতর্কতা: $activeMortality টি মুরগি মারা গেছে",
                            description = "ফার্মে মৃত মুরগির সংখ্যা লক্ষ্য করা গেছে। জীবাণুনাশক স্প্রে ও লক্ষণ পর্যবেক্ষণ করুন।",
                            actionLabel = null,
                            onAction = null
                        )
                    } else {
                        NotificationAlertCard(
                            icon = Icons.Default.Pets,
                            iconTint = primaryGreen,
                            backgroundColor = Color(0xFFF1F8E9),
                            borderColor = Color(0xFFDCEDC8),
                            title = "মুরগির স্বাস্থ্য স্ট্যাটাস স্বাভাবিক",
                            description = "আজ কোনো মৃত মুরগি রিপোর্ট করা হয়নি। বর্তমান সক্রিয় মুরগি: ${BanglaNumberFormatter.formatNumber(stats.currentBirds)} টি।",
                            actionLabel = null,
                            onAction = null
                        )
                    }

                    // 3. PENDING ADMIN USERS (If admin and pending users exist)
                    if (currentUser?.isAdmin() == true && pendingUsers.isNotEmpty()) {
                        NotificationAlertCard(
                            icon = Icons.Default.Person,
                            iconTint = MaterialTheme.colorScheme.primary,
                            backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            title = "অনুমোদন পেন্ডিং: ${pendingUsers.size} জন নতুন ইউজার",
                            description = "নতুন ব্যবহারকারী খামারের তথ্যে প্রবেশের অনুমোদনের অপেক্ষায় আছেন।",
                            actionLabel = null,
                            onAction = null
                        )
                    }

                    // 4. TODAY'S PULSE (QUICK STATS BENTO)
                    Text(
                        text = "আজকের সারসংক্ষেপ",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryGreen
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MiniPulseMetric(
                                    icon = Icons.Default.Egg,
                                    label = "ডিম উৎপাদন",
                                    value = "${BanglaNumberFormatter.formatNumber(todayReport?.eggProduction ?: stats.todayEggProduction)} টি",
                                    color = primaryGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                MiniPulseMetric(
                                    icon = Icons.Default.Pets,
                                    label = "জীবিত মুরগি",
                                    value = "${BanglaNumberFormatter.formatNumber(stats.currentBirds)} টি",
                                    color = Color(0xFF1976D2),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MiniPulseMetric(
                                    icon = Icons.Default.Paid,
                                    label = "মোট বিক্রয়",
                                    value = BanglaNumberFormatter.formatCurrency(todayReport?.totalSale ?: 0.0),
                                    color = Color(0xFF388E3C),
                                    modifier = Modifier.weight(1f)
                                )
                                MiniPulseMetric(
                                    icon = Icons.Default.Inventory2,
                                    label = "মজুদ ডিম",
                                    value = "${BanglaNumberFormatter.formatNumber(todayReport?.currentStock ?: (latestReport?.currentStock ?: 0))} টি",
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // 5. RECENT UPDATES / LOGS (TIMELINE)
                    Text(
                        text = "সাম্প্রতিক দৈনিক কার্যক্রম",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = primaryGreen
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    if (dailyReports.isEmpty()) {
                        Text(
                            text = "কোনো সাম্প্রতিক দৈনিক রিপোর্ট পাওয়া যায়নি।",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            dailyReports.take(4).forEach { report ->
                                RecentActivityItem(report = report)
                            }
                        }
                    }
                }

                HorizontalDivider(color = cardBorder)

                // -------------------------------------------------------------
                // BOTTOM ACTIONS
                // -------------------------------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Text("বন্ধ করুন", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToDailyReportList()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f).height(42.dp)
                    ) {
                        Icon(imageVector = Icons.Default.EventNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("রিপোর্ট দেখুন", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationAlertCard(
    icon: ImageVector,
    iconTint: Color,
    backgroundColor: Color,
    borderColor: Color,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )

                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = iconTint,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onAction() }
                    ) {
                        Text(
                            text = actionLabel,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPulseMetric(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8E8E8)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    ),
                    maxLines = 1
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = color,
                        fontSize = 13.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecentActivityItem(report: DailyReportEntity) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAEAEA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0D631B))
                )
                Column {
                    Text(
                        text = BanglaNumberFormatter.formatBanglaDate(report.date),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "ডিম: ${BanglaNumberFormatter.formatNumber(report.eggProduction)} | বিক্রয়: ${BanglaNumberFormatter.formatNumber(report.eggSold)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            if (report.totalSale > 0) {
                Text(
                    text = BanglaNumberFormatter.formatCurrency(report.totalSale),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D631B),
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
}

