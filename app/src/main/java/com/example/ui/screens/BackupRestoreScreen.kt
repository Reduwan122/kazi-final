package com.example.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddModerator
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.BackupProgressState
import com.example.data.backup.DriveFileInfo
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.SnackbarController
import com.example.ui.components.rememberHaptics
import com.example.ui.viewmodel.PoultryViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    viewModel: PoultryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()

    val googleAccountEmail by viewModel.googleAccountEmail.collectAsState()
    val isConnected = googleAccountEmail != null
    val progressState by viewModel.backupProgressState.collectAsState()
    val driveBackups by viewModel.driveBackupsList.collectAsState()
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsState()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()
    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsState()

    var showPasswordModalForBackup by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var confirmBackupPassword by remember { mutableStateOf("") }
    var enableEncryption by remember { mutableStateOf(false) }

    var showRestoreSelectDialog by remember { mutableStateOf(false) }
    var selectedBackupForRestore by remember { mutableStateOf<DriveFileInfo?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showPasswordModalForRestore by remember { mutableStateOf(false) }
    var restorePassword by remember { mutableStateOf("") }

    var selectedFileUriForRestore by remember { mutableStateOf<Uri?>(null) }
    var showRestoreFromFileConfirmDialog by remember { mutableStateOf(false) }
    var showPasswordModalForFileRestore by remember { mutableStateOf(false) }

    var showSetupHelpDialog by remember { mutableStateOf(false) }
    var deleteConfirmBackup by remember { mutableStateOf<DriveFileInfo?>(null) }

    // System File Save (CreateDocument) Launcher - Allows saving directly to Google Drive App or Local Storage
    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackupToUri(
                context = context,
                uri = uri,
                password = if (enableEncryption && backupPassword.isNotBlank()) backupPassword else null
            )
        }
    }

    // System File Open (OpenDocument) Launcher - Allows picking backup file from Google Drive App, Files, or Downloads
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            selectedFileUriForRestore = uri
            showRestoreFromFileConfirmDialog = true
        }
    }

    // Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                if (account != null) {
                    viewModel.refreshGoogleAccountStatus()
                    SnackbarController.showMessage("গুগল ড্রাইভ সফলভাবে সংযুক্ত হয়েছে (${account.email ?: ""})")
                } else {
                    val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                    if (lastAccount != null) {
                        viewModel.refreshGoogleAccountStatus()
                        SnackbarController.showMessage("গুগল ড্রাইভ সফলভাবে সংযুক্ত হয়েছে (${lastAccount.email ?: ""})")
                    } else {
                        SnackbarController.showError("গুগল সাইন-ইন বাতিল করা হয়েছে")
                    }
                }
            } catch (e: Exception) {
                val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                if (lastAccount != null) {
                    viewModel.refreshGoogleAccountStatus()
                    SnackbarController.showMessage("গুগল ড্রাইভ সফলভাবে সংযুক্ত হয়েছে (${lastAccount.email ?: ""})")
                } else {
                    SnackbarController.showError("গুগল সাইন-ইন বাতিল করা হয়েছে")
                }
            }
        } else {
            val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
            if (lastAccount != null) {
                viewModel.refreshGoogleAccountStatus()
                SnackbarController.showMessage("গুগল ড্রাইভ সংযুক্ত হয়েছে (${lastAccount.email ?: ""})")
            } else {
                SnackbarController.showError("গুগল সাইন-ইন বাতিল করা হয়েছে")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshGoogleAccountStatus()
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "গুগল ড্রাইভ ব্যাকআপ ও রিস্টোর",
                isRootScreen = false,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("backup_restore_screen"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ══════════════════════════════════════════════════════════════
            // 1. Universal One-Tap Google Drive & Local Storage Card
            // ══════════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color(0xFF0D631B).copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = Color(0xFF0D631B),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "তাত্ক্ষণিক ব্যাকআপ ও এক্সপোর্ট",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "গুগল ড্রাইভ ফোল্ডার, মেমোরি ও শেয়ারিং",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Optional Encryption Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (enableEncryption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "পাসওয়ার্ড এনক্রিপশন (AES-256)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Switch(
                            checked = enableEncryption,
                            onCheckedChange = { enableEncryption = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Action 1: Save directly to Google Drive / Local Storage
                    Button(
                        onClick = {
                            haptics.tap()
                            val sdf = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
                            val defaultFileName = "Kazi_Agrotech_Backup_${sdf.format(Date())}.kazi"
                            saveFileLauncher.launch(defaultFileName)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D631B))
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("গুগল ড্রাইভ ও মেমোরিতে ফাইল সেভ করুন", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }

                    // Action 2: Restore from Google Drive / Local File
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            openFileLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("গুগল ড্রাইভ / ফাইল থেকে রিস্টোর করুন", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    }

                    // Action 3: Direct Share
                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            viewModel.shareBackupFile(context, if (enableEncryption) backupPassword else null)
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("অন্য অ্যাপ বা জিমেইলে সরাসরি শেয়ার করুন", fontSize = 13.sp)
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // 2. Google Drive Cloud API Sync Card
            // ══════════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isConnected) Color(0xFFE8F5E9)
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "গুগল ড্রাইভ ক্লাউড সিঙ্ক",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isConnected) "অ্যাকাউন্ট সক্রিয় ও সংযুক্ত" else "ক্লাউড অটোমেটিক সিঙ্ক্রোনাইজেশন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { showSetupHelpDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Help",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (isConnected) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE8F5E9).copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = googleAccountEmail ?: "",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color(0xFF1B5E20)
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        haptics.tap()
                                        viewModel.disconnectGoogleAccount()
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("ডিসকানেক্ট", fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                haptics.tap()
                                val client = viewModel.driveBackupManager.getGoogleSignInClient()
                                googleSignInLauncher.launch(client.signInIntent)
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("গুগল ড্রাইভ ক্লাউড কানেক্ট করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // 3. Live Progress Banner (if active)
            // ══════════════════════════════════════════════════════════════
            when (val state = progressState) {
                is BackupProgressState.Connecting,
                is BackupProgressState.Preparing,
                is BackupProgressState.Uploading,
                is BackupProgressState.Downloading,
                is BackupProgressState.Restoring -> {
                    val msg = when (state) {
                        is BackupProgressState.Connecting -> state.stepMessage
                        is BackupProgressState.Preparing -> state.stepMessage
                        is BackupProgressState.Uploading -> state.stepMessage
                        is BackupProgressState.Downloading -> state.stepMessage
                        is BackupProgressState.Restoring -> state.stepMessage
                        else -> "প্রক্রিয়াধীন..."
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                is BackupProgressState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = state.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                is BackupProgressState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
                is BackupProgressState.Idle -> {}
            }

            // ══════════════════════════════════════════════════════════════
            // 4. Automatic Backup Card (WorkManager)
            // ══════════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "স্বয়ংক্রিয় ব্যাকগ্রাউন্ড ব্যাকআপ",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ইন্টারনেট থাকলে ক্লাউডে স্বয়ংক্রিয় ব্যাকআপ হবে",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isAutoBackupEnabled,
                            onCheckedChange = { enabled ->
                                haptics.tap()
                                viewModel.updateAutoBackupSettings(enabled, autoBackupFrequency)
                            },
                            enabled = isConnected,
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (isAutoBackupEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Text(
                            text = "ব্যাকআপ ফ্রিকোয়েন্সি:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    viewModel.updateAutoBackupSettings(true, "DAILY")
                                }
                            ) {
                                RadioButton(
                                    selected = autoBackupFrequency == "DAILY",
                                    onClick = { viewModel.updateAutoBackupSettings(true, "DAILY") }
                                )
                                Text("দৈনিক (Daily)")
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    viewModel.updateAutoBackupSettings(true, "WEEKLY")
                                }
                            ) {
                                RadioButton(
                                    selected = autoBackupFrequency == "WEEKLY",
                                    onClick = { viewModel.updateAutoBackupSettings(true, "WEEKLY") }
                                )
                                Text("সাপ্তাহিক (Weekly)")
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // 5. Cloud Backup List (if connected)
            // ══════════════════════════════════════════════════════════════
            if (isConnected) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "গুগল ড্রাইভ ক্লাউড ব্যাকআপ হিস্ট্রি",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = {
                                    haptics.tap()
                                    viewModel.fetchDriveBackupsList()
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        if (driveBackups.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "গুগল ড্রাইভে কোনো ক্লাউড ব্যাকআপ পাওয়া যায়নি।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            driveBackups.forEach { file ->
                                DriveBackupItemRow(
                                    file = file,
                                    onRestoreClick = {
                                        haptics.tap()
                                        selectedBackupForRestore = file
                                        showRestoreConfirmDialog = true
                                    },
                                    onDeleteClick = {
                                        haptics.tap()
                                        deleteConfirmBackup = file
                                    }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog 1: File Restore Confirmation
    // ══════════════════════════════════════════════════════════════
    if (showRestoreFromFileConfirmDialog && selectedFileUriForRestore != null) {
        val targetUri = selectedFileUriForRestore!!
        AlertDialog(
            onDismissRequest = {
                showRestoreFromFileConfirmDialog = false
                selectedFileUriForRestore = null
            },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("ফাইল থেকে রিস্টোর করবেন?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "নির্বাচিত ব্যাকআপ ফাইল থেকে সকল খামার রেকর্ড ও স্টক তথ্য রিস্টোর করা হবে।",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "⚠️ সতর্কতা: বর্তমান ডেটাবেজের তথ্য ব্যাকআপ ফাইলের ডেটা দ্বারা প্রতিস্থাপিত হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreFromFileConfirmDialog = false
                        viewModel.restoreBackupFromUri(
                            context = context,
                            uri = targetUri,
                            password = null,
                            onError = { err ->
                                if (err == "ENCRYPTION_PASSWORD_REQUIRED") {
                                    showPasswordModalForFileRestore = true
                                }
                            }
                        )
                    }
                ) {
                    Text("নিশ্চিত ও রিস্টোর করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRestoreFromFileConfirmDialog = false
                    selectedFileUriForRestore = null
                }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog 2: Password prompt for file restore
    // ══════════════════════════════════════════════════════════════
    if (showPasswordModalForFileRestore && selectedFileUriForRestore != null) {
        val targetUri = selectedFileUriForRestore!!
        AlertDialog(
            onDismissRequest = { showPasswordModalForFileRestore = false },
            title = { Text("ডিক্রিপশন পাসওয়ার্ড লিখুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("এই ব্যাকআপ ফাইলটি পাসওয়ার্ড দিয়ে সুরক্ষিত। ব্যাকআপ তৈরির সময় প্রদত্ত পাসওয়ার্ডটি লিখুন।", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("পাসওয়ার্ড") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restorePassword.isBlank()) {
                            SnackbarController.showError("পাসওয়ার্ড লিখুন")
                            return@Button
                        }
                        showPasswordModalForFileRestore = false
                        viewModel.restoreBackupFromUri(
                            context = context,
                            uri = targetUri,
                            password = restorePassword
                        )
                    }
                ) {
                    Text("ডিক্রিপ্ট ও রিস্টোর করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordModalForFileRestore = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog 3: Google Drive API Setup Help Information
    // ══════════════════════════════════════════════════════════════
    if (showSetupHelpDialog) {
        AlertDialog(
            onDismissRequest = { showSetupHelpDialog = false },
            icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("গুগল ক্লাউড সিঙ্ক নির্দেশিকা", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "১. তাত্ক্ষণিক ব্যাকআপ:\n'গুগল ড্রাইভ ও মেমোরিতে ফাইল সেভ করুন' বাটন চাপলে কোনো কনফিগারেশন ছাড়াই সরাসরি গুগল ড্রাইভ ফোল্ডারে ফাইল সংরক্ষণ করা যায়।",
                        style = MaterialTheme.typography.bodySmall
                    )
                    HorizontalDivider()
                    Text(
                        text = "২. অটো ক্লাউড সিঙ্ক:\nসরাসরি গুগল ড্রাইভ ব্যাকগ্রাউন্ড সিঙ্ক চালু করতে ফায়ারবেস কনসোলে আপনার অ্যাপের SHA-1 ফিঙ্গারপ্রিন্ট যোগ করুন:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Package: com.aistudio.kaziagro.poultr\nSHA-1: 0A:A4:F8:1F:F6:17:9F:B1:2F:BA:55:DE:FC:F2:23:3D:CB:DF:C2:B2",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showSetupHelpDialog = false }) {
                    Text("বুঝেছি")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog 4: Restore Confirmation (Cloud)
    // ══════════════════════════════════════════════════════════════
    if (showRestoreConfirmDialog && selectedBackupForRestore != null) {
        val targetFile = selectedBackupForRestore!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("ডেটা রিস্টোর নিশ্চিতকরণ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("আপনি কি নিশ্চিত যে '${targetFile.name}' ব্যাকআপ থেকে ডেটা রিস্টোর করতে চান?")
                    Text("⚠️ সতর্কতা: রিস্টোর সম্পন্ন হলে বর্তমান অ্যাপের লোকাল ডেটা প্রতিস্থাপিত হবে। নিরাপত্তার জন্য পূর্ববর্তী ডেটার একটি স্বয়ংক্রিয় সেফটি ব্যাকআপ নেওয়া হবে।", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        viewModel.performRestore(
                            driveFile = targetFile,
                            password = null,
                            onError = { err ->
                                if (err == "ENCRYPTION_PASSWORD_REQUIRED") {
                                    showPasswordModalForRestore = true
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("নিশ্চিত ও রিস্টোর করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog 5: Password prompt for encrypted cloud restore
    // ══════════════════════════════════════════════════════════════
    if (showPasswordModalForRestore && selectedBackupForRestore != null) {
        val targetFile = selectedBackupForRestore!!
        AlertDialog(
            onDismissRequest = { showPasswordModalForRestore = false },
            title = { Text("ডিক্রিপশন পাসওয়ার্ড লিখুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("এই ব্যাকআপ ফাইলটি পাসওয়ার্ড দিয়ে সুরক্ষিত। ব্যাকআপ তৈরির সময় প্রদত্ত পাসওয়ার্ডটি লিখুন।", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("পাসওয়ার্ড") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restorePassword.isBlank()) {
                            SnackbarController.showError("পাসওয়ার্ড লিখুন")
                            return@Button
                        }
                        showPasswordModalForRestore = false
                        viewModel.performRestore(
                            driveFile = targetFile,
                            password = restorePassword
                        )
                    }
                ) {
                    Text("ডিক্রিপ্ট ও রিস্টোর করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordModalForRestore = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog 6: Delete confirmation
    // ══════════════════════════════════════════════════════════════
    if (deleteConfirmBackup != null) {
        val delFile = deleteConfirmBackup!!
        AlertDialog(
            onDismissRequest = { deleteConfirmBackup = null },
            title = { Text("ব্যাকআপ ফাইল ডিলিট করবেন?") },
            text = {
                Text("আপনি কি নিশ্চিত যে গুগল ড্রাইভ থেকে '${delFile.name}' ফাইলটি স্থায়ীভাবে ডিলিট করতে চান?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = deleteConfirmBackup ?: return@Button
                        deleteConfirmBackup = null
                        viewModel.deleteDriveBackup(file)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmBackup = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun DriveBackupItemRow(
    file: DriveFileInfo,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (file.isPreRestoreBackup) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (file.isPreRestoreBackup) Icons.Default.Security else Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = if (file.isPreRestoreBackup) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (file.isPreRestoreBackup) "সুরক্ষা ব্যাকআপ (Pre-Restore)" else "সম্পূর্ণ ব্যাকআপ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${file.formattedDate} • ${file.formattedTime}  (${file.formattedSize})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onRestoreClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
