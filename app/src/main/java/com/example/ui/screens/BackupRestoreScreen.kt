package com.example.ui.screens

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.BackupProgressState
import com.example.data.backup.DriveFileInfo
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.SnackbarController
import com.example.ui.components.rememberHaptics
import com.example.ui.viewmodel.PoultryViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun BackupRestoreScreen(
    viewModel: PoultryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val TAG = "BackupRestoreScreen"

    val googleAccountEmail by viewModel.googleAccountEmail.collectAsState()
    val isConnected = googleAccountEmail != null
    val progressState by viewModel.backupProgressState.collectAsState()
    val driveBackups by viewModel.driveBackupsList.collectAsState()
    val isAutoBackupEnabled by viewModel.isAutoBackupEnabled.collectAsState()
    val autoBackupFrequency by viewModel.autoBackupFrequency.collectAsState()

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var selectedBackupForRestore by remember { mutableStateOf<DriveFileInfo?>(null) }
    var showPasswordModalForRestore by remember { mutableStateOf(false) }
    var restorePassword by remember { mutableStateOf("") }
    var deleteConfirmBackup by remember { mutableStateOf<DriveFileInfo?>(null) }

    // Google Sign-In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        Log.d(TAG, "Sign-in resultCode=${result.resultCode} dataNull=${data == null}")
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Sign-in SUCCESS email=${account?.email}")
                viewModel.refreshGoogleAccountStatus()
                SnackbarController.showMessage("à¦—à§à¦—à¦² à¦¡à§à¦°à¦¾à¦‡à¦­ à¦¸à¦‚à¦¯à§à¦•à§à¦¤ à¦¹à¦¯à¦¼à§‡à¦›à§‡: ${account?.email ?: ""}")
            } catch (e: ApiException) {
                Log.e(TAG, "Sign-in FAILED statusCode=${e.statusCode} message=${e.message}")
                val errMsg = when (e.statusCode) {
                    10 -> "à¦¡à§‡à¦­à§‡à¦²à¦ªà¦¾à¦° à¦•à¦¨à¦«à¦¿à¦— à¦¤à§à¦°à§à¦Ÿà¦¿ (Error 10) â€” SHA-1 à¦®à¦¿à¦²à¦›à§‡ à¦¨à¦¾"
                    12500 -> "Google Play Services à¦¸à¦¾à¦‡à¦¨-à¦‡à¦¨ à¦¬à¦¾à¦¤à¦¿à¦² à¦•à¦°à§‡à¦›à§‡"
                    12501 -> "à¦¬à§à¦¯à¦¬à¦¹à¦¾à¦°à¦•à¦¾à¦°à§€ à¦¸à¦¾à¦‡à¦¨-à¦‡à¦¨ à¦¬à¦¾à¦¤à¦¿à¦² à¦•à¦°à§‡à¦›à§‡à¦¨"
                    12502 -> "à¦¸à¦¾à¦‡à¦¨-à¦‡à¦¨ à¦¬à¦°à§à¦¤à¦®à¦¾à¦¨à§‡ à¦šà¦²à¦›à§‡, à¦…à¦ªà§‡à¦•à§à¦·à¦¾ à¦•à¦°à§à¦¨"
                    7 -> "à¦¨à§‡à¦Ÿà¦“à¦¯à¦¼à¦¾à¦°à§à¦• à¦¸à¦‚à¦¯à§‹à¦— à¦¨à§‡à¦‡"
                    else -> "à¦¸à¦¾à¦‡à¦¨-à¦‡à¦¨ à¦¬à§à¦¯à¦°à§à¦¥ à¦¹à¦¯à¦¼à§‡à¦›à§‡ (à¦•à§‹à¦¡: ${e.statusCode})"
                }
                SnackbarController.showError(errMsg)
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in EXCEPTION ${e.message}")
                // Check if already signed in
                val last = GoogleSignIn.getLastSignedInAccount(context)
                if (last != null) {
                    viewModel.refreshGoogleAccountStatus()
                    SnackbarController.showMessage("à¦¸à¦‚à¦¯à§à¦•à§à¦¤: ${last.email}")
                } else {
                    SnackbarController.showError("à¦¸à¦¾à¦‡à¦¨-à¦‡à¦¨ à¦¬à§à¦¯à¦°à§à¦¥: ${e.message}")
                }
            }
        } else {
            val last = GoogleSignIn.getLastSignedInAccount(context)
            if (last != null) {
                viewModel.refreshGoogleAccountStatus()
                SnackbarController.showMessage("à¦¸à¦‚à¦¯à§à¦•à§à¦¤: ${last.email}")
            } else {
                Log.w(TAG, "Sign-in returned null data")
                SnackbarController.showError("à¦¸à¦¾à¦‡à¦¨-à¦‡à¦¨ à¦¬à¦¾à¦¤à¦¿à¦² à¦¹à¦¯à¦¼à§‡à¦›à§‡")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshGoogleAccountStatus()
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "à¦—à§à¦—à¦² à¦¡à§à¦°à¦¾à¦‡à¦­ à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª",
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

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 1. Google Drive Connection Card
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isConnected) Color(0xFFE8F5E9)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "à¦—à§à¦—à¦² à¦¡à§à¦°à¦¾à¦‡à¦­",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (isConnected) "à¦¸à¦‚à¦¯à§à¦•à§à¦¤" else "à¦¸à¦‚à¦¯à§à¦•à§à¦¤ à¦¨à¦¯à¦¼",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Account info (if connected)
                    if (isConnected) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE8F5E9).copy(alpha = 0.6f))
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = googleAccountEmail ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF1B5E20)
                                )
                            }
                        }

                        // Action buttons when connected
                        Button(
                            onClick = {
                                haptics.tap()
                                viewModel.performManualBackup()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D631B)),
                            enabled = progressState !is BackupProgressState.Uploading &&
                                      progressState !is BackupProgressState.Connecting &&
                                      progressState !is BackupProgressState.Preparing
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("à¦à¦–à¦¨à¦‡ à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª à¦•à¦°à§à¦¨", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                haptics.tap()
                                viewModel.disconnectGoogleAccount()
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("à¦¡à¦¿à¦¸à¦•à¦¾à¦¨à§‡à¦•à§à¦Ÿ à¦•à¦°à§à¦¨", fontWeight = FontWeight.SemiBold)
                        }

                    } else {
                        // Connect button when not connected
                        Button(
                            onClick = {
                                haptics.tap()
                                val client = viewModel.driveBackupManager.getGoogleSignInClient()
                                // Sign out first to show account picker
                                client.signOut().addOnCompleteListener {
                                    googleSignInLauncher.launch(client.signInIntent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("à¦—à§à¦—à¦² à¦…à§à¦¯à¦¾à¦•à¦¾à¦‰à¦¨à§à¦Ÿ à¦¦à¦¿à¦¯à¦¼à§‡ à¦•à¦¾à¦¨à§‡à¦•à§à¦Ÿ à¦•à¦°à§à¦¨", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 2. Progress Banner
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
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
                        else -> "à¦ªà§à¦°à¦•à§à¦°à¦¿à¦¯à¦¼à¦¾à¦§à§€à¦¨..."
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

            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
            // 3. Auto Backup Settings (only when connected)
            // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "à¦¸à§à¦¬à¦¯à¦¼à¦‚à¦•à§à¦°à¦¿à¦¯à¦¼ à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "à¦¬à§à¦¯à¦¾à¦•à¦—à§à¦°à¦¾à¦‰à¦¨à§à¦¡à§‡ à¦•à§à¦²à¦¾à¦‰à¦¡à§‡ à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª à¦¹à¦¬à§‡",
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
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                            )
                        }

                        if (isAutoBackupEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Text(
                                text = "à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª à¦«à§à¦°à¦¿à¦•à§‹à¦¯à¦¼à§‡à¦¨à§à¦¸à¦¿:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { viewModel.updateAutoBackupSettings(true, "DAILY") }
                                ) {
                                    RadioButton(
                                        selected = autoBackupFrequency == "DAILY",
                                        onClick = { viewModel.updateAutoBackupSettings(true, "DAILY") }
                                    )
                                    Text("à¦ªà§à¦°à¦¤à¦¿à¦¦à¦¿à¦¨", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { viewModel.updateAutoBackupSettings(true, "WEEKLY") }
                                ) {
                                    RadioButton(
                                        selected = autoBackupFrequency == "WEEKLY",
                                        onClick = { viewModel.updateAutoBackupSettings(true, "WEEKLY") }
                                    )
                                    Text("à¦¸à¦¾à¦ªà§à¦¤à¦¾à¦¹à¦¿à¦•", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
                // 4. Drive Backup History
                // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
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
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "à¦•à§à¦²à¦¾à¦‰à¦¡ à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª à¦¹à¦¿à¦¸à§à¦Ÿà§à¦°à¦¿",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            IconButton(onClick = {
                                haptics.tap()
                                viewModel.fetchDriveBackupsList()
                            }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
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
                                    text = "à¦¡à§à¦°à¦¾à¦‡à¦­à§‡ à¦•à§‹à¦¨à§‹ à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª à¦¨à§‡à¦‡",
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
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // â”€â”€ Restore Confirmation Dialog â”€â”€
    if (showRestoreConfirmDialog && selectedBackupForRestore != null) {
        val targetFile = selectedBackupForRestore!!
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("à¦¡à§‡à¦Ÿà¦¾ à¦°à¦¿à¦¸à§à¦Ÿà§‹à¦° à¦•à¦°à¦¬à§‡à¦¨?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("'${targetFile.name}' à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª à¦¥à§‡à¦•à§‡ à¦¡à§‡à¦Ÿà¦¾ à¦°à¦¿à¦¸à§à¦Ÿà§‹à¦° à¦¹à¦¬à§‡à¥¤")
                    Text(
                        text = "âš ï¸ à¦¬à¦°à§à¦¤à¦®à¦¾à¦¨ à¦²à§‹à¦•à¦¾à¦² à¦¡à§‡à¦Ÿà¦¾ à¦ªà§à¦°à¦¤à¦¿à¦¸à§à¦¥à¦¾à¦ªà¦¿à¦¤ à¦¹à¦¬à§‡à¥¤",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
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
                    }
                ) { Text("à¦°à¦¿à¦¸à§à¦Ÿà§‹à¦° à¦•à¦°à§à¦¨") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) { Text("à¦¬à¦¾à¦¤à¦¿à¦²") }
            }
        )
    }

    // â”€â”€ Password Dialog for Encrypted Restore â”€â”€
    if (showPasswordModalForRestore && selectedBackupForRestore != null) {
        val targetFile = selectedBackupForRestore!!
        AlertDialog(
            onDismissRequest = { showPasswordModalForRestore = false },
            title = { Text("à¦ªà¦¾à¦¸à¦“à¦¯à¦¼à¦¾à¦°à§à¦¡ à¦²à¦¿à¦–à§à¦¨") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "à¦¬à§à¦¯à¦¾à¦•à¦†à¦ªà¦Ÿà¦¿ à¦ªà¦¾à¦¸à¦“à¦¯à¦¼à¦¾à¦°à§à¦¡ à¦¦à¦¿à¦¯à¦¼à§‡ à¦¸à§à¦°à¦•à§à¦·à¦¿à¦¤à¥¤ à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª à¦¤à§ˆà¦°à¦¿à¦° à¦¸à¦®à¦¯à¦¼à§‡à¦° à¦ªà¦¾à¦¸à¦“à¦¯à¦¼à¦¾à¦°à§à¦¡ à¦²à¦¿à¦–à§à¦¨à¥¤",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text("à¦ªà¦¾à¦¸à¦“à¦¯à¦¼à¦¾à¦°à§à¦¡") },
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
                            SnackbarController.showError("à¦ªà¦¾à¦¸à¦“à¦¯à¦¼à¦¾à¦°à§à¦¡ à¦²à¦¿à¦–à§à¦¨")
                            return@Button
                        }
                        showPasswordModalForRestore = false
                        viewModel.performRestore(driveFile = targetFile, password = restorePassword)
                    }
                ) { Text("à¦°à¦¿à¦¸à§à¦Ÿà§‹à¦° à¦•à¦°à§à¦¨") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordModalForRestore = false }) { Text("à¦¬à¦¾à¦¤à¦¿à¦²") }
            }
        )
    }

    // â”€â”€ Delete Confirmation Dialog â”€â”€
    if (deleteConfirmBackup != null) {
        val delFile = deleteConfirmBackup!!
        AlertDialog(
            onDismissRequest = { deleteConfirmBackup = null },
            title = { Text("à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª à¦¡à¦¿à¦²à¦¿à¦Ÿ à¦•à¦°à¦¬à§‡à¦¨?") },
            text = { Text("à¦—à§à¦—à¦² à¦¡à§à¦°à¦¾à¦‡à¦­ à¦¥à§‡à¦•à§‡ '${delFile.name}' à¦¸à§à¦¥à¦¾à¦¯à¦¼à§€à¦­à¦¾à¦¬à§‡ à¦¡à¦¿à¦²à¦¿à¦Ÿ à¦¹à¦¬à§‡à¥¤") },
            confirmButton = {
                Button(
                    onClick = {
                        val file = deleteConfirmBackup ?: return@Button
                        deleteConfirmBackup = null
                        viewModel.deleteDriveBackup(file)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("à¦¡à¦¿à¦²à¦¿à¦Ÿ à¦•à¦°à§à¦¨") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmBackup = null }) { Text("à¦¬à¦¾à¦¤à¦¿à¦²") }
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
                    tint = if (file.isPreRestoreBackup) MaterialTheme.colorScheme.onSecondaryContainer
                           else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = if (file.isPreRestoreBackup) "à¦¸à§à¦°à¦•à§à¦·à¦¾ à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª" else "à¦•à§à¦²à¦¾à¦‰à¦¡ à¦¬à§à¦¯à¦¾à¦•à¦†à¦ª",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${file.formattedDate} â€¢ ${file.formattedTime} (${file.formattedSize})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRestoreClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Restore,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
