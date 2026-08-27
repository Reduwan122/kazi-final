package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Data class representing update metadata from Firebase Realtime Database (appUpdate node).
 */
data class AppUpdateInfo(
    val versionCode: Int = 0,
    val versionName: String = "",
    val apkUrl: String = "",
    val forceUpdate: Boolean = false,
    val title: String = "নতুন আপডেট পাওয়া গেছে",
    val message: String = "কাজী এগ্রোটেক-এর নতুন সংস্করণ পাওয়া গেছে।",
    val releaseNotes: String = "",
    val fileSize: Long = 0L,
    val sha256: String = "",
    val releaseDate: String = ""
)

/**
 * State of in-app update workflow.
 */
sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class Available(val info: AppUpdateInfo) : UpdateState()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateState()
    data class Downloaded(val apkFile: File, val info: AppUpdateInfo) : UpdateState()
    data class Installing(val info: AppUpdateInfo) : UpdateState()
    data class Error(val message: String) : UpdateState()
    object UpToDate : UpdateState()
}

/**
 * Production-grade In-App Update Engine for Kazi Agrotech.
 */
class AppUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateManager"
        private const val UPDATE_NODE = "appUpdate"
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _availableUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val availableUpdate: StateFlow<AppUpdateInfo?> = _availableUpdate.asStateFlow()

    private var activeDownloadConnection: HttpURLConnection? = null
    @Volatile
    private var isDownloadCancelled = false

    init {
        startListeningForUpdates()
    }

    /**
     * Real-time Firebase listener to immediately detect new releases even while the app is running.
     */
    fun startListeningForUpdates() {
        try {
            val database = FirebaseDatabase.getInstance()
            database.reference.child(UPDATE_NODE).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) return
                    val remoteVersionCode = snapshot.child("versionCode").getValue(Long::class.java)?.toInt()
                        ?: snapshot.child("versionCode").getValue(Int::class.java)
                        ?: 0

                    val remoteVersionName = snapshot.child("versionName").getValue(String::class.java) ?: ""
                    val apkUrl = snapshot.child("apkUrl").getValue(String::class.java) ?: ""
                    val forceUpdate = snapshot.child("forceUpdate").getValue(Boolean::class.java) ?: false
                    val title = snapshot.child("title").getValue(String::class.java) ?: "নতুন আপডেট পাওয়া গেছে"
                    val message = snapshot.child("message").getValue(String::class.java)
                        ?: "কাজী এগ্রোটেক-এর নতুন সংস্করণ পাওয়া গেছে।"
                    val releaseNotes = snapshot.child("releaseNotes").getValue(String::class.java) ?: ""
                    val fileSize = snapshot.child("fileSize").getValue(Long::class.java) ?: 0L
                    val sha256 = snapshot.child("sha256").getValue(String::class.java) ?: ""
                    val releaseDate = snapshot.child("releaseDate").getValue(String::class.java) ?: ""

                    val installedVersionCode = BuildConfig.VERSION_CODE
                    Log.d(TAG, "Realtime update check: installed=$installedVersionCode, remote=$remoteVersionCode")

                    if (remoteVersionCode > installedVersionCode && apkUrl.isNotBlank()) {
                        val info = AppUpdateInfo(
                            versionCode = remoteVersionCode,
                            versionName = remoteVersionName,
                            apkUrl = apkUrl,
                            forceUpdate = forceUpdate,
                            title = title,
                            message = message,
                            releaseNotes = releaseNotes,
                            fileSize = fileSize,
                            sha256 = sha256,
                            releaseDate = releaseDate
                        )
                        _availableUpdate.value = info
                        if (_updateState.value is UpdateState.Idle || _updateState.value is UpdateState.Error || _updateState.value is UpdateState.Checking) {
                            _updateState.value = UpdateState.Available(info)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "Update listener cancelled: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach real-time update listener: ${e.message}")
        }
    }

    /**
     * Checks Firebase for available updates immediately on demand or on app launch.
     */
    suspend fun checkForUpdates(
        isManual: Boolean = false,
        onResult: ((hasUpdate: Boolean, message: String?) -> Unit)? = null
    ): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            _updateState.value = UpdateState.Checking
            val database = FirebaseDatabase.getInstance()
            val snapshot = database.reference.child(UPDATE_NODE).get().await()

            if (!snapshot.exists()) {
                Log.d(TAG, "No update node exists in Firebase Realtime Database.")
                if (isManual) {
                    _updateState.value = UpdateState.UpToDate
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false, "আপনি সর্বশেষ সংস্করণ ব্যবহার করছেন।")
                    }
                } else {
                    _updateState.value = UpdateState.Idle
                }
                return@withContext null
            }

            val remoteVersionCode = snapshot.child("versionCode").getValue(Long::class.java)?.toInt()
                ?: snapshot.child("versionCode").getValue(Int::class.java)
                ?: 0

            val remoteVersionName = snapshot.child("versionName").getValue(String::class.java) ?: ""
            val apkUrl = snapshot.child("apkUrl").getValue(String::class.java) ?: ""
            val forceUpdate = snapshot.child("forceUpdate").getValue(Boolean::class.java) ?: false
            val title = snapshot.child("title").getValue(String::class.java) ?: "নতুন আপডেট পাওয়া গেছে"
            val message = snapshot.child("message").getValue(String::class.java)
                ?: "কাজী এগ্রোটেক-এর নতুন সংস্করণ পাওয়া গেছে।"
            val releaseNotes = snapshot.child("releaseNotes").getValue(String::class.java) ?: ""
            val fileSize = snapshot.child("fileSize").getValue(Long::class.java) ?: 0L
            val sha256 = snapshot.child("sha256").getValue(String::class.java) ?: ""
            val releaseDate = snapshot.child("releaseDate").getValue(String::class.java) ?: ""

            val installedVersionCode = BuildConfig.VERSION_CODE
            Log.d(TAG, "Installed VersionCode: $installedVersionCode, Remote VersionCode: $remoteVersionCode")

            if (remoteVersionCode > installedVersionCode && apkUrl.isNotBlank()) {
                val info = AppUpdateInfo(
                    versionCode = remoteVersionCode,
                    versionName = remoteVersionName,
                    apkUrl = apkUrl,
                    forceUpdate = forceUpdate,
                    title = title,
                    message = message,
                    releaseNotes = releaseNotes,
                    fileSize = fileSize,
                    sha256 = sha256,
                    releaseDate = releaseDate
                )
                _availableUpdate.value = info
                _updateState.value = UpdateState.Available(info)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(true, "নতুন সংস্করণ $remoteVersionName পাওয়া গেছে।")
                }
                return@withContext info
            } else {
                _availableUpdate.value = null
                if (isManual) {
                    _updateState.value = UpdateState.UpToDate
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false, "আপনি সর্বশেষ সংস্করণ ব্যবহার করছেন।")
                    }
                } else {
                    _updateState.value = UpdateState.Idle
                }
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update check failed: ${e.message}", e)
            _updateState.value = UpdateState.Error("আপডেট চেক ব্যর্থ হয়েছে: ${e.localizedMessage ?: "নেটওয়ার্ক সমস্যা"}")
            withContext(Dispatchers.Main) {
                onResult?.invoke(false, e.localizedMessage)
            }
            return@withContext null
        }
    }

    /**
     * Downloads APK directly from GitHub Releases via streaming HTTPS, tracking progress and validating SHA-256.
     */
    suspend fun downloadApk(info: AppUpdateInfo): Result<File> = withContext(Dispatchers.IO) {
        isDownloadCancelled = false
        _updateState.value = UpdateState.Downloading(0f, 0L, info.fileSize)

        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val targetFile = File(updatesDir, "kazi-agrotech-${info.versionName}.apk")

        if (targetFile.exists() && targetFile.length() > 0 && info.sha256.isNotBlank()) {
            val existingHash = calculateFileSha256(targetFile)
            if (existingHash.equals(info.sha256.trim(), ignoreCase = true)) {
                Log.d(TAG, "Valid cached APK found with matching SHA-256.")
                _updateState.value = UpdateState.Downloaded(targetFile, info)
                return@withContext Result.success(targetFile)
            } else {
                targetFile.delete()
            }
        }

        try {
            var urlString = info.apkUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0
            val maxRedirects = 6

            while (redirectCount < maxRedirects) {
                val url = URL(urlString)
                connection = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 30000
                    readTimeout = 60000
                    setRequestProperty("User-Agent", "KaziAgrotech-Android-Updater")
                    setRequestProperty("Accept-Encoding", "identity")
                }
                activeDownloadConnection = connection
                connection.connect()

                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_SEE_OTHER ||
                    status == 307 || status == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl == null) throw Exception("Redirect location not provided by server")
                    urlString = newUrl
                    redirectCount++
                } else if (status == HttpURLConnection.HTTP_OK) {
                    break
                } else {
                    connection.disconnect()
                    throw Exception("সার্ভার থেকে ফাইল ডাউনলোডে ত্রুটি: HTTP $status")
                }
            }

            val finalConnection = connection ?: throw Exception("ডাউনলোড সংযোগ স্থাপন সম্ভব হয়নি")
            val totalBytes = if (finalConnection.contentLengthLong > 0) finalConnection.contentLengthLong else info.fileSize
            val digest = MessageDigest.getInstance("SHA-256")

            finalConnection.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesRead: Int
                    var totalDownloaded = 0L
                    var lastProgressEmit = System.currentTimeMillis()

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (isDownloadCancelled) {
                            targetFile.delete()
                            _updateState.value = UpdateState.Idle
                            return@withContext Result.failure(Exception("ডাউনলোড বাতিল করা হয়েছে"))
                        }
                        output.write(buffer, 0, bytesRead)
                        digest.update(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastProgressEmit > 100 || totalDownloaded == totalBytes) {
                            val progress = if (totalBytes > 0) (totalDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                            _updateState.value = UpdateState.Downloading(progress, totalDownloaded, totalBytes)
                            lastProgressEmit = now
                        }
                    }
                    output.flush()
                }
            }

            activeDownloadConnection = null

            // Validate SHA-256
            if (info.sha256.isNotBlank()) {
                val computedHash = digest.digest().joinToString("") { "%02x".format(it) }
                val expectedHash = info.sha256.trim().lowercase()

                Log.d(TAG, "Computed SHA-256: $computedHash")
                Log.d(TAG, "Expected SHA-256: $expectedHash")

                if (!computedHash.equals(expectedHash, ignoreCase = true)) {
                    targetFile.delete()
                    val errorMsg = "ফাইল ভেরিফিকেশন ব্যর্থ (SHA-256 মিসম্যাচ)। দয়া করে পুনরায় চেষ্টা করুন।"
                    _updateState.value = UpdateState.Error(errorMsg)
                    return@withContext Result.failure(Exception(errorMsg))
                }
            }

            _updateState.value = UpdateState.Downloaded(targetFile, info)
            Result.success(targetFile)
        } catch (e: Exception) {
            activeDownloadConnection = null
            Log.e(TAG, "APK Download failed: ${e.message}", e)
            val errorMsg = "ডাউনলোড ব্যর্থ: ${e.localizedMessage ?: "অজ্ঞাত সমস্যা"}"
            _updateState.value = UpdateState.Error(errorMsg)
            Result.failure(e)
        }
    }

    /**
     * Cancels an ongoing APK download.
     */
    fun cancelDownload() {
        isDownloadCancelled = true
        try {
            activeDownloadConnection?.disconnect()
        } catch (ignored: Exception) {
            // ignore
        }
        _updateState.value = UpdateState.Idle
    }

    /**
     * Launches native Android Package Installer for the downloaded APK file.
     */
    fun installApk(apkFile: File, info: AppUpdateInfo? = null): Result<Unit> {
        return try {
            if (!apkFile.exists() || apkFile.length() == 0L) {
                return Result.failure(Exception("ইনস্টলেশন ফাইল পাওয়া যায়নি"))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return Result.failure(Exception("আপডেট ইনস্টল করতে অ্যাপ পারমিশন চালু করুন এবং পুনরায় চেষ্টা করুন।"))
                }
            }

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            context.startActivity(installIntent)
            if (info != null) {
                _updateState.value = UpdateState.Installing(info)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package installer: ${e.message}", e)
            _updateState.value = UpdateState.Error("ইনস্টলার চালুকরণে সমস্যা: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    /**
     * Computes SHA-256 for a given local file.
     */
    fun calculateFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(file.inputStream()).use { input ->
            val buffer = ByteArray(16384)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Dismisses the update dialog / clears current update state.
     */
    fun dismissUpdate() {
        _availableUpdate.value = null
        _updateState.value = UpdateState.Idle
    }
}
