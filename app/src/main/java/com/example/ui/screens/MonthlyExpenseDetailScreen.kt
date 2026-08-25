package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MonthlyExpenseEntity
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MonthlyExpenseShareDialog
import com.example.ui.components.DetailAction
import com.example.ui.components.DetailActionBar
import com.example.ui.components.DetailActionTone
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.rememberHaptics
import com.example.ui.viewmodel.PoultryViewModel

/** One line of the expense breakdown: the category, its icon and the amount spent. */
private data class ExpenseLine(
    val label: String,
    val icon: ImageVector,
    val amount: Double
)

@Composable
fun MonthlyExpenseDetailScreen(
    expenseId: Long,
    viewModel: PoultryViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onPdfPreview: (MonthlyExpenseEntity) -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val expenses by viewModel.expenses.collectAsState()
    val farmProfile by viewModel.farmProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val rolePermissionsMap by viewModel.rolePermissions.collectAsState()
    val expense = expenses.find { it.id == expenseId }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showShareCardDialog by remember { mutableStateOf(false) }

    val userPerms = currentUser?.let { rolePermissionsMap[it.role.uppercase()] }
    val canEdit = currentUser?.canEditExpense(userPerms) ?: false
    val canDelete = currentUser?.canDeleteExpense(userPerms) ?: false

    if (expense == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("ব্যয় এন্ট্রিটি পাওয়া যায়নি")
        }
        return
    }

    val breakdown = listOf(
        ExpenseLine("খাদ্য / ফিড", Icons.Default.Restaurant, expense.feedCost),
        ExpenseLine("মেডিসিন ও ভ্যাকসিন", Icons.Default.MedicalServices, expense.medicineCost),
        ExpenseLine("স্টাফ বাজার", Icons.Default.ShoppingCart, expense.staffMarket),
        ExpenseLine("স্টাফ বেতন / মজুরি", Icons.Default.Payments, expense.staffSalary),
        ExpenseLine("গাড়ি মেরামত / যন্ত্র", Icons.Default.Build, expense.vehicleRepair),
        ExpenseLine("সম্পদ / সরঞ্জাম", Icons.Default.Inventory2, expense.assets),
        ExpenseLine("বিদ্যুৎ বিল", Icons.Default.Bolt, expense.electricityBill),
        ExpenseLine("অন্যান্য খরচ", Icons.Default.MoreHoriz, expense.otherExpense)
    )

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "মাসিক ব্যয় বিবরণ",
                isRootScreen = false,
                onBackClick = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            haptics.tap()
                            showShareCardDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            DetailActionBar(
                actions = listOfNotNull(
                    if (canDelete) DetailAction(
                        icon = Icons.Default.Delete,
                        label = "মুছে ফেলুন",
                        tone = DetailActionTone.Danger,
                        testTag = "btn_delete_detail"
                    ) { showDeleteConfirm = true } else null,

                    DetailAction(
                        icon = Icons.Default.PictureAsPdf,
                        label = "পিডিএফ",
                        tone = DetailActionTone.Neutral,
                        testTag = "btn_pdf_detail"
                    ) { onPdfPreview(expense) },

                    if (canEdit) DetailAction(
                        icon = Icons.Default.Edit,
                        label = "সম্পাদনা",
                        tone = DetailActionTone.Primary,
                        testTag = "btn_edit_detail"
                    ) { onEdit(expense.id) } else null
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("monthly_expense_detail_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Date & Status Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = BanglaNumberFormatter.formatBanglaDate(expense.date),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "মাসিক ব্যয়ের রেকর্ড",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "সম্পন্ন",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // Total Expense Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Total Expense",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সর্বমোট ব্যয়",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }

                    Text(
                        text = BanglaNumberFormatter.formatCurrency(expense.totalExpense),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 20.sp
                        )
                    )
                }
            }

            // Expense Breakdown Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = "Breakdown",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ব্যয়ের খাতসমূহ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    breakdown.forEach { line ->
                        ExpenseBreakdownRow(line = line)
                    }
                }
            }

            // Remarks Card
            if (expense.remarks.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "মন্তব্য",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = expense.remarks,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showShareCardDialog) {
        MonthlyExpenseShareDialog(
            expense = expense,
            farmProfile = farmProfile,
            onDismiss = { showShareCardDialog = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("মুছে ফেলার নিশ্চিতকরণ") },
            text = { Text("আপনি কি নিশ্চিত যে এই ব্যয় এন্ট্রিটি মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.confirm()
                        viewModel.deleteExpense(expense.id)
                        showDeleteConfirm = false
                        onBack()
                    }
                ) {
                    Text("মুছে ফেলুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun ExpenseBreakdownRow(line: ExpenseLine) {
    // Unused categories stay visible but muted, so the row order is the same on every entry.
    val isSpent = line.amount > 0.0
    val labelColor = if (isSpent) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }
    val amountColor = if (isSpent) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = line.icon,
                contentDescription = line.label,
                tint = labelColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = line.label,
                style = MaterialTheme.typography.bodyMedium.copy(color = labelColor)
            )
        }

        Text(
            text = BanglaNumberFormatter.formatCurrency(line.amount),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = if (isSpent) FontWeight.Bold else FontWeight.Normal,
                color = amountColor
            )
        )
    }
}
