package com.binaryxbeast.krewzo.core.model

enum class BookingStatus(val label: String) {
    SEARCHING("Searching for Crew"),
    PARTIALLY_FILLED("Partially Filled"),
    CONFIRMED("Confirmed"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    EXPIRED("Offer Expired");

    companion object {
        fun fromString(value: String?): BookingStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: SEARCHING
        }
    }
}

data class WorkLocation(
    val address: String,
    val city: String = "Pune",
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class AssignedWorker(
    val workerId: String,
    val fullName: String,
    val phone: String,
    val photoUrl: String? = null,
    val rating: Double? = 4.8,
    val assignedAt: String,
    val paymentStatus: String = "pending"
)

data class Booking(
    val id: String,
    val clientId: String,
    val clientName: String? = null,
    val workersNeeded: Int,
    val workersAccepted: Int,
    val payPerWorker: Double,
    val workAddress: String,
    val workDate: String,         // YYYY-MM-DD
    val workStartTime: String,    // HH:MM
    val workEndTime: String? = null,
    val status: BookingStatus,
    val jobNotes: String? = null,
    val currentWave: Int = 1,
    val assignedWorkers: List<AssignedWorker> = emptyList(),
    val createdAt: String? = null
) {
    val totalCost: Double
        get() = workersNeeded * payPerWorker

    val fillProgress: Float
        get() = if (workersNeeded > 0) workersAccepted.toFloat() / workersNeeded.toFloat() else 0f

    val isFilled: Boolean
        get() = workersAccepted >= workersNeeded
}
