package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.viewmodel.PoultryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddEditMonthlyExpenseScreen(
    expenseId: Long = 0L,
    viewModel: PoultryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var date by remember { mutableStateOf(BanglaNumberFormatter.getCurrentDateFormatted()) }
    var feedCostText by remember { mutableStateOf("") }
    var medicineCostText by remember { mutableStateOf("") }
    var staffMarketText by remember { mutableStateOf("") }
    var staffSalaryText by remember { mutableStateOf("") }
    var vehicleRepairText by remember { mutableStateOf("") }
    var assetsText by remember { mutableStateOf("") }
    var electricityBillText by remember { mutableStateOf("") }
    var otherExpenseText by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    val isEditMode = expenseId > 0L

    LaunchedEffect(expenseId) {
        if (isEditMode) {
            val list = viewModel.expenses.value
            val existing = list.find { it.id == expenseId }
            if (existing != null) {
                date = existing.date
                feedCostText = if (existing.feedCost > 0) existing.feedCost.toString() else ""
                medicineCostText = if (existing.medicineCost > 0) existing.medicineCost.toString() else ""
                staffMarketText = if (existing.staffMarket > 0) existing.staffMarket.toString() else ""
                staffSalaryText = if (existing.staffSalary > 0) existing.staffSalary.toString() else ""
                vehicleRepairText = if (existing.vehicleRepair > 0) existing.vehicleRepair.toString() else ""
                assetsText = if (existing.assets > 0) existing.assets.toString() else ""
                electricityBillText = if (existing.electricityBill > 0) existing.electricityBill.toString() else ""
                otherExpenseText = if (existing.otherExpense > 0) existing.otherExpense.toString() else ""
                remarks = existing.remarks
            }
        }
    }

    // Live parsed values
    val feed = feedCostText.toDoubleOrNull() ?: 0.0
    val medicine = medicineCostText.toDoubleOrNull() ?: 0.0
    val market = staffMarketText.toDoubleOrNull() ?: 0.0
    val salary = staffSalaryText.toDoubleOrNull() ?: 0.0
    val vehicle = vehicleRepairText.toDoubleOrNull() ?: 0.0
    val assets = assetsText.toDoubleOrNull() ?: 0.0
    val electricity = electricityBillText.toDoubleOrNull() ?: 0.0
    val others = otherExpenseText.toDoubleOrNull() ?: 0.0
    val liveTotalExpense = feed + medicine + market + salary + vehicle + assets + electricity + others

    // Calendar Picker Dialog
    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply {
                set(year, month, dayOfMonth)
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            date = sdf.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = if (isEditMode) "ব্যয় সম্পাদনা" else "মাসিক ব্যয় এন্ট্রি",
                isRootScreen = false,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .testTag("add_edit_expense_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Date Picker Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "তারিখ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = "${BanglaNumberFormatter.formatBanglaDate(date)} ($date)",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Select Date",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { datePickerDialog.show() }
                        .testTag("expense_field_date"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Feed & Medicine Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExpenseInputField(
                    label = "খাদ্য / ফিড (টাকা)",
                    value = feedCostText,
                    onValueChange = { feedCostText = BanglaNumberFormatter.toEnglishDigits(it) },
                    placeholder = "০",
                    modifier = Modifier.weight(1f),
                    testTag = "field_feed_cost"
                )

                ExpenseInputField(
                    label = "মেডিসিন ও ভ্যাকসিন (টাকা)",
                    value = medicineCostText,
                    onValueChange = { medicineCostText = BanglaNumberFormatter.toEnglishDigits(it) },
                    placeholder = "০",
                    modifier = Modifier.weight(1f),
                    testTag = "field_med_cost"
                )
            }

            // Staff Market & Salary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExpenseInputField(
                    label = "স্টাফ বাজার (টাকা)",
                    value = staffMarketText,
                    onValueChange = { staffMarketText = BanglaNumberFormatter.toEnglishDigits(it) },
                    placeholder = "০",
                    modifier = Modifier.weight(1f),
                    testTag = "field_staff_market"
                )

                ExpenseInputField(
                    label = "স্টাফ বেতন / মজুরি (টাকা)",
                    value = staffSalaryText,
                    onValueChange = { staffSalaryText = BanglaNumberFormatter.toEnglishDigits(it) },
                    placeholder = "০",
                    modifier = Modifier.weight(1f),
                    testTag = "field_staff_salary"
                )
            }

            // Vehicle Repair & Assets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExpenseInputField(
                    label = "গাড়ি মেরামত / যন্ত্র (টাকা)",
                    value = vehicleRepairText,
                    onValueChange = { vehicleRepairText = BanglaNumberFormatter.toEnglishDigits(it) },
                    placeholder = "০",
                    modifier = Modifier.weight(1f),
                    testTag = "field_vehicle_repair"
                )

                ExpenseInputField(
                    label = "সম্পদ / সরঞ্জাম (টাকা)",
                    value = assetsText,
                    onValueChange = { assetsText = BanglaNumberFormatter.toEnglishDigits(it) },
                    placeholder = "০",
                    modifier = Modifier.weight(1f),
                    testTag = "field_assets"
                )
            }

            // Electricity & Others
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExpenseInputField(
                    label = "বিদ্যুৎ বিল (টাকা)",
                    value = electricityBillText,
                    onValueChange = { electricityBillText = BanglaNumberFormatter.toEnglishDigits(it) },
                    placeholder = "০",
                    modifier = Modifier.weight(1f),
                    testTag = "field_electricity"
                )

                ExpenseInputField(
                    label = "অন্যান্য খরচ (টাকা)",
                    value = otherExpenseText,
                    onValueChange = { otherExpenseText = BanglaNumberFormatter.toEnglishDigits(it) },
                    placeholder = "০",
                    modifier = Modifier.weight(1f),
                    testTag = "field_other_expense"
                )
            }

            // Live Calculation Card: Total Expense
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
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
                            text = "সর্বমোট ব্যয় (টাকা)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        )
                    }

                    Text(
                        text = BanglaNumberFormatter.formatCurrency(liveTotalExpense),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 18.sp
                        )
                    )
                }
            }

            // Remarks
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "মন্তব্য / বিবরণ",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    placeholder = { Text("খরচের বিস্তারিত নোট...") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("field_expense_remarks")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_cancel_expense")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("বাতিল", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        viewModel.saveMonthlyExpense(
                            id = expenseId,
                            date = date,
                            feedCost = feed,
                            medicineCost = medicine,
                            staffMarket = market,
                            staffSalary = salary,
                            vehicleRepair = vehicle,
                            assets = assets,
                            electricityBill = electricity,
                            otherExpense = others,
                            remarks = remarks,
                            onSuccess = onBack
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("btn_save_expense")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ExpenseInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "০",
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp
            ),
            maxLines = 1
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag(testTag)
        )
    }
}
