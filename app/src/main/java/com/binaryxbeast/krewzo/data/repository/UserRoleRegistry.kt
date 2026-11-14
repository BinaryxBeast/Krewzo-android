package com.binaryxbeast.krewzo.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.UserRole
import com.binaryxbeast.krewzo.core.network.KrewzoSupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RoleAssignment(
    val phone: String,
    val role: String, // String for serialization: "ADMIN", "WORKER", "CLIENT"
    val name: String? = null,
    val isPrimaryAdmin: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val userRole: UserRole get() = UserRole.fromString(role)
}

/**
 * Centralized registry for phone-to-role mappings.
 * Supports:
 * 1. Default Super Admin: 8275976150
 * 2. Dynamic role assignment by Admin from the UI
 * 3. Fallback: Any number not designated as Admin or Worker defaults to CLIENT
 */
object UserRoleRegistry {

    const val PRIMARY_ADMIN_PHONE = "8275976150"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var prefs: SharedPreferences? = null
    private val json = Json { ignoreUnknownKeys = true }

    // Seed initial assignments
    private val defaultAssignments = mapOf(
        PRIMARY_ADMIN_PHONE to RoleAssignment(
            phone = PRIMARY_ADMIN_PHONE,
            role = UserRole.ADMIN.name,
            name = "Super Admin (Owner)",
            isPrimaryAdmin = true
        )
    )

    private val _assignmentsFlow = MutableStateFlow<Map<String, RoleAssignment>>(defaultAssignments)
    val assignmentsFlow: Flow<Map<String, RoleAssignment>> = _assignmentsFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.applicationContext.getSharedPreferences("krewzo_role_registry", Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val sp = prefs ?: return
        val rawJson = sp.getString("roles_map", null)
        val loaded = mutableMapOf<String, RoleAssignment>()
        loaded.putAll(defaultAssignments)

        if (!rawJson.isNullOrBlank()) {
            try {
                val list = json.decodeFromString<List<RoleAssignment>>(rawJson)
                list.forEach { item ->
                    val clean = item.phone.filter { it.isDigit() }.takeLast(10)
                    loaded[clean] = item.copy(phone = clean)
                }
            } catch (e: Exception) {
                Log.e("UserRoleRegistry", "Failed to deserialize stored roles", e)
            }
        }

        // Always ensure primary admin cannot be removed or demoted
        loaded[PRIMARY_ADMIN_PHONE] = RoleAssignment(
            phone = PRIMARY_ADMIN_PHONE,
            role = UserRole.ADMIN.name,
            name = "Super Admin (Owner)",
            isPrimaryAdmin = true
        )

        _assignmentsFlow.value = loaded
    }

    private fun persistToPrefs() {
        val sp = prefs ?: return
        val list = _assignmentsFlow.value.values.toList()
        try {
            val raw = json.encodeToString(list)
            sp.edit().putString("roles_map", raw).apply()
        } catch (e: Exception) {
            Log.e("UserRoleRegistry", "Failed to persist roles", e)
        }
    }

    /**
     * Resolves the role for a mobile number.
     * 1. If 8275976150 -> ALWAYS ADMIN
     * 2. If designated in registry -> Returns that role
     * 3. Else -> CLIENT
     */
    fun getRole(phone: String): UserRole {
        val clean = phone.filter { it.isDigit() }.takeLast(10)
        if (clean == PRIMARY_ADMIN_PHONE) return UserRole.ADMIN

        val assignment = _assignmentsFlow.value[clean]
        return assignment?.userRole ?: UserRole.CLIENT
    }

    fun getAssignment(phone: String): RoleAssignment? {
        val clean = phone.filter { it.isDigit() }.takeLast(10)
        return _assignmentsFlow.value[clean]
    }

    fun getAllAssignments(): List<RoleAssignment> {
        return _assignmentsFlow.value.values.sortedWith(
            compareByDescending<RoleAssignment> { it.isPrimaryAdmin }
                .thenBy { it.role }
                .thenBy { it.name ?: it.phone }
        )
    }

    suspend fun setRole(phone: String, role: UserRole, name: String? = null): Resource<Unit> {
        val clean = phone.filter { it.isDigit() }.takeLast(10)
        if (clean.length != 10) {
            return Resource.Error("Please enter a valid 10-digit mobile number")
        }

        if (clean == PRIMARY_ADMIN_PHONE && role != UserRole.ADMIN) {
            return Resource.Error("8275976150 is the primary super admin and cannot be demoted")
        }

        val existing = _assignmentsFlow.value[clean]
        val updated = RoleAssignment(
            phone = clean,
            role = role.name,
            name = name?.takeIf { it.isNotBlank() } ?: existing?.name ?: if (role == UserRole.ADMIN) "Admin User" else if (role == UserRole.WORKER) "Worker" else "Client",
            isPrimaryAdmin = clean == PRIMARY_ADMIN_PHONE,
            updatedAt = System.currentTimeMillis()
        )

        val newMap = _assignmentsFlow.value.toMutableMap()
        newMap[clean] = updated
        _assignmentsFlow.value = newMap
        persistToPrefs()

        // Sync to Supabase in background if possible
        scope.launch {
            try {
                KrewzoSupabaseClient.client.from("users")
                    .update(
                        {
                            set("role", role.name.lowercase())
                        }
                    ) {
                        filter {
                            eq("phone", clean)
                        }
                    }
                Log.d("UserRoleRegistry", "Synced role change for $clean to Supabase: $role")
            } catch (e: Exception) {
                Log.d("UserRoleRegistry", "Remote Supabase sync skipped or failed (offline/sandbox): ${e.message}")
            }
        }

        return Resource.Success(Unit)
    }

    suspend fun removeAssignment(phone: String): Resource<Unit> {
        val clean = phone.filter { it.isDigit() }.takeLast(10)
        if (clean == PRIMARY_ADMIN_PHONE) {
            return Resource.Error("Primary super admin cannot be removed")
        }

        val newMap = _assignmentsFlow.value.toMutableMap()
        newMap.remove(clean)
        _assignmentsFlow.value = newMap
        persistToPrefs()
        return Resource.Success(Unit)
    }
}
