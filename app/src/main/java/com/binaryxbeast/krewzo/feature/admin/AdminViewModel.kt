package com.binaryxbeast.krewzo.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.Booking
import com.binaryxbeast.krewzo.core.model.Client
import com.binaryxbeast.krewzo.core.model.UserRole
import com.binaryxbeast.krewzo.core.model.Worker
import com.binaryxbeast.krewzo.data.repository.AdminDashboardMetrics
import com.binaryxbeast.krewzo.data.repository.AdminRepository
import com.binaryxbeast.krewzo.data.repository.RoleAssignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository
) : ViewModel() {

    val dashboardMetrics: StateFlow<AdminDashboardMetrics> = adminRepository.getDashboardMetrics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminDashboardMetrics())

    val workers: StateFlow<List<Worker>> = adminRepository.getAllWorkers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<Client>> = adminRepository.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookings: StateFlow<List<Booking>> = adminRepository.getAllBookings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val roleAssignments: StateFlow<List<RoleAssignment>> = adminRepository.getAllRoleAssignments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Admin Profile management (Full Name, Phone, Email, Avatar)
    private val _adminProfile = MutableStateFlow(
        com.binaryxbeast.krewzo.core.model.AdminProfile(
            id = "admin_01",
            phone = com.binaryxbeast.krewzo.data.repository.UserRoleRegistry.PRIMARY_ADMIN_PHONE,
            fullName = "Super Admin (Owner)",
            email = "admin@krewzo.com",
            avatarUrl = null,
            roleTitle = "Super Administrator",
            operatingHub = "Pune Operations Hub",
            isActive = true
        )
    )
    val adminProfile: StateFlow<com.binaryxbeast.krewzo.core.model.AdminProfile> = _adminProfile.asStateFlow()

    private val _saveAdminProfileState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val saveAdminProfileState: StateFlow<Resource<Unit>> = _saveAdminProfileState.asStateFlow()

    fun updateAdminProfile(fullName: String, phone: String, email: String, avatarUrl: String?, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _saveAdminProfileState.value = Resource.Loading
            kotlinx.coroutines.delay(400)
            val updated = _adminProfile.value.copy(
                fullName = fullName,
                phone = phone,
                email = email,
                avatarUrl = avatarUrl
            )
            _adminProfile.value = updated
            adminRepository.saveRoleAssignment(phone, UserRole.ADMIN, fullName)
            _saveAdminProfileState.value = Resource.Success(Unit)
            onSuccess()
        }
    }

    fun resetSaveAdminProfileState() {
        _saveAdminProfileState.value = Resource.Idle
    }

    // Worker operations
    private val _saveWorkerState = MutableStateFlow<Resource<Worker>>(Resource.Idle)
    val saveWorkerState: StateFlow<Resource<Worker>> = _saveWorkerState.asStateFlow()


    fun saveWorker(
        worker: Worker,
        pendingPhotoBytes: ByteArray? = null,
        pendingAadhaarFrontBytes: ByteArray? = null,
        pendingAadhaarBackBytes: ByteArray? = null,
        aadhaarNumber: String? = null,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _saveWorkerState.value = Resource.Loading
            var targetWorker = if (worker.id.isBlank()) {
                worker.copy(id = java.util.UUID.randomUUID().toString())
            } else {
                worker
            }

            if (pendingPhotoBytes != null) {
                when (val photoRes = adminRepository.uploadWorkerPhoto(targetWorker.id, pendingPhotoBytes)) {
                    is Resource.Success -> {
                        targetWorker = targetWorker.copy(photoUrl = photoRes.data)
                    }
                    is Resource.Error -> {
                        _saveWorkerState.value = Resource.Error("Failed to upload photograph: ${photoRes.message}")
                        return@launch
                    }
                    else -> {}
                }
            }

            var frontUrl: String? = null
            if (pendingAadhaarFrontBytes != null) {
                when (val docRes = adminRepository.uploadWorkerDocument(targetWorker.id, "aadhaar", "front", pendingAadhaarFrontBytes)) {
                    is Resource.Success -> frontUrl = docRes.data
                    is Resource.Error -> {
                        _saveWorkerState.value = Resource.Error("Failed to upload Aadhaar front: ${docRes.message}")
                        return@launch
                    }
                    else -> {}
                }
            }

            var backUrl: String? = null
            if (pendingAadhaarBackBytes != null) {
                when (val docRes = adminRepository.uploadWorkerDocument(targetWorker.id, "aadhaar", "back", pendingAadhaarBackBytes)) {
                    is Resource.Success -> backUrl = docRes.data
                    is Resource.Error -> {
                        _saveWorkerState.value = Resource.Error("Failed to upload Aadhaar back: ${docRes.message}")
                        return@launch
                    }
                    else -> {}
                }
            }

            if (frontUrl != null || backUrl != null || !aadhaarNumber.isNullOrBlank()) {
                val existingNotes = targetWorker.adminNotes ?: ""
                val existingFront = frontUrl ?: existingNotes.extractAadhaarPart("FRONT")
                val existingBack  = backUrl ?: existingNotes.extractAadhaarPart("BACK")
                val existingNum   = if (!aadhaarNumber.isNullOrBlank()) aadhaarNumber else existingNotes.extractAadhaarPart("NUM")

                val generalNotes = existingNotes.split("|")
                    .map { it.trim() }
                    .filterNot {
                        it.startsWith("Aadhaar", ignoreCase = true) ||
                        it.startsWith("FRONT:", ignoreCase = true) ||
                        it.startsWith("BACK:", ignoreCase = true) ||
                        it.startsWith("NUM:", ignoreCase = true)
                    }
                    .joinToString(" | ")

                val noteParts = mutableListOf<String>()
                if (!existingNum.isNullOrBlank()) noteParts.add("Aadhaar No: $existingNum")
                if (!existingFront.isNullOrBlank()) noteParts.add("Aadhaar Front: $existingFront")
                if (!existingBack.isNullOrBlank()) noteParts.add("Aadhaar Back: $existingBack")
                if (generalNotes.isNotBlank()) noteParts.add(generalNotes)

                targetWorker = targetWorker.copy(adminNotes = noteParts.joinToString(" | ").ifBlank { null })
            }

            val res = adminRepository.saveWorker(targetWorker)
            _saveWorkerState.value = res
            if (res is Resource.Success) {
                onSuccess()
            }
        }
    }

    private fun String.extractAadhaarPart(key: String): String? {
        if (isBlank()) return null
        val prefixes = when (key.uppercase()) {
            "FRONT" -> listOf("Aadhaar Front:", "FRONT:")
            "BACK" -> listOf("Aadhaar Back:", "BACK:")
            "NUM" -> listOf("Aadhaar No:", "Aadhaar Number:", "NUM:")
            else -> listOf("$key:")
        }
        for (part in split("|").map { it.trim() }) {
            for (prefix in prefixes) {
                if (part.startsWith(prefix, ignoreCase = true)) {
                    return part.substring(prefix.length).trim().ifBlank { null }
                }
            }
        }
        return null
    }

    fun resetSaveWorkerState() {
        _saveWorkerState.value = Resource.Idle
    }

    // Step-by-Step Worker Onboarding
    private val _onboardWorkerState = MutableStateFlow<Resource<Worker>>(Resource.Idle)
    val onboardWorkerState: StateFlow<Resource<Worker>> = _onboardWorkerState.asStateFlow()

    fun resetOnboardWorkerState() {
        _onboardWorkerState.value = Resource.Idle
    }

    fun checkPhoneRegistered(phone: String, onResult: (isRegistered: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            when (val res = adminRepository.checkPhoneExists(phone)) {
                is Resource.Success -> onResult(res.data, null)
                is Resource.Error -> onResult(false, res.message)
                else -> onResult(false, null)
            }
        }
    }

    fun onboardWorker(
        worker: Worker,
        photoBytes: ByteArray?,
        aadhaarFrontBytes: ByteArray?,
        aadhaarBackBytes: ByteArray?,
        aadhaarNumber: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _onboardWorkerState.value = Resource.Loading
            val res = adminRepository.saveWorkerWithOnboarding(
                worker = worker,
                photoBytes = photoBytes,
                aadhaarFrontBytes = aadhaarFrontBytes,
                aadhaarBackBytes = aadhaarBackBytes,
                aadhaarNumber = aadhaarNumber
            )
            _onboardWorkerState.value = res
            if (res is Resource.Success) {
                onSuccess()
            }
        }
    }

    fun resetSaveClientState() {
        _saveClientState.value = Resource.Idle
    }

    fun deleteWorker(workerId: String) {
        viewModelScope.launch {
            adminRepository.deleteWorker(workerId)
        }
    }

    fun toggleWorkerActive(workerId: String, isActive: Boolean) {
        viewModelScope.launch {
            adminRepository.toggleWorkerActive(workerId, isActive)
        }
    }

    // Client operations
    private val _saveClientState = MutableStateFlow<Resource<Client>>(Resource.Idle)
    val saveClientState: StateFlow<Resource<Client>> = _saveClientState.asStateFlow()

    fun saveClient(client: Client, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _saveClientState.value = Resource.Loading
            val res = adminRepository.saveClient(client)
            _saveClientState.value = res
            if (res is Resource.Success) {
                onSuccess()
            }
        }
    }

    fun deleteClient(clientId: String) {
        viewModelScope.launch {
            adminRepository.deleteClient(clientId)
        }
    }

    fun toggleClientActive(clientId: String, isActive: Boolean) {
        viewModelScope.launch {
            adminRepository.toggleClientActive(clientId, isActive)
        }
    }

    fun toggleClientVerified(clientId: String, isVerified: Boolean) {
        viewModelScope.launch {
            adminRepository.toggleClientVerified(clientId, isVerified)
        }
    }

    // Booking operations
    fun manuallyAssignWorker(bookingId: String, workerId: String) {
        viewModelScope.launch {
            adminRepository.manuallyAssignWorker(bookingId, workerId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            adminRepository.refreshAll()
        }
    }

    fun resetSaveState() {
        _saveWorkerState.value = Resource.Idle
        _saveClientState.value = Resource.Idle
        _saveRoleState.value = Resource.Idle
    }

    // Role operations
    private val _saveRoleState = MutableStateFlow<Resource<Unit>>(Resource.Idle)
    val saveRoleState: StateFlow<Resource<Unit>> = _saveRoleState.asStateFlow()

    fun saveRoleAssignment(phone: String, role: UserRole, name: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _saveRoleState.value = Resource.Loading
            val res = adminRepository.saveRoleAssignment(phone, role, name)
            _saveRoleState.value = res
            if (res is Resource.Success) {
                onSuccess()
            }
        }
    }

    fun removeRoleAssignment(phone: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _saveRoleState.value = Resource.Loading
            val res = adminRepository.removeRoleAssignment(phone)
            _saveRoleState.value = res
            if (res is Resource.Success) {
                onSuccess()
            }
        }
    }

    fun resetSaveRoleState() {
        _saveRoleState.value = Resource.Idle
    }
}
