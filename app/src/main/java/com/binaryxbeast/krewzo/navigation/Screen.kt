package com.binaryxbeast.krewzo.navigation

sealed class Screen(val route: String) {

    // ── Root / Splash ────────────────────────────────────────
    data object Splash : Screen("splash")

    // ── Authentication Graph ─────────────────────────────────
    data object PhoneInput : Screen("auth/phone_input")
    data object OtpVerify : Screen("auth/otp_verify/{phone}") {
        fun createRoute(phone: String) = "auth/otp_verify/$phone"
    }
    data object ClientOnboarding : Screen("auth/client_onboarding/{phone}") {
        fun createRoute(phone: String) = "auth/client_onboarding/$phone"
    }

    // ── Client Feature Graph ─────────────────────────────────
    data object ClientHome : Screen("client/home")
    data object CreateBooking : Screen("client/create_booking")
    data object BookingDetail : Screen("client/booking_detail/{bookingId}") {
        fun createRoute(bookingId: String) = "client/booking_detail/$bookingId"
    }
    data object ClientHistory : Screen("client/history")
    data object ClientProfile : Screen("client/profile")

    // ── Worker Feature Graph ─────────────────────────────────
    data object WorkerHome : Screen("worker/home")
    data object WorkerSchedule : Screen("worker/schedule")
    data object WorkerEarnings : Screen("worker/earnings")
    data object WorkerProfile : Screen("worker/profile")
    data object WorkerJobDetail : Screen("worker/job_detail/{offerId}") {
        fun createRoute(offerId: String) = "worker/job_detail/$offerId"
    }

    // ── Admin Feature Graph ──────────────────────────────────
    data object AdminDashboard : Screen("admin/dashboard")
    data object AdminUserRoles : Screen("admin/user_roles")
    data object AdminWorkers : Screen("admin/workers")
    data object AdminAddWorker : Screen("admin/workers/add")
    data object AdminEditWorker : Screen("admin/workers/edit/{workerId}") {
        fun createRoute(workerId: String) = "admin/workers/edit/$workerId"
    }
    data object AdminClients : Screen("admin/clients")
    data object AdminAddClient : Screen("admin/clients/add")
    data object AdminEditClient : Screen("admin/clients/edit/{clientId}") {
        fun createRoute(clientId: String) = "admin/clients/edit/$clientId"
    }
    data object AdminBookings : Screen("admin/bookings")
    data object AdminBookingDetail : Screen("admin/bookings/{bookingId}") {
        fun createRoute(bookingId: String) = "admin/bookings/$bookingId"
    }
    data object AdminProfile : Screen("admin/profile")
}

