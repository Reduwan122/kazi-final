package com.example.ui.screens

import android.app.Activity
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
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

    var deleteConfirmBackup by remember { mutableStateOf<DriveFileInfo?>(null) }

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
            // ══════════════════════════════════════════════════════════════
            // 1. Modern Google Drive Account Connection Card
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Accent Status Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(
                                if (isConnected) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (isConnected) {
                            // Connected Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFE8F5E9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDone,
                                            contentDescription = "Connected",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "গুগল ড্রাইভ ক্লাউড",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFE8F5E9))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF2E7D32))
                                                    )
                                                    Text(
                                                        text = "সংযুক্ত",
                                                        color = Color(0xFF2E7D32),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = googleAccountEmail ?: "অ্যাকাউন্ট সংযুক্ত",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        haptics.tap()
                                        viewModel.fetchDriveBackupsList()
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Metadata Card
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ক্লাউড ফোল্ডার:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = viewModel.driveBackupManager.FOLDER_NAME,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "সর্বশেষ ক্লাউড ব্যাকআপ:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (lastBackupTimestamp > 0) {
                                                viewModel.driveBackupManager.formatDateFromMillis(lastBackupTimestamp)
                                            } else {
                                                "এখনো নেওয়া হয়নি"
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (lastBackupTimestamp > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }

                            // Disconnect Button
                            OutlinedButton(
                                onClick = {
                                    haptics.tap()
                                    viewModel.disconnectGoogleAccount()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("অ্যাকাউন্ট ডিসকানেক্ট করুন", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            // Disconnected State
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Cloud",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "গুগল ড্রাইভ ব্যাকআপ",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "স্বয়ংক্রিয় ক্লাউড সিঙ্ক ও এনক্রিপশন সুরক্ষা",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

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
                                Text("গুগল ড্রাইভ কানেক্ট করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // 2. Live Progress Banner (if active)
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
            // 3. Manual Backup Card
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
                        Text(
                            text = "ম্যানুয়াল ব্যাকআপ",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("সর্বশেষ ব্যাকআপ:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (lastBackupTimestamp > 0L) {
                                "${viewModel.driveBackupManager.formatDateFromMillis(lastBackupTimestamp)} • ${viewModel.driveBackupManager.formatTimeFromMillis(lastBackupTimestamp)}"
                            } else "কখনও নেওয়া হয়নি",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Optional Encryption Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = "পাসওয়ার্ড দিয়ে এনক্রিপ্ট করুন (AES-256)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Switch(
                            checked = enableEncryption,
                            onCheckedChange = { enableEncryption = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    Button(
                        onClick = {
                            haptics.tap()
                            if (enableEncryption) {
                                showPasswordModalForBackup = true
                            } else {
                                viewModel.performManualBackup(password = null)
                            }
                        },
                        enabled = isConnected,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("এখনই ব্যাকআপ নিন", fontWeight = FontWeight.Bold)
                    }
                }
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
                                    text = "স্বয়ংক্রিয় ব্যাকআপ",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ইন্টারনেট থাকলে ব্যাকগ্রাউন্ডে সংরক্ষিত হবে",
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
            // 5. Restore From Google Drive Card
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
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "গুগল ড্রাইভ থেকে রিস্টোর",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "গুগল ড্রাইভে সংরক্ষিত যেকোনো পূর্ববর্তী ব্যাকআপ ফাইল থেকে সম্পূর্ণ খামার ডেটা ও স্টক হিসাব রিস্টোর করতে পারেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            haptics.tap()
                            viewModel.fetchDriveBackupsList()
                            showRestoreSelectDialog = true
                        },
                        enabled = isConnected,
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("রিস্টোর ব্যাকআপ নির্বাচন করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ══════════════════════════════════════════════════════════════
            // 6. Google Drive Backup History List
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
                                text = "গুগল ড্রাইভ ব্যাকআপ হিস্ট্রি",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "${BanglaNumberFormatter.formatNumber(driveBackups.size)} টি ফাইল",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    if (driveBackups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isConnected) "গুগল ড্রাইভে কোনো ব্যাকআপ ফাইল নেই।" else "গুগল ড্রাইভ কানেক্ট করে হিস্ট্রি দেখুন।",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        driveBackups.forEach { file ->
                            DriveBackupItemRow(
                                file = file,
                                onRestoreClick = {
                                    selectedBackupForRestore = file
                                    showRestoreConfirmDialog = true
                                },
                                onDeleteClick = {
                                    deleteConfirmBackup = file
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog 1: Password input for manual backup
    // ══════════════════════════════════════════════════════════════
    if (showPasswordModalForBackup) {
        AlertDialog(
            onDismissRequest = { showPasswordModalForBackup = false },
            title = { Text("ব্যাকআপ এনক্রিপশন পাসওয়ার্ড") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("এই পাসওয়ার্ডটি সুরক্ষিত রাখুন। রিস্টোর করার সময় এই পাসওয়ার্ডটি প্রয়োজন হবে।", style = MaterialTheme.typography.bodySmall)

                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("পাসওয়ার্ড") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = confirmBackupPassword,
                        onValueChange = { confirmBackupPassword = it },
                        label = { Text("পাসওয়ার্ড নিশ্চিত করুন") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (backupPassword.isBlank()) {
                            SnackbarController.showError("পাসওয়ার্ড লিখুন")
                            return@Button
                        }
                        if (backupPassword != confirmBackupPassword) {
                            SnackbarController.showError("পাসওয়ার্ড দুটি মিলছে না")
                            return@Button
                        }
                        showPasswordModalForBackup = false
                        viewModel.performManualBackup(password = backupPassword)
                    }
                ) {
                    Text("এনক্রিপ্ট করে ব্যাকআপ নিন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordModalForBackup = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog 2: Select backup to restore
    // ══════════════════════════════════════════════════════════════
    if (showRestoreSelectDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreSelectDialog = false },
            title = { Text("রিস্টোর করার ব্যাকআপ নির্বাচন করুন") },
            text = {
                if (driveBackups.isEmpty()) {
                    Text("গুগল ড্রাইভে কোনো ব্যাকআপ ফাইল পাওয়া যায়নি।")
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        driveBackups.forEach { file ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedBackupForRestore = file
                                        showRestoreSelectDialog = false
                                        showRestoreConfirmDialog = true
                                    }
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${file.formattedDate} • ${file.formattedTime} (${file.formattedSize})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRestoreSelectDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    // ══════════════════════════════════════════════════════════════
    // Dialog 3: Confirmation Dialog before Restore
    // ══════════════════════════════════════════════════════════════
    if (showRestoreConfirmDialog && selectedBackupForRestore != null) {
        val targetFile = selectedBackupForRestore!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = { Text("ব্যাকআপ রিস্টোর করবেন?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ফাইল: ${targetFile.name}\nতারিখ: ${targetFile.formattedDate} (${targetFile.formattedTime})",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "⚠️ এই ব্যাকআপটি রিস্টোর করলে বর্তমান খামার ডেটা প্রতিস্থাপিত হবে।\n\nসুরক্ষার জন্য, রিস্টোর শুরুর আগে স্বয়ংক্রিয়ভাবে বর্তমান তথ্যের একটি নিরাপত্তা ব্যাকআপ গুগল ড্রাইভে নেওয়া হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
    // Dialog 4: Password prompt for encrypted restore
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
    // Dialog 5: Delete confirmation
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

