package com.binaryxbeast.krewzo.feature.worker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.JobOffer
import com.binaryxbeast.krewzo.core.model.OfferStatus
import com.binaryxbeast.krewzo.core.model.SkillCategory
import com.binaryxbeast.krewzo.core.model.Worker
import com.binaryxbeast.krewzo.data.repository.AdminRepository
import com.binaryxbeast.krewzo.data.repository.WorkerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period

class WorkerViewModel(
    private val workerRepository: WorkerRepository,
    private val adminRepository: AdminRepository? = null
) : ViewModel() {

    val isAvailable: StateFlow<Boolean> = workerRepository.getAvailability()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val incomingOffers: StateFlow<List<JobOffer>> = workerRepository.getIncomingOffers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val earnings: StateFlow<Pair<Double, Int>> = workerRepository.getEarningsSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(24800.0, 29))

    // Worker Profile State (Read-only core credentials + editable address, emergency contact, DOB)
    private val _workerProfile = MutableStateFlow<Worker?>(
        Worker(
            id = "wrk_pune_01",
            phone = "9822198765",
            fullName = "Ramesh Tukaram Shinde",
            age = 26,
            dateOfBirth = "2000-05-15",
            photoUrl = null,
            homeAddress = "Dhayari Phata, Sinhagad Road, Pune",
            isAvailable = true,
            isActive = true,
            totalJobsCompleted = 29,
            totalEarnings = 24800.0,
            rating = 4.9,
            skills = listOf(
                SkillCategory.LOADING_UNLOADING,
                SkillCategory.PACKAGING,
                SkillCategory.DELIVERY
            ),
            emergencyContact = "9822112233",
            adminNotes = "Verified Aadhar card & Pune resident"
        )
    )
    val workerProfile: StateFlow<Worker?> = _workerProfile.asStateFlow()

    private val _saveProfileResult = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val saveProfileResult: StateFlow<Resource<Unit>> = _saveProfileResult.asStateFlow()

    private val _actionResult = MutableStateFlow<Resource<String>>(Resource.Idle)
    val actionResult: StateFlow<Resource<String>> = _actionResult.asStateFlow()

    fun initProfileFromSession(phone: String?, fullName: String?) {
        val current = _workerProfile.value
        if (phone != null && current != null) {
            _workerProfile.value = current.copy(
                phone = phone,
                fullName = fullName ?: current.fullName
            )
        }
    }

    /**
     * Validates DOB and updates only worker-editable fields.
     * Denies update if calculated age is less than 18.
     */
    fun updateWorkerDetails(
        homeAddress: String,
        emergencyContact: String,
        dobString: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _saveProfileResult.value = Resource.Loading

            // 1. Validate Date of Birth and enforce age >= 18
            val calculatedAge = calculateAgeFromDob(dobString)
            if (calculatedAge == null) {
                _saveProfileResult.value = Resource.Error("Please enter a valid Date of Birth in YYYY-MM-DD format (e.g., 2000-05-15).")
                return@launch
            }

            if (calculatedAge < 18) {
                _saveProfileResult.value = Resource.Error("Age Restriction: Workers must be at least 18 years old to operate on Krewzo. Calculated age is $calculatedAge.")
                return@launch
            }

            kotlinx.coroutines.delay(400)
            val current = _workerProfile.value
            if (current != null) {
                val updated = current.copy(
                    homeAddress = homeAddress,
                    emergencyContact = emergencyContact,
                    dateOfBirth = dobString,
                    age = calculatedAge
                )
                _workerProfile.value = updated
                if (adminRepository != null) {
                    adminRepository.saveWorker(updated)
                }
            }

            _saveProfileResult.value = Resource.Success(Unit)
            onSuccess()
        }
    }

    private fun calculateAgeFromDob(dobString: String): Int? {
        val clean = dobString.trim()
        val parts = clean.split("-", "/", ".")
        if (parts.size != 3) return null
        return try {
            val (year, month, day) = if (parts[0].length == 4) {
                Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            } else {
                Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
            }
            val birthDate = LocalDate.of(year, month, day)
            val today = LocalDate.now()
            Period.between(birthDate, today).years
        } catch (_: Exception) {
            null
        }
    }

    fun resetSaveProfileResult() {
        _saveProfileResult.value = Resource.Idle
    }

    fun toggleAvailability(isAvailable: Boolean) {
        viewModelScope.launch {
            workerRepository.setAvailability(isAvailable)
        }
    }

    fun acceptOffer(offerId: String) {
        viewModelScope.launch {
            _actionResult.value = Resource.Loading
            val res = workerRepository.respondOffer(offerId, "accept")
            if (res is Resource.Success) {
                _actionResult.value = Resource.Success("Job accepted! Added to your schedule.")
            } else if (res is Resource.Error) {
                _actionResult.value = Resource.Error(res.message)
            }
        }
    }

    fun declineOffer(offerId: String) {
        viewModelScope.launch {
            workerRepository.respondOffer(offerId, "decline")
        }
    }

    fun clearActionResult() {
        _actionResult.value = Resource.Idle
    }
}

