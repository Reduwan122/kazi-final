package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.domain.StockCalculationService
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isDailyReport = dailyReports.isNotEmpty()
    // Chronologically sort entries for PDF preview and print (oldest/first date on top, newest/last date at bottom)
    val sortedDailyReports = remember(dailyReports) { dailyReports.sortedBy { it.date } }
    val sortedExpenses = remember(expenses) { expenses.sortedBy { it.date } }
    // Use full history for stock calculations; fall back to filtered reports if no full history
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
                                    printHtmlDocument(context, printDocName, generateHtmlContent(title, farmProfile, sortedDailyReports, fullReports, baselineStock, sortedExpenses))
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
                                            "কাজী এগ্রোটেক অফিসিয়াল রিপোর্ট\n" +
                                                    "ফার্ম: ${farmProfile.farmName}\n" +
                                                    "মালিক: ${farmProfile.ownerName}\n" +
                                                    "মোবাইল: ${farmProfile.mobileNumber}\n" +
                                                    "ঠিকানা: ${farmProfile.address}\n\n" +
                                                    "মোট রেকর্ড: ${if (isDailyReport) sortedDailyReports.size else sortedExpenses.size} টি"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "রিপোর্ট শেয়ার করুন"))
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

                            // Table Content
                            if (isDailyReport) {
                                DailyReportPdfTable(sortedDailyReports, fullReports, baselineStock)
                            } else {
                                MonthlyExpensePdfTable(sortedExpenses)
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
                                printHtmlDocument(context, printDocName, generateHtmlContent(title, farmProfile, sortedDailyReports, fullReports, baselineStock, sortedExpenses))
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

@Composable
fun DailyReportPdfTable(
    reports: List<DailyReportEntity>,
    allReports: List<DailyReportEntity> = reports,
    baselineStock: Int = 0
) {
    val scrollState = rememberScrollState()
    val sortedReports = remember(reports) { reports.sortedBy { it.date } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(1.dp, Color.Black)
    ) {
        // Table Header
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
        }

        // Table Rows
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
            }
        }

        // Grand Total Row
        val totalProd = sortedReports.sumOf { it.eggProduction }
        val totalSold = sortedReports.sumOf { it.eggSold }
        val totalSale = sortedReports.sumOf { it.totalSale }

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
        }
    }
}

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
        // Table Header
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

        // Table Rows
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

        // Grand Total Row
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
    expenses: List<MonthlyExpenseEntity>
): String {
    // Chronologically sort entries for PDF generation (oldest/first date on top, newest/last date at bottom)
    val sortedReports = dailyReports.sortedBy { it.date }
    val sortedExpenses = expenses.sortedBy { it.date }
    val isDaily = sortedReports.isNotEmpty()
    val currentDateStr = BanglaNumberFormatter.toBanglaDigits(SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date()))

    val logoHtml = if (farmProfile.logoUri.isNotBlank()) {
        """<img src="${farmProfile.logoUri}" style="max-height: 60px; max-width: 90px; object-fit: contain; border-radius: 6px;" alt="Farm Logo" />"""
    } else if (farmProfile.logoEmoji.isNotBlank() && farmProfile.logoEmoji != "🐔") {
        """<div style="font-size: 32px;">${farmProfile.logoEmoji}</div>"""
    } else {
        """<div style="font-size: 32px;">🐔</div>"""
    }

    val tableHeaders = if (isDaily) {
        "<th>তারিখ</th><th>মুরগি</th><th>মৃত</th><th>উৎপাদন</th><th>বিক্রয়</th><th>দর (৳)</th><th>মোট বিক্রয় (৳)</th>"
    } else {
        "<th>তারিখ</th><th>খাদ্য (৳)</th><th>মেডিসিন (৳)</th><th>বাজার (৳)</th><th>বেতন (৳)</th><th>মেরামত (৳)</th><th>সম্পদ (৳)</th><th>বিদ্যুৎ (৳)</th><th>অন্যান্য (৳)</th><th>মোট ব্যয় (৳)</th>"
    }

    val tableRows = StringBuilder()
    if (isDaily) {
        for (r in sortedReports) {
            tableRows.append("<tr>")
            tableRows.append("<td style='text-align:center;'>${BanglaNumberFormatter.formatShortDate(r.date)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.currentBirds)}</td>")
            tableRows.append("<td>${if (r.deadBirds > 0) BanglaNumberFormatter.formatNumber(r.deadBirds) else "০"}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.eggProduction)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatNumber(r.eggSold)}</td>")
            tableRows.append("<td>${BanglaNumberFormatter.formatDecimal(r.eggPrice)}</td>")
            tableRows.append("<td style='font-weight:bold; color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(r.totalSale)}</td>")
            tableRows.append("</tr>")
        }

        // Totals
        val totalProd = sortedReports.sumOf { it.eggProduction }
        val totalSold = sortedReports.sumOf { it.eggSold }
        val totalSale = sortedReports.sumOf { it.totalSale }

        tableRows.append("<tr style='background-color:#E8F5E9; font-weight:bold;'>")
        tableRows.append("<td style='text-align:center;'>সর্বমোট</td><td>-</td><td>-</td>")
        tableRows.append("<td>${BanglaNumberFormatter.formatNumber(totalProd)}</td>")
        tableRows.append("<td>${BanglaNumberFormatter.formatNumber(totalSold)}</td>")
        tableRows.append("<td>-</td>")
        tableRows.append("<td style='color:#0D631B;'>${BanglaNumberFormatter.formatCurrency(totalSale)}</td></tr>")
    } else {
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

        // Totals
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
