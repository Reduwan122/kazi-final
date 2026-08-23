package com.example.data.local

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class MonthlyExpenseEntity(
    val id: Long = 0,
    val date: String = "", // Format: YYYY-MM-DD
    val feedCost: Double = 0.0, // ফিড / খাবার
    val medicineCost: Double = 0.0, // মেডিসিন ও ভ্যাকসিন
    val staffMarket: Double = 0.0, // স্টাফ বাজার
    val staffSalary: Double = 0.0, // স্টাফ বেতন / মজুরি
    val vehicleRepair: Double = 0.0, // গাড়ি মেরামত / যন্ত্র
    val assets: Double = 0.0, // আসবাবপত্র / সম্পদ ক্রয়
    val electricityBill: Double = 0.0, // বিদ্যুৎ বিল
    val otherExpense: Double = 0.0, // অন্যান্য খরচ
    val totalExpense: Double = 0.0, // মোট ব্যয়
    val remarks: String = "", // মন্তব্য
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
