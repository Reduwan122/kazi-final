package com.example.data.backup

import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.data.local.RolePermissionConfig

/**
 * Root JSON backup payload format
 */
data class BackupPayload(
    val backupVersion: Int = 1,
    val appName: String = "Kazi Agrotech",
    val backupType: String = "full",
    val createdAt: String = "",
    val createdAtTimestamp: Long = 0L,
    val firebaseUserId: String = "",
    val userEmail: String = "",
    val isEncrypted: Boolean = false,
    val salt: String = "",
    val iv: String = "",
    val encryptedPayload: String = "",
    val data: BackupDataContent? = null
)

/**
 * Underlying farm and application data container
 */
data class BackupDataContent(
    val farmProfile: FarmProfileEntity? = null,
    val dailyReports: List<DailyReportEntity> = emptyList(),
    val monthlyExpenses: List<MonthlyExpenseEntity> = emptyList(),
    val rolePermissions: Map<String, RolePermissionConfig> = emptyMap(),
    val userCount: Int = 0,
    val totalProductionSummary: Int = 0,
    val totalSalesSummary: Int = 0,
    val latestClosingStock: Int = 0
)

/**
 * Google Drive file metadata representation
 */
data class DriveFileInfo(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val createdTime: Long,
    val formattedDate: String,
    val formattedTime: String,
    val formattedSize: String,
    val isPreRestoreBackup: Boolean = false
)

/**
 * Live UI and background task states
 */
sealed class BackupProgressState {
    object Idle : BackupProgressState()
    data class Connecting(val stepMessage: String = "গুগল ড্রাইভের সাথে সংযোগ স্থাপন হচ্ছে...") : BackupProgressState()
    data class Preparing(val stepMessage: String = "খামার ও দৈনিক রিপোর্ট ডেটা প্রস্তুত হচ্ছে...") : BackupProgressState()
    data class Uploading(val stepMessage: String = "গুগল ড্রাইভে আপলোড হচ্ছে...") : BackupProgressState()
    data class Downloading(val stepMessage: String = "গুগল ড্রাইভ থেকে ব্যাকআপ ডাউনলোড হচ্ছে...") : BackupProgressState()
    data class Restoring(val stepMessage: String = "নিরাপত্তা ব্যাকআপ তৈরি ও ডেটাবেজ রিস্টোর হচ্ছে...") : BackupProgressState()
    data class Success(val message: String, val timestamp: Long = System.currentTimeMillis()) : BackupProgressState()
    data class Error(val errorMessage: String) : BackupProgressState()
}

