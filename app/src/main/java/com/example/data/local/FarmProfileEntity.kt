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
    val lastSyncTime: Long = System.currentTimeMillis(),
    // প্রথম দৈনিক রেকর্ডের আগের পরিচিত closing stock।
    // উদাহরণ: 31/07/2026 এর closing stock যদি 729 হয়, তাহলে এখানে 729 বসাতে হবে।
    val initialOpeningStock: Int = 0,
    // কোন তারিখের closing stock হিসেবে initialOpeningStock সেট করা হয়েছে (YYYY-MM-DD)।
    val initialOpeningDate: String = ""
)
