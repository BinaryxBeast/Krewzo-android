package com.binaryxbeast.krewzo.navigation

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.binaryxbeast.krewzo.core.model.UserRole
import com.binaryxbeast.krewzo.core.model.UserSession

/**
 * Root Navigation Graph for Krewzo.
 * Handles role-based entry point dispatching and connects all sub-graphs with fluid transitions.
 */
@Composable
fun KrewzoNavGraph(
    navController: NavHostController,
    currentSession: UserSession?,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route,
    onSignOut: () -> Unit = {
        navController.navigate(Screen.PhoneInput.route) {
            popUpTo(0) { inclusive = true }
        }
    },
    // Screen composables passed as lambda slots for clean modular decoupling
    splashContent: @Composable (onNavigateNext: (String) -> Unit) -> Unit,
    phoneInputContent: @Composable (onOtpSent: (String) -> Unit) -> Unit,
    otpVerifyContent: @Composable (phone: String, onRouteResolved: (UserSession) -> Unit) -> Unit,
    clientOnboardingContent: @Composable (phone: String, onOnboardingComplete: () -> Unit, onSignOut: () -> Unit) -> Unit,
    clientHomeContent: @Composable (onCreateBookingClick: () -> Unit, onBookingClick: (String) -> Unit, onNavigateProfile: () -> Unit, onSignOut: () -> Unit) -> Unit,
    createBookingContent: @Composable (onBookingCreated: (String) -> Unit, onBack: () -> Unit) -> Unit,
    bookingDetailContent: @Composable (bookingId: String, onBack: () -> Unit) -> Unit,
    clientHistoryContent: @Composable (onRepeatBooking: (String) -> Unit) -> Unit,
    clientProfileContent: @Composable (onNavigateHome: () -> Unit, onNavigateCreateBooking: () -> Unit, onNavigateHistory: () -> Unit, onSignOut: () -> Unit) -> Unit,
    workerHomeContent: @Composable (onOfferClick: (String) -> Unit, onNavigateProfile: () -> Unit, onSignOut: () -> Unit) -> Unit,
    workerScheduleContent: @Composable () -> Unit,
    workerEarningsContent: @Composable () -> Unit,
    workerProfileContent: @Composable (onNavigateHome: () -> Unit, onSignOut: () -> Unit) -> Unit,
    adminDashboardContent: @Composable (
        onNavigateWorkers: () -> Unit,
        onNavigateAddWorker: () -> Unit,
        onNavigateClients: () -> Unit,
        onNavigateAddClient: () -> Unit,
        onNavigateBookings: () -> Unit,
        onNavigateUserRoles: () -> Unit,
        onNavigateProfile: () -> Unit,
        onBookingClick: (String) -> Unit,
        onSignOut: () -> Unit
    ) -> Unit,
    adminUserRolesContent: @Composable (onBack: () -> Unit) -> Unit,
    adminWorkersContent: @Composable (
        onAddWorkerClick: () -> Unit,
        onEditWorkerClick: (String) -> Unit,
        onNavigateDashboard: () -> Unit,
        onNavigateClients: () -> Unit,
        onNavigateBookings: () -> Unit,
        onNavigateProfile: () -> Unit,
        onBack: () -> Unit
    ) -> Unit,
    adminAddEditWorkerContent: @Composable (workerId: String?, onSaveComplete: () -> Unit, onBack: () -> Unit) -> Unit,
    adminClientsContent: @Composable (
        onAddClientClick: () -> Unit,
        onEditClientClick: (String) -> Unit,
        onNavigateDashboard: () -> Unit,
        onNavigateWorkers: () -> Unit,
        onNavigateBookings: () -> Unit,
        onNavigateProfile: () -> Unit,
        onBack: () -> Unit
    ) -> Unit,
    adminAddEditClientContent: @Composable (clientId: String?, onSaveComplete: () -> Unit, onBack: () -> Unit) -> Unit,
    adminBookingsContent: @Composable (
        onBookingClick: (String) -> Unit,
        onNavigateDashboard: () -> Unit,
        onNavigateWorkers: () -> Unit,
        onNavigateClients: () -> Unit,
        onNavigateProfile: () -> Unit,
        onBack: () -> Unit
    ) -> Unit,
    adminBookingDetailContent: @Composable (bookingId: String, onBack: () -> Unit) -> Unit,
    adminProfileContent: @Composable (
        onNavigateDashboard: () -> Unit,
        onNavigateWorkers: () -> Unit,
        onNavigateClients: () -> Unit,
        onNavigateBookings: () -> Unit,
        onSignOut: () -> Unit
    ) -> Unit
) {
    val mainTabRoutes = setOf(
        Screen.AdminDashboard.route,
        Screen.AdminWorkers.route,
        Screen.AdminClients.route,
        Screen.AdminBookings.route,
        Screen.AdminProfile.route,
        Screen.ClientHome.route,
        Screen.ClientHistory.route,
        Screen.ClientProfile.route,
        Screen.WorkerHome.route,
        Screen.WorkerSchedule.route,
        Screen.WorkerEarnings.route,
        Screen.WorkerProfile.route
    )


    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            val fromRoute = initialState.destination.route
            val toRoute = targetState.destination.route
            if (fromRoute in mainTabRoutes && toRoute in mainTabRoutes) {
                fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.97f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f))
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                ) + fadeIn(animationSpec = tween(260))
            }
        },
        exitTransition = {
            val fromRoute = initialState.destination.route
            val toRoute = targetState.destination.route
            if (fromRoute in mainTabRoutes && toRoute in mainTabRoutes) {
                fadeOut(animationSpec = tween(180))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -(fullWidth * 0.28f).toInt() },
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
                ) + fadeOut(animationSpec = tween(220))
            }
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -(fullWidth * 0.28f).toInt() },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
            ) + fadeIn(animationSpec = tween(260))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = spring(dampingRatio = 0.82f, stiffness = 420f)
            ) + fadeOut(animationSpec = tween(220))
        }
    ) {
        // ── Splash Screen ────────────────────────────────────
        composable(Screen.Splash.route) {
            splashContent { targetRoute ->
                navController.navigate(targetRoute) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        // ── Auth Flow ────────────────────────────────────────
        composable(Screen.PhoneInput.route) {
            phoneInputContent { phone ->
                navController.navigate(Screen.OtpVerify.createRoute(phone))
            }
        }

        composable(
            route = Screen.OtpVerify.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone").orEmpty()
            otpVerifyContent(phone) { session ->
                when {
                    session.role == UserRole.WORKER -> {
                        navController.navigate(Screen.WorkerHome.route) {
                            popUpTo(Screen.PhoneInput.route) { inclusive = true }
                        }
                    }
                    session.role == UserRole.ADMIN -> {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.PhoneInput.route) { inclusive = true }
                        }
                    }
                    session.role == UserRole.CLIENT && !session.isOnboarded -> {
                        navController.navigate(Screen.ClientOnboarding.createRoute(phone)) {
                            popUpTo(Screen.PhoneInput.route) { inclusive = true }
                        }
                    }
                    else -> {
                        navController.navigate(Screen.ClientHome.route) {
                            popUpTo(Screen.PhoneInput.route) { inclusive = true }
                        }
                    }
                }
            }
        }

        composable(
            route = Screen.ClientOnboarding.route,
            arguments = listOf(navArgument("phone") { type = NavType.StringType })
        ) { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone").orEmpty()
            clientOnboardingContent(
                phone,
                {
                    navController.navigate(Screen.ClientHome.route) {
                        popUpTo(Screen.ClientOnboarding.route) { inclusive = true }
                    }
                },
                onSignOut
            )
        }

        // ── Client Flow ──────────────────────────────────────
        composable(Screen.ClientHome.route) {
            clientHomeContent(
                { navController.navigate(Screen.CreateBooking.route) },
                { bookingId -> navController.navigate(Screen.BookingDetail.createRoute(bookingId)) },
                { navController.navigateToClientTab(Screen.ClientProfile.route) },
                onSignOut
            )
        }

        composable(Screen.CreateBooking.route) {
            createBookingContent(
                { bookingId ->
                    navController.navigate(Screen.BookingDetail.createRoute(bookingId)) {
                        popUpTo(Screen.CreateBooking.route) { inclusive = true }
                    }
                },
                { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.BookingDetail.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId").orEmpty()
            bookingDetailContent(bookingId) {
                navController.popBackStack()
            }
        }

        composable(Screen.ClientHistory.route) {
            clientHistoryContent { _ ->
                navController.navigate(Screen.CreateBooking.route)
            }
        }

        composable(Screen.ClientProfile.route) {
            clientProfileContent(
                { navController.navigateToClientTab(Screen.ClientHome.route) },
                { navController.navigate(Screen.CreateBooking.route) },
                { navController.navigateToClientTab(Screen.ClientHistory.route) },
                onSignOut
            )
        }

        // ── Worker Flow ──────────────────────────────────────
        composable(Screen.WorkerHome.route) {
            workerHomeContent(
                { _ -> },
                { navController.navigateToWorkerTab(Screen.WorkerProfile.route) },
                onSignOut
            )
        }

        composable(Screen.WorkerSchedule.route) {
            workerScheduleContent()
        }

        composable(Screen.WorkerEarnings.route) {
            workerEarningsContent()
        }

        composable(Screen.WorkerProfile.route) {
            workerProfileContent(
                { navController.navigateToWorkerTab(Screen.WorkerHome.route) },
                onSignOut
            )
        }

        // ── Admin Flow ───────────────────────────────────────
        composable(Screen.AdminDashboard.route) {
            adminDashboardContent(
                { navController.navigateToAdminTab(Screen.AdminWorkers.route) },
                { navController.navigate(Screen.AdminAddWorker.route) },
                { navController.navigateToAdminTab(Screen.AdminClients.route) },
                { navController.navigate(Screen.AdminAddClient.route) },
                { navController.navigateToAdminTab(Screen.AdminBookings.route) },
                { navController.navigate(Screen.AdminUserRoles.route) },
                { navController.navigateToAdminTab(Screen.AdminProfile.route) },
                { bookingId -> navController.navigate(Screen.AdminBookingDetail.createRoute(bookingId)) },
                onSignOut
            )
        }

        composable(Screen.AdminUserRoles.route) {
            adminUserRolesContent {
                navController.popBackStack()
            }
        }

        composable(Screen.AdminWorkers.route) {
            adminWorkersContent(
                { navController.navigate(Screen.AdminAddWorker.route) },
                { workerId -> navController.navigate(Screen.AdminEditWorker.createRoute(workerId)) },
                { navController.navigateToAdminTab(Screen.AdminDashboard.route) },
                { navController.navigateToAdminTab(Screen.AdminClients.route) },
                { navController.navigateToAdminTab(Screen.AdminBookings.route) },
                { navController.navigateToAdminTab(Screen.AdminProfile.route) },
                { navController.popBackStack() }
            )
        }

        composable(Screen.AdminAddWorker.route) {
            adminAddEditWorkerContent(
                null,
                { navController.popBackStack() },
                { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AdminEditWorker.route,
            arguments = listOf(navArgument("workerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val workerId = backStackEntry.arguments?.getString("workerId")
            adminAddEditWorkerContent(
                workerId,
                { navController.popBackStack() },
                { navController.popBackStack() }
            )
        }

        composable(Screen.AdminClients.route) {
            adminClientsContent(
                { navController.navigate(Screen.AdminAddClient.route) },
                { clientId -> navController.navigate(Screen.AdminEditClient.createRoute(clientId)) },
                { navController.navigateToAdminTab(Screen.AdminDashboard.route) },
                { navController.navigateToAdminTab(Screen.AdminWorkers.route) },
                { navController.navigateToAdminTab(Screen.AdminBookings.route) },
                { navController.navigateToAdminTab(Screen.AdminProfile.route) },
                { navController.popBackStack() }
            )
        }

        composable(Screen.AdminAddClient.route) {
            adminAddEditClientContent(
                null,
                { navController.popBackStack() },
                { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AdminEditClient.route,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId")
            adminAddEditClientContent(
                clientId,
                { navController.popBackStack() },
                { navController.popBackStack() }
            )
        }

        composable(Screen.AdminBookings.route) {
            adminBookingsContent(
                { bookingId -> navController.navigate(Screen.AdminBookingDetail.createRoute(bookingId)) },
                { navController.navigateToAdminTab(Screen.AdminDashboard.route) },
                { navController.navigateToAdminTab(Screen.AdminWorkers.route) },
                { navController.navigateToAdminTab(Screen.AdminClients.route) },
                { navController.navigateToAdminTab(Screen.AdminProfile.route) },
                { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AdminBookingDetail.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId").orEmpty()
            adminBookingDetailContent(bookingId) {
                navController.popBackStack()
            }
        }

        composable(Screen.AdminProfile.route) {
            adminProfileContent(
                { navController.navigateToAdminTab(Screen.AdminDashboard.route) },
                { navController.navigateToAdminTab(Screen.AdminWorkers.route) },
                { navController.navigateToAdminTab(Screen.AdminClients.route) },
                { navController.navigateToAdminTab(Screen.AdminBookings.route) },
                onSignOut
            )
        }
    }
}

private fun NavHostController.navigateToAdminTab(route: String) {
    navigate(route) {
        popUpTo(Screen.AdminDashboard.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateToClientTab(route: String) {
    navigate(route) {
        popUpTo(Screen.ClientHome.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateToWorkerTab(route: String) {
    navigate(route) {
        popUpTo(Screen.WorkerHome.route) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

