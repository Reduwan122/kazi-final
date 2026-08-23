package com.example.data.local

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class FarmProfileEntity(
    val id: Int = 1,
    val farmName: String = "কাজী এগ্রোটেক",
    val ownerName: String = "মোঃ আব্দুল্লাহ",
    val mobileNumber: String = "০১৭১২-৩৪৫৬৭৮",
    val address: String = "খামার পাড়া, গাজীপুর সদর, গাজীপুর",
    val logoUri: String = "",
    val logoEmoji: String = "🐔",
    val autoBackup: Boolean = true,
    val isDarkMode: Boolean = false,
    val lastSyncTime: Long = System.currentTimeMillis()
)

