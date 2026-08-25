package com.example.data.local

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

enum class UserRole {
    ADMIN,
    MANAGER,
    SUPERVISOR,
    WORKER;

    fun getBanglaName(): String = when (this) {
        ADMIN -> "এডমিন (Admin)"
        MANAGER -> "ম্যানেজার (Manager)"
        SUPERVISOR -> "সুপারভাইজার (Supervisor)"
        WORKER -> "কর্মী (Worker)"
    }
}

@IgnoreExtraProperties
data class UserEntity(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUri: String = "",
    val role: String = "WORKER", // ADMIN, MANAGER, SUPERVISOR, WORKER
    @get:PropertyName("isApproved")
    @set:PropertyName("isApproved")
    var isApproved: Boolean = false,
    val registeredDate: Long = System.currentTimeMillis(),
    val passwordHash: String = "",
    val rememberLogin: Boolean = true,
    val isLoggedIn: Boolean = true
) {
    // True if user is approved by admin, or is the root Admin
    fun isApprovedUser(): Boolean = isApproved || isAdmin()

    fun isAdmin(): Boolean = role.equals("ADMIN", ignoreCase = true) || email.equals("sahariarredwan5@gmail.com", ignoreCase = true)
    fun isManager(): Boolean = role.equals("MANAGER", ignoreCase = true)
    fun isSupervisor(): Boolean = role.equals("SUPERVISOR", ignoreCase = true)
    fun isWorker(): Boolean = role.equals("WORKER", ignoreCase = true)

    // Role-based capabilities with dynamic RolePermissionConfig support
    fun canViewReport(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.dailyReportView
        if (isAdmin()) return true
        return true
    }

    fun canAddReport(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.dailyReportAdd
        if (isAdmin()) return true
        return true
    }

    fun canEditReport(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.dailyReportAdd
        if (isAdmin()) return true
        return isManager() || isSupervisor()
    }

    fun canDeleteReport(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null && !isAdmin()) return false
        if (isAdmin()) return true
        return false
    }

    fun canViewExpense(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.expenseView
        if (isAdmin()) return true
        return true
    }

    fun canAddExpense(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.expenseAdd
        if (isAdmin()) return true
        return isManager() || isSupervisor()
    }

    fun canEditExpense(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.expenseAdd
        if (isAdmin()) return true
        return isManager()
    }

    fun canDeleteExpense(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.expenseDelete
        if (isAdmin()) return true
        return false
    }

    fun canViewReportsAndAnalytics(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.reportAnalyticsView
        if (isAdmin()) return true
        return true
    }

    fun canDownloadReports(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.reportAnalyticsDownload
        if (isAdmin()) return true
        return isManager() || isSupervisor()
    }

    fun canManageUsers(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.userManagementView
        if (isAdmin()) return true
        return false
    }

    fun canEditFarmProfile(): Boolean = isApprovedUser() && isAdmin()

    fun roleNameBengali(): String = when {
        isAdmin() -> "অ্যাডমিন (Admin)"
        isManager() -> "খামার ম্যানেজার (Manager)"
        isSupervisor() -> "সুপারভাইজার (Supervisor)"
        else -> "ডাটা এন্ট্রি অপারেটর (Worker)"
    }
}


