package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.ui.components.BanglaNumberFormatter
import com.example.ui.components.MainTopAppBar
import com.example.ui.viewmodel.PoultryViewModel
import com.example.ui.components.SnackbarController
import com.example.ui.components.rememberHaptics

@Composable
fun AdminUserManagementScreen(
    viewModel: PoultryViewModel,
    onBack: () -> Unit,
    onNavigateToRolePermissions: (String) -> Unit = {}
) {
    val haptics = rememberHaptics()
    val focusManager = LocalFocusManager.current
    val allUsers by viewModel.allUsers.collectAsState()
    val farmProfile by viewModel.farmProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Dialog States
    var showAddUserDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }

    // Refined Colors
    val primaryGreen = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorder = MaterialTheme.colorScheme.outlineVariant
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    // Base user dataset
    val displayUsers = remember(allUsers) {
        if (allUsers.isEmpty()) {
            listOf(
                UserEntity(
                    id = "sample_1",
                    username = "আহমেদ কবির",
                    phone = "01711-223344",
                    email = "ahmed.kabir@kaziagro.com",
                    role = "ADMIN",
                    isApproved = true,
                    registeredDate = 1724400000000L
                ),
                UserEntity(
                    id = "sample_2",
                    username = "রহিম মিয়া",
                    phone = "01822-556677",
                    email = "rahim.mia@kaziagro.com",
                    role = "MANAGER",
                    isApproved = true,
                    registeredDate = 1724401000000L
                ),
                UserEntity(
                    id = "sample_3",
                    username = "সাদিয়া আলম",
                    phone = "01933-889900",
                    email = "sadia.alam@kaziagro.com",
                    role = "WORKER",
                    isApproved = false,
                    registeredDate = 1724402000000L
                )
            )
        } else {
            allUsers
        }
    }

    // Filtering logic
    val filteredUsers = remember(displayUsers, searchQuery) {
        if (searchQuery.isBlank()) {
            displayUsers
        } else {
            val q = searchQuery.trim().lowercase()
            displayUsers.filter { user ->
                user.username.lowercase().contains(q) ||
                user.phone.contains(q) ||
                user.email.lowercase().contains(q) ||
                user.role.lowercase().contains(q)
            }
        }
    }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = "ইউজার তালিকা",
                isRootScreen = false,
                onBackClick = onBack,
                logoUri = farmProfile.logoUri,
                logoEmoji = farmProfile.logoEmoji
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptics.tap()
                    showAddUserDialog = true
                },
                containerColor = primaryGreen,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .size(56.dp)
                    .testTag("fab_add_user")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "নতুন ইউজার",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("admin_user_management_screen")
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // -------------------------------------------------------------
            // HEADER BAR: USER LIST BADGE & TOTAL COUNT
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "ইউজার তালিকা",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = textPrimary
                        )
                    )
                    Text(
                        text = "খামারের সকল ব্যবহারকারী ও রোল ব্যবস্থাপনা",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            color = textSecondary
                        )
                    )
                }

                // Member count chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(primaryContainerColor)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${BanglaNumberFormatter.formatNumber(displayUsers.size)} জন ইউজার",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // -------------------------------------------------------------
            // SEARCH BAR
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "নাম, মোবাইল বা রোল দিয়ে খুঁজুন...",
                                color = textSecondary.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            cursorColor = primaryGreen
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_search_user"),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // -------------------------------------------------------------
            // USER LIST
            // -------------------------------------------------------------
            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "কোনো ব্যবহারকারী পাওয়া যায়নি" else "কোনো ইউজার তালিকাভুক্ত নেই",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "নতুন ইউজার যোগ করতে নিচের বোতামে চাপুন।",
                            color = textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showAddUserDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("নতুন ইউজার যোগ করুন", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 88.dp)
                ) {
                    items(filteredUsers, key = { it.id }) { user ->
                        UserItemCardDesign(
                            user = user,
                            cardBg = cardBg,
                            cardBorder = cardBorder,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            onEditClick = { editingUser = user },
                            onDeleteClick = { userToDelete = user }
                        )
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // DIALOG: EDIT USER & ROLE
    // -------------------------------------------------------------
    if (editingUser != null) {
        val user = editingUser!!
        var editName by remember { mutableStateOf(user.username) }
        var editPhone by remember { mutableStateOf(user.phone) }
        var editRole by remember { mutableStateOf(user.role.uppercase()) }
        var editIsApproved by remember { mutableStateOf(user.isApproved) }

        AlertDialog(
            onDismissRequest = { editingUser = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg,
            title = {
                Text(
                    text = "ইউজার রোল ও স্ট্যাটাস পরিবর্তন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryGreen
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("ব্যবহারকারীর নাম") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            focusedLabelColor = primaryGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("মোবাইল নম্বর") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            focusedLabelColor = primaryGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "রোল নির্ধারণ করুন:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = textPrimary)
                    )

                    val availableRoles = listOf(
                        "ADMIN" to "অ্যাডমিন (Admin)",
                        "MANAGER" to "খামার ম্যানেজার (Manager)",
                        "SUPERVISOR" to "সুপারভাইজার (Supervisor)",
                        "WORKER" to "ডাটা এন্ট্রি অপারেটর (Worker)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableRoles.forEach { (rKey, rLabel) ->
                            val isSelected = editRole.equals(rKey, ignoreCase = true)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editRole = rKey },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) primaryContainerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, primaryGreen)
                                         else androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rLabel,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) primaryGreen else textPrimary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = primaryGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Active Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "অ্যাকাউন্ট স্ট্যাটাস",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = textPrimary
                            )
                            Text(
                                text = if (editIsApproved) "সক্রিয় (Active)" else "নিষ্ক্রিয় (Inactive)",
                                color = if (editIsApproved) primaryGreen else MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = editIsApproved,
                            onCheckedChange = { editIsApproved = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = primaryGreen
                            )
                        )
                    }

                    // Delete User option
                    if (!user.isAdmin()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        TextButton(
                            onClick = {
                                userToDelete = user
                                editingUser = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("এই অ্যাকাউন্ট মুছে ফেলুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.tap()
                        val updated = user.copy(
                            username = editName.trim(),
                            phone = editPhone.trim(),
                            role = editRole,
                            isApproved = editIsApproved
                        )
                        viewModel.adminUpdateUser(
                            user = updated,
                            onSuccess = {
                                SnackbarController.showMessage("ইউজার সফলভাবে আপডেট হয়েছে!")
                                editingUser = null
                            },
                            onError = { err ->
                                SnackbarController.showError("ত্রুটি: $err")
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingUser = null }) {
                    Text("বাতিল", color = textSecondary)
                }
            }
        )
    }

    // -------------------------------------------------------------
    // DIALOG: ADD NEW USER
    // -------------------------------------------------------------
    if (showAddUserDialog) {
        var newName by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newEmail by remember { mutableStateOf("") }
        var newRole by remember { mutableStateOf("WORKER") }
        var newIsApproved by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg,
            title = {
                Text(
                    text = "নতুন ইউজার যোগ করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryGreen
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("পুরো নাম *") },
                        placeholder = { Text("যেমন: আহমেদ কবির") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            focusedLabelColor = primaryGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("মোবাইল নম্বর *") },
                        placeholder = { Text("017XXXXXXXX") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            focusedLabelColor = primaryGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("ইমেইল (ঐচ্ছিক)") },
                        placeholder = { Text("user@kaziagro.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            focusedLabelColor = primaryGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "রোল নির্বাচন করুন:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = textPrimary)
                    )

                    val availableRoles = listOf(
                        "WORKER" to "ডাটা এন্ট্রি অপারেটর (Worker)",
                        "SUPERVISOR" to "সুপারভাইজার (Supervisor)",
                        "MANAGER" to "খামার ম্যানেজার (Manager)",
                        "ADMIN" to "অ্যাডমিন (Admin)"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        availableRoles.forEach { (rKey, rLabel) ->
                            val isSelected = newRole == rKey
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { newRole = rKey },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) primaryContainerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, primaryGreen)
                                         else androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = rLabel,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) primaryGreen else textPrimary
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = primaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.tap()
                        if (newName.isBlank()) {
                            SnackbarController.showError("অনুগ্রহ করে নাম লিখুন")
                            return@Button
                        }
                        val user = UserEntity(
                            id = "user_${System.currentTimeMillis()}",
                            username = newName.trim(),
                            phone = newPhone.trim(),
                            email = if (newEmail.isNotBlank()) newEmail.trim() else "${newPhone.trim().replace("-", "")}@kaziagro.com",
                            role = newRole,
                            isApproved = newIsApproved,
                            registeredDate = System.currentTimeMillis()
                        )
                        viewModel.adminAddUser(
                            user = user,
                            onSuccess = {
                                SnackbarController.showMessage("নতুন ইউজার সফলভাবে যুক্ত করা হয়েছে!")
                                showAddUserDialog = false
                            },
                            onError = { err ->
                                SnackbarController.showError("ত্রুটি: $err")
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("যুক্ত করুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("বাতিল", color = textSecondary)
                }
            }
        )
    }

    // -------------------------------------------------------------
    // DELETE CONFIRMATION DIALOG
    // -------------------------------------------------------------
    if (userToDelete != null) {
        val u = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            shape = RoundedCornerShape(18.dp),
            containerColor = cardBg,
            title = { Text("অ্যাকাউন্ট মুছে ফেলার নিশ্চিতকরণ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("আপনি কি নিশ্চিত যে '${u.username}' এর অ্যাকাউন্ট মুছে ফেলতে চান?", color = textPrimary) },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.confirm()
                        viewModel.deleteUser(
                            userId = u.id,
                            onSuccess = {
                                SnackbarController.showMessage("অ্যাকাউন্ট মুছে ফেলা হয়েছে")
                                userToDelete = null
                            },
                            onError = { err ->
                                SnackbarController.showError("ত্রুটি: $err")
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("মুছে ফেলুন", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("বাতিল", color = textSecondary)
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------
// USER CARD ITEM (Refined, Modern, Clean)
// -----------------------------------------------------------------------------
@Composable
fun UserItemCardDesign(
    user: UserEntity,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textSecondary: Color,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit = {}
) {
    val initialBengali = user.username.trim().take(1).ifBlank { "ইউ" }
    val isAdmin = user.isAdmin()
    val isManager = user.isManager()
    val isActive = user.isApproved

    // Avatar background and text color
    val avatarBg = when {
        isAdmin -> Color(0xFF0D631B)
        isManager -> Color(0xFF2E7D32)
        user.isSupervisor() -> Color(0xFF374151)
        else -> Color(0xFFE5E7EB)
    }
    val avatarText = when {
        isAdmin || isManager || user.isSupervisor() -> Color.White
        else -> Color(0xFF1F2937)
    }

    // Role badge colors
    val (roleBadgeBg, roleBadgeText, roleDisplayName) = when {
        isAdmin -> Triple(Color(0xFFE8F5E9), Color(0xFF0D631B), "অ্যাডমিন")
        isManager -> Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), "খামার ম্যানেজার")
        user.isSupervisor() -> Triple(Color(0xFFF3F4F6), Color(0xFF374151), "সুপারভাইজার")
        else -> Triple(Color(0xFFF3F4F6), Color(0xFF4B5563), "ডাটা এন্ট্রি অপারেটর")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .testTag("user_card_${user.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Avatar + Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular Avatar with Bengali initial
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(avatarBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialBengali,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = avatarText
                        )
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // User details
                Column {
                    Text(
                        text = user.username.ifBlank { "ব্যবহারকারী" },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = textSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Tags: Role Badge + Status Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Role pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(roleBadgeBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = roleDisplayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = roleBadgeText
                            )
                        }

                        // Status dot & text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isActive) Color(0xFF0D631B) else Color(0xFFDC2626))
                            )
                            Text(
                                text = if (isActive) "সক্রিয়" else "নিষ্ক্রিয়",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isActive) Color(0xFF0D631B) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // Right: Edit + Delete Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                // Wider than the usual 8dp so the two 38dp targets are harder to mis-tap on small phones.
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .testTag("btn_edit_user_${user.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "রোল পরিবর্তন",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete icon — hidden for Admin accounts (can't be deleted)
                if (!isAdmin) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .testTag("btn_delete_user_${user.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "ইউজার মুছে ফেলুন",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
