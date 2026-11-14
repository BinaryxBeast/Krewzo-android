package com.binaryxbeast.krewzo.feature.client

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.Booking
import com.binaryxbeast.krewzo.core.model.Client
import com.binaryxbeast.krewzo.data.repository.AdminRepository
import com.binaryxbeast.krewzo.data.repository.BookingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClientViewModel(
    private val bookingRepository: BookingRepository,
    private val adminRepository: AdminRepository? = null
) : ViewModel() {

    val bookings: StateFlow<List<Booking>> = bookingRepository.getClientBookings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _createBookingState = MutableStateFlow<Resource<Booking>>(Resource.Idle)
    val createBookingState: StateFlow<Resource<Booking>> = _createBookingState.asStateFlow()

    // Client Profile State
    private val _clientProfile = MutableStateFlow<Client?>(
        Client(
            id = "cli_demo",
            businessName = "Krewzo Pune Logistics",
            contactPerson = "Ajay Deshmukh",
            contactPersonRole = "Operations Lead",
            phone = "9823012345",
            email = "ajay@krewzologistics.in",
            employeeCount = 35,
            address = "Plot 42, Phase 1, Hinjawadi Rajiv Gandhi Infotech Park",
            city = "Pune",
            pincode = "411057",
            latitude = 18.5913,
            longitude = 73.7389,
            gstin = "27ABCDE1234F1Z5",
            isVerified = true,
            isActive = true
        )
    )
    val clientProfile: StateFlow<Client?> = _clientProfile.asStateFlow()

    private val _saveProfileState = MutableStateFlow<Resource<Client>>(Resource.Idle)
    val saveProfileState: StateFlow<Resource<Client>> = _saveProfileState.asStateFlow()

    fun initProfileFromSession(phone: String?, businessName: String?, contactName: String?) {
        val current = _clientProfile.value
        if (phone != null && current != null) {
            _clientProfile.value = current.copy(
                phone = phone,
                businessName = businessName ?: current.businessName,
                contactPerson = contactName ?: current.contactPerson
            )
        }
    }

    fun updateClientProfile(updatedClient: Client, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _saveProfileState.value = Resource.Loading
            kotlinx.coroutines.delay(400)
            _clientProfile.value = updatedClient
            if (adminRepository != null) {
                adminRepository.saveClient(updatedClient)
            }
            _saveProfileState.value = Resource.Success(updatedClient)
            onSuccess()
        }
    }

    fun resetSaveProfileState() {
        _saveProfileState.value = Resource.Idle
    }

    fun createBooking(
        workersNeeded: Int,
        workDate: String,
        workStartTime: String,
        workEndTime: String?,
        workAddress: String,
        payPerWorker: Double,
        jobNotes: String?,
        onSuccess: (String) -> Unit
    ) {
        viewModelScope.launch {
            _createBookingState.value = Resource.Loading
            val result = bookingRepository.createBooking(
                workersNeeded = workersNeeded,
                workDate = workDate,
                workStartTime = workStartTime,
                workEndTime = workEndTime,
                workAddress = workAddress,
                payPerWorker = payPerWorker,
                jobNotes = jobNotes
            )
            _createBookingState.value = result
            if (result is Resource.Success) {
                onSuccess(result.data.id)
            }
        }
    }

    fun cancelBooking(bookingId: String) {
        viewModelScope.launch {
            bookingRepository.cancelBooking(bookingId)
        }
    }

    fun resetCreateState() {
        _createBookingState.value = Resource.Idle
    }
}

