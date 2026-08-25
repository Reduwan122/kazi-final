package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.DailyReportEntity
import com.example.data.local.MonthlyExpenseEntity
import com.example.ui.components.AppBottomNavBar
import com.example.ui.components.AppSnackbarHost
import com.example.ui.components.BottomNavTab
import com.example.ui.components.FarmNotificationDialog
import com.example.ui.components.PdfPreviewModalDialog
import com.example.ui.components.SnackbarBottomInset
import com.example.ui.screens.AddEditDailyReportScreen
import com.example.ui.screens.AddEditMonthlyExpenseScreen
import com.example.ui.screens.AdminUserManagementScreen
import com.example.ui.screens.DailyReportDetailScreen
import com.example.ui.screens.DailyReportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MonthlyExpenseDetailScreen
import com.example.ui.screens.MonthlyExpenseScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.RolePermissionEditorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.theme.KaziAgrotechTheme
import com.example.ui.viewmodel.PoultryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PoultryViewModel = viewModel()
            val farmProfile by viewModel.farmProfile.collectAsState()

            KaziAgrotechTheme(darkTheme = farmProfile.isDarkMode) {
                // One snackbar host for the whole app: several screens (login, splash) are not
                // built on a Scaffold, and messages also come from the ViewModel and dialogs.
                Box(modifier = Modifier.fillMaxSize()) {
                    MainAppNavigation(viewModel = viewModel)

                    AppSnackbarHost(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .testTag("app_snackbar_host")
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation(viewModel: PoultryViewModel) {
    val navController = rememberNavController()
    val farmProfile by viewModel.farmProfile.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                logoUri = farmProfile.logoUri,
                logoEmoji = farmProfile.logoEmoji,
                onFinished = {
                    val destination = if (viewModel.isUserLoggedInAndApproved()) "main" else "login"
                    navController.navigate(destination) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainContainerScreen(
                viewModel = viewModel,
                onNavigateToAddDailyReport = { navController.navigate("add_edit_daily_report/0") },
                onNavigateToEditDailyReport = { id -> navController.navigate("add_edit_daily_report/$id") },
                onNavigateToDailyReportDetail = { id -> navController.navigate("daily_report_detail/$id") },
                onNavigateToAddExpense = { navController.navigate("add_edit_expense/0") },
                onNavigateToEditExpense = { id -> navController.navigate("add_edit_expense/$id") },
                onNavigateToExpenseDetail = { id -> navController.navigate("expense_detail/$id") },
                onNavigateToAdmin = { navController.navigate("admin_management") },
                onNavigateToRolePermissions = { role -> navController.navigate("role_permissions/$role") },
                onNavigateToProfile = { navController.navigate("user_profile") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "add_edit_daily_report/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getLong("reportId") ?: 0L
            AddEditDailyReportScreen(
                reportId = reportId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "daily_report_detail/{reportId}",
            arguments = listOf(navArgument("reportId") { type = NavType.LongType })
        ) { backStackEntry ->
            val reportId = backStackEntry.arguments?.getLong("reportId") ?: 0L
            var pdfReportToPreview by remember { mutableStateOf<DailyReportEntity?>(null) }
            val farmProfile by viewModel.farmProfile.collectAsState()

            DailyReportDetailScreen(
                reportId = reportId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("add_edit_daily_report/$id") },
                onPdfPreview = { report -> pdfReportToPreview = report }
            )

            if (pdfReportToPreview != null) {
                PdfPreviewModalDialog(
                    title = "দৈনিক প্রতিবেদন (${pdfReportToPreview!!.date})",
                    farmProfile = farmProfile,
                    dailyReports = listOf(pdfReportToPreview!!),
                    onDismiss = { pdfReportToPreview = null }
                )
            }
        }

        composable(
            route = "add_edit_expense/{expenseId}",
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
            AddEditMonthlyExpenseScreen(
                expenseId = expenseId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "expense_detail/{expenseId}",
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
            var pdfExpenseToPreview by remember { mutableStateOf<MonthlyExpenseEntity?>(null) }
            val farmProfile by viewModel.farmProfile.collectAsState()

            MonthlyExpenseDetailScreen(
                expenseId = expenseId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("add_edit_expense/$id") },
                onPdfPreview = { expense -> pdfExpenseToPreview = expense }
            )

            if (pdfExpenseToPreview != null) {
                PdfPreviewModalDialog(
                    title = "মাসিক ব্যয় (${pdfExpenseToPreview!!.date})",
                    farmProfile = farmProfile,
                    expenses = listOf(pdfExpenseToPreview!!),
                    onDismiss = { pdfExpenseToPreview = null }
                )
            }
        }

        composable("admin_management") {
            AdminUserManagementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToRolePermissions = { role ->
                    navController.navigate("role_permissions/$role")
                }
            )
        }

        composable(
            route = "role_permissions/{roleKey}",
            arguments = listOf(navArgument("roleKey") { type = NavType.StringType; defaultValue = "MANAGER" })
        ) { backStackEntry ->
            val roleKey = backStackEntry.arguments?.getString("roleKey") ?: "MANAGER"
            RolePermissionEditorScreen(
                viewModel = viewModel,
                initialRole = roleKey,
                onBack = { navController.popBackStack() }
            )
        }

        composable("role_permissions") {
            RolePermissionEditorScreen(
                viewModel = viewModel,
                initialRole = "MANAGER",
                onBack = { navController.popBackStack() }
            )
        }

        composable("user_profile") {
            UserProfileScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}

@Composable
fun MainContainerScreen(
    viewModel: PoultryViewModel,
    onNavigateToAddDailyReport: () -> Unit,
    onNavigateToEditDailyReport: (Long) -> Unit,
    onNavigateToDailyReportDetail: (Long) -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    onNavigateToExpenseDetail: (Long) -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToRolePermissions: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(BottomNavTab.DASHBOARD) }
    var showFarmNotifications by remember { mutableStateOf(false) }
    var pdfPreviewDailyReports by remember { mutableStateOf<List<DailyReportEntity>?>(null) }
    var pdfPreviewExpenses by remember { mutableStateOf<List<MonthlyExpenseEntity>?>(null) }
    val farmProfile by viewModel.farmProfile.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    androidx.compose.runtime.LaunchedEffect(currentUser) {
        if (currentUser != null && !currentUser!!.isApprovedUser()) {
            onLogout()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AppBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        // Keep the app-wide snackbar above the bottom navigation bar while this tabbed screen is shown.
        SnackbarBottomInset(innerPadding.calculateBottomPadding())

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (currentTab) {
                BottomNavTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToAddReport = onNavigateToAddDailyReport,
                    onNavigateToAddExpense = onNavigateToAddExpense,
                    onNavigateToReports = { currentTab = BottomNavTab.REPORTS },
                    onNavigateToDailyReport = { currentTab = BottomNavTab.DAILY_REPORT },
                    onNavigateToExpense = { currentTab = BottomNavTab.EXPENSE },
                    onOpenNotifications = { showFarmNotifications = true },
                    onNavigateToProfile = onNavigateToProfile
                )

                BottomNavTab.DAILY_REPORT -> DailyReportScreen(
                    viewModel = viewModel,
                    onNavigateToAddReport = onNavigateToAddDailyReport,
                    onNavigateToEditReport = onNavigateToEditDailyReport,
                    onNavigateToDetail = onNavigateToDailyReportDetail,
                    onPreviewPdf = { list -> pdfPreviewDailyReports = list },
                    onOpenNotifications = { showFarmNotifications = true },
                    onNavigateToProfile = onNavigateToProfile
                )

                BottomNavTab.EXPENSE -> MonthlyExpenseScreen(
                    viewModel = viewModel,
                    onNavigateToAddExpense = onNavigateToAddExpense,
                    onNavigateToEditExpense = onNavigateToEditExpense,
                    onNavigateToDetail = onNavigateToExpenseDetail,
                    onPreviewExpensePdf = { list -> pdfPreviewExpenses = list },
                    onOpenNotifications = { showFarmNotifications = true },
                    onNavigateToProfile = onNavigateToProfile
                )

                BottomNavTab.REPORTS -> ReportsScreen(
                    viewModel = viewModel,
                    onOpenNotifications = { showFarmNotifications = true },
                    onNavigateToProfile = onNavigateToProfile
                )

                BottomNavTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToAdmin = onNavigateToAdmin,
                    onNavigateToRolePermissions = onNavigateToRolePermissions,
                    onNavigateToProfile = onNavigateToProfile,
                    onOpenNotifications = { showFarmNotifications = true },
                    onLogout = onLogout
                )
            }

            if (showFarmNotifications) {
                FarmNotificationDialog(
                    viewModel = viewModel,
                    onDismiss = {
                        showFarmNotifications = false
                        viewModel.markNotificationsRead()
                    },
                    onNavigateToAddDailyReport = {
                        viewModel.markNotificationsRead()
                        onNavigateToAddDailyReport()
                    },
                    onNavigateToDailyReportList = {
                        viewModel.markNotificationsRead()
                        currentTab = BottomNavTab.DAILY_REPORT
                    }
                )
            }

            if (pdfPreviewDailyReports != null) {
                PdfPreviewModalDialog(
                    title = "দৈনিক প্রতিবেদন রেজিস্টার",
                    farmProfile = farmProfile,
                    dailyReports = pdfPreviewDailyReports!!,
                    onDismiss = { pdfPreviewDailyReports = null }
                )
            }

            if (pdfPreviewExpenses != null) {
                PdfPreviewModalDialog(
                    title = "মাসিক ব্যয় রেজিস্টার",
                    farmProfile = farmProfile,
                    expenses = pdfPreviewExpenses!!,
                    onDismiss = { pdfPreviewExpenses = null }
                )
            }
        }
    }
}
