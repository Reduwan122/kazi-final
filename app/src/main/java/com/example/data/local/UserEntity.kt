package com.example.data.local

import com.google.firebase.database.Exclude
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
    @Exclude
    fun isApprovedUser(): Boolean = isApproved || isAdmin()

    @Exclude
    fun isAdmin(): Boolean = role.equals("ADMIN", ignoreCase = true) || email.equals("sahariarredwan5@gmail.com", ignoreCase = true)
    
    @Exclude
    fun isManager(): Boolean = role.equals("MANAGER", ignoreCase = true)
    
    @Exclude
    fun isSupervisor(): Boolean = role.equals("SUPERVISOR", ignoreCase = true)
    
    @Exclude
    fun isWorker(): Boolean = role.equals("WORKER", ignoreCase = true)

    // Role-based capabilities with dynamic RolePermissionConfig support
    @Exclude
    fun canViewReport(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.dailyReportView
        if (isAdmin()) return true
        return true
    }

    @Exclude
    fun canAddReport(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.dailyReportAdd
        if (isAdmin()) return true
        return true
    }

    @Exclude
    fun canEditReport(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.dailyReportAdd
        if (isAdmin()) return true
        return isManager() || isSupervisor()
    }

    @Exclude
    fun canDeleteReport(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null && !isAdmin()) return false
        if (isAdmin()) return true
        return false
    }

    @Exclude
    fun canViewExpense(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.expenseView
        if (isAdmin()) return true
        return true
    }

    @Exclude
    fun canAddExpense(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.expenseAdd
        if (isAdmin()) return true
        return isManager() || isSupervisor()
    }

    @Exclude
    fun canEditExpense(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.expenseAdd
        if (isAdmin()) return true
        return isManager()
    }

    @Exclude
    fun canDeleteExpense(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.expenseDelete
        if (isAdmin()) return true
        return false
    }

    @Exclude
    fun canViewReportsAndAnalytics(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.reportAnalyticsView
        if (isAdmin()) return true
        return true
    }

    @Exclude
    fun canDownloadReports(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.reportAnalyticsDownload
        if (isAdmin()) return true
        return isManager() || isSupervisor()
    }

    @Exclude
    fun canManageUsers(permissions: RolePermissionConfig? = null): Boolean {
        if (!isApprovedUser()) return false
        if (permissions != null) return permissions.userManagementView
        if (isAdmin()) return true
        return false
    }

    @Exclude
    fun canEditFarmProfile(): Boolean = isApprovedUser() && isAdmin()

    @Exclude
    fun roleNameBengali(): String = when {
        isAdmin() -> "অ্যাডমিন (Admin)"
        isManager() -> "খামার ম্যানেজার (Manager)"
        isSupervisor() -> "সুপারভাইজার (Supervisor)"
        else -> "ডাটা এন্ট্রি অপারেটর (Worker)"
    }
}


