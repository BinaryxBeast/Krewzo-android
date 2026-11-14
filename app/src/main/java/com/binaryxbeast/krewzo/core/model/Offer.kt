package com.binaryxbeast.krewzo.core.model

enum class OfferStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    CANCELLED;

    companion object {
        fun fromString(value: String?): OfferStatus {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

data class JobOffer(
    val id: String,
    val bookingId: String,
    val workerId: String,
    val payAmount: Double,
    val workAddress: String,
    val workDate: String,
    val workStartTime: String,
    val workEndTime: String? = null,
    val distanceKm: Double,
    val status: OfferStatus = OfferStatus.PENDING,
    val jobNotes: String? = null,
    val expiresAt: String,
    val waveNumber: Int = 1
)
