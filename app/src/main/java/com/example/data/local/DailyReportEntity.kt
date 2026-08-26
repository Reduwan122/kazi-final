package com.example.data.local

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class DailyReportEntity(
    val id: Long = 0,
    val date: String = "", // Format: YYYY-MM-DD
    val currentBirds: Int = 0, // বর্তমান মুরগী
    val deadBirds: Int = 0, // মৃত মুরগী
    val eggProduction: Int = 0, // ডিম উৎপাদন
    val eggSold: Int = 0, // বিক্রয় (ডিম)
    val eggPrice: Double = 0.0, // ডিমের দাম
    val totalSale: Double = 0.0, // মোট বিক্রয় (বিক্রয় × ডিমের দাম)
    val medicineCost: Double = 0.0, // ঔষধ খরচ
    val currentStock: Int = 0, // বর্তমান সমাপনী স্টক
    val otherStockIn: Int = 0, // অন্যান্য স্টক বৃদ্ধি
    val otherStockOut: Int = 0, // অন্যান্য স্টক হ্রাস / নষ্ট / ভাঙা
    val stockAdjustment: Int = 0, // স্টক সমন্বয় (Adjustment)
    val adjustmentReason: String = "", // সমন্বয়ের কারণ
    val remarks: String = "", // মন্তব্য
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
