package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TextSnippet
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Data model representing a single metric line inside the Report Share Card.
 */
data class ShareCardMetric(
    val icon: ImageVector,
    val label: String,
    val value: String,
    val unit: String,
    val isError: Boolean = false,
    val isHighlighted: Boolean = false
)

/**
 * Exact Visual Share Card matching the requested design:
 * - Rounded container with crisp border & subtle shadow
 * - Top header with Farm Logo, Farm Name ("কাজী এগ্রোটেক"), Subtitle ("আধুনিক খামার ব্যবস্থাপনা")
 * - Summary title ("দৈনিক প্রতিবেদন সারাংশ" / "মাসিক ব্যয় সারাংশ") + Bangla date with calendar icon
 * - Clean tabular metric rows with icons, Bengali labels, and bold accented numeric values with units
 */
@Composable
fun ReportShareCard(
    farmProfile: FarmProfileEntity?,
    title: String,
    dateString: String,
    metrics: List<ShareCardMetric>,
    remarks: String? = null,
    modifier: Modifier = Modifier
) {
    val farmName = farmProfile?.farmName?.takeIf { it.isNotBlank() } ?: "কাজী এগ্রোটেক"
    val formattedDate = BanglaNumberFormatter.formatBanglaDate(dateString)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE3E2E2), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFFFFF))
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Logo box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFEDED)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.kazi_agro_logo),
                        contentDescription = "Kazi Agrotech Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                }

                // Farm Title and Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = farmName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D631B),
                            fontSize = 20.sp,
                            lineHeight = 24.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "আধুনিক খামার ব্যবস্থাপনা",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF5D5F5F),
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }

            // Header Bottom Divider
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = Color(0xFFE3E2E2)
            )

            // Main Content / Metrics Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Section Title + Date
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1C1C),
                            fontSize = 16.sp
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Date",
                            tint = Color(0xFF5D5F5F),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF5D5F5F),
                                fontSize = 13.sp
                            )
                        )
                    }
                }

                // Section Sub-divider
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = Color(0xFFE3E2E2)
                )

                // Metric Rows
                metrics.forEachIndexed { index, item ->
                    val isLast = index == metrics.size - 1

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Icon + Label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (item.isError) Color(0xFFBA1A1A) else Color(0xFF0D631B),
                                modifier = Modifier.size(19.dp)
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color(0xFF1B1C1C),
                                    fontSize = 14.sp,
                                    fontWeight = if (item.isHighlighted) FontWeight.SemiBold else FontWeight.Normal
                                )
                            )
                        }

                        // Right: Value + Unit
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = item.value,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isError) Color(0xFFBA1A1A) else Color(0xFF0D631B),
                                    fontSize = 15.sp
                                )
                            )
                            if (item.unit.isNotBlank()) {
                                Text(
                                    text = item.unit,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFF5D5F5F),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }

                    if (!isLast) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = Color(0xFFE3E2E2).copy(alpha = 0.6f)
                        )
                    }
                }

                // Optional Remarks
                if (!remarks.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F3F3))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Remarks",
                                tint = Color(0xFF5D5F5F),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "মন্তব্য: $remarks",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF40493D),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modal Dialog for previewing the card and sharing it either as a high-res image or text.
 */
@Composable
fun ReportShareDialog(
    farmProfile: FarmProfileEntity?,
    title: String,
    dateString: String,
    metrics: List<ShareCardMetric>,
    remarks: String? = null,
    textSummary: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "প্রতিবেদন কার্ড শেয়ার করুন",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Card Container recorded by GraphicsLayer for Bitmap generation
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawWithContent {
                                graphicsLayer.record {
                                    this@drawWithContent.drawContent()
                                }
                                drawLayer(graphicsLayer)
                            }
                    ) {
                        ReportShareCard(
                            farmProfile = farmProfile,
                            title = title,
                            dateString = dateString,
                            metrics = metrics,
                            remarks = remarks
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share Image Button (Primary Action)
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val imageBitmap = graphicsLayer.toImageBitmap()
                                    val bitmap = imageBitmap.asAndroidBitmap()
                                    val uri = withContext(Dispatchers.IO) {
                                        saveBitmapToCache(context, bitmap, "kazi_agro_report_${System.currentTimeMillis()}.png")
                                    }

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, textSummary)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, "প্রতিবেদন কার্ড শেয়ার করুন")
                                    )
                                    onDismiss()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "ছবি তৈরিতে সমস্যা হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_share_card_image"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D631B),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Image",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "কার্ড শেয়ার করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Share Text Button (Secondary Action)
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, textSummary)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "টেক্সট শেয়ার করুন")
                            )
                            onDismiss()
                        },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_share_text"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextSnippet,
                            contentDescription = "Share Text",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "টেক্সট",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Builds the metrics list and opens the Share Dialog for a Daily Report.
 */
@Composable
fun DailyReportShareDialog(
    report: DailyReportEntity,
    farmProfile: FarmProfileEntity?,
    onDismiss: () -> Unit
) {
    val metrics = listOf(
        ShareCardMetric(
            icon = Icons.Default.Egg,
            label = "মোট উৎপাদন",
            value = BanglaNumberFormatter.formatNumber(report.eggProduction),
            unit = "ডিম"
        ),
        ShareCardMetric(
            icon = Icons.Default.Pets,
            label = "সুস্থ মুরগি",
            value = BanglaNumberFormatter.formatNumber(report.currentBirds),
            unit = "টি"
        ),
        ShareCardMetric(
            icon = Icons.Default.ShoppingCart,
            label = "ডিম বিক্রয়",
            value = BanglaNumberFormatter.formatNumber(report.eggSold),
            unit = "টি"
        ),
        ShareCardMetric(
            icon = Icons.Default.Payments,
            label = "ডিমের দর",
            value = BanglaNumberFormatter.formatDecimal(report.eggPrice),
            unit = "টাকা"
        ),
        ShareCardMetric(
            icon = Icons.Default.AccountBalanceWallet,
            label = "মোট বিক্রয়",
            value = BanglaNumberFormatter.formatCurrency(report.totalSale),
            unit = "টাকা",
            isHighlighted = true
        ),
        ShareCardMetric(
            icon = Icons.Default.MedicalServices,
            label = "ওষুধ খরচ",
            value = BanglaNumberFormatter.formatCurrency(report.medicineCost),
            unit = "টাকা"
        ),
        ShareCardMetric(
            icon = Icons.Default.Inventory2,
            label = "বর্তমান স্টক",
            value = BanglaNumberFormatter.formatNumber(report.currentStock),
            unit = "ডিম"
        ),
        ShareCardMetric(
            icon = Icons.Default.Warning,
            label = "মর্টালিটি",
            value = BanglaNumberFormatter.formatNumber(report.deadBirds),
            unit = "টি",
            isError = report.deadBirds > 0
        )
    )

    val textSummary = buildString {
        append("কাজী এগ্রোটেক - দৈনিক রিপোর্ট সারাংশ\n")
        append("তারিখ: ${BanglaNumberFormatter.formatBanglaDate(report.date)}\n")
        append("মোট উৎপাদন: ${BanglaNumberFormatter.formatNumber(report.eggProduction)} ডিম\n")
        append("সুস্থ মুরগি: ${BanglaNumberFormatter.formatNumber(report.currentBirds)} টি\n")
        append("ডিম বিক্রয়: ${BanglaNumberFormatter.formatNumber(report.eggSold)} টি (দর: ${BanglaNumberFormatter.formatDecimal(report.eggPrice)} ৳)\n")
        append("মোট বিক্রয়: ${BanglaNumberFormatter.formatCurrency(report.totalSale)}\n")
        append("ওষুধ খরচ: ${BanglaNumberFormatter.formatCurrency(report.medicineCost)}\n")
        append("বর্তমান স্টক: ${BanglaNumberFormatter.formatNumber(report.currentStock)} ডিম\n")
        append("মর্টালিটি: ${BanglaNumberFormatter.formatNumber(report.deadBirds)} টি\n")
        if (report.remarks.isNotBlank()) {
            append("মন্তব্য: ${report.remarks}")
        }
    }

    ReportShareDialog(
        farmProfile = farmProfile,
        title = "দৈনিক প্রতিবেদন সারাংশ",
        dateString = report.date,
        metrics = metrics,
        remarks = report.remarks.takeIf { it.isNotBlank() },
        textSummary = textSummary,
        onDismiss = onDismiss
    )
}

/**
 * Builds the metrics list and opens the Share Dialog for a Monthly Expense.
 */
@Composable
fun MonthlyExpenseShareDialog(
    expense: MonthlyExpenseEntity,
    farmProfile: FarmProfileEntity?,
    onDismiss: () -> Unit
) {
    val metrics = listOf(
        ShareCardMetric(
            icon = Icons.Default.Restaurant,
            label = "খাদ্য / ফিড",
            value = BanglaNumberFormatter.formatCurrency(expense.feedCost),
            unit = "টাকা"
        ),
        ShareCardMetric(
            icon = Icons.Default.MedicalServices,
            label = "মেডিসিন ও ভ্যাকসিন",
            value = BanglaNumberFormatter.formatCurrency(expense.medicineCost),
            unit = "টাকা"
        ),
        ShareCardMetric(
            icon = Icons.Default.ShoppingCart,
            label = "স্টাফ বাজার",
            value = BanglaNumberFormatter.formatCurrency(expense.staffMarket),
            unit = "টাকা"
        ),
        ShareCardMetric(
            icon = Icons.Default.Payments,
            label = "স্টাফ বেতন / মজুরি",
            value = BanglaNumberFormatter.formatCurrency(expense.staffSalary),
            unit = "টাকা"
        ),
        ShareCardMetric(
            icon = Icons.Default.Build,
            label = "গাড়ি মেরামত / যন্ত্র",
            value = BanglaNumberFormatter.formatCurrency(expense.vehicleRepair),
            unit = "টাকা"
        ),
        ShareCardMetric(
            icon = Icons.Default.Inventory2,
            label = "সম্পদ / সরঞ্জাম",
            value = BanglaNumberFormatter.formatCurrency(expense.assets),
            unit = "টাকা"
        ),
        ShareCardMetric(
            icon = Icons.Default.Bolt,
            label = "বিদ্যুৎ বিল",
            value = BanglaNumberFormatter.formatCurrency(expense.electricityBill),
            unit = "টাকা"
        ),
        ShareCardMetric(
            icon = Icons.Default.MoreHoriz,
            label = "অন্যান্য খরচ",
            value = BanglaNumberFormatter.formatCurrency(expense.otherExpense),
            unit = "টাকা"
        ),
        ShareCardMetric(
            icon = Icons.Default.AccountBalanceWallet,
            label = "সর্বমোট ব্যয়",
            value = BanglaNumberFormatter.formatCurrency(expense.totalExpense),
            unit = "টাকা",
            isHighlighted = true
        )
    )

    val textSummary = buildString {
        append("কাজী এগ্রোটেক - মাসিক ব্যয় সারাংশ\n")
        append("তারিখ: ${BanglaNumberFormatter.formatBanglaDate(expense.date)}\n")
        append("খাদ্য / ফিড: ${BanglaNumberFormatter.formatCurrency(expense.feedCost)}\n")
        append("মেডিসিন ও ভ্যাকসিন: ${BanglaNumberFormatter.formatCurrency(expense.medicineCost)}\n")
        append("স্টাফ বাজার: ${BanglaNumberFormatter.formatCurrency(expense.staffMarket)}\n")
        append("স্টাফ বেতন: ${BanglaNumberFormatter.formatCurrency(expense.staffSalary)}\n")
        append("গাড়ি মেরামত: ${BanglaNumberFormatter.formatCurrency(expense.vehicleRepair)}\n")
        append("সম্পদ / সরঞ্জাম: ${BanglaNumberFormatter.formatCurrency(expense.assets)}\n")
        append("বিদ্যুৎ বিল: ${BanglaNumberFormatter.formatCurrency(expense.electricityBill)}\n")
        append("অন্যান্য খরচ: ${BanglaNumberFormatter.formatCurrency(expense.otherExpense)}\n")
        append("সর্বমোট ব্যয়: ${BanglaNumberFormatter.formatCurrency(expense.totalExpense)}\n")
        if (expense.remarks.isNotBlank()) {
            append("মন্তব্য: ${expense.remarks}")
        }
    }

    ReportShareDialog(
        farmProfile = farmProfile,
        title = "মাসিক ব্যয় সারাংশ",
        dateString = expense.date,
        metrics = metrics,
        remarks = expense.remarks.takeIf { it.isNotBlank() },
        textSummary = textSummary,
        onDismiss = onDismiss
    )
}

/**
 * Builds the metrics list and opens the Share Dialog for an aggregated Monthly / Filtered Summary Report.
 */
@Composable
fun MonthlySummaryShareDialog(
    monthLabel: String,
    totalBirds: Int,
    totalProduction: Int,
    totalSold: Int,
    totalSale: Double,
    totalMedicine: Double,
    totalExpense: Double,
    totalMortality: Int,
    farmProfile: FarmProfileEntity?,
    onDismiss: () -> Unit
) {
    val netProfit = totalSale - totalExpense

    val metrics = mutableListOf<ShareCardMetric>().apply {
        if (totalBirds > 0) {
            add(
                ShareCardMetric(
                    icon = Icons.Default.Pets,
                    label = "বর্তমান মুরগি",
                    value = BanglaNumberFormatter.formatNumber(totalBirds),
                    unit = "টি"
                )
            )
        }
        add(
            ShareCardMetric(
                icon = Icons.Default.Egg,
                label = "মোট ডিম উৎপাদন",
                value = BanglaNumberFormatter.formatNumber(totalProduction),
                unit = "ডিম"
            )
        )
        add(
            ShareCardMetric(
                icon = Icons.Default.ShoppingCart,
                label = "মোট ডিম বিক্রয়",
                value = BanglaNumberFormatter.formatNumber(totalSold),
                unit = "টি"
            )
        )
        add(
            ShareCardMetric(
                icon = Icons.Default.AccountBalanceWallet,
                label = "মোট বিক্রয় আয়",
                value = BanglaNumberFormatter.formatCurrency(totalSale),
                unit = "টাকা",
                isHighlighted = true
            )
        )
        if (totalMedicine > 0) {
            add(
                ShareCardMetric(
                    icon = Icons.Default.MedicalServices,
                    label = "মোট ওষুধ খরচ",
                    value = BanglaNumberFormatter.formatCurrency(totalMedicine),
                    unit = "টাকা"
                )
            )
        }
        add(
            ShareCardMetric(
                icon = Icons.Default.Payments,
                label = "মোট খামার ব্যয়",
                value = BanglaNumberFormatter.formatCurrency(totalExpense),
                unit = "টাকা"
            )
        )
        if (totalMortality > 0) {
            add(
                ShareCardMetric(
                    icon = Icons.Default.Warning,
                    label = "মোট মর্টালিটি",
                    value = BanglaNumberFormatter.formatNumber(totalMortality),
                    unit = "টি",
                    isError = true
                )
            )
        }
        add(
            ShareCardMetric(
                icon = Icons.Default.AccountBalanceWallet,
                label = if (netProfit >= 0) "নিট আনুমানিক লাভ" else "নিট আনুমানিক ক্ষতি",
                value = BanglaNumberFormatter.formatCurrency(netProfit),
                unit = "টাকা",
                isError = netProfit < 0,
                isHighlighted = true
            )
        )
    }

    val textSummary = buildString {
        append("কাজী এগ্রোটেক - মাসিক সারাংশ প্রতিবেদন\n")
        append("মাস/সময়: $monthLabel\n")
        append("মোট উৎপাদন: ${BanglaNumberFormatter.formatNumber(totalProduction)} ডিম\n")
        append("মোট বিক্রয়: ${BanglaNumberFormatter.formatNumber(totalSold)} ডিম (${BanglaNumberFormatter.formatCurrency(totalSale)})\n")
        append("মোট ব্যয়: ${BanglaNumberFormatter.formatCurrency(totalExpense)}\n")
        append("নিট লাভ/ক্ষতি: ${BanglaNumberFormatter.formatCurrency(netProfit)}\n")
    }

    ReportShareDialog(
        farmProfile = farmProfile,
        title = "মাসিক প্রতিবেদন সারাংশ",
        dateString = monthLabel,
        metrics = metrics,
        remarks = null,
        textSummary = textSummary,
        onDismiss = onDismiss
    )
}

/**
 * Saves a Bitmap image to the application cache directory and returns a secure content URI via FileProvider.
 */
private fun saveBitmapToCache(context: Context, bitmap: Bitmap, fileName: String): Uri {
    val cachePath = File(context.cacheDir, "shared_cards")
    if (!cachePath.exists()) {
        cachePath.mkdirs()
    }
    val file = File(cachePath, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
