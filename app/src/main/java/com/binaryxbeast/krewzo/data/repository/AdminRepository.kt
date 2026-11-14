package com.binaryxbeast.krewzo.data.repository

import android.util.Log
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.Booking
import com.binaryxbeast.krewzo.core.model.BookingStatus
import com.binaryxbeast.krewzo.core.model.Client
import com.binaryxbeast.krewzo.core.model.Worker
import com.binaryxbeast.krewzo.core.model.UserRole
import com.binaryxbeast.krewzo.core.network.KrewzoSupabaseClient
import com.binaryxbeast.krewzo.core.network.dto.BookingDto
import com.binaryxbeast.krewzo.core.network.dto.ClientDto
import com.binaryxbeast.krewzo.core.network.dto.WorkerDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class AdminDashboardMetrics(
    val totalWorkers: Int = 0,
    val activeWorkers: Int = 0,
    val totalClients: Int = 0,
    val verifiedClients: Int = 0,
    val bookingsToday: Int = 0,
    val totalVolumeRupees: Double = 0.0,
    val fillRatePercent: Double = 0.0,
    val searchingBookingsCount: Int = 0
)

interface AdminRepository {
    fun getDashboardMetrics(): Flow<AdminDashboardMetrics>

    // Worker operations
    fun getAllWorkers(): Flow<List<Worker>>
    suspend fun saveWorker(worker: Worker): Resource<Worker>
    suspend fun uploadWorkerPhoto(workerId: String, imageBytes: ByteArray): Resource<String>
    suspend fun uploadWorkerDocument(workerId: String, docType: String, side: String, imageBytes: ByteArray): Resource<String>
    suspend fun checkPhoneExists(phone: String): Resource<Boolean>
    suspend fun saveWorkerWithOnboarding(
        worker: Worker,
        photoBytes: ByteArray?,
        aadhaarFrontBytes: ByteArray?,
        aadhaarBackBytes: ByteArray?,
        aadhaarNumber: String?
    ): Resource<Worker>
    suspend fun deleteWorker(workerId: String): Resource<Unit>
    suspend fun toggleWorkerActive(workerId: String, isActive: Boolean): Resource<Unit>

    // Client operations
    fun getAllClients(): Flow<List<Client>>
    suspend fun saveClient(client: Client): Resource<Client>
    suspend fun deleteClient(clientId: String): Resource<Unit>
    suspend fun toggleClientActive(clientId: String, isActive: Boolean): Resource<Unit>
    suspend fun toggleClientVerified(clientId: String, isVerified: Boolean): Resource<Unit>

    // Booking operations
    fun getAllBookings(): Flow<List<Booking>>
    suspend fun manuallyAssignWorker(bookingId: String, workerId: String): Resource<Unit>

    // Role management operations
    fun getAllRoleAssignments(): Flow<List<RoleAssignment>>
    suspend fun saveRoleAssignment(phone: String, role: UserRole, name: String? = null): Resource<Unit>
    suspend fun removeRoleAssignment(phone: String): Resource<Unit>

    // Direct synchronization
    suspend fun refreshAll()
}

class AdminRepositoryImpl : AdminRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val supabase = KrewzoSupabaseClient.client

    private val _workers = MutableStateFlow<List<Worker>>(emptyList())
    private val _clients = MutableStateFlow<List<Client>>(emptyList())
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())

    init {
        scope.launch {
            refreshAll()
        }
    }

    override suspend fun refreshAll() {
        refreshWorkers()
        refreshClients()
        refreshBookings()
    }

    private suspend fun refreshWorkers() {
        try {
            val dtos = supabase.from("workers")
                .select()
                .decodeList<WorkerDto>()
            val domainWorkers = dtos.map { it.toDomain() }
            _workers.value = domainWorkers
            Log.d("AdminRepository", "Loaded ${domainWorkers.size} workers from Supabase")
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to fetch workers from Supabase", e)
        }
    }

    private suspend fun refreshClients() {
        try {
            val dtos = supabase.from("clients")
                .select()
                .decodeList<ClientDto>()
            val domainClients = dtos.map { it.toDomain() }
            _clients.value = domainClients
            Log.d("AdminRepository", "Loaded ${domainClients.size} clients from Supabase")
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to fetch clients from Supabase", e)
        }
    }

    private suspend fun refreshBookings() {
        try {
            val dtos = supabase.from("bookings")
                .select()
                .decodeList<BookingDto>()
            val domainBookings = dtos.map { it.toDomain() }
            _bookings.value = domainBookings
            Log.d("AdminRepository", "Loaded ${domainBookings.size} bookings from Supabase")
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to fetch bookings from Supabase", e)
        }
    }

    override fun getDashboardMetrics(): Flow<AdminDashboardMetrics> {
        return combine(_workers, _clients, _bookings) { workers, clients, bookings ->
            val totalNeeded = bookings.sumOf { it.workersNeeded }
            val totalAccepted = bookings.sumOf { it.workersAccepted }
            val fillRate = if (totalNeeded > 0) (totalAccepted.toDouble() / totalNeeded * 100) else 0.0
            val volume = bookings.sumOf { it.totalCost }
            val searchingCount = bookings.count { it.status == BookingStatus.SEARCHING }

            AdminDashboardMetrics(
                totalWorkers = workers.size,
                activeWorkers = workers.count { it.isActive },
                totalClients = clients.size,
                verifiedClients = clients.count { it.isVerified },
                bookingsToday = bookings.size,
                totalVolumeRupees = volume,
                fillRatePercent = (fillRate * 10).toInt() / 10.0,
                searchingBookingsCount = searchingCount
            )
        }
    }

    // ── Worker Operations ──────────────────────────────────────────

    override fun getAllWorkers(): Flow<List<Worker>> = _workers.asStateFlow()

    override suspend fun saveWorker(worker: Worker): Resource<Worker> {
        return try {
            val validId = if (worker.id.isNotBlank() && isValidUuid(worker.id)) {
                worker.id
            } else {
                UUID.randomUUID().toString()
            }
            val target = worker.copy(id = validId)
            val dto = WorkerDto.fromDomain(target)

            supabase.from("workers").upsert(dto)

            _workers.update { list ->
                val index = list.indexOfFirst { it.id == target.id }
                if (index >= 0) {
                    list.toMutableList().apply { set(index, target) }
                } else {
                    listOf(target) + list
                }
            }
            Resource.Success(target)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error saving worker to Supabase", e)
            Resource.Error(e.localizedMessage ?: "Failed to save worker to Supabase")
        }
    }

    override suspend fun uploadWorkerPhoto(workerId: String, imageBytes: ByteArray): Resource<String> {
        return try {
            val fileName = "workers/${workerId}.jpg"
            val bucket = supabase.storage.from("avatars")
            bucket.upload(fileName, imageBytes) {
                upsert = true
            }
            val publicUrl = bucket.publicUrl(fileName)
            Resource.Success(publicUrl)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to upload worker photo to Supabase Storage", e)
            Resource.Error(e.localizedMessage ?: "Failed to upload worker photo")
        }
    }

    override suspend fun uploadWorkerDocument(
        workerId: String,
        docType: String,
        side: String,
        imageBytes: ByteArray
    ): Resource<String> {
        return try {
            val fileName = "documents/${workerId}_${docType}_${side}.jpg"
            // Upload to Supabase storage (worker-documents or avatars)
            val bucketName = try {
                val bucket = supabase.storage.from("worker-documents")
                bucket.upload(fileName, imageBytes) { upsert = true }
                "worker-documents"
            } catch (e: Exception) {
                Log.d("AdminRepository", "Falling back to avatars bucket for doc: ${e.message}")
                val fallbackBucket = supabase.storage.from("avatars")
                fallbackBucket.upload(fileName, imageBytes) { upsert = true }
                "avatars"
            }
            val publicUrl = supabase.storage.from(bucketName).publicUrl(fileName)
            Resource.Success(publicUrl)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Failed to upload document ($side) to Supabase Storage", e)
            Resource.Error(e.localizedMessage ?: "Failed to upload document ($side)")
        }
    }

    override suspend fun checkPhoneExists(phone: String): Resource<Boolean> {
        val clean = phone.filter { it.isDigit() }.takeLast(10)
        if (clean.length != 10) {
            return Resource.Success(false)
        }

        // 1. Check local in-memory workers
        if (_workers.value.any { it.phone.filter { ch -> ch.isDigit() }.takeLast(10) == clean }) {
            return Resource.Success(true)
        }

        // 2. Check local role registry
        val assignment = UserRoleRegistry.getAssignment(clean)
        if (assignment != null && (assignment.role == UserRole.WORKER.name || assignment.role == UserRole.ADMIN.name)) {
            return Resource.Success(true)
        }

        // 3. Query Supabase workers table
        try {
            val dtos = supabase.from("workers")
                .select {
                    filter {
                        or {
                            eq("phone", clean)
                            eq("phone", "+91$clean")
                            eq("phone", "+91 $clean")
                        }
                    }
                }
                .decodeList<WorkerDto>()
            if (dtos.isNotEmpty()) {
                return Resource.Success(true)
            }
        } catch (e: Exception) {
            Log.w("AdminRepository", "Supabase workers phone check error: ${e.message}")
        }

        // 4. Query Supabase users table
        try {
            val users = supabase.from("users")
                .select {
                    filter {
                        or {
                            eq("phone", clean)
                            eq("phone", "+91$clean")
                            eq("phone", "+91 $clean")
                        }
                    }
                }
            val raw = users.data
            if (raw != "[]" && raw.isNotBlank()) {
                return Resource.Success(true)
            }
        } catch (e: Exception) {
            Log.w("AdminRepository", "Supabase users phone check error: ${e.message}")
        }

        return Resource.Success(false)
    }

    override suspend fun saveWorkerWithOnboarding(
        worker: Worker,
        photoBytes: ByteArray?,
        aadhaarFrontBytes: ByteArray?,
        aadhaarBackBytes: ByteArray?,
        aadhaarNumber: String?
    ): Resource<Worker> {
        return try {
            val validId = if (worker.id.isNotBlank() && isValidUuid(worker.id)) {
                worker.id
            } else {
                UUID.randomUUID().toString()
            }

            var uploadedPhotoUrl = worker.photoUrl
            if (photoBytes != null) {
                when (val photoRes = uploadWorkerPhoto(validId, photoBytes)) {
                    is Resource.Success -> uploadedPhotoUrl = photoRes.data
                    is Resource.Error -> Log.w("AdminRepository", "Photo upload warning: ${photoRes.message}")
                    else -> {}
                }
            }

            var aadhaarFrontUrl: String? = null
            if (aadhaarFrontBytes != null) {
                when (val docRes = uploadWorkerDocument(validId, "aadhaar", "front", aadhaarFrontBytes)) {
                    is Resource.Success -> aadhaarFrontUrl = docRes.data
                    is Resource.Error -> Log.w("AdminRepository", "Aadhaar front upload warning: ${docRes.message}")
                    else -> {}
                }
            }

            var aadhaarBackUrl: String? = null
            if (aadhaarBackBytes != null) {
                when (val docRes = uploadWorkerDocument(validId, "aadhaar", "back", aadhaarBackBytes)) {
                    is Resource.Success -> aadhaarBackUrl = docRes.data
                    is Resource.Error -> Log.w("AdminRepository", "Aadhaar back upload warning: ${docRes.message}")
                    else -> {}
                }
            }

            // Build documentation notes
            val notesParts = mutableListOf<String>()
            if (!aadhaarNumber.isNullOrBlank()) notesParts.add("Aadhaar No: $aadhaarNumber")
            if (!aadhaarFrontUrl.isNullOrBlank()) notesParts.add("Aadhaar Front: $aadhaarFrontUrl")
            if (!aadhaarBackUrl.isNullOrBlank()) notesParts.add("Aadhaar Back: $aadhaarBackUrl")
            if (!worker.adminNotes.isNullOrBlank()) notesParts.add(worker.adminNotes)
            val combinedNotes = notesParts.joinToString(" | ")

            val finalWorker = worker.copy(
                id = validId,
                photoUrl = uploadedPhotoUrl,
                adminNotes = combinedNotes.ifBlank { null },
                isActive = true,
                isAvailable = true
            )

            val dto = WorkerDto.fromDomain(finalWorker)
            supabase.from("workers").upsert(dto)

            // Register role so worker can authenticate
            UserRoleRegistry.setRole(finalWorker.phone, UserRole.WORKER, finalWorker.fullName)

            // Update in-memory state
            _workers.update { list ->
                val index = list.indexOfFirst { it.id == finalWorker.id }
                if (index >= 0) {
                    list.toMutableList().apply { set(index, finalWorker) }
                } else {
                    listOf(finalWorker) + list
                }
            }

            Resource.Success(finalWorker)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error completing worker onboarding in Supabase", e)
            Resource.Error(e.localizedMessage ?: "Failed to save worker to Supabase")
        }
    }

    override suspend fun deleteWorker(workerId: String): Resource<Unit> {
        return try {
            supabase.from("workers").delete {
                filter {
                    eq("id", workerId)
                }
            }
            _workers.update { list -> list.filterNot { it.id == workerId } }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error deleting worker from Supabase", e)
            Resource.Error(e.localizedMessage ?: "Failed to delete worker")
        }
    }

    override suspend fun toggleWorkerActive(workerId: String, isActive: Boolean): Resource<Unit> {
        return try {
            supabase.from("workers").update(
                {
                    set("is_active", isActive)
                }
            ) {
                filter {
                    eq("id", workerId)
                }
            }
            _workers.update { list ->
                list.map { if (it.id == workerId) it.copy(isActive = isActive) else it }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error updating worker active status", e)
            Resource.Error(e.localizedMessage ?: "Failed to toggle worker active status")
        }
    }

    // ── Client Operations ──────────────────────────────────────────

    override fun getAllClients(): Flow<List<Client>> = _clients.asStateFlow()

    override suspend fun saveClient(client: Client): Resource<Client> {
        return try {
            val validId = if (client.id.isNotBlank() && isValidUuid(client.id)) {
                client.id
            } else {
                UUID.randomUUID().toString()
            }
            val target = client.copy(id = validId)
            val dto = ClientDto.fromDomain(target)

            supabase.from("clients").upsert(dto)

            _clients.update { list ->
                val index = list.indexOfFirst { it.id == target.id }
                if (index >= 0) {
                    list.toMutableList().apply { set(index, target) }
                } else {
                    listOf(target) + list
                }
            }
            Resource.Success(target)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error saving client to Supabase", e)
            Resource.Error(e.localizedMessage ?: "Failed to save client to Supabase")
        }
    }

    override suspend fun deleteClient(clientId: String): Resource<Unit> {
        return try {
            supabase.from("clients").delete {
                filter {
                    eq("id", clientId)
                }
            }
            _clients.update { list -> list.filterNot { it.id == clientId } }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error deleting client from Supabase", e)
            Resource.Error(e.localizedMessage ?: "Failed to delete client")
        }
    }

    override suspend fun toggleClientActive(clientId: String, isActive: Boolean): Resource<Unit> {
        return try {
            supabase.from("clients").update(
                {
                    set("is_active", isActive)
                }
            ) {
                filter {
                    eq("id", clientId)
                }
            }
            _clients.update { list ->
                list.map { if (it.id == clientId) it.copy(isActive = isActive) else it }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error toggling client active state", e)
            Resource.Error(e.localizedMessage ?: "Failed to toggle client active")
        }
    }

    override suspend fun toggleClientVerified(clientId: String, isVerified: Boolean): Resource<Unit> {
        return try {
            supabase.from("clients").update(
                {
                    set("is_verified", isVerified)
                }
            ) {
                filter {
                    eq("id", clientId)
                }
            }
            _clients.update { list ->
                list.map { if (it.id == clientId) it.copy(isVerified = isVerified) else it }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Log.e("AdminRepository", "Error toggling client verified state", e)
            Resource.Error(e.localizedMessage ?: "Failed to toggle client verification")
        }
    }

    // ── Booking Operations ─────────────────────────────────────────

    override fun getAllBookings(): Flow<List<Booking>> = _bookings.asStateFlow()

    override suspend fun manuallyAssignWorker(bookingId: String, workerId: String): Resource<Unit> {
        return try {
            _bookings.update { list ->
                list.map { b ->
                    if (b.id == bookingId) b.copy(workersAccepted = b.workersAccepted + 1) else b
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to manually assign worker")
        }
    }

    // ── Role Management Operations ──────────────────────────────────

    override fun getAllRoleAssignments(): Flow<List<RoleAssignment>> {
        return UserRoleRegistry.assignmentsFlow.map { map ->
            map.values.sortedWith(
                compareByDescending<RoleAssignment> { it.isPrimaryAdmin }
                    .thenBy { it.role }
                    .thenBy { it.name ?: it.phone }
            )
        }
    }

    override suspend fun saveRoleAssignment(phone: String, role: UserRole, name: String?): Resource<Unit> {
        return UserRoleRegistry.setRole(phone, role, name)
    }

    override suspend fun removeRoleAssignment(phone: String): Resource<Unit> {
        return UserRoleRegistry.removeAssignment(phone)
    }

    private fun isValidUuid(value: String): Boolean {
        return try {
            UUID.fromString(value)
            true
        } catch (_: Exception) {
            false
        }
    }
}
