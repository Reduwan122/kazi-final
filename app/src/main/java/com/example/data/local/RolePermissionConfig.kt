package com.example.data.local

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class RolePermissionConfig(
    val roleKey: String = "MANAGER", // ADMIN, MANAGER, SUPERVISOR, WORKER
    val roleDisplayName: String = "খামার ম্যানেজার",

    // দৈনিক রিপোর্ট
    val dailyReportView: Boolean = true,
    val dailyReportAdd: Boolean = true,

    // ইউজার ম্যানেজমেন্ট
    val userManagementView: Boolean = false,

    // খরচ ম্যানেজমেন্ট
    val expenseView: Boolean = true,
    val expenseAdd: Boolean = true,
    val expenseDelete: Boolean = false,

    // রিপোর্ট ও অ্যানালিটিক্স
    val reportAnalyticsView: Boolean = true,
    val reportAnalyticsDownload: Boolean = true
) {
    companion object {
        fun getDefaultPermissionsForRole(role: String): RolePermissionConfig {
            return when (role.uppercase()) {
                "ADMIN" -> RolePermissionConfig(
                    roleKey = "ADMIN",
                    roleDisplayName = "এডমিন (Admin)",
                    dailyReportView = true,
                    dailyReportAdd = true,
                    userManagementView = true,
                    expenseView = true,
                    expenseAdd = true,
                    expenseDelete = true,
                    reportAnalyticsView = true,
                    reportAnalyticsDownload = true
                )
                "MANAGER" -> RolePermissionConfig(
                    roleKey = "MANAGER",
                    roleDisplayName = "খামার ম্যানেজার",
                    dailyReportView = true,
                    dailyReportAdd = true,
                    userManagementView = false,
                    expenseView = true,
                    expenseAdd = true,
                    expenseDelete = false,
                    reportAnalyticsView = true,
                    reportAnalyticsDownload = true
                )
                "SUPERVISOR" -> RolePermissionConfig(
                    roleKey = "SUPERVISOR",
                    roleDisplayName = "সুপারভাইজার",
                    dailyReportView = true,
                    dailyReportAdd = true,
                    userManagementView = false,
                    expenseView = true,
                    expenseAdd = true,
                    expenseDelete = false,
                    reportAnalyticsView = true,
                    reportAnalyticsDownload = false
                )
                "WORKER" -> RolePermissionConfig(
                    roleKey = "WORKER",
                    roleDisplayName = "কর্মী (Worker)",
                    dailyReportView = true,
                    dailyReportAdd = true,
                    userManagementView = false,
                    expenseView = true,
                    expenseAdd = false,
                    expenseDelete = false,
                    reportAnalyticsView = false,
                    reportAnalyticsDownload = false
                )
                else -> RolePermissionConfig(
                    roleKey = role,
                    roleDisplayName = role
                )
            }
        }

        fun getAllRoles(): List<Pair<String, String>> = listOf(
            "MANAGER" to "খামার ম্যানেজার",
            "SUPERVISOR" to "সুপারভাইজার",
            "WORKER" to "কর্মী (Worker)",
            "ADMIN" to "এডমিন (Admin)"
        )
    }
}
