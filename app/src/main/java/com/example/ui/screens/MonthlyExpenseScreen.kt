package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MonthlyExpenseEntity
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.MonthlyExpenseShareDialog
import com.example.ui.components.RowActionBottomSheetDialog
import com.example.ui.viewmodel.PoultryViewModel
import com.example.ui.components.rememberHaptics

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyExpenseScreen(
    viewModel: PoultryViewModel,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onPreviewExpensePdf: (List<MonthlyExpenseEntity>) -> Unit,
    onOpenNotifications: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val expenses by viewModel.expenses.collectAsState()
    val dailyReports by viewModel.dailyReports.collectAsState()
    val farmProfile by viewModel.farmProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val rolePermissionsMap by viewModel.rolePermissions.collectAsState()

    val todayDate = remember { BanglaNumberFormatter.getCurrentDateFormatted() }
    val hasTodayReport = remember(dailyReports, todayDate) { dailyReports.any { it.date == todayDate } }
    val hasUnreadNotification = !hasTodayReport

    val userPerms = currentUser?.let { rolePermissionsMap[it.role.uppercase()] }
    val canViewExpense = currentUser?.canViewExpense(userPerms) ?: false
    val canAddExpense = currentUser?.canAddExpense(userPerms) ?: false
    val canEditExpense = currentUser?.canEditExpense(userPerms) ?: false
    val canDeleteExpense = currentUser?.canDeleteExpense(userPerms) ?: false
    val canDownloadExpensePdf = currentUser?.canDownloadReports(userPerms) ?: false

    var searchQuery by remember { mutableStateOf("") }
    var selectedMonth by remember { mutableStateOf("সকল রেকর্ড") }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    var selectedExpenseForAction by remember { mutableStateOf<MonthlyExpenseEntity?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var shareCardExpense by remember { mutableStateOf<MonthlyExpenseEntity?>(null) }

    // Months that actually have data, plus the current month, newest first
    val availableMonths = remember(expenses) {
        (expenses.map { it.date.take(7) } + BanglaNumberFormatter.getCurrentDateFormatted().take(7))
            .toSortedSet(compareByDescending { it })
            .toList()
    }

    // Filter expenses
    val filteredExpenses = remember(expenses, searchQuery, selectedMonth) {
        expenses.filter { exp ->
            val matchesSearch = searchQuery.isEmpty() ||
                    exp.date.contains(searchQuery, ignoreCase = true) ||
                    exp.remarks.contains(searchQuery, ignoreCase = true) ||
                    BanglaNumberFormatter.formatBanglaDate(exp.date).contains(searchQuery, ignoreCase = true)

            val matchesMonth = if (selectedMonth == "সকল রেকর্ড") {
                true
            } else {
                exp.date.startsWith(selectedMonth)
            }

            matchesSearch && matchesMonth
        }
    }

    // Totals
    val totalFeed = filteredExpenses.sumOf { it.feedCost }
    val totalMedicine = filteredExpenses.sumOf { it.medicineCost }
    val totalStaff = filteredExpenses.sumOf { it.staffMarket + it.staffSalary }
    val totalVehicle = filteredExpenses.sumOf { it.vehicleRepair }
    val totalAssets = filteredExpenses.sumOf { it.assets }
    val totalElectricity = filteredExpenses.sumOf { it.electricityBill }
    val totalOthers = filteredExpenses.sumOf { it.otherExpense }
    val grandTotalExpense = filteredExpenses.sumOf { it.totalExpense }

    val horizontalScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "মাসিক ব্যয়",
                isRootScreen = true,
                logoUri = farmProfile.logoUri,
                logoEmoji = farmProfile.logoEmoji,
                userProfileImageUri = currentUser?.profileImageUri ?: "",
                username = currentUser?.username ?: "",
                hasUnreadNotification = hasUnreadNotification,
                onNotificationClick = onOpenNotifications,
                onProfileClick = onNavigateToProfile
            )
        },
        floatingActionButton = {
            if (canAddExpense) {
                FloatingActionButton(
                    onClick = {
                        haptics.tap()
                        onNavigateToAddExpense()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("expense_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Expense",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (!canViewExpense) {
            com.example.ui.components.AccessDeniedView(
                title = "মাসিক ব্যয় সংরক্ষিত",
                message = "আপনার রোলে ব্যয়ের হিসাব দেখার অনুমতি সক্রিয় করা নেই।",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(innerPadding)
                    .testTag("monthly_expense_screen")
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
                                text = "মাসিক ব্যয় রেজিস্টার",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            if (canDownloadExpensePdf) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onPreviewExpensePdf(filteredExpenses) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp).testTag("btn_expense_pdf")
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
                                        onClick = { viewModel.exportExpensesCsv(context) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp).testTag("btn_expense_excel")
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

                    // Search & Filter Row
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
                                .testTag("expense_search_field")
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

            // Mobile Sheet Table View
            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "কোনো ব্যয় রেকর্ড পাওয়া যায়নি।\nনতুন ব্যয় যোগ করতে '+' বাটনে চাপুন।",
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
                                        text = "খাদ্য/ফিড (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(90.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "মেডিসিন (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(85.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "স্টাফ বাজার (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(90.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "বেতন (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(85.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "মেরামত (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "সম্পদ (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "বিদ্যুৎ (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(75.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "অন্যান্য (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(75.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "মোট ব্যয় (৳)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.width(105.dp).padding(end = 14.dp),
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }

                        // Data Rows
                        items(filteredExpenses, key = { it.id }) { exp ->
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onNavigateToDetail(exp.id) },
                                        onLongClick = {
                                            selectedExpenseForAction = exp
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
                                        text = BanglaNumberFormatter.formatShortDate(exp.date),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.width(85.dp).padding(start = 14.dp),
                                        textAlign = TextAlign.Start
                                    )
                                    Text(
                                        text = if (exp.feedCost > 0) BanglaNumberFormatter.formatCurrency(exp.feedCost) else "০",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(90.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (exp.medicineCost > 0) BanglaNumberFormatter.formatCurrency(exp.medicineCost) else "০",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(85.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (exp.staffMarket > 0) BanglaNumberFormatter.formatCurrency(exp.staffMarket) else "০",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(90.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (exp.staffSalary > 0) BanglaNumberFormatter.formatCurrency(exp.staffSalary) else "০",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(85.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (exp.vehicleRepair > 0) BanglaNumberFormatter.formatCurrency(exp.vehicleRepair) else "০",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (exp.assets > 0) BanglaNumberFormatter.formatCurrency(exp.assets) else "০",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (exp.electricityBill > 0) BanglaNumberFormatter.formatCurrency(exp.electricityBill) else "০",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(75.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = if (exp.otherExpense > 0) BanglaNumberFormatter.formatCurrency(exp.otherExpense) else "০",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(75.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(exp.totalExpense),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.width(105.dp).padding(end = 14.dp),
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
                                        text = BanglaNumberFormatter.formatCurrency(totalFeed),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(90.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(totalMedicine),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(85.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(filteredExpenses.sumOf { it.staffMarket }),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(90.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(filteredExpenses.sumOf { it.staffSalary }),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(85.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(totalVehicle),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(totalAssets),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(80.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(totalElectricity),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(75.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(totalOthers),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.width(75.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = BanglaNumberFormatter.formatCurrency(grandTotalExpense),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 15.sp
                                        ),
                                        modifier = Modifier.width(105.dp).padding(end = 14.dp),
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

    if (showActionDialog && selectedExpenseForAction != null) {
        val exp = selectedExpenseForAction!!
        RowActionBottomSheetDialog(
            title = "ব্যয়: ${BanglaNumberFormatter.formatBanglaDate(exp.date)}",
            onDismiss = { showActionDialog = false },
            onView = { onNavigateToDetail(exp.id) },
            onShare = { shareCardExpense = exp },
            onEdit = { onNavigateToEditExpense(exp.id) },
            onDelete = {
                haptics.confirm()
                viewModel.deleteExpense(exp.id)
            },
            canEdit = canEditExpense,
            canDelete = canDeleteExpense
        )
    }

    if (shareCardExpense != null) {
        MonthlyExpenseShareDialog(
            expense = shareCardExpense!!,
            farmProfile = farmProfile,
            onDismiss = { shareCardExpense = null }
        )
    }
}
