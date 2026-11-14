package com.binaryxbeast.krewzo.core.network.dto

import com.binaryxbeast.krewzo.core.model.Booking
import com.binaryxbeast.krewzo.core.model.BookingStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BookingDto(
    @SerialName("id") val id: String? = null,
    @SerialName("client_id") val clientId: String,
    @SerialName("workers_needed") val workersNeeded: Int,
    @SerialName("workers_accepted") val workersAccepted: Int = 0,
    @SerialName("pay_per_worker") val payPerWorker: Double,
    @SerialName("work_address") val workAddress: String,
    @SerialName("work_date") val workDate: String,
    @SerialName("work_start_time") val workStartTime: String,
    @SerialName("work_end_time") val workEndTime: String? = null,
    @SerialName("status") val status: String = "searching",
    @SerialName("job_notes") val jobNotes: String? = null,
    @SerialName("current_wave") val currentWave: Int = 1,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toDomain(): Booking {
        return Booking(
            id = id ?: "",
            clientId = clientId,
            workersNeeded = workersNeeded,
            workersAccepted = workersAccepted,
            payPerWorker = payPerWorker,
            workAddress = workAddress,
            workDate = workDate,
            workStartTime = workStartTime,
            workEndTime = workEndTime,
            status = BookingStatus.fromString(status),
            jobNotes = jobNotes,
            currentWave = currentWave,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(booking: Booking): BookingDto {
            return BookingDto(
                id = booking.id.ifBlank { null },
                clientId = booking.clientId,
                workersNeeded = booking.workersNeeded,
                workersAccepted = booking.workersAccepted,
                payPerWorker = booking.payPerWorker,
                workAddress = booking.workAddress,
                workDate = booking.workDate,
                workStartTime = booking.workStartTime,
                workEndTime = booking.workEndTime,
                status = booking.status.name.lowercase(),
                jobNotes = booking.jobNotes,
                currentWave = booking.currentWave,
                createdAt = booking.createdAt
            )
        }
    }
}
