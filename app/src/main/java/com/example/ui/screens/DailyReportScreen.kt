package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyReportEntity
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.RowActionBottomSheetDialog
import com.example.ui.viewmodel.PoultryViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyReportScreen(
    viewModel: PoultryViewModel,
    onNavigateToAddReport: () -> Unit,
    onNavigateToEditReport: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onPreviewPdf: (List<DailyReportEntity>) -> Unit
) {
    val context = LocalContext.current
    val dailyReports by viewModel.dailyReports.collectAsState()
    val farmProfile by viewModel.farmProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val rolePermissionsMap by viewModel.rolePermissions.collectAsState()

    val userPerms = currentUser?.let { rolePermissionsMap[it.role.uppercase()] }
    val canAddReport = currentUser?.canAddReport(userPerms) ?: false
    val canEditReport = currentUser?.canEditReport(userPerms) ?: false
    val canDeleteReport = currentUser?.canDeleteReport(userPerms) ?: false
    val canDownloadPdf = currentUser?.canDownloadReports(userPerms) ?: false

    var searchQuery by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("সকল রেকর্ড") }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    var selectedReportForAction by remember { mutableStateOf<DailyReportEntity?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }

    // Months that actually have data, plus the current month, newest first
    val availableMonths = remember(dailyReports) {
        (dailyReports.map { it.date.take(7) } + BanglaNumberFormatter.getCurrentDateFormatted().take(7))
            .toSortedSet(compareByDescending { it })
            .toList()
    }

    // Filter reports
    val filteredReports = remember(dailyReports, searchQuery, selectedMonth) {
        dailyReports.filter { report ->
            val matchesSearch = searchQuery.isEmpty() ||
                    report.date.contains(searchQuery, ignoreCase = true) ||
                    report.remarks.contains(searchQuery, ignoreCase = true) ||
                    BanglaNumberFormatter.formatBanglaDate(report.date).contains(searchQuery, ignoreCase = true)

            val matchesMonth = if (selectedMonth == "সকল রেকর্ড") {
                true
            } else {
                report.date.startsWith(selectedMonth)
            }

            matchesSearch && matchesMonth
        }
    }

    // Totals
    val totalProduction = filteredReports.sumOf { it.eggProduction }
    val totalSold = filteredReports.sumOf { it.eggSold }
    val totalSaleAmount = filteredReports.sumOf { it.totalSale }
    val totalMedicine = filteredReports.sumOf { it.medicineCost }

    val horizontalScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "দৈনিক রিপোর্ট",
                isRootScreen = true,
                logoUri = farmProfile.logoUri,
                logoEmoji = farmProfile.logoEmoji
            )
        },
        floatingActionButton = {
            if (canAddReport) {
                FloatingActionButton(
                    onClick = onNavigateToAddReport,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("daily_report_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Report",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .testTag("daily_report_screen")
        ) {
            // Sticky Toolbar: Search & Export
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "দৈনিক রেজিস্টার",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { onPreviewPdf(filteredReports) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp).testTag("btn_export_pdf")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পিডিএফ", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            OutlinedButton(
                                onClick = { viewModel.exportDailyReportsCsv(context) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp).testTag("btn_export_excel")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TableView,
                                    contentDescription = "Excel",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("এক্সেল", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Search & Month Filter Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("খুঁজুন...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("daily_search_field")
                        )

                        Box {
                            OutlinedButton(
                                onClick = { monthMenuExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text(
                                    text = if (selectedMonth == "সকল রেকর্ড") "সকল রেকর্ড" else BanglaNumberFormatter.formatYearMonth(selectedMonth),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Month",
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = monthMenuExpanded,
                                onDismissRequest = { monthMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("সকল রেকর্ড") },
                                    onClick = {
                                        selectedMonth = "সকল রেকর্ড"
                                        monthMenuExpanded = false
                                    }
                                )
                                availableMonths.forEach { month ->
                                    val currentMonth = BanglaNumberFormatter.getCurrentDateFormatted().take(7)
                                    val label = BanglaNumberFormatter.formatYearMonth(month) +
                                        if (month == currentMonth) " (চলতি মাস)" else ""
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedMonth = month
                                            monthMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Modern Mobile Sheet Table View
            if (filteredReports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "কোনো দৈনিক রিপোর্ট পাওয়া যায়নি।\nনতুন রিপোর্ট যোগ করতে '+' বাটনে চাপুন।",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 72.dp)
                    ) {
                        // Sticky Header Row
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(horizontalScrollState)
                                        .padding(vertical = 10.dp)
                                ) {
                                    // Frozen Date Column Header
                                    Text(
                                        text = "তারিখ",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(85.dp).padding(start = 14.dp),
                                        textAlign = TextAlign.Start
                                    )
                                    Text(
                                        text = "মুরগী",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(75.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "মৃত",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.width(60.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "উৎপাদন",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "বিক্রয় (পিস)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(85.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "দর (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(70.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "মোট বিক্রয় (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.width(105.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "ঔষধ (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "ডিম স্টক",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(85.dp).padding(end = 14.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        // Data Rows
                        items(filteredReports, key = { it.id }) { report ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onNavigateToDetail(report.id) },
                                        onLongClick = {
                                            selectedReportForAction = report
                                            showActionDialog = true
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(horizontalScrollState)
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Frozen Date
                                    Text(
                                        text = BanglaNumberFormatter.formatShortDate(report.date),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.width(85.dp).padding(start = 14.dp),
                                        textAlign = TextAlign.Start
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatNumber(report.currentBirds),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(75.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (report.deadBirds > 0) BanglaNumberFormatter.formatNumber(report.deadBirds) else "০",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (report.deadBirds > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (report.deadBirds > 0) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        modifier = Modifier.width(60.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatNumber(report.eggProduction),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatNumber(report.eggSold),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(85.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatDecimal(report.eggPrice),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(70.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(report.totalSale),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.width(105.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (report.medicineCost > 0) BanglaNumberFormatter.formatCurrency(report.medicineCost) else "০",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatNumber(report.currentStock),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.width(85.dp).padding(end = 14.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        }

                        // Summary Grand Total Footer
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(horizontalScrollState)
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "সর্বমোট",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(85.dp).padding(start = 14.dp),
                                        textAlign = TextAlign.Start
                                    )
                                    Text(
                                        text = "-",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(75.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "-",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(60.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatNumber(totalProduction),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatNumber(totalSold),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(85.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "-",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(70.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(totalSaleAmount),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 15.sp
                                        ),
                                        modifier = Modifier.width(105.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(totalMedicine),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "-",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(85.dp).padding(end = 14.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showActionDialog && selectedReportForAction != null) {
        val report = selectedReportForAction!!
        RowActionBottomSheetDialog(
            title = "রিপোর্ট: ${BanglaNumberFormatter.formatBanglaDate(report.date)}",
            onDismiss = { showActionDialog = false },
            onView = { onNavigateToDetail(report.id) },
            onEdit = { onNavigateToEditReport(report.id) },
            onDelete = { viewModel.deleteDailyReport(report.id) },
            canEdit = canEditReport,
            canDelete = canDeleteReport
        )
    }
}
