package com.example.data.backup

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.repository.PoultryRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val TAG = "AutoBackupWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val driveManager = DriveBackupManager(applicationContext)

        if (!driveManager.isAutoBackupEnabled()) {
            Log.d(TAG, "Auto backup is disabled. Skipping.")
            return@withContext Result.success()
        }

        if (!driveManager.isConnected()) {
            Log.w(TAG, "Google Drive account not connected. Skipping auto backup.")
            return@withContext Result.success()
        }

        val auth = try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
        val currentUser = auth?.currentUser
        if (currentUser == null) {
            Log.w(TAG, "User not authenticated. Skipping auto backup.")
            return@withContext Result.success()
        }

        val accessToken = driveManager.getAccessToken()
        if (accessToken == null) {
            Log.w(TAG, "Could not acquire Google OAuth token for auto backup.")
            return@withContext Result.retry()
        }

        try {
            val repository = PoultryRepository(applicationContext)
            val farmProfile = repository.farmProfile.first()
            val dailyReports = repository.allDailyReports.first()
            val monthlyExpenses = repository.allExpenses.first()
            val rolePermissions = repository.rolePermissions.first()

            val backupJson = driveManager.createBackupJson(
                farmProfile = farmProfile,
                dailyReports = dailyReports,
                monthlyExpenses = monthlyExpenses,
                rolePermissions = rolePermissions,
                userId = currentUser.uid,
                userEmail = currentUser.email ?: "",
                password = null,
                isPreRestore = false
            )

            val uploadResult = driveManager.uploadBackupToDrive(
                backupJsonString = backupJson,
                isPreRestore = false,
                accessToken = accessToken
            )

            if (uploadResult.isSuccess) {
                Log.i(TAG, "Automatic backup succeeded: ${uploadResult.getOrNull()?.name}")
                Result.success()
            } else {
                Log.e(TAG, "Automatic backup upload failed: ${uploadResult.exceptionOrNull()?.message}")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto backup execution error: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "KaziAgroAutoBackupWork"

        fun schedule(context: Context, frequency: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val intervalHours = if (frequency.equals("WEEKLY", ignoreCase = true)) 24L * 7L else 24L

            val workRequest = PeriodicWorkRequestBuilder<AutoBackupWorker>(
                intervalHours, TimeUnit.HOURS,
                1, TimeUnit.HOURS // flex interval
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

