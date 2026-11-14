package com.binaryxbeast.krewzo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.binaryxbeast.krewzo.core.model.UserRole
import com.binaryxbeast.krewzo.data.repository.AdminRepositoryImpl
import com.binaryxbeast.krewzo.data.repository.AuthRepositoryImpl
import com.binaryxbeast.krewzo.data.repository.BookingRepositoryImpl
import com.binaryxbeast.krewzo.data.repository.UserRoleRegistry
import com.binaryxbeast.krewzo.data.repository.WorkerRepositoryImpl
import com.binaryxbeast.krewzo.feature.admin.AddEditClientScreen
import com.binaryxbeast.krewzo.feature.admin.AddEditWorkerScreen
import com.binaryxbeast.krewzo.feature.admin.AdminBookingDetailScreen
import com.binaryxbeast.krewzo.feature.admin.AdminBookingsScreen
import com.binaryxbeast.krewzo.feature.admin.AdminClientsScreen
import com.binaryxbeast.krewzo.feature.admin.AdminDashboardScreen
import com.binaryxbeast.krewzo.feature.admin.AdminProfileScreen
import com.binaryxbeast.krewzo.feature.admin.AdminUserRolesScreen
import com.binaryxbeast.krewzo.feature.admin.AdminViewModel
import com.binaryxbeast.krewzo.feature.admin.AdminWorkersScreen
import com.binaryxbeast.krewzo.feature.admin.WorkerOnboardingScreen
import com.binaryxbeast.krewzo.feature.auth.AuthViewModel
import com.binaryxbeast.krewzo.feature.auth.ClientOnboardingScreen
import com.binaryxbeast.krewzo.feature.auth.OtpVerificationScreen
import com.binaryxbeast.krewzo.feature.auth.PhoneInputScreen
import com.binaryxbeast.krewzo.feature.client.BookingDetailLiveScreen
import com.binaryxbeast.krewzo.feature.client.ClientHomeScreen
import com.binaryxbeast.krewzo.feature.client.ClientProfileScreen
import com.binaryxbeast.krewzo.feature.client.ClientViewModel
import com.binaryxbeast.krewzo.feature.client.CreateBookingScreen
import com.binaryxbeast.krewzo.feature.worker.WorkerHomeScreen
import com.binaryxbeast.krewzo.feature.worker.WorkerProfileScreen
import com.binaryxbeast.krewzo.feature.worker.WorkerViewModel
import com.binaryxbeast.krewzo.navigation.KrewzoNavGraph
import com.binaryxbeast.krewzo.navigation.Screen
import com.binaryxbeast.krewzo.ui.theme.KrewzoDimens
import com.binaryxbeast.krewzo.ui.theme.KrewzoTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize phone role registry with primary admin (8275976150)
        UserRoleRegistry.init(this)

        // Core Repositories
        val authRepository = AuthRepositoryImpl(applicationContext)
        val bookingRepository = BookingRepositoryImpl()
        val workerRepository = WorkerRepositoryImpl()
        val adminRepository = AdminRepositoryImpl()

        setContent {
            KrewzoTheme {
                val navController = rememberNavController()

                // ViewModels retain state across configuration changes (e.g. screen rotation)
                val authViewModel: AuthViewModel = viewModel { AuthViewModel(authRepository) }
                val adminViewModel: AdminViewModel = viewModel { AdminViewModel(adminRepository) }
                val clientViewModel: ClientViewModel = viewModel { ClientViewModel(bookingRepository, adminRepository) }
                val workerViewModel: WorkerViewModel = viewModel { WorkerViewModel(workerRepository, adminRepository) }

                val currentSession by authRepository.getCurrentSession().collectAsState(initial = authRepository.currentSessionValue)

                LaunchedEffect(currentSession) {
                    val session = currentSession
                    if (session != null) {
                        if (session.role == UserRole.CLIENT) {
                            clientViewModel.initProfileFromSession(session.phone, session.businessName, session.fullName)
                        } else if (session.role == UserRole.WORKER) {
                            workerViewModel.initProfileFromSession(session.phone, session.fullName)
                        }
                    }
                }

                val handleSignOut: () -> Unit = {
                    authViewModel.signOut {
                        navController.navigate(Screen.PhoneInput.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }

                KrewzoNavGraph(
                    navController = navController,
                    currentSession = currentSession,
                    startDestination = Screen.Splash.route,
                    onSignOut = handleSignOut,
                    splashContent = { onNavigateNext ->
                        SplashScreen(
                            onTimeout = {
                                val targetRoute = when {
                                    currentSession == null -> Screen.PhoneInput.route
                                    currentSession?.role == UserRole.ADMIN -> Screen.AdminDashboard.route
                                    currentSession?.role == UserRole.WORKER -> Screen.WorkerHome.route
                                    currentSession?.role == UserRole.CLIENT && currentSession?.isOnboarded == false ->
                                        Screen.ClientOnboarding.createRoute(currentSession?.phone.orEmpty())
                                    else -> Screen.ClientHome.route
                                }
                                onNavigateNext(targetRoute)
                            }
                        )
                    },
                    phoneInputContent = { onOtpSent ->
                        PhoneInputScreen(
                            viewModel = authViewModel,
                            onOtpSent = onOtpSent
                        )
                    },
                    otpVerifyContent = { phone, onRouteResolved ->
                        OtpVerificationScreen(
                            phone = phone,
                            viewModel = authViewModel,
                            onBack = { navController.popBackStack() },
                            onRouteResolved = onRouteResolved
                        )
                    },
                    clientOnboardingContent = { phone, onOnboardingComplete, onSignOut ->
                        ClientOnboardingScreen(
                            phone = phone,
                            viewModel = authViewModel,
                            onOnboardingComplete = onOnboardingComplete,
                            onSignOut = onSignOut
                        )
                    },
                    clientHomeContent = { onCreateClick, onBookingClick, onNavigateProfile, onSignOut ->
                        ClientHomeScreen(
                            viewModel = clientViewModel,
                            onCreateBookingClick = onCreateClick,
                            onBookingClick = onBookingClick,
                            onNavigateProfile = onNavigateProfile,
                            onSignOut = onSignOut
                        )
                    },
                    createBookingContent = { onCreated, onBack ->
                        CreateBookingScreen(
                            viewModel = clientViewModel,
                            onBookingCreated = onCreated,
                            onBack = onBack
                        )
                    },
                    bookingDetailContent = { bookingId, onBack ->
                        BookingDetailLiveScreen(
                            bookingId = bookingId,
                            viewModel = clientViewModel,
                            onBack = onBack
                        )
                    },
                    clientHistoryContent = {
                        // Navigates or refreshes history
                    },
                    clientProfileContent = { onNavigateHome, onCreateBooking, onNavigateHistory, onSignOut ->
                        ClientProfileScreen(
                            viewModel = clientViewModel,
                            onNavigateHome = onNavigateHome,
                            onNavigateCreateBooking = onCreateBooking,
                            onNavigateHistory = onNavigateHistory,
                            onSignOut = onSignOut
                        )
                    },
                    workerHomeContent = { _, onNavigateProfile, onSignOut ->
                        WorkerHomeScreen(
                            viewModel = workerViewModel,
                            onOfferClick = {},
                            onNavigateProfile = onNavigateProfile,
                            onSignOut = onSignOut
                        )
                    },
                    workerScheduleContent = {},
                    workerEarningsContent = {},
                    workerProfileContent = { onNavigateHome, onSignOut ->
                        WorkerProfileScreen(
                            viewModel = workerViewModel,
                            onNavigateHome = onNavigateHome,
                            onSignOut = onSignOut
                        )
                    },
                    adminDashboardContent = { onWorkers, onAddWorker, onClients, onAddClient, onBookings, onUserRoles, onNavigateProfile, onBookingClick, onSignOut ->
                        AdminDashboardScreen(
                            viewModel = adminViewModel,
                            onNavigateWorkers = onWorkers,
                            onNavigateAddWorker = onAddWorker,
                            onNavigateClients = onClients,
                            onNavigateAddClient = onAddClient,
                            onNavigateBookings = onBookings,
                            onNavigateUserRoles = onUserRoles,
                            onNavigateProfile = onNavigateProfile,
                            onBookingClick = onBookingClick,
                            onSignOut = onSignOut
                        )
                    },
                    adminUserRolesContent = { onBack ->
                        AdminUserRolesScreen(
                            viewModel = adminViewModel,
                            onBack = onBack
                        )
                    },
                    adminWorkersContent = { onAdd, onEdit, onDashboard, onClients, onBookings, onNavigateProfile, onBack ->
                        AdminWorkersScreen(
                            viewModel = adminViewModel,
                            onAddWorkerClick = onAdd,
                            onEditWorkerClick = onEdit,
                            onNavigateDashboard = onDashboard,
                            onNavigateClients = onClients,
                            onNavigateBookings = onBookings,
                            onNavigateProfile = onNavigateProfile,
                            onBack = onBack
                        )
                    },
                    adminAddEditWorkerContent = { workerId, onSave, onBack ->
                        if (workerId == null) {
                            WorkerOnboardingScreen(
                                viewModel = adminViewModel,
                                onSaveComplete = onSave,
                                onBack = onBack
                            )
                        } else {
                            AddEditWorkerScreen(
                                workerId = workerId,
                                viewModel = adminViewModel,
                                onSaveComplete = onSave,
                                onBack = onBack
                            )
                        }
                    },
                    adminClientsContent = { onAdd, onEdit, onDashboard, onWorkers, onBookings, onNavigateProfile, onBack ->
                        AdminClientsScreen(
                            viewModel = adminViewModel,
                            onAddClientClick = onAdd,
                            onEditClientClick = onEdit,
                            onNavigateDashboard = onDashboard,
                            onNavigateWorkers = onWorkers,
                            onNavigateBookings = onBookings,
                            onNavigateProfile = onNavigateProfile,
                            onBack = onBack
                        )
                    },
                    adminAddEditClientContent = { clientId, onSave, onBack ->
                        AddEditClientScreen(
                            clientId = clientId,
                            viewModel = adminViewModel,
                            onSaveComplete = onSave,
                            onBack = onBack
                        )
                    },
                    adminBookingsContent = { onBookingClick, onDashboard, onWorkers, onClients, onNavigateProfile, onBack ->
                        AdminBookingsScreen(
                            viewModel = adminViewModel,
                            onBookingClick = onBookingClick,
                            onNavigateDashboard = onDashboard,
                            onNavigateWorkers = onWorkers,
                            onNavigateClients = onClients,
                            onNavigateProfile = onNavigateProfile,
                            onBack = onBack
                        )
                    },
                    adminBookingDetailContent = { bookingId, onBack ->
                        AdminBookingDetailScreen(
                            bookingId = bookingId,
                            viewModel = adminViewModel,
                            onBack = onBack
                        )
                    },
                    adminProfileContent = { onDashboard, onWorkers, onClients, onBookings, onSignOut ->
                        AdminProfileScreen(
                            viewModel = adminViewModel,
                            onNavigateDashboard = onDashboard,
                            onNavigateWorkers = onWorkers,
                            onNavigateClients = onClients,
                            onNavigateBookings = onBookings,
                            onSignOut = onSignOut
                        )
                    }
                )

            }
        }
    }
}

@Composable
fun SplashScreen(
    onTimeout: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "logoAlpha"
    )

    val contentAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 250),
        label = "contentAlpha"
    )

    val contentOffsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 24f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "contentOffsetY"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1400) // Fluid splash display
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                    }
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(KrewzoDimens.RadiusXl),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    .clip(RoundedCornerShape(KrewzoDimens.RadiusXl))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                Color(0xFF7582FF)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "K",
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha
                    translationY = contentOffsetY
                }
            ) {
                Text(
                    text = "Krewzo",
                    style = MaterialTheme.typography.headlineLarge,
                    color = KrewzoTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "On-demand B2B worker booking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KrewzoTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(36.dp))

                com.binaryxbeast.krewzo.ui.components.ExpressLoadingIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}
