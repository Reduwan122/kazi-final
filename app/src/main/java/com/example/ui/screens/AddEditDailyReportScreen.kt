package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Pets
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DailyReportEntity
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.viewmodel.PoultryViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddEditDailyReportScreen(
    reportId: Long = 0L,
    viewModel: PoultryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var date by remember { mutableStateOf(BanglaNumberFormatter.getCurrentDateFormatted()) }
    var currentBirdsText by remember { mutableStateOf("12500") }
    var deadBirdsText by remember { mutableStateOf("0") }
    var eggProductionText by remember { mutableStateOf("") }
    var eggSoldText by remember { mutableStateOf("") }
    var eggPriceText by remember { mutableStateOf("10.50") }
    var medicineCostText by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    var validationError by remember { mutableStateOf<String?>(null) }
    val isEditMode = reportId > 0L

    // Fetch existing data if editing
    LaunchedEffect(reportId) {
        if (isEditMode) {
            val list = viewModel.dailyReports.value
            val existing = list.find { it.id == reportId }
            if (existing != null) {
                date = existing.date
                currentBirdsText = existing.currentBirds.toString()
                deadBirdsText = existing.deadBirds.toString()
                eggProductionText = existing.eggProduction.toString()
                eggSoldText = existing.eggSold.toString()
                eggPriceText = existing.eggPrice.toString()
                medicineCostText = if (existing.medicineCost > 0) existing.medicineCost.toString() else ""
                remarks = existing.remarks
            }
        } else {
            // Auto fill latest flock count
            val latestBirds = viewModel.getLatestFlockCount()
            currentBirdsText = latestBirds.toString()
        }
    }

    // Live calculated numbers
    val parsedBirds = currentBirdsText.toIntOrNull() ?: 0
    val parsedDead = deadBirdsText.toIntOrNull() ?: 0
    val liveBirdStock = (parsedBirds - parsedDead).coerceAtLeast(0)

    val parsedProduction = eggProductionText.toIntOrNull() ?: 0
    val parsedSold = eggSoldText.toIntOrNull() ?: 0
    val parsedPrice = eggPriceText.toDoubleOrNull() ?: 0.0
    val parsedMedicine = medicineCostText.toDoubleOrNull() ?: 0.0
    val liveTotalSale = parsedSold * parsedPrice

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
                title = if (isEditMode) "রিপোর্ট সম্পাদনা" else "দৈনিক রিপোর্ট এন্ট্রি",
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
                .testTag("add_edit_daily_report_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        .testTag("field_date"),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // Current Birds & Dead Birds
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "বর্তমান মুরগি",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = currentBirdsText,
                        onValueChange = {
                            currentBirdsText = BanglaNumberFormatter.toEnglishDigits(it)
                            validationError = null
                        },
                        placeholder = { Text("৫,০০০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("field_current_birds")
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "মৃত মুরগি",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                    )
                    OutlinedTextField(
                        value = deadBirdsText,
                        onValueChange = {
                            deadBirdsText = BanglaNumberFormatter.toEnglishDigits(it)
                            validationError = null
                        },
                        placeholder = { Text("০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("field_dead_birds")
                    )
                }
            }

            // Auto Calculation Card 1: Current Birds Stock
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
                            imageVector = Icons.Default.Pets,
                            contentDescription = "Flock",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "বর্তমান স্টক (মুরগি)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Text(
                        text = BanglaNumberFormatter.formatNumber(liveBirdStock),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Egg Production & Egg Sold
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ডিম উৎপাদন (পিস)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = eggProductionText,
                        onValueChange = {
                            eggProductionText = BanglaNumberFormatter.toEnglishDigits(it)
                            validationError = null
                        },
                        placeholder = { Text("৪,৮৫০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("field_egg_production")
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ডিম বিক্রি (পিস)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = eggSoldText,
                        onValueChange = {
                            eggSoldText = BanglaNumberFormatter.toEnglishDigits(it)
                            validationError = null
                        },
                        placeholder = { Text("৪,৫০০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("field_egg_sold")
                    )
                }
            }

            // Egg Price & Medicine Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ডিমের দর (টাকা)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = eggPriceText,
                        onValueChange = {
                            eggPriceText = BanglaNumberFormatter.toEnglishDigits(it)
                            validationError = null
                        },
                        placeholder = { Text("১১.০০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("field_egg_price")
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ঔষধ খরচ (টাকা)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    OutlinedTextField(
                        value = medicineCostText,
                        onValueChange = {
                            medicineCostText = BanglaNumberFormatter.toEnglishDigits(it)
                            validationError = null
                        },
                        placeholder = { Text("০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("field_medicine_cost")
                    )
                }
            }

            // Auto Calculation Card 2: Total Sale
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                            imageVector = Icons.Default.Paid,
                            contentDescription = "Total Sale",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সর্বমোট বিক্রি (টাকা)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    Text(
                        text = BanglaNumberFormatter.formatCurrency(liveTotalSale),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 18.sp
                        )
                    )
                }
            }

            // Remarks
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "মন্তব্য",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    placeholder = { Text("যেমন: খাবার কম দেওয়া হয়েছে / আবহাওয়া স্বাভাবিক ছিল") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("field_remarks")
                )
            }

            if (validationError != null) {
                Text(
                    text = validationError ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
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
                        .testTag("btn_cancel_report")
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
                        if (currentBirdsText.isEmpty() || currentBirdsText.toIntOrNull() == null) {
                            validationError = "বর্তমান মুরগির সংখ্যা সঠিকভাবে লিখুন"
                            return@Button
                        }
                        if (eggProductionText.isEmpty() || eggProductionText.toIntOrNull() == null) {
                            validationError = "ডিম উৎপাদনের সংখ্যা সঠিকভাবে লিখুন"
                            return@Button
                        }

                        viewModel.saveDailyReport(
                            id = reportId,
                            date = date,
                            currentBirds = parsedBirds,
                            deadBirds = parsedDead,
                            eggProduction = parsedProduction,
                            eggSold = parsedSold,
                            eggPrice = parsedPrice,
                            medicineCost = parsedMedicine,
                            remarks = remarks,
                            onSuccess = onBack
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("btn_save_report")
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
