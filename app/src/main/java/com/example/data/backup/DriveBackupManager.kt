package com.example.data.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.DailyReportEntity
import com.example.data.local.FarmProfileEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.data.local.RolePermissionConfig
import com.example.ui.components.BanglaNumberFormatter
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DriveBackupManager(private val context: Context) {
    private val TAG = "DriveBackupManager"
    private val PREFS_NAME = "kazi_agro_drive_prefs"
    private val PREF_KEY_DRIVE_FOLDER_ID = "key_drive_folder_id"
    private val PREF_KEY_LAST_BACKUP_TIMESTAMP = "key_last_backup_timestamp"
    private val PREF_KEY_LAST_BACKUP_NAME = "key_last_backup_name"
    private val PREF_KEY_AUTO_BACKUP_ENABLED = "key_auto_backup_enabled"
    private val PREF_KEY_AUTO_BACKUP_FREQ = "key_auto_backup_freq" // "DAILY" or "WEEKLY"

    val FOLDER_NAME = "Kazi Agrotech Backups"
    val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Obtains official GoogleSignInClient
     */
    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Checks if a Google account is currently signed in
     */
    fun getConnectedAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return if (account != null && !account.email.isNullOrBlank()) {
            account
        } else {
            null
        }
    }

    fun isConnected(): Boolean = getConnectedAccount() != null

    /**
     * Checks internet connectivity
     */
    fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Gets valid OAuth Access Token for Google Drive API
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        val account = getConnectedAccount() ?: return@withContext null
        try {
            val androidAccount = account.account ?: return@withContext null
            GoogleAuthUtil.getToken(context, androidAccount, "oauth2:$DRIVE_SCOPE")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Google OAuth token: ${e.message}", e)
            null
        }
    }

    /**
     * Clears local token and folder cache on disconnect
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            getGoogleSignInClient().signOut()
            prefs.edit()
                .remove(PREF_KEY_DRIVE_FOLDER_ID)
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting: ${e.message}")
        }
    }

    // ── Drive Folder Management ──

    /**
     * Finds or creates dedicated folder "Kazi Agrotech Backups" in Google Drive
     */
    suspend fun getOrCreateBackupFolderId(accessToken: String): String = withContext(Dispatchers.IO) {
        val cachedFolderId = prefs.getString(PREF_KEY_DRIVE_FOLDER_ID, null)
        if (!cachedFolderId.isNullOrBlank()) {
            // Verify cached folder still exists and is not trashed
            if (verifyFolderExists(cachedFolderId, accessToken)) {
                return@withContext cachedFolderId
            }
        }

        // Search for existing folder in user's Drive
        val query = "name = '$FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false"
        val url = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&spaces=drive&fields=files(id,name)"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = httpClient.newCall(request).execute()
        if (response.isSuccessful) {
            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val files = json.optJSONArray("files")
            if (files != null && files.length() > 0) {
                val existingId = files.getJSONObject(0).getString("id")
                prefs.edit().putString(PREF_KEY_DRIVE_FOLDER_ID, existingId).apply()
                return@withContext existingId
            }
        }

        // Create new folder
        val createUrl = "https://www.googleapis.com/drive/v3/files"
        val folderJson = JSONObject().apply {
            put("name", FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }

        val createRequest = Request.Builder()
            .url(createUrl)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json; charset=UTF-8")
            .post(folderJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .build()

        val createResponse = httpClient.newCall(createRequest).execute()
        if (createResponse.isSuccessful) {
            val responseBody = createResponse.body?.string() ?: ""
            val createdId = JSONObject(responseBody).getString("id")
            prefs.edit().putString(PREF_KEY_DRIVE_FOLDER_ID, createdId).apply()
            return@withContext createdId
        } else {
            throw Exception("গুগল ড্রাইভে ব্যাকআপ ফোল্ডার তৈরি ব্যর্থ হয়েছে: ${createResponse.code}")
        }
    }

    private fun verifyFolderExists(folderId: String, accessToken: String): Boolean {
        return try {
            val url = "https://www.googleapis.com/drive/v3/files/$folderId?fields=id,trashed"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()
            val res = httpClient.newCall(request).execute()
            if (res.isSuccessful) {
                val json = JSONObject(res.body?.string() ?: "")
                !json.optBoolean("trashed", false)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // ── Backup Generation & Upload ──

    /**
     * Constructs the structured BackupPayload and converts to JSON (with optional AES-256 encryption)
     */
    fun createBackupJson(
        farmProfile: FarmProfileEntity?,
        dailyReports: List<DailyReportEntity>,
        monthlyExpenses: List<MonthlyExpenseEntity>,
        rolePermissions: Map<String, RolePermissionConfig>,
        userId: String,
        userEmail: String,
        password: String? = null,
        isPreRestore: Boolean = false
    ): String {
        val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val now = Date()
        val createdAtStr = sdfIso.format(now)

        val totalProd = dailyReports.sumOf { it.eggProduction }
        val totalSale = dailyReports.sumOf { it.eggSold }
        val closingStock = dailyReports.firstOrNull()?.currentStock ?: 0

        val content = BackupDataContent(
            farmProfile = farmProfile,
            dailyReports = dailyReports,
            monthlyExpenses = monthlyExpenses,
            rolePermissions = rolePermissions,
            userCount = 1,
            totalProductionSummary = totalProd,
            totalSalesSummary = totalSale,
            latestClosingStock = closingStock
        )

        val dataJsonAdapter = moshi.adapter(BackupDataContent::class.java)
        val contentJsonString = dataJsonAdapter.toJson(content)

        return if (!password.isNullOrBlank()) {
            val encResult = BackupCryptoHelper.encrypt(contentJsonString, password)
            val payload = BackupPayload(
                backupVersion = 1,
                appName = "Kazi Agrotech",
                backupType = if (isPreRestore) "pre_restore_safety" else "full",
                createdAt = createdAtStr,
                createdAtTimestamp = now.time,
                firebaseUserId = userId,
                userEmail = userEmail,
                isEncrypted = true,
                salt = encResult.saltBase64,
                iv = encResult.ivBase64,
                encryptedPayload = encResult.cipherTextBase64,
                data = null
            )
            moshi.adapter(BackupPayload::class.java).toJson(payload)
        } else {
            val payload = BackupPayload(
                backupVersion = 1,
                appName = "Kazi Agrotech",
                backupType = if (isPreRestore) "pre_restore_safety" else "full",
                createdAt = createdAtStr,
                createdAtTimestamp = now.time,
                firebaseUserId = userId,
                userEmail = userEmail,
                isEncrypted = false,
                salt = "",
                iv = "",
                encryptedPayload = "",
                data = content
            )
            moshi.adapter(BackupPayload::class.java).toJson(payload)
        }
    }

    /**
     * Uploads the backup JSON string to Google Drive
     */
    suspend fun uploadBackupToDrive(
        backupJsonString: String,
        isPreRestore: Boolean = false,
        accessToken: String
    ): Result<DriveFileInfo> = withContext(Dispatchers.IO) {
        try {
            val folderId = getOrCreateBackupFolderId(accessToken)

            val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
            val timestampStr = sdf.format(Date())
            val filename = if (isPreRestore) {
                "Kazi_Agrotech_PreRestore_$timestampStr.json"
            } else {
                "Kazi_Agrotech_Backup_$timestampStr.json"
            }

            val metadataJson = JSONObject().apply {
                put("name", filename)
                put("parents", JSONArray().apply { put(folderId) })
                put("mimeType", "application/json")
            }

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    "metadata",
                    metadataJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    filename,
                    backupJsonString.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .build()

            val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,size,createdTime,modifiedTime"

            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("ড্রাইভে আপলোড ব্যর্থ হয়েছে (${response.code})"))
            }

            val responseBody = response.body?.string() ?: ""
            val fileJson = JSONObject(responseBody)
            val fileId = fileJson.getString("id")
            val name = fileJson.getString("name")
            val size = fileJson.optLong("size", backupJsonString.toByteArray(Charsets.UTF_8).size.toLong())
            val nowTime = System.currentTimeMillis()

            if (!isPreRestore) {
                saveLastBackupMetadata(nowTime, name)
            }

            val info = DriveFileInfo(
                id = fileId,
                name = name,
                sizeBytes = size,
                createdTime = nowTime,
                formattedDate = formatDateFromMillis(nowTime),
                formattedTime = formatTimeFromMillis(nowTime),
                formattedSize = formatBytesToHumanReadable(size),
                isPreRestoreBackup = isPreRestore
            )
            Result.success(info)
        } catch (e: Exception) {
            Log.e(TAG, "Error during drive upload: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── List, Download, Delete ──

    /**
     * Lists all backup files in the Kazi Agrotech Backups folder
     */
    suspend fun listBackupsFromDrive(accessToken: String): Result<List<DriveFileInfo>> = withContext(Dispatchers.IO) {
        try {
            val folderId = getOrCreateBackupFolderId(accessToken)
            val query = "'$folderId' in parents and trashed = false"
            val url = "https://www.googleapis.com/drive/v3/files?q=${java.net.URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,size,createdTime,modifiedTime)&orderBy=createdTime+desc&pageSize=50"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("ব্যাকআপ তালিকা লোড ব্যর্থ: ${response.code}"))
            }

            val list = mutableListOf<DriveFileInfo>()
            val body = response.body?.string() ?: ""
            val json = JSONObject(body)
            val files = json.optJSONArray("files") ?: JSONArray()

            val sdfIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val id = f.getString("id")
                val name = f.getString("name")
                val size = f.optLong("size", 0L)
                val createdStr = f.optString("createdTime", "")
                val createdMillis = try {
                    if (createdStr.isNotBlank()) sdfIso.parse(createdStr.take(19))?.time ?: System.currentTimeMillis()
                    else System.currentTimeMillis()
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }

                if (name.startsWith("Kazi_Agrotech_")) {
                    list.add(
                        DriveFileInfo(
                            id = id,
                            name = name,
                            sizeBytes = size,
                            createdTime = createdMillis,
                            formattedDate = formatDateFromMillis(createdMillis),
                            formattedTime = formatTimeFromMillis(createdMillis),
                            formattedSize = formatBytesToHumanReadable(size),
                            isPreRestoreBackup = name.contains("PreRestore")
                        )
                    )
                }
            }

            Result.success(list.sortedByDescending { it.createdTime })
        } catch (e: Exception) {
            Log.e(TAG, "Error listing backups: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Downloads a backup file content from Google Drive
     */
    suspend fun downloadBackupFromDrive(fileId: String, accessToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("ফাইল ডাউনলোড ব্যর্থ: ${response.code}"))
            }

            val content = response.body?.string() ?: ""
            Result.success(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading backup: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Validates and parses the backup JSON payload
     */
    fun validateAndParseBackup(
        jsonString: String,
        currentUserId: String,
        password: String? = null
    ): Result<BackupDataContent> {
        try {
            val payloadAdapter = moshi.adapter(BackupPayload::class.java)
            val payload = payloadAdapter.fromJson(jsonString)
                ?: return Result.failure(Exception("অবৈধ ব্যাকআপ ফাইল ফরম্যাট।"))

            if (payload.appName != "Kazi Agrotech" && payload.appName != "কাজী এগ্রোটেক") {
                return Result.failure(Exception("এই ব্যাকআপ ফাইলটি কাজী এগ্রোটেক অ্যাপের নয়।"))
            }

            if (payload.backupVersion > 1) {
                return Result.failure(Exception("ব্যাকআপ ভার্সন অসমর্থিত। অ্যাপটি আপডেট করুন।"))
            }

            // User/Farm ownership validation
            if (payload.firebaseUserId.isNotBlank() && currentUserId.isNotBlank() && payload.firebaseUserId != currentUserId) {
                return Result.failure(Exception("এই ব্যাকআপটি আপনার অ্যাকাউন্টের সাথে মেলে না (মালিকানা ভিন্ন)।"))
            }

            if (payload.isEncrypted) {
                if (password.isNullOrBlank()) {
                    return Result.failure(Exception("ENCRYPTION_PASSWORD_REQUIRED"))
                }
                val decryptedJson = try {
                    BackupCryptoHelper.decrypt(
                        cipherTextBase64 = payload.encryptedPayload,
                        saltBase64 = payload.salt,
                        ivBase64 = payload.iv,
                        password = password
                    )
                } catch (e: Exception) {
                    return Result.failure(Exception("ভুল পাসওয়ার্ড! ব্যাকআপ ডিক্রিপ্ট করা যায়নি।"))
                }

                val dataAdapter = moshi.adapter(BackupDataContent::class.java)
                val content = dataAdapter.fromJson(decryptedJson)
                    ?: return Result.failure(Exception("ডিক্রিপ্ট করা ডেটা ত্রুটিপূর্ণ।"))
                return Result.success(content)
            } else {
                val content = payload.data ?: return Result.failure(Exception("ব্যাকআপ ফাইলে কোনো তথ্য পাওয়া যায়নি।"))
                return Result.success(content)
            }
        } catch (e: Exception) {
            return Result.failure(Exception("ব্যাকআপ যাচাইকরণ ব্যর্থ: ${e.message}"))
        }
    }

    /**
     * Deletes a backup file from Google Drive
     */
    suspend fun deleteBackupFromDrive(fileId: String, accessToken: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files/$fileId"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $accessToken")
                .delete()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful || response.code == 204) {
                Result.success(true)
            } else {
                Result.failure(Exception("ফাইল ডিলিট ব্যর্থ: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Local Metadata & Preferences ──

    private fun saveLastBackupMetadata(timestamp: Long, name: String) {
        prefs.edit()
            .putLong(PREF_KEY_LAST_BACKUP_TIMESTAMP, timestamp)
            .putString(PREF_KEY_LAST_BACKUP_NAME, name)
            .apply()
    }

    fun getLastBackupTimestamp(): Long = prefs.getLong(PREF_KEY_LAST_BACKUP_TIMESTAMP, 0L)
    fun getLastBackupName(): String = prefs.getString(PREF_KEY_LAST_BACKUP_NAME, "") ?: ""

    fun isAutoBackupEnabled(): Boolean = prefs.getBoolean(PREF_KEY_AUTO_BACKUP_ENABLED, false)
    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getAutoBackupFrequency(): String = prefs.getString(PREF_KEY_AUTO_BACKUP_FREQ, "DAILY") ?: "DAILY"
    fun setAutoBackupFrequency(freq: String) {
        prefs.edit().putString(PREF_KEY_AUTO_BACKUP_FREQ, freq).apply()
    }

    // ── Helpers ──

    fun formatBytesToHumanReadable(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }

    fun formatDateFromMillis(millis: Long): String {
        if (millis <= 0L) return "কখনও নয়"
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return BanglaNumberFormatter.formatBanglaDate(sdf.format(Date(millis)))
    }

    fun formatTimeFromMillis(millis: Long): String {
        if (millis <= 0L) return ""
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        return BanglaNumberFormatter.toBanglaDigits(sdf.format(Date(millis)))
    }
}

