package com.binaryxbeast.krewzo.data.repository

import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.JobOffer
import com.binaryxbeast.krewzo.core.model.OfferStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface WorkerRepository {
    fun getIncomingOffers(): Flow<List<JobOffer>>
    fun getAvailability(): Flow<Boolean>
    suspend fun setAvailability(isAvailable: Boolean): Resource<Boolean>
    suspend fun respondOffer(offerId: String, decision: String): Resource<Boolean>
    fun getEarningsSummary(): Flow<Pair<Double, Int>> // Total earnings, total jobs completed
}

class WorkerRepositoryImpl : WorkerRepository {

    private val _isAvailable = MutableStateFlow(false)
    private val _offers = MutableStateFlow<List<JobOffer>>(emptyList())

    override fun getIncomingOffers(): Flow<List<JobOffer>> = _offers.asStateFlow()

    override fun getAvailability(): Flow<Boolean> = _isAvailable.asStateFlow()

    override suspend fun setAvailability(isAvailable: Boolean): Resource<Boolean> {
        delay(300)
        _isAvailable.value = isAvailable
        return Resource.Success(isAvailable)
    }

    override suspend fun respondOffer(offerId: String, decision: String): Resource<Boolean> {
        delay(600) // Simulates atomic RPC accept_booking_offer or decline_booking_offer
        val status = if (decision == "accept") OfferStatus.ACCEPTED else OfferStatus.DECLINED

        _offers.update { list ->
            list.map { if (it.id == offerId) it.copy(status = status) else it }
        }
        return Resource.Success(true)
    }

    override fun getEarningsSummary(): Flow<Pair<Double, Int>> {
        return MutableStateFlow(Pair(0.0, 0)).asStateFlow()
    }
}
