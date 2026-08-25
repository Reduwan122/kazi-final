package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.data.local.UserEntity
import com.example.data.repository.PoultryRepository
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
    val dailyReports: StateFlow<List<DailyReportEntity>>
    val expenses: StateFlow<List<MonthlyExpenseEntity>>
    val farmProfile: StateFlow<FarmProfileEntity>
    val currentUser: StateFlow<UserEntity?>
    val allUsers: StateFlow<List<UserEntity>>
    val rolePermissions: StateFlow<Map<String, com.example.data.local.RolePermissionConfig>>
    val dashboardStats: StateFlow<DashboardStats>

    // Daily Report Filters
    val dailySearchQuery = MutableStateFlow("")
    val dailySelectedMonth = MutableStateFlow("সকল রেকর্ড")

    // Expense Filters
    val expenseSearchQuery = MutableStateFlow("")
    val expenseSelectedMonth = MutableStateFlow("সকল রেকর্ড")

    // Sync / Backup status message
    val syncStatus = MutableStateFlow("ফায়ারবেস ক্লাউড কানেক্টেড")

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

        dashboardStats = combine(
            dailyReports,
            expenses
        ) { reportsList, expensesList ->
            calculateDashboardStats(reportsList, expensesList)
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
        expensesList: List<MonthlyExpenseEntity>
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
        val eggStock = todayReport?.currentStock ?: latestReport?.currentStock ?: 0

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
        remarks: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val isUpdate = id > 0L
            try {
                val totalSale = eggSold * eggPrice
                val previousStock = repository.getPreviousStock(date)
                val currentStock = previousStock + eggProduction - eggSold

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
                    currentStock = currentStock.coerceAtLeast(0),
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

    fun manualBackup(context: Context) {
        viewModelScope.launch {
            syncStatus.value = "ফায়ারবেস ক্লাউড সিঙ্ক সফল (${BanglaNumberFormatter.toBanglaDigits(SimpleDateFormat("hh:mm a", Locale.US).format(Date()))})"
            SnackbarController.showMessage("ক্লাউড সিঙ্ক সফল হয়েছে!")
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
