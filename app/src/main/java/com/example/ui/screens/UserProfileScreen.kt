package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MainTopAppBar
import com.example.ui.components.SnackbarController
import com.example.ui.components.UserProfileAvatar
import com.example.ui.components.rememberHaptics
import com.example.ui.viewmodel.PoultryViewModel

@Composable
fun UserProfileScreen(
    viewModel: PoultryViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val currentUser by viewModel.currentUser.collectAsState()
    val rolePermissionsMap by viewModel.rolePermissions.collectAsState()
    val userPerms = currentUser?.let { rolePermissionsMap[it.role.uppercase()] }

    var name by remember(currentUser) { mutableStateOf(currentUser?.username ?: "") }
    var phone by remember(currentUser) { mutableStateOf(currentUser?.phone ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var isUploadingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingPhoto = true
            viewModel.uploadUserProfileImageFromUri(
                context = context,
                imageUri = uri,
                onSuccess = {
                    isUploadingPhoto = false
                    SnackbarController.showMessage("প্রোফাইল ছবি সফলভাবে আপডেট হয়েছে!")
                },
                onError = { err ->
                    isUploadingPhoto = false
                    SnackbarController.showError("ছবি আপলোড ব্যর্থ: $err")
                }
            )
        }
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "ব্যবহারকারী প্রোফাইল",
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("user_profile_screen"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ══════════════════════════════════════════════════════════
            // 1. PROFESSIONAL PROFILE HEADER CARD
            // ══════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Gradient Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF0D631B),
                                        Color(0xFF1B8A2B),
                                        Color(0xFF2E7D32)
                                    )
                                )
                            )
                    )

                    // Profile Details Content Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar overlapping banner
                        Box(
                            modifier = Modifier
                                .offset(y = (-44).dp)
                                .size(96.dp)
                        ) {
                            // Avatar container with white elevated border ring
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { photoPickerLauncher.launch("image/*") }
                                    .testTag("btn_change_profile_photo")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(3.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    UserProfileAvatar(
                                        profileImageUri = currentUser?.profileImageUri ?: "",
                                        username = currentUser?.username ?: "",
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (isUploadingPhoto) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color.White,
                                                modifier = Modifier.size(28.dp),
                                                strokeWidth = 3.dp
                                            )
                                        }
                                    }
                                }
                            }

                            // Camera overlay badge on avatar
                            if (!isUploadingPhoto) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF0D631B),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(30.dp)
                                        .clickable { photoPickerLauncher.launch("image/*") }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Change Photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Adjust for negative offset
                        Spacer(modifier = Modifier.height((-32).dp))

                        // User Name
                        Text(
                            text = currentUser?.username ?: "ব্যবহারকারী",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        // Email
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = currentUser?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Role Badge & Status Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val role = currentUser?.role?.uppercase() ?: "WORKER"
                            val (roleIcon, roleBg, roleTextColor) = when (role) {
                                "ADMIN" -> Triple(Icons.Default.AdminPanelSettings, Color(0xFFE8F5E9), Color(0xFF1B5E20))
                                "MANAGER" -> Triple(Icons.Default.Badge, Color(0xFFE0F2F1), Color(0xFF00695C))
                                "SUPERVISOR" -> Triple(Icons.Default.AssignmentInd, Color(0xFFFFF3E0), Color(0xFFE65100))
                                else -> Triple(Icons.Default.Engineering, Color(0xFFF3E5F5), Color(0xFF6A1B9A))
                            }

                            // Role Chip
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = roleBg
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = roleIcon,
                                        contentDescription = null,
                                        tint = roleTextColor,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = currentUser?.roleNameBengali() ?: "কর্মী",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = roleTextColor
                                    )
                                }
                            }

                            // Active Status Chip
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFE8F5E9)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "অনুমোদিত",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Photo Upload / Change / Remove Actions Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(36.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if ((currentUser?.profileImageUri ?: "").isNotBlank()) "ছবি পরিবর্তন করুন" else "ছবি আপলোড করুন",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if ((currentUser?.profileImageUri ?: "").isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.removeUserProfileImage {
                                            SnackbarController.showMessage("প্রোফাইল ছবি সরানো হয়েছে")
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(36.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "মুছুন",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════
            // 2. PERSONAL INFORMATION EDIT CARD
            // ══════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "ব্যক্তিগত তথ্য সম্পাদনা",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("আপনার পুরো নাম") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("মোবাইল নম্বর") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currentUser?.email ?: "",
                        onValueChange = {},
                        enabled = false,
                        label = { Text("লগইন ইমেইল (পরিবর্তনযোগ্য নয়)") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            haptics.tap()
                            isSaving = true
                            viewModel.updateCurrentUserProfile(
                                name = name,
                                phone = phone,
                                onSuccess = {
                                    isSaving = false
                                    SnackbarController.showMessage("প্রোফাইল তথ্য সফলভাবে সংরক্ষণ করা হয়েছে!")
                                },
                                onError = { err ->
                                    isSaving = false
                                    SnackbarController.showError("তথ্য সংরক্ষণ ব্যর্থ: $err")
                                }
                            )
                        },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D631B))
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("সংরক্ষণ হচ্ছে...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("তথ্য সংরক্ষণ করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // ══════════════════════════════════════════════════════════
            // 3. ROLE-BASED ACCESS PERMISSIONS CARD
            // ══════════════════════════════════════════════════════════
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "আপনার এক্সেস সুবিধাসমূহ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "রোল অনুযায়ী আপনার নির্ধারিত অনুমতিসমূহ",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Live dynamic permissions evaluated based on rolePermissionsMap
                    PermissionRow(
                        icon = Icons.Default.Visibility,
                        title = "দৈনিক রিপোর্ট রেজিস্টার প্রদর্শন",
                        allowed = currentUser?.canViewReport(userPerms) == true
                    )
                    PermissionRow(
                        icon = Icons.Default.Edit,
                        title = "দৈনিক রিপোর্ট তৈরি ও ডাটা এন্ট্রি",
                        allowed = currentUser?.canAddReport(userPerms) == true
                    )
                    PermissionRow(
                        icon = Icons.Default.Edit,
                        title = "দৈনিক রিপোর্ট এডিট ও আপডেট",
                        allowed = currentUser?.canEditReport(userPerms) == true
                    )
                    PermissionRow(
                        icon = Icons.Default.Paid,
                        title = "মাসিক ব্যয় রেজিস্টার প্রদর্শন",
                        allowed = currentUser?.canViewExpense(userPerms) == true
                    )
                    PermissionRow(
                        icon = Icons.Default.Paid,
                        title = "মাসিক ব্যয় এন্ট্রি ও সংরক্ষণ",
                        allowed = currentUser?.canAddExpense(userPerms) == true
                    )
                    PermissionRow(
                        icon = Icons.Default.Assessment,
                        title = "রিপোর্ট ও অ্যানালিটিক্স প্রদর্শন",
                        allowed = currentUser?.canViewReportsAndAnalytics(userPerms) == true
                    )
                    PermissionRow(
                        icon = Icons.Default.CloudDownload,
                        title = "পিডিএফ ও এক্সেল রিপোর্ট ডাউনলোড",
                        allowed = currentUser?.canDownloadReports(userPerms) == true
                    )
                    PermissionRow(
                        icon = Icons.Default.Store,
                        title = "খামার প্রোফাইল ও লোগো পরিবর্তন",
                        allowed = currentUser?.canEditFarmProfile() == true
                    )
                    PermissionRow(
                        icon = Icons.Default.SupervisorAccount,
                        title = "ব্যবহারকারী রেজিস্ট্রেশন অনুমোদন ও রোল নিয়ন্ত্রণ",
                        allowed = currentUser?.canManageUsers(userPerms) == true || currentUser?.isAdmin() == true
                    )
                }
            }

            // ══════════════════════════════════════════════════════════
            // 4. LOGOUT SECTION
            // ══════════════════════════════════════════════════════════
            OutlinedButton(
                onClick = {
                    viewModel.logout {
                        onLogout()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("profile_logout_button"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("অ্যাকাউন্ট লগআউট করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    allowed: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (allowed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = if (allowed) FontWeight.Medium else FontWeight.Normal,
                    color = if (allowed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            )
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (allowed) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = if (allowed) Icons.Default.CheckCircle else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (allowed) Color(0xFF2E7D32) else Color(0xFF9E9E9E),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = if (allowed) "সক্রিয়" else "অনুমতি নেই",
                    color = if (allowed) Color(0xFF1B5E20) else Color(0xFF757575),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
