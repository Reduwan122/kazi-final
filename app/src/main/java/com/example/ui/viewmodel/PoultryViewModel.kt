package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.AutoBackupWorker
import com.example.data.backup.BackupDataContent
import com.example.data.backup.BackupProgressState
import com.example.data.backup.DriveBackupManager
import com.example.data.backup.DriveFileInfo
import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.data.local.UserEntity
import com.example.data.repository.PoultryRepository
import com.example.domain.DailyStockRecord
import com.example.domain.StockCalculationService
import com.example.domain.StockSummary
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.SnackbarController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PoultryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PoultryRepository = PoultryRepository(application)
    val driveBackupManager: DriveBackupManager = DriveBackupManager(application)

    val dailyReports: StateFlow<List<DailyReportEntity>>
    val expenses: StateFlow<List<MonthlyExpenseEntity>>
    val farmProfile: StateFlow<FarmProfileEntity>
    val currentUser: StateFlow<UserEntity?>
    val allUsers: StateFlow<List<UserEntity>>
    val rolePermissions: StateFlow<Map<String, com.example.data.local.RolePermissionConfig>>
    val dashboardStats: StateFlow<DashboardStats>
    val stockLedger: StateFlow<Map<String, DailyStockRecord>>
    val syncStatus = MutableStateFlow("ফায়ারবেস ক্লাউড সিঙ্ক সফল")

    // Google Drive Backup StateFlows
    private val _googleAccountEmail = MutableStateFlow<String?>(driveBackupManager.getConnectedAccount()?.email)
    val googleAccountEmail: StateFlow<String?> = _googleAccountEmail.asStateFlow()

    private val _backupProgressState = MutableStateFlow<BackupProgressState>(BackupProgressState.Idle)
    val backupProgressState: StateFlow<BackupProgressState> = _backupProgressState.asStateFlow()

    private val _driveBackupsList = MutableStateFlow<List<DriveFileInfo>>(emptyList())
    val driveBackupsList: StateFlow<List<DriveFileInfo>> = _driveBackupsList.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow(driveBackupManager.getLastBackupTimestamp())
    val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()

    private val _isAutoBackupEnabled = MutableStateFlow(driveBackupManager.isAutoBackupEnabled())
    val isAutoBackupEnabled: StateFlow<Boolean> = _isAutoBackupEnabled.asStateFlow()

    private val _autoBackupFrequency = MutableStateFlow(driveBackupManager.getAutoBackupFrequency())
    val autoBackupFrequency: StateFlow<String> = _autoBackupFrequency.asStateFlow()

    // Daily Report Filters
    val dailySearchQuery = MutableStateFlow("")
    val dailySelectedMonth = MutableStateFlow("সকল রেকর্ড")

    // Expense Filters
    val expenseSearchQuery = MutableStateFlow("")
    val expenseSelectedMonth = MutableStateFlow("সকল রেকর্ড")

    // User Management Filters
    val userSearchQuery = MutableStateFlow("")
    val userSelectedRole = MutableStateFlow("সকল")

    // App Preferences
    val isDarkMode = MutableStateFlow(false)
    val isRememberMe = MutableStateFlow(false)

    // Notification read/unread state — stores the date when notifications were last dismissed
    private val _notificationDismissedDate = MutableStateFlow<String?>(null)
    val notificationDismissedDate: StateFlow<String?> = _notificationDismissedDate.asStateFlow()

    fun markNotificationsRead() {
        _notificationDismissedDate.value = BanglaNumberFormatter.getCurrentDateFormatted()
    }

    init {
        dailyReports = repository.allDailyReports.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        expenses = repository.allExpenses.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        farmProfile = repository.farmProfile.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            FarmProfileEntity()
        )

        currentUser = repository.currentUser.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null
        )

        allUsers = repository.allUsers.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyList()
        )

        rolePermissions = repository.rolePermissions.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            mapOf(
                "ADMIN" to com.example.data.local.RolePermissionConfig.getDefaultPermissionsForRole("ADMIN"),
                "MANAGER" to com.example.data.local.RolePermissionConfig.getDefaultPermissionsForRole("MANAGER"),
                "SUPERVISOR" to com.example.data.local.RolePermissionConfig.getDefaultPermissionsForRole("SUPERVISOR"),
                "WORKER" to com.example.data.local.RolePermissionConfig.getDefaultPermissionsForRole("WORKER")
            )
        )

        stockLedger = combine(dailyReports, farmProfile) { reportsList, profile ->
            StockCalculationService.calculateSequentialStockLedger(
                reportsList,
                baselineInitialStock = profile.initialOpeningStock
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            emptyMap()
        )

        dashboardStats = combine(
            dailyReports,
            expenses,
            farmProfile
        ) { reportsList, expensesList, profile ->
            calculateDashboardStats(reportsList, expensesList, profile.initialOpeningStock)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            DashboardStats(
                currentBirds = 0,
                todayEggProduction = 0,
                todayTotalSale = 0.0,
                todayTotalExpense = 0.0,
                currentEggStock = 0,
                thisMonthTotalSale = 0.0,
                thisMonthTotalExpense = 0.0
            )
        )
    }

    private fun calculateDashboardStats(
        reportsList: List<DailyReportEntity>,
        expensesList: List<MonthlyExpenseEntity>,
        baselineInitialStock: Int = 0
    ): DashboardStats {
        val todayStr = BanglaNumberFormatter.getCurrentDateFormatted()

        val todayReport = reportsList.find { it.date == todayStr }
        val latestReport = reportsList.firstOrNull()
        val todayExpense = expensesList.find { it.date == todayStr }

        val currentBirds = todayReport?.currentBirds
            ?: latestReport?.let { (it.currentBirds - it.deadBirds).coerceAtLeast(0) }
            ?: 0

        val todayProduction = todayReport?.eggProduction ?: 0
        val todaySale = todayReport?.totalSale ?: 0.0
        val todayExp = todayExpense?.totalExpense ?: 0.0

        // Use central stock engine with correct baseline for 100% accurate closing stock
        val eggStock = StockCalculationService.calculateCurrentStock(reportsList, baselineInitialStock)

        // Current Month total calculations
        val currentMonthPrefix = todayStr.take(7) // "YYYY-MM"
        val thisMonthSale = reportsList.filter { it.date.startsWith(currentMonthPrefix) }
            .sumOf { it.totalSale }
        val thisMonthExpense = expensesList.filter { it.date.startsWith(currentMonthPrefix) }
            .sumOf { it.totalExpense }

        return DashboardStats(
            currentBirds = currentBirds,
            todayEggProduction = todayProduction,
            todayTotalSale = todaySale,
            todayTotalExpense = todayExp,
            currentEggStock = eggStock,
            thisMonthTotalSale = thisMonthSale,
            thisMonthTotalExpense = thisMonthExpense
        )
    }

    // Dashboard Calculations using 100% real data
    fun getDashboardStats(): DashboardStats {
        return dashboardStats.value
    }

    // Daily Report Operations (Realtime Firebase)
    fun saveDailyReport(
        id: Long,
        date: String,
        currentBirds: Int,
        deadBirds: Int,
        eggProduction: Int,
        eggSold: Int,
        eggPrice: Double,
        medicineCost: Double,
        otherStockIn: Int = 0,
        otherStockOut: Int = 0,
        stockAdjustment: Int = 0,
        adjustmentReason: String = "",
        remarks: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val isUpdate = id > 0L
            try {
                val totalSale = eggSold * eggPrice
                val priorReports = dailyReports.value.filter { it.id != id }
                val baseline = farmProfile.value.initialOpeningStock
                val openingStock = StockCalculationService.calculateOpeningStockForDate(priorReports, date, baseline)
                val closingStock = openingStock + eggProduction - eggSold - otherStockOut + otherStockIn + stockAdjustment

                val entity = DailyReportEntity(
                    id = id,
                    date = date,
                    currentBirds = currentBirds,
                    deadBirds = deadBirds,
                    eggProduction = eggProduction,
                    eggSold = eggSold,
                    eggPrice = eggPrice,
                    totalSale = totalSale,
                    medicineCost = medicineCost,
                    currentStock = closingStock,
                    otherStockIn = otherStockIn,
                    otherStockOut = otherStockOut,
                    stockAdjustment = stockAdjustment,
                    adjustmentReason = adjustmentReason,
                    remarks = remarks,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveDailyReport(entity)
                SnackbarController.showMessage(
                    if (isUpdate) "দৈনিক রিপোর্ট আপডেট করা হয়েছে!"
                    else "নতুন দৈনিক রিপোর্ট সংরক্ষণ করা হয়েছে!"
                )
                onSuccess()
            } catch (e: Exception) {
                SnackbarController.showError(
                    if (isUpdate) "রিপোর্ট আপডেট ব্যর্থ হয়েছে: ${e.message}"
                    else "রিপোর্ট সংরক্ষণ ব্যর্থ হয়েছে: ${e.message}"
                )
            }
        }
    }

    /**
     * Retrieves the opening stock for a target date from the central stock engine.
     */
    fun getOpeningStockForDate(targetDate: String, excludeReportId: Long = 0L): Int {
        val list = if (excludeReportId > 0L) dailyReports.value.filter { it.id != excludeReportId } else dailyReports.value
        val baseline = farmProfile.value.initialOpeningStock
        return StockCalculationService.calculateOpeningStockForDate(list, targetDate, baseline)
    }

    /**
     * Retrieves the stock summary for a specific period (e.g. month or date range).
     */
    fun getStockSummaryForPeriod(startDate: String?, endDate: String?): StockSummary {
        val baseline = farmProfile.value.initialOpeningStock
        return StockCalculationService.calculateStockForPeriod(dailyReports.value, startDate, endDate, baseline)
    }

    fun deleteDailyReport(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteDailyReportById(id)
                SnackbarController.showMessage("দৈনিক রিপোর্ট মুছে ফেলা হয়েছে")
            } catch (e: Exception) {
                SnackbarController.showError("রিপোর্ট মুছে ফেলা যায়নি: ${e.message}")
            }
        }
    }

    // Monthly Expense Operations (Realtime Firebase)
    fun saveMonthlyExpense(
        id: Long,
        date: String,
        feedCost: Double,
        medicineCost: Double,
        staffMarket: Double,
        staffSalary: Double,
        vehicleRepair: Double,
        assets: Double,
        electricityBill: Double,
        otherExpense: Double,
        remarks: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val isUpdate = id > 0L
            try {
                val total = feedCost + medicineCost + staffMarket + staffSalary +
                        vehicleRepair + assets + electricityBill + otherExpense

                val entity = MonthlyExpenseEntity(
                    id = id,
                    date = date,
                    feedCost = feedCost,
                    medicineCost = medicineCost,
                    staffMarket = staffMarket,
                    staffSalary = staffSalary,
                    vehicleRepair = vehicleRepair,
                    assets = assets,
                    electricityBill = electricityBill,
                    otherExpense = otherExpense,
                    totalExpense = total,
                    remarks = remarks,
                    updatedAt = System.currentTimeMillis()
                )
                repository.saveMonthlyExpense(entity)
                SnackbarController.showMessage(
                    if (isUpdate) "মাসিক ব্যয় এন্ট্রি আপডেট করা হয়েছে!"
                    else "নতুন মাসিক ব্যয় এন্ট্রি সংরক্ষণ করা হয়েছে!"
                )
                onSuccess()
            } catch (e: Exception) {
                SnackbarController.showError(
                    if (isUpdate) "ব্যয় এন্ট্রি আপডেট ব্যর্থ হয়েছে: ${e.message}"
                    else "ব্যয় এন্ট্রি সংরক্ষণ ব্যর্থ হয়েছে: ${e.message}"
                )
            }
        }
    }

    fun deleteExpense(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteExpenseById(id)
                SnackbarController.showMessage("মাসিক ব্যয় এন্ট্রি মুছে ফেলা হয়েছে")
            } catch (e: Exception) {
                SnackbarController.showError("ব্যয় এন্ট্রি মুছে ফেলা যায়নি: ${e.message}")
            }
        }
    }

    // Settings & Profile
    fun updateFarmProfile(
        farmName: String,
        ownerName: String,
        mobileNumber: String,
        address: String,
        logoEmoji: String = "🐔"
    ) {
        viewModelScope.launch {
            val current = farmProfile.value
            repository.updateFarmProfile(
                current.copy(
                    farmName = farmName,
                    ownerName = ownerName,
                    mobileNumber = mobileNumber,
                    address = address,
                    logoEmoji = logoEmoji
                )
            )
        }
    }

    fun updateFarmLogo(emoji: String) {
        viewModelScope.launch {
            val current = farmProfile.value
            repository.updateFarmProfile(
                current.copy(
                    logoUri = "",
                    logoEmoji = emoji
                )
            )
        }
    }

    /**
     * Updates the baseline initial opening stock (pre-history closing stock) in farm profile.
     * This is the closing stock of a date before the first daily report was recorded.
     * Example: if the first daily report is 01/08, set initialOpeningStock = 729 (closing stock of 31/07).
     */
    fun updateInitialOpeningStock(stock: Int, date: String) {
        viewModelScope.launch {
            val current = farmProfile.value
            repository.updateFarmProfile(
                current.copy(
                    initialOpeningStock = stock,
                    initialOpeningDate = date
                )
            )
            SnackbarController.showMessage("প্রারম্ভিক স্টক আপডেট করা হয়েছে: $stock ডিম")
        }
    }

    fun uploadFarmLogoFromUri(
        context: Context,
        imageUri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: throw Exception("ছবি ফাইল ওপেন করা যায়নি")
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (originalBitmap == null) {
                    withContext(Dispatchers.Main) { onError("ছবির ফাইল সঠিক নয় বা ক্ষতিগ্রস্থ") }
                    return@launch
                }

                // Scale down if image is larger than 512px to optimize storage & bandwidth
                val maxDimension = 512
                val width = originalBitmap.width
                val height = originalBitmap.height
                val scale = if (width > height) {
                    if (width > maxDimension) maxDimension.toFloat() / width else 1.0f
                } else {
                    if (height > maxDimension) maxDimension.toFloat() / height else 1.0f
                }

                val scaledBitmap = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    originalBitmap
                }

                val outputStream = ByteArrayOutputStream()
                // Compress to PNG for clean transparency/sharp edges
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                val dataUri = "data:image/png;base64,$base64String"

                val current = farmProfile.value
                repository.updateFarmProfile(
                    current.copy(
                        logoUri = dataUri,
                        logoEmoji = ""
                    )
                )

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "লোগো আপলোড ব্যর্থ হয়েছে")
                }
            }
        }
    }

    fun resetToDefaultLogo(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val current = farmProfile.value
            repository.updateFarmProfile(
                current.copy(
                    logoUri = "",
                    logoEmoji = "🐔"
                )
            )
            onSuccess()
        }
    }

    fun updateCurrentUserProfile(
        name: String,
        phone: String,
        profileImageUri: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updateCurrentUserProfile(name, phone, profileImageUri)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "আপডেট করা যায়নি")
        }
    }

    fun uploadUserProfileImageFromUri(
        context: Context,
        imageUri: Uri,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                    ?: throw Exception("ছবি ফাইল ওপেন করা যায়নি")
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (originalBitmap == null) {
                    withContext(Dispatchers.Main) { onError("ছবির ফাইল সঠিক নয় বা ক্ষতিগ্রস্থ") }
                    return@launch
                }

                // Scale down to max 512px
                val maxDimension = 512
                val width = originalBitmap.width
                val height = originalBitmap.height
                val scale = if (width > height) {
                    if (width > maxDimension) maxDimension.toFloat() / width else 1.0f
                } else {
                    if (height > maxDimension) maxDimension.toFloat() / height else 1.0f
                }

                val scaledBitmap = if (scale < 1.0f) {
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (width * scale).toInt().coerceAtLeast(1),
                        (height * scale).toInt().coerceAtLeast(1),
                        true
                    )
                } else {
                    originalBitmap
                }

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                val dataUri = "data:image/png;base64,$base64String"

                val current = currentUser.value
                if (current != null) {
                    repository.updateCurrentUserProfile(
                        name = current.username,
                        phone = current.phone,
                        profileImageUri = dataUri
                    )
                }

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "প্রোফাইল ছবি আপলোড ব্যর্থ হয়েছে")
                }
            }
        }
    }

    fun removeUserProfileImage(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val current = currentUser.value
            if (current != null) {
                repository.updateCurrentUserProfile(
                    name = current.username,
                    phone = current.phone,
                    profileImageUri = ""
                )
                onSuccess()
            }
        }
    }

    fun adminAddUser(user: UserEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.adminAddUser(user)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "ইউজার যোগ করা যায়নি")
        }
    }

    fun adminUpdateUser(user: UserEntity, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.adminUpdateUser(user)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "ইউজার আপডেট করা যায়নি")
        }
    }

    fun deleteUser(userId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteUser(userId)
            if (result.isSuccess) onSuccess() else onError(result.exceptionOrNull()?.message ?: "ইউজার ডিলিট করা যায়নি")
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDarkMode(enabled)
        }
    }


    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAutoBackup(enabled)
        }
    }

    // Real Firebase Auth
    fun login(email: String, pass: String, onSuccess: (UserEntity) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.signInWithEmail(email, pass)
            if (result.isSuccess) {
                val user = result.getOrNull() ?: UserEntity()
                onSuccess(user)
            } else {
                onError(result.exceptionOrNull()?.message ?: "লগইন ব্যর্থ হয়েছে")
            }
        }
    }

    fun register(email: String, pass: String, fullName: String, phone: String = "", onSuccess: (UserEntity) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.signUpWithEmail(email, pass, fullName, phone)
            if (result.isSuccess) {
                val user = result.getOrNull() ?: UserEntity()
                onSuccess(user)
            } else {
                onError(result.exceptionOrNull()?.message ?: "রেজিস্ট্রেশন ব্যর্থ হয়েছে")
            }
        }
    }

    fun checkUserApproval(onApproved: () -> Unit, onNotApproved: () -> Unit) {
        viewModelScope.launch {
            val user = currentUser.value
            if (user != null && user.isApprovedUser()) {
                onApproved()
            } else {
                onNotApproved()
            }
        }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.sendPasswordResetEmail(email)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "পাসওয়ার্ড রিসেট ইমেইল পাঠানো যায়নি")
            }
        }
    }

    fun changePassword(newPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.changePassword(newPass)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "পাসওয়ার্ড পরিবর্তন ব্যর্থ হয়েছে")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        repository.signOut()
        onSuccess()
    }

    fun isUserLoggedIn(): Boolean {
        return repository.isUserLoggedIn()
    }

    fun isUserLoggedInAndApproved(): Boolean {
        return repository.isUserLoggedInAndApproved()
    }

    fun updateRolePermissions(
        config: com.example.data.local.RolePermissionConfig,
        onComplete: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.updateRolePermissions(config)
            onComplete?.invoke()
        }
    }

    fun getPermissionsForRole(roleKey: String): com.example.data.local.RolePermissionConfig {
        return rolePermissions.value[roleKey.uppercase()]
            ?: com.example.data.local.RolePermissionConfig.getDefaultPermissionsForRole(roleKey)
    }

    // Export Excel / CSV
    fun exportDailyReportsCsv(context: Context) {
        viewModelScope.launch {
            try {
                val reports = dailyReports.value
                val file = repository.exportDailyReportsToCsv(reports)
                shareCsvFile(context, file, "কাজী এগ্রোটেক - দৈনিক রিপোর্ট")
            } catch (e: Exception) {
                SnackbarController.showError("এক্সেল এক্সপোর্ট ব্যর্থ হয়েছে: ${e.message}")
            }
        }
    }

    fun exportExpensesCsv(context: Context) {
        viewModelScope.launch {
            try {
                val list = expenses.value
                val file = repository.exportMonthlyExpensesToCsv(list)
                shareCsvFile(context, file, "কাজী এগ্রোটেক - মাসিক ব্যয় রেজিস্টার")
            } catch (e: Exception) {
                SnackbarController.showError("এক্সেল এক্সপোর্ট ব্যর্থ হয়েছে: ${e.message}")
            }
        }
    }

    private fun shareCsvFile(context: Context, file: File, subject: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "$subject এক্সপোর্ট করুন"))
    }

    // ══════════════════════════════════════════════════════════════════════
    // Google Drive Backup & Restore Operations
    // ══════════════════════════════════════════════════════════════════════

    fun refreshGoogleAccountStatus() {
        _googleAccountEmail.value = driveBackupManager.getConnectedAccount()?.email
        _lastBackupTimestamp.value = driveBackupManager.getLastBackupTimestamp()
        _isAutoBackupEnabled.value = driveBackupManager.isAutoBackupEnabled()
        _autoBackupFrequency.value = driveBackupManager.getAutoBackupFrequency()
        if (driveBackupManager.isConnected()) {
            fetchDriveBackupsList()
        }
    }

    fun disconnectGoogleAccount(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            driveBackupManager.disconnect()
            _googleAccountEmail.value = null
            _driveBackupsList.value = emptyList()
            AutoBackupWorker.cancel(getApplication())
            driveBackupManager.setAutoBackupEnabled(false)
            _isAutoBackupEnabled.value = false
            SnackbarController.showMessage("গুগল ড্রাইভ অ্যাকাউন্ট সংযোগ বিচ্ছিন্ন করা হয়েছে")
            onDone()
        }
    }

    fun fetchDriveBackupsList() {
        viewModelScope.launch {
            val token = driveBackupManager.getAccessToken() ?: return@launch
            val res = driveBackupManager.listBackupsFromDrive(token)
            if (res.isSuccess) {
                _driveBackupsList.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun performManualBackup(
        password: String? = null,
        onSuccess: (DriveFileInfo) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!driveBackupManager.isNetworkAvailable()) {
                val err = "ইন্টারনেট সংযোগ নেই। ইন্টারনেট চালু করে আবার চেষ্টা করুন।"
                _backupProgressState.value = BackupProgressState.Error(err)
                SnackbarController.showError(err)
                onError(err)
                return@launch
            }

            if (!driveBackupManager.isConnected()) {
                val err = "প্রথমে গুগল ড্রাইভ অ্যাকাউন্ট সংযোগ করুন।"
                _backupProgressState.value = BackupProgressState.Error(err)
                SnackbarController.showError(err)
                onError(err)
                return@launch
            }

            _backupProgressState.value = BackupProgressState.Connecting()

            val token = driveBackupManager.getAccessToken()
            if (token == null) {
                val err = "গুগল ড্রাইভ অনুমোদন টোকেন প্রাপ্তি ব্যর্থ হয়েছে।"
                _backupProgressState.value = BackupProgressState.Error(err)
                SnackbarController.showError(err)
                onError(err)
                return@launch
            }

            _backupProgressState.value = BackupProgressState.Preparing()

            val user = currentUser.value
            val userId = user?.id ?: ""
            val userEmail = user?.email ?: ""

            val backupJson = driveBackupManager.createBackupJson(
                farmProfile = farmProfile.value,
                dailyReports = dailyReports.value,
                monthlyExpenses = expenses.value,
                rolePermissions = rolePermissions.value,
                userId = userId,
                userEmail = userEmail,
                password = password,
                isPreRestore = false
            )

            _backupProgressState.value = BackupProgressState.Uploading()

            val result = driveBackupManager.uploadBackupToDrive(
                backupJsonString = backupJson,
                isPreRestore = false,
                accessToken = token
            )

            if (result.isSuccess) {
                val fileInfo = result.getOrThrow()
                _lastBackupTimestamp.value = fileInfo.createdTime
                _backupProgressState.value = BackupProgressState.Success("আপনার খামার ডেটা সফলভাবে গুগল ড্রাইভে ব্যাকআপ করা হয়েছে।")
                SnackbarController.showMessage("গুগল ড্রাইভে ব্যাকআপ সফল হয়েছে!")
                fetchDriveBackupsList()
                onSuccess(fileInfo)
            } else {
                val err = result.exceptionOrNull()?.message ?: "ব্যাকআপ আপলোড ব্যর্থ হয়েছে।"
                _backupProgressState.value = BackupProgressState.Error(err)
                SnackbarController.showError(err)
                onError(err)
            }
        }
    }

    fun performRestore(
        driveFile: DriveFileInfo,
        password: String? = null,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            if (!driveBackupManager.isNetworkAvailable()) {
                val err = "ইন্টারনেট সংযোগ নেই। ইন্টারনেট চালু করে আবার চেষ্টা করুন।"
                _backupProgressState.value = BackupProgressState.Error(err)
                SnackbarController.showError(err)
                onError(err)
                return@launch
            }

            val token = driveBackupManager.getAccessToken()
            if (token == null) {
                val err = "গুগল ড্রাইভ অনুমোদন টোকেন প্রাপ্তি ব্যর্থ হয়েছে।"
                _backupProgressState.value = BackupProgressState.Error(err)
                SnackbarController.showError(err)
                onError(err)
                return@launch
            }

            _backupProgressState.value = BackupProgressState.Downloading()

            val downloadResult = driveBackupManager.downloadBackupFromDrive(driveFile.id, token)
            if (downloadResult.isFailure) {
                val err = downloadResult.exceptionOrNull()?.message ?: "ব্যাকআপ ফাইল ডাউনলোড ব্যর্থ।"
                _backupProgressState.value = BackupProgressState.Error(err)
                SnackbarController.showError(err)
                onError(err)
                return@launch
            }

            val jsonContent = downloadResult.getOrThrow()
            val currentUid = currentUser.value?.id ?: ""

            val parseResult = driveBackupManager.validateAndParseBackup(
                jsonString = jsonContent,
                currentUserId = currentUid,
                password = password
            )

            if (parseResult.isFailure) {
                val err = parseResult.exceptionOrNull()?.message ?: "অবৈধ ব্যাকআপ ফাইল।"
                _backupProgressState.value = BackupProgressState.Error(err)
                SnackbarController.showError(err)
                onError(err)
                return@launch
            }

            val restoredData = parseResult.getOrThrow()

            _backupProgressState.value = BackupProgressState.Restoring("পূর্ববর্তী ডেটার সুরক্ষা ব্যাকআপ তৈরি হচ্ছে...")

            // Safety pre-restore backup
            try {
                val safetyBackupJson = driveBackupManager.createBackupJson(
                    farmProfile = farmProfile.value,
                    dailyReports = dailyReports.value,
                    monthlyExpenses = expenses.value,
                    rolePermissions = rolePermissions.value,
                    userId = currentUid,
                    userEmail = currentUser.value?.email ?: "",
                    password = null,
                    isPreRestore = true
                )
                driveBackupManager.uploadBackupToDrive(
                    backupJsonString = safetyBackupJson,
                    isPreRestore = true,
                    accessToken = token
                )
            } catch (e: Exception) {
                Log.w("PoultryViewModel", "Safety pre-restore backup skipped or failed: ${e.message}")
            }

            _backupProgressState.value = BackupProgressState.Restoring("ডেটাবেজ ও স্টক হিসাব রিস্টোর হচ্ছে...")

            try {
                repository.restoreCompleteBackup(restoredData)
                _backupProgressState.value = BackupProgressState.Success("ব্যাকআপ সফলভাবে রিস্টোর করা হয়েছে!")
                SnackbarController.showMessage("ব্যাকআপ সফলভাবে রিস্টোর ও স্টক হিসাব সম্পন্ন হয়েছে!")
                fetchDriveBackupsList()
                onSuccess()
            } catch (e: Exception) {
                val err = "রিস্টোর ব্যর্থ হয়েছে: ${e.message}"
                _backupProgressState.value = BackupProgressState.Error(err)
                SnackbarController.showError(err)
                onError(err)
            }
        }
    }

    fun deleteDriveBackup(
        driveFile: DriveFileInfo,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val token = driveBackupManager.getAccessToken() ?: return@launch
            val res = driveBackupManager.deleteBackupFromDrive(driveFile.id, token)
            if (res.isSuccess) {
                SnackbarController.showMessage("ব্যাকআপ ফাইল ডিলিট করা হয়েছে")
                fetchDriveBackupsList()
                onSuccess()
            } else {
                val err = res.exceptionOrNull()?.message ?: "ফাইল ডিলিট ব্যর্থ হয়েছে"
                SnackbarController.showError(err)
                onError(err)
            }
        }
    }

    fun updateAutoBackupSettings(enabled: Boolean, frequency: String) {
        driveBackupManager.setAutoBackupEnabled(enabled)
        driveBackupManager.setAutoBackupFrequency(frequency)
        _isAutoBackupEnabled.value = enabled
        _autoBackupFrequency.value = frequency

        if (enabled && driveBackupManager.isConnected()) {
            AutoBackupWorker.schedule(getApplication(), frequency)
            SnackbarController.showMessage("স্বয়ংক্রিয় ব্যাকআপ সক্রিয় করা হয়েছে ($frequency)")
        } else {
            AutoBackupWorker.cancel(getApplication())
            if (!enabled) {
                SnackbarController.showMessage("স্বয়ংক্রিয় ব্যাকআপ নিষ্ক্রিয় করা হয়েছে")
            }
        }
    }

    fun manualBackup(context: Context) {
        viewModelScope.launch {
            performManualBackup()
        }
    }

    suspend fun getPreviousStockForDate(date: String): Int {
        return repository.getPreviousStock(date)
    }

    suspend fun getLatestFlockCount(): Int {
        return repository.getLatestFlockCount()
    }
}

data class DashboardStats(
    val currentBirds: Int,
    val todayEggProduction: Int,
    val todayTotalSale: Double,
    val todayTotalExpense: Double,
    val currentEggStock: Int,
    val thisMonthTotalSale: Double,
    val thisMonthTotalExpense: Double
)
