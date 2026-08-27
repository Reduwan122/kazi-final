package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PdfPreviewModalDialog(
    title: String,
    farmProfile: FarmProfileEntity,
    dailyReports: List<DailyReportEntity> = emptyList(),
    allReports: List<DailyReportEntity> = emptyList(),
    baselineStock: Int = 0,
    expenses: List<MonthlyExpenseEntity> = emptyList(),
    reportCategory: String = if (dailyReports.isEmpty() && expenses.isNotEmpty()) "EXPENSE" else "DAILY",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sortedDailyReports = remember(dailyReports) { dailyReports.sortedBy { it.date } }
    val sortedExpenses = remember(expenses) { expenses.sortedBy { it.date } }
    val fullReports = if (allReports.isNotEmpty()) allReports else dailyReports

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modal Header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পিডিএফ প্রিন্ট প্রিভিউ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    val printDocName = "Kazi_Agrotech_${System.currentTimeMillis()}"
                                    printHtmlDocument(
                                        context = context,
                                        docName = printDocName,
                                        html = generateHtmlContent(
                                            title = title,
                                            farmProfile = farmProfile,
                                            dailyReports = sortedDailyReports,
                                            allReports = fullReports,
                                            baselineStock = baselineStock,
                                            expenses = sortedExpenses,
                                            reportCategory = reportCategory
                                        )
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Print",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(
                                onClick = {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "কাজী এগ্রোটেক অফিসিয়াল রিপোর্ট: $title\n" +
                                                    "ফার্ম: ${farmProfile.farmName}\n" +
                                                    "মালিক: ${farmProfile.ownerName}\n" +
                                                    "মোবাইল: ${farmProfile.mobileNumber}\n" +
                                                    "ঠিকানা: ${farmProfile.address}\n\n" +
                                                    "মোট দৈনিক রেকর্ড: ${sortedDailyReports.size} টি, ব্যয় রেকর্ড: ${sortedExpenses.size} টি"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "রিপোর্ট শেয়ার করুন"))
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Printable Document Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFFE2E4E2))
                        .padding(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        shape = RoundedCornerShape(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Official Farm Letterhead
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0FDF4)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FarmLogoDisplay(
                                        logoUri = farmProfile.logoUri,
                                        logoEmoji = farmProfile.logoEmoji,
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = farmProfile.farmName,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D631B),
                                            fontSize = 20.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "লেয়ার পোল্ট্রি ফার্ম",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF333333),
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "প্রোঃ ${farmProfile.ownerName} | মোবাইলঃ ${farmProfile.mobileNumber}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF555555),
                                            fontSize = 11.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "ঠিকানাঃ ${farmProfile.address}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF555555),
                                            fontSize = 10.sp
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.size(60.dp))
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF0D631B), thickness = 2.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Report Title & Meta
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111111)
                                    )
                                )
                                Text(
                                    text = "তারিখ: ${BanglaNumberFormatter.toBanglaDigits(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()))}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF444444)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Table Content Rendered According to Selected Report Category
                            when (reportCategory) {
                                "MONTHLY" -> MonthlyOverviewPdfTable(sortedDailyReports, sortedExpenses)
                                "SALES" -> SalesReportPdfTable(sortedDailyReports)
                                "PRODUCTION" -> ProductionReportPdfTable(sortedDailyReports)
                                "EXPENSE" -> MonthlyExpensePdfTable(sortedExpenses)
                                "PROFIT_LOSS" -> ProfitLossPdfTable(sortedDailyReports, sortedExpenses)
                                else -> DailyReportPdfTable(sortedDailyReports)
                            }

                            Spacer(modifier = Modifier.height(36.dp))

                            // Signature Lines matching official paper registers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(1.dp)
                                            .background(Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "প্রস্তুতকারক",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.Black,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .width(100.dp)
                                            .height(1.dp)
                                            .background(Color.Gray)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "অনুমোদনকারী",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color.Black,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "কাজী এগ্রোটেক স্বয়ংক্রিয় ফার্ম ম্যানেজমেন্ট সিস্টেম দ্বারা প্রস্তুতকৃত।",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color.Gray,
                                    fontSize = 9.sp
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("বন্ধ করুন")
                        }

                        Button(
                            onClick = {
                                val printDocName = "Kazi_Agrotech_Report"
                                printHtmlDocument(
                                    context = context,
                                    docName = printDocName,
                                    html = generateHtmlContent(
                                        title = title,
                                        farmProfile = farmProfile,
                                        dailyReports = sortedDailyReports,
                                        allReports = fullReports,
                                        baselineStock = baselineStock,
                                        expenses = sortedExpenses,
                                        reportCategory = reportCategory
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1.3f).height(46.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Print, contentDescription = "Print")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("প্রিন্ট / সেভ করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 1. Daily Report Table
@Composable
fun DailyReportPdfTable(reports: List<DailyReportEntity>) {
    val scrollState = rememberScrollState()
    val sortedReports = remember(reports) { reports.sortedBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell("মুরগি", width = 65.dp, isHeader = true)
            TableCell("মৃত", width = 50.dp, isHeader = true)
            TableCell("উৎপাদন", width = 75.dp, isHeader = true)
            TableCell("বিক্রয়", width = 75.dp, isHeader = true)
            TableCell("দর (৳)", width = 60.dp, isHeader = true)
            TableCell("মোট বিক্রয় (৳)", width = 110.dp, isHeader = true)
            TableCell("মেডিসিন (৳)", width = 85.dp, isHeader = true)
        }

        sortedReports.forEachIndexed { index, r ->
            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(r.date), width = 75.dp, align = TextAlign.Center)
                TableCell(BanglaNumberFormatter.formatNumber(r.currentBirds), width = 65.dp)
                TableCell(if (r.deadBirds > 0) BanglaNumberFormatter.formatNumber(r.deadBirds) else "০", width = 50.dp)
                TableCell(BanglaNumberFormatter.formatNumber(r.eggProduction), width = 75.dp)
                TableCell(BanglaNumberFormatter.formatNumber(r.eggSold), width = 75.dp)
                TableCell(BanglaNumberFormatter.formatDecimal(r.eggPrice), width = 60.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(r.totalSale), width = 110.dp, isBold = true)
                TableCell(if (r.medicineCost > 0) BanglaNumberFormatter.formatCurrency(r.medicineCost) else "০", width = 85.dp)
            }
        }

        val totalProd = sortedReports.sumOf { it.eggProduction }
        val totalSold = sortedReports.sumOf { it.eggSold }
        val totalSale = sortedReports.sumOf { it.totalSale }
        val totalMed = sortedReports.sumOf { it.medicineCost }

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell("-", width = 65.dp, isHeader = true)
            TableCell("-", width = 50.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatNumber(totalProd), width = 75.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatNumber(totalSold), width = 75.dp, isHeader = true)
            TableCell("-", width = 60.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalSale), width = 110.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalMed), width = 85.dp, isHeader = true)
        }
    }
}

// 2. Monthly Comprehensive Overview Table
@Composable
fun MonthlyOverviewPdfTable(reports: List<DailyReportEntity>, expenses: List<MonthlyExpenseEntity>) {
    val scrollState = rememberScrollState()
    val allDates = remember(reports, expenses) {
        (reports.map { it.date } + expenses.map { it.date }).toSortedSet().toList()
    }
    val reportsByDate = remember(reports) { reports.associateBy { it.date } }
    val expensesByDate = remember(expenses) { expenses.associateBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell("উৎপাদন", width = 65.dp, isHeader = true)
            TableCell("বিক্রয়", width = 65.dp, isHeader = true)
            TableCell("বিক্রয় আয় (৳)", width = 95.dp, isHeader = true)
            TableCell("দৈনিক ব্যয় (৳)", width = 85.dp, isHeader = true)
            TableCell("মাসিক ব্যয় (৳)", width = 85.dp, isHeader = true)
            TableCell("মোট ব্যয় (৳)", width = 95.dp, isHeader = true)
            TableCell("নিট উদ্বৃত্ত (৳)", width = 105.dp, isHeader = true)
        }

        var sumProd = 0
        var sumSold = 0
        var sumSale = 0.0
        var sumDailyExp = 0.0
        var sumMonthlyExp = 0.0
        var sumTotalExp = 0.0
        var sumNet = 0.0

        allDates.forEachIndexed { index, date ->
            val r = reportsByDate[date]
            val e = expensesByDate[date]

            val prod = r?.eggProduction ?: 0
            val sold = r?.eggSold ?: 0
            val sale = r?.totalSale ?: 0.0
            val dailyExp = r?.medicineCost ?: 0.0
            val monthlyExp = e?.totalExpense ?: 0.0
            val totalExp = dailyExp + monthlyExp
            val net = sale - totalExp

            sumProd += prod
            sumSold += sold
            sumSale += sale
            sumDailyExp += dailyExp
            sumMonthlyExp += monthlyExp
            sumTotalExp += totalExp
            sumNet += net

            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(date), width = 75.dp, align = TextAlign.Center)
                TableCell(if (prod > 0) BanglaNumberFormatter.formatNumber(prod) else "-", width = 65.dp)
                TableCell(if (sold > 0) BanglaNumberFormatter.formatNumber(sold) else "-", width = 65.dp)
                TableCell(if (sale > 0) BanglaNumberFormatter.formatCurrency(sale) else "০", width = 95.dp)
                TableCell(if (dailyExp > 0) BanglaNumberFormatter.formatCurrency(dailyExp) else "০", width = 85.dp)
                TableCell(if (monthlyExp > 0) BanglaNumberFormatter.formatCurrency(monthlyExp) else "০", width = 85.dp)
                TableCell(if (totalExp > 0) BanglaNumberFormatter.formatCurrency(totalExp) else "০", width = 95.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(net), width = 105.dp, isBold = true)
            }
        }

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatNumber(sumProd), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatNumber(sumSold), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(sumSale), width = 95.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(sumDailyExp), width = 85.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(sumMonthlyExp), width = 85.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(sumTotalExp), width = 95.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(sumNet), width = 105.dp, isHeader = true)
        }
    }
}

// 3. Sales Report Table
@Composable
fun SalesReportPdfTable(reports: List<DailyReportEntity>) {
    val scrollState = rememberScrollState()
    val salesReports = remember(reports) { reports.filter { it.eggSold > 0 || it.totalSale > 0.0 }.sortedBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 80.dp, isHeader = true, align = TextAlign.Center)
            TableCell("বিক্রির ডিম (টি)", width = 100.dp, isHeader = true)
            TableCell("দর/টি (৳)", width = 80.dp, isHeader = true)
            TableCell("দর/১০০টি (৳)", width = 90.dp, isHeader = true)
            TableCell("মোট বিক্রয় মূল্য (৳)", width = 130.dp, isHeader = true)
            TableCell("মন্তব্য", width = 110.dp, isHeader = true, align = TextAlign.Start)
        }

        salesReports.forEachIndexed { index, r ->
            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            val pricePerHundred = r.eggPrice * 100
            val remarkText = if (r.remarks.isNotBlank()) r.remarks else "নগদ বিক্রয়"
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(r.date), width = 80.dp, align = TextAlign.Center)
                TableCell(BanglaNumberFormatter.formatNumber(r.eggSold), width = 100.dp)
                TableCell(BanglaNumberFormatter.formatDecimal(r.eggPrice), width = 80.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(pricePerHundred), width = 90.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(r.totalSale), width = 130.dp, isBold = true)
                TableCell(remarkText, width = 110.dp, align = TextAlign.Start)
            }
        }

        val totalEggs = salesReports.sumOf { it.eggSold }
        val totalRevenue = salesReports.sumOf { it.totalSale }
        val avgPrice = if (totalEggs > 0) totalRevenue / totalEggs else 0.0

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট / গড়", width = 80.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatNumber(totalEggs), width = 100.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatDecimal(avgPrice), width = 80.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(avgPrice * 100), width = 90.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalRevenue), width = 130.dp, isHeader = true)
            TableCell("-", width = 110.dp, isHeader = true, align = TextAlign.Start)
        }
    }
}

// 4. Production & Flock Health Report Table
@Composable
fun ProductionReportPdfTable(reports: List<DailyReportEntity>) {
    val scrollState = rememberScrollState()
    val sortedReports = remember(reports) { reports.sortedBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 80.dp, isHeader = true, align = TextAlign.Center)
            TableCell("মোট মুরগি", width = 80.dp, isHeader = true)
            TableCell("মৃত মুরগি", width = 70.dp, isHeader = true)
            TableCell("মৃত্যুর হার %", width = 80.dp, isHeader = true)
            TableCell("ডিম উৎপাদন", width = 90.dp, isHeader = true)
            TableCell("লেইং হার %", width = 85.dp, isHeader = true)
            TableCell("মন্তব্য", width = 105.dp, isHeader = true, align = TextAlign.Start)
        }

        sortedReports.forEachIndexed { index, r ->
            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            val mortalityRate = if (r.currentBirds > 0) (r.deadBirds.toDouble() / r.currentBirds * 100) else 0.0
            val layingRate = if (r.currentBirds > 0) (r.eggProduction.toDouble() / r.currentBirds * 100) else 0.0
            val remarkText = if (r.remarks.isNotBlank()) r.remarks else "স্বাভাবিক"

            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(r.date), width = 80.dp, align = TextAlign.Center)
                TableCell(BanglaNumberFormatter.formatNumber(r.currentBirds), width = 80.dp)
                TableCell(if (r.deadBirds > 0) BanglaNumberFormatter.formatNumber(r.deadBirds) else "০", width = 70.dp)
                TableCell(if (mortalityRate > 0) String.format(Locale.US, "%.2f%%", mortalityRate) else "০.০%", width = 80.dp)
                TableCell(BanglaNumberFormatter.formatNumber(r.eggProduction), width = 90.dp, isBold = true)
                TableCell(String.format(Locale.US, "%.1f%%", layingRate), width = 85.dp)
                TableCell(remarkText, width = 105.dp, align = TextAlign.Start)
            }
        }

        val totalMortality = sortedReports.sumOf { it.deadBirds }
        val totalProd = sortedReports.sumOf { it.eggProduction }
        val avgBirds = if (sortedReports.isNotEmpty()) sortedReports.map { it.currentBirds }.average().toInt() else 0
        val overallLayingRate = if (avgBirds > 0 && sortedReports.isNotEmpty()) (totalProd.toDouble() / (avgBirds * sortedReports.size) * 100) else 0.0

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট / গড়", width = 80.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatNumber(avgBirds), width = 80.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatNumber(totalMortality), width = 70.dp, isHeader = true)
            TableCell("-", width = 80.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatNumber(totalProd), width = 90.dp, isHeader = true)
            TableCell(String.format(Locale.US, "%.1f%%", overallLayingRate), width = 85.dp, isHeader = true)
            TableCell("-", width = 105.dp, isHeader = true, align = TextAlign.Start)
        }
    }
}

// 5. Monthly Expense Report Table
@Composable
fun MonthlyExpensePdfTable(expenses: List<MonthlyExpenseEntity>) {
    val scrollState = rememberScrollState()
    val sortedExpenses = remember(expenses) { expenses.sortedBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell("খাদ্য (৳)", width = 75.dp, isHeader = true)
            TableCell("মেডিসিন (৳)", width = 75.dp, isHeader = true)
            TableCell("বাজার (৳)", width = 65.dp, isHeader = true)
            TableCell("বেতন (৳)", width = 75.dp, isHeader = true)
            TableCell("মেরামত (৳)", width = 70.dp, isHeader = true)
            TableCell("সম্পদ (৳)", width = 65.dp, isHeader = true)
            TableCell("বিদ্যুৎ (৳)", width = 65.dp, isHeader = true)
            TableCell("অন্যান্য (৳)", width = 65.dp, isHeader = true)
            TableCell("মোট ব্যয় (৳)", width = 95.dp, isHeader = true)
        }

        sortedExpenses.forEachIndexed { index, e ->
            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(e.date), width = 75.dp, align = TextAlign.Center)
                TableCell(if (e.feedCost > 0) BanglaNumberFormatter.formatCurrency(e.feedCost) else "০", width = 75.dp)
                TableCell(if (e.medicineCost > 0) BanglaNumberFormatter.formatCurrency(e.medicineCost) else "০", width = 75.dp)
                TableCell(if (e.staffMarket > 0) BanglaNumberFormatter.formatCurrency(e.staffMarket) else "০", width = 65.dp)
                TableCell(if (e.staffSalary > 0) BanglaNumberFormatter.formatCurrency(e.staffSalary) else "০", width = 75.dp)
                TableCell(if (e.vehicleRepair > 0) BanglaNumberFormatter.formatCurrency(e.vehicleRepair) else "০", width = 70.dp)
                TableCell(if (e.assets > 0) BanglaNumberFormatter.formatCurrency(e.assets) else "০", width = 65.dp)
                TableCell(if (e.electricityBill > 0) BanglaNumberFormatter.formatCurrency(e.electricityBill) else "০", width = 65.dp)
                TableCell(if (e.otherExpense > 0) BanglaNumberFormatter.formatCurrency(e.otherExpense) else "০", width = 65.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(e.totalExpense), width = 95.dp, isBold = true)
            }
        }

        val totalFeed = sortedExpenses.sumOf { it.feedCost }
        val totalMed = sortedExpenses.sumOf { it.medicineCost }
        val totalMarket = sortedExpenses.sumOf { it.staffMarket }
        val totalSalary = sortedExpenses.sumOf { it.staffSalary }
        val totalVehicle = sortedExpenses.sumOf { it.vehicleRepair }
        val totalAssets = sortedExpenses.sumOf { it.assets }
        val totalElec = sortedExpenses.sumOf { it.electricityBill }
        val totalOthers = sortedExpenses.sumOf { it.otherExpense }
        val grandTotal = sortedExpenses.sumOf { it.totalExpense }

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট", width = 75.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatCurrency(totalFeed), width = 75.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalMed), width = 75.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalMarket), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalSalary), width = 75.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalVehicle), width = 70.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalAssets), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalElec), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalOthers), width = 65.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(grandTotal), width = 95.dp, isHeader = true)
        }
    }
}

// 6. Profit & Loss Statement Table
@Composable
fun ProfitLossPdfTable(reports: List<DailyReportEntity>, expenses: List<MonthlyExpenseEntity>) {
    val scrollState = rememberScrollState()
    val allDates = remember(reports, expenses) {
        (reports.map { it.date } + expenses.map { it.date }).toSortedSet().toList()
    }
    val reportsByDate = remember(reports) { reports.associateBy { it.date } }
    val expensesByDate = remember(expenses) { expenses.associateBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 6.dp)
        ) {
            TableCell("তারিখ", width = 85.dp, isHeader = true, align = TextAlign.Center)
            TableCell("বিক্রয় আয় (৳)", width = 110.dp, isHeader = true)
            TableCell("খামার ব্যয় (৳)", width = 110.dp, isHeader = true)
            TableCell("মুনাফা / (ঘাটতি) (৳)", width = 130.dp, isHeader = true)
            TableCell("স্ট্যাটাস", width = 80.dp, isHeader = true, align = TextAlign.Center)
        }

        var totalSale = 0.0
        var totalExpense = 0.0
        var totalNet = 0.0

        allDates.forEachIndexed { index, date ->
            val r = reportsByDate[date]
            val e = expensesByDate[date]

            val sale = r?.totalSale ?: 0.0
            val exp = (r?.medicineCost ?: 0.0) + (e?.totalExpense ?: 0.0)
            val net = sale - exp
            val isProfit = net >= 0

            totalSale += sale
            totalExpense += exp
            totalNet += net

            val bg = if (index % 2 == 0) Color.White else Color(0xFFF9F9F9)
            Row(
                modifier = Modifier
                    .background(bg)
                    .padding(vertical = 5.dp)
            ) {
                TableCell(BanglaNumberFormatter.formatShortDate(date), width = 85.dp, align = TextAlign.Center)
                TableCell(BanglaNumberFormatter.formatCurrency(sale), width = 110.dp)
                TableCell(BanglaNumberFormatter.formatCurrency(exp), width = 110.dp)
                TableCell(
                    text = BanglaNumberFormatter.formatCurrency(net),
                    width = 130.dp,
                    isBold = true
                )
                TableCell(
                    text = if (isProfit) "মুনাফা" else "ঘাটতি",
                    width = 80.dp,
                    isBold = true,
                    align = TextAlign.Center
                )
            }
        }

        Row(
            modifier = Modifier
                .background(Color(0xFFE8F5E9))
                .padding(vertical = 7.dp)
        ) {
            TableCell("সর্বমোট", width = 85.dp, isHeader = true, align = TextAlign.Center)
            TableCell(BanglaNumberFormatter.formatCurrency(totalSale), width = 110.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalExpense), width = 110.dp, isHeader = true)
            TableCell(BanglaNumberFormatter.formatCurrency(totalNet), width = 130.dp, isHeader = true)
            TableCell(if (totalNet >= 0) "মুনাফা" else "ঘাটতি", width = 80.dp, isHeader = true, align = TextAlign.Center)
        }
    }
}

@Composable
fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    isBold: Boolean = false,
    align: TextAlign = TextAlign.End
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            color = Color.Black,
            fontSize = if (isHeader) 11.sp else 10.sp,
            fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal
        ),
        modifier = Modifier
            .width(width)
            .padding(horizontal = 4.dp),
        textAlign = align
    )
}

fun printHtmlDocument(context: Context, docName: String, html: String) {
    try {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(docName)
                printManager?.print(
                    docName,
                    printAdapter,
                    PrintAttributes.Builder().build()
                )
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        SnackbarController.showError("প্রিন্ট সেবা চালু করা যায়নি: ${e.message}")
    }
}

fun generateHtmlContent(
    title: String,
    farmProfile: FarmProfileEntity,
    dailyReports: List<DailyReportEntity>,
    allReports: List<DailyReportEntity> = dailyReports,
    baselineStock: Int = 0,
    expenses: List<MonthlyExpenseEntity>,
    reportCategory: String = "DAILY"
): String {
    val sortedReports = dailyReports.sortedBy { it.date }
    val sortedExpenses = expenses.sortedBy { it.date }
    val currentDateStr = BanglaNumberFormatter.toBanglaDigits(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()))

    val logoHtml = if (farmProfile.logoUri.isNotBlank()) {
        """<img src="${farmProfile.logoUri}" style="max-height: 60px; max-width: 90px; object-fit: contain; border-radius: 6px;" alt="Farm Logo" />"""
    } else if (farmProfile.logoEmoji.isNotBlank() && farmProfile.logoEmoji != "🐔") {
        """<div style="font-size: 32px;">${farmProfile.logoEmoji}</div>"""
    } else {
        """<div style="font-size: 32px;">🐔</div>"""
    }

    val tableHeaders: String
    val tableRows = StringBuilder()

    when (reportCategory) {
        "MONTHLY" -> {
            tableHeaders = "<th>তারিখ</th><th>উৎপাদন</th><th>বিক্রয়</th><th>বিক্রয় আয় (৳)</th><th>দৈনিক ব্যয় (৳)</th><th>মাসিক ব্যয় (৳)</th><th>মোট ব্যয় (৳)</th><th>নিট উদ্বৃত্ত (৳)</th>"
            val allDates = (sortedReports.map { it.date } + sortedExpenses.map { it.date }).toSortedSet().toList()
            val reportsByDate = sortedReports.associateBy { it.date }
            val expensesByDate = sortedExpenses.associateBy { it.date }

            var sumProd = 0
            var sumSold = 0
            var sumSale = 0.0
            var sumDailyExp = 0.0
            var sumMonthlyExp = 0.0
            var sumTotalExp = 0.0
            var sumNet = 0.0

            for (date in allDates) {
                val r = reportsByDate[date]
                val e = expensesByDate[date]
                val prod = r?.eggProduction ?: 0
                val sold = r?.eggSold ?: 0
                val sale = r?.totalSale ?: 0.0
                val dailyExp = r?.medicineCost ?: 0.0
                val monthlyExp = e?.totalExpense ?: 0.0
                val totalExp = dailyExp + monthlyExp
                val net = sale - totalExp

                sumProd += prod
                sumSold += sold
                sumSale += sale
                sumDailyExp += dailyExp
                sumMonthlyExp += monthlyExp
                sumTotalExp += totalExp
                sumNet += net

                tableRows.append("<tr>")
                tableRows.append("<td style='text-align:center;'>${BanglaNumberFormatter.formatShortDate(date)}</td>")
                tableRows.append("<td>${if (prod > 0) BanglaNumberFormatter.formatNumber(prod) else "-"}</td>")
                tableRows.append("<td>${if (sold > 0) BanglaNumberFormatter.formatNumber(sold) else "-"}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(sale)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(dailyExp)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(monthlyExp)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalExp)}</td>")
                tableRows.append("<td style='font-weight:bold; color:${if (net >= 0) "#0D631B" else "#BA1A1A"};'>${BanglaNumberFormatter.formatCurrency(net)}</td>")
                tableRows.append("</tr>")
            }

            tableRows.append("<tr style='background-color:#E8F5E9; font-weight:bold;'>")
            tableRows.append("<td style='text-align:center;'>সর্বমোট</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatNumber(sumProd)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatNumber(sumSold)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(sumSale)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(sumDailyExp)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(sumMonthlyExp)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(sumTotalExp)}</td>")
            tableRows.append("<td style='color:${if (sumNet >= 0) "#0D631B" else "#BA1A1A"};'>${BanglaNumberFormatter.formatCurrency(sumNet)}</td></tr>")
        }

        "SALES" -> {
            tableHeaders = "<th>তারিখ</th><th>বিক্রির ডিম (টি)</th><th>দর/টি (৳)</th><th>দর/১০০টি (৳)</th><th>মোট বিক্রয় মূল্য (৳)</th><th>মন্তব্য</th>"
            val salesReports = sortedReports.filter { it.eggSold > 0 || it.totalSale > 0.0 }
            for (r in salesReports) {
                val pricePerHundred = r.eggPrice * 100
                val remarkText = if (r.remarks.isNotBlank()) r.remarks else "নগদ বিক্রয়"
                tableRows.append("<tr>")
                tableRows.append("<td style='text-align:center;'>${BanglaNumberFormatter.formatShortDate(r.date)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.eggSold)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatDecimal(r.eggPrice)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(pricePerHundred)}</td>")
                tableRows.append("<td style='font-weight:bold; color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(r.totalSale)}</td>")
                tableRows.append("<td style='text-align:left;'>$remarkText</td>")
                tableRows.append("</tr>")
            }
            val totalEggs = salesReports.sumOf { it.eggSold }
            val totalRevenue = salesReports.sumOf { it.totalSale }
            val avgPrice = if (totalEggs > 0) totalRevenue / totalEggs else 0.0
            tableRows.append("<tr style='background-color:#E8F5E9; font-weight:bold;'>")
            tableRows.append("<td style='text-align:center;'>সর্বমোট</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatNumber(totalEggs)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatDecimal(avgPrice)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(avgPrice * 100)}</td>")
            tableRows.append("<td style='color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(totalRevenue)}</td><td>-</td></tr>")
        }

        "PRODUCTION" -> {
            tableHeaders = "<th>তারিখ</th><th>মোট মুরগি</th><th>মৃত মুরগি</th><th>মৃত্যুর হার %</th><th>ডিম উৎপাদন</th><th>লেইং হার %</th><th>মন্তব্য</th>"
            for (r in sortedReports) {
                val mortalityRate = if (r.currentBirds > 0) (r.deadBirds.toDouble() / r.currentBirds * 100) else 0.0
                val layingRate = if (r.currentBirds > 0) (r.eggProduction.toDouble() / r.currentBirds * 100) else 0.0
                val remarkText = if (r.remarks.isNotBlank()) r.remarks else "স্বাভাবিক"
                tableRows.append("<tr>")
                tableRows.append("<td style='text-align:center;'>${BanglaNumberFormatter.formatShortDate(r.date)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.currentBirds)}</td>")
                tableRows.append("<td>${if (r.deadBirds > 0) BanglaNumberFormatter.formatNumber(r.deadBirds) else "০"}</td>")
                tableRows.append("<td>${String.format(Locale.US, "%.2f%%", mortalityRate)}</td>")
                tableRows.append("<td style='font-weight:bold; color:#0D631B;'>${BanglaNumberFormatter.formatNumber(r.eggProduction)}</td>")
                tableRows.append("<td>${String.format(Locale.US, "%.1f%%", layingRate)}</td>")
                tableRows.append("<td style='text-align:left;'>$remarkText</td>")
                tableRows.append("</tr>")
            }
            val totalMortality = sortedReports.sumOf { it.deadBirds }
            val totalProd = sortedReports.sumOf { it.eggProduction }
            val avgBirds = if (sortedReports.isNotEmpty()) sortedReports.map { it.currentBirds }.average().toInt() else 0
            val overallLayingRate = if (avgBirds > 0 && sortedReports.isNotEmpty()) (totalProd.toDouble() / (avgBirds * sortedReports.size) * 100) else 0.0
            tableRows.append("<tr style='background-color:#E8F5E9; font-weight:bold;'>")
            tableRows.append("<td style='text-align:center;'>সর্বমোট / গড়</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatNumber(avgBirds)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatNumber(totalMortality)}</td>")
            tableRows.append("<td>-</td>")
            tableRows.append("<td style='color:#0D631B;'>${BanglaNumberFormatter.formatNumber(totalProd)}</td>")
            tableRows.append("<td>${String.format(Locale.US, "%.1f%%", overallLayingRate)}</td><td>-</td></tr>")
        }

        "EXPENSE" -> {
            tableHeaders = "<th>তারিখ</th><th>খাদ্য (৳)</th><th>মেডিসিন (৳)</th><th>বাজার (৳)</th><th>বেতন (৳)</th><th>মেরামত (৳)</th><th>সম্পদ (৳)</th><th>বিদ্যুৎ (৳)</th><th>অন্যান্য (৳)</th><th>মোট ব্যয় (৳)</th>"
            for (e in sortedExpenses) {
                tableRows.append("<tr>")
                tableRows.append("<td style='text-align:center;'>${BanglaNumberFormatter.formatShortDate(e.date)}</td>")
                tableRows.append("<td>${if (e.feedCost > 0) BanglaNumberFormatter.formatCurrency(e.feedCost) else "০"}</td>")
                tableRows.append("<td>${if (e.medicineCost > 0) BanglaNumberFormatter.formatCurrency(e.medicineCost) else "০"}</td>")
                tableRows.append("<td>${if (e.staffMarket > 0) BanglaNumberFormatter.formatCurrency(e.staffMarket) else "০"}</td>")
                tableRows.append("<td>${if (e.staffSalary > 0) BanglaNumberFormatter.formatCurrency(e.staffSalary) else "০"}</td>")
                tableRows.append("<td>${if (e.vehicleRepair > 0) BanglaNumberFormatter.formatCurrency(e.vehicleRepair) else "০"}</td>")
                tableRows.append("<td>${if (e.assets > 0) BanglaNumberFormatter.formatCurrency(e.assets) else "০"}</td>")
                tableRows.append("<td>${if (e.electricityBill > 0) BanglaNumberFormatter.formatCurrency(e.electricityBill) else "০"}</td>")
                tableRows.append("<td>${if (e.otherExpense > 0) BanglaNumberFormatter.formatCurrency(e.otherExpense) else "০"}</td>")
                tableRows.append("<td style='font-weight:bold; color:#BA1A1A;'>${BanglaNumberFormatter.formatCurrency(e.totalExpense)}</td>")
                tableRows.append("</tr>")
            }
            val totalFeed = sortedExpenses.sumOf { it.feedCost }
            val totalMed = sortedExpenses.sumOf { it.medicineCost }
            val totalMarket = sortedExpenses.sumOf { it.staffMarket }
            val totalSalary = sortedExpenses.sumOf { it.staffSalary }
            val totalVehicle = sortedExpenses.sumOf { it.vehicleRepair }
            val totalAssets = sortedExpenses.sumOf { it.assets }
            val totalElec = sortedExpenses.sumOf { it.electricityBill }
            val totalOthers = sortedExpenses.sumOf { it.otherExpense }
            val grandTotal = sortedExpenses.sumOf { it.totalExpense }
            tableRows.append("<tr style='background-color:#E8F5E9; font-weight:bold;'>")
            tableRows.append("<td style='text-align:center;'>সর্বমোট</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalFeed)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalMed)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalMarket)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalSalary)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalVehicle)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalAssets)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalElec)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalOthers)}</td>")
            tableRows.append("<td style='color:#BA1A1A;'>${BanglaNumberFormatter.formatCurrency(grandTotal)}</td></tr>")
        }

        "PROFIT_LOSS" -> {
            tableHeaders = "<th>তারিখ</th><th>বিক্রয় আয় (৳)</th><th>খামার ব্যয় (৳)</th><th>মুনাফা / (ঘাটতি) (৳)</th><th>স্ট্যাটাস</th>"
            val allDates = (sortedReports.map { it.date } + sortedExpenses.map { it.date }).toSortedSet().toList()
            val reportsByDate = sortedReports.associateBy { it.date }
            val expensesByDate = sortedExpenses.associateBy { it.date }

            var totalSale = 0.0
            var totalExpense = 0.0
            var totalNet = 0.0

            for (date in allDates) {
                val r = reportsByDate[date]
                val e = expensesByDate[date]
                val sale = r?.totalSale ?: 0.0
                val exp = (r?.medicineCost ?: 0.0) + (e?.totalExpense ?: 0.0)
                val net = sale - exp
                val isProfit = net >= 0

                totalSale += sale
                totalExpense += exp
                totalNet += net

                tableRows.append("<tr>")
                tableRows.append("<td style='text-align:center;'>${BanglaNumberFormatter.formatShortDate(date)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(sale)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(exp)}</td>")
                tableRows.append("<td style='font-weight:bold; color:${if (isProfit) "#0D631B" else "#BA1A1A"};'>${BanglaNumberFormatter.formatCurrency(net)}</td>")
                tableRows.append("<td style='text-align:center; font-weight:bold; color:${if (isProfit) "#0D631B" else "#BA1A1A"};'>${if (isProfit) "মুনাফা" else "ঘাটতি"}</td>")
                tableRows.append("</tr>")
            }

            tableRows.append("<tr style='background-color:#E8F5E9; font-weight:bold;'>")
            tableRows.append("<td style='text-align:center;'>সর্বমোট</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalSale)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalExpense)}</td>")
            tableRows.append("<td style='color:${if (totalNet >= 0) "#0D631B" else "#BA1A1A"};'>${BanglaNumberFormatter.formatCurrency(totalNet)}</td>")
            tableRows.append("<td style='text-align:center;'>${if (totalNet >= 0) "মুনাফা" else "ঘাটতি"}</td></tr>")
        }

        else -> { // DAILY
            tableHeaders = "<th>তারিখ</th><th>মুরগি</th><th>মৃত</th><th>উৎপাদন</th><th>বিক্রয়</th><th>দর (৳)</th><th>মোট বিক্রয় (৳)</th><th>মেডিসিন (৳)</th>"
            for (r in sortedReports) {
                tableRows.append("<tr>")
                tableRows.append("<td style='text-align:center;'>${BanglaNumberFormatter.formatShortDate(r.date)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.currentBirds)}</td>")
                tableRows.append("<td>${if (r.deadBirds > 0) BanglaNumberFormatter.formatNumber(r.deadBirds) else "০"}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.eggProduction)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.eggSold)}</td>")
                tableRows.append("<td>${BanglaNumberFormatter.formatDecimal(r.eggPrice)}</td>")
                tableRows.append("<td style='font-weight:bold; color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(r.totalSale)}</td>")
                tableRows.append("<td>${if (r.medicineCost > 0) BanglaNumberFormatter.formatCurrency(r.medicineCost) else "০"}</td>")
                tableRows.append("</tr>")
            }
            val totalProd = sortedReports.sumOf { it.eggProduction }
            val totalSold = sortedReports.sumOf { it.eggSold }
            val totalSale = sortedReports.sumOf { it.totalSale }
            val totalMed = sortedReports.sumOf { it.medicineCost }
            tableRows.append("<tr style='background-color:#E8F5E9; font-weight:bold;'>")
            tableRows.append("<td style='text-align:center;'>সর্বমোট</td><td>-</td><td>-</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatNumber(totalProd)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatNumber(totalSold)}</td>")
            tableRows.append("<td>-</td>")
            tableRows.append("<td style='color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(totalSale)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatCurrency(totalMed)}</td></tr>")
        }
    }

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: 'SolaimanLipi', Arial, sans-serif; margin: 20px; color: #111; }
                .header { display: flex; align-items: center; border-bottom: 2px solid #0D631B; padding-bottom: 8px; margin-bottom: 12px; }
                .header-logo { flex: 0 0 90px; margin-right: 14px; text-align: left; }
                .header-text { flex: 1 1 auto; text-align: center; }
                .header-spacer { flex: 0 0 90px; margin-left: 14px; }
                .title { font-size: 22px; font-weight: bold; color: #0D631B; margin: 0; }
                .subtitle { font-size: 13px; color: #444; margin: 3px 0; }
                .meta { display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 13px; }
                table { width: 100%; border-collapse: collapse; margin-top: 8px; font-size: 11px; }
                th, td { border: 1px solid #777; padding: 6px 4px; text-align: right; }
                th { background-color: #E8F5E9; font-weight: bold; text-align: center; }
                .footer-signatures { display: flex; justify-content: space-between; margin-top: 60px; font-size: 12px; }
                .sig-line { border-top: 1px solid #444; width: 120px; text-align: center; padding-top: 4px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="header-logo">$logoHtml</div>
                <div class="header-text">
                    <h1 class="title">${farmProfile.farmName}</h1>
                    <div class="subtitle">লেয়ার পোল্ট্রি ফার্ম</div>
                    <div class="subtitle">প্রোঃ ${farmProfile.ownerName} | মোবাইলঃ ${farmProfile.mobileNumber}</div>
                    <div class="subtitle">ঠিকানাঃ ${farmProfile.address}</div>
                </div>
                <div class="header-spacer"></div>
            </div>
            <div class="meta">
                <strong>$title</strong>
                <span>তারিখ: $currentDateStr</span>
            </div>
            <table>
                <thead>
                    <tr>$tableHeaders</tr>
                </thead>
                <tbody>
                    $tableRows
                </tbody>
            </table>
            <div class="footer-signatures">
                <div class="sig-line">প্রস্তুতকারক</div>
                <div class="sig-line">অনুমোদনকারী</div>
            </div>
        </body>
        </html>
    """.trimIndent()
}
