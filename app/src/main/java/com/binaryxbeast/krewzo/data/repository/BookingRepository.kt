package com.binaryxbeast.krewzo.data.repository

import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.AssignedWorker
import com.binaryxbeast.krewzo.core.model.Booking
import com.binaryxbeast.krewzo.core.model.BookingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface BookingRepository {
    fun getClientBookings(): Flow<List<Booking>>
    fun getBookingLive(bookingId: String): Flow<Booking?>
    suspend fun createBooking(
        workersNeeded: Int,
        workDate: String,
        workStartTime: String,
        workEndTime: String?,
        workAddress: String,
        payPerWorker: Double,
        jobNotes: String?
    ): Resource<Booking>
    suspend fun cancelBooking(bookingId: String): Resource<Unit>
}

class BookingRepositoryImpl : BookingRepository {

    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())

    override fun getClientBookings(): Flow<List<Booking>> = _bookings.asStateFlow()

    override fun getBookingLive(bookingId: String): Flow<Booking?> {
        val singleFlow = MutableStateFlow<Booking?>(_bookings.value.find { it.id == bookingId })
        return singleFlow.asStateFlow()
    }

    override suspend fun createBooking(
        workersNeeded: Int,
        workDate: String,
        workStartTime: String,
        workEndTime: String?,
        workAddress: String,
        payPerWorker: Double,
        jobNotes: String?
    ): Resource<Booking> {
        delay(900) // Simulating network & geocoding & wave 1 dispatch

        val newBooking = Booking(
            id = "bkg_${System.currentTimeMillis() % 10000}",
            clientId = "cli_001",
            clientName = "My Business",
            workersNeeded = workersNeeded,
            workersAccepted = 0,
            payPerWorker = payPerWorker,
            workAddress = workAddress,
            workDate = workDate,
            workStartTime = workStartTime,
            workEndTime = workEndTime,
            status = BookingStatus.SEARCHING,
            jobNotes = jobNotes,
            currentWave = 1,
            assignedWorkers = emptyList()
        )

        _bookings.update { listOf(newBooking) + it }
        return Resource.Success(newBooking)
    }

    override suspend fun cancelBooking(bookingId: String): Resource<Unit> {
        delay(400)
        _bookings.update { list ->
            list.map { b ->
                if (b.id == bookingId) b.copy(status = BookingStatus.CANCELLED) else b
            }
        }
        return Resource.Success(Unit)
    }
}
