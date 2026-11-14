package com.binaryxbeast.krewzo.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    CLIENT,
    WORKER,
    ADMIN;

    companion object {
        fun fromString(value: String?): UserRole {
            return when (value?.lowercase()) {
                "worker" -> WORKER
                "admin" -> ADMIN
                else -> CLIENT
            }
        }
    }
}

data class UserProfile(
    val id: String,
    val phone: String,
    val role: UserRole,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class UserSession(
    val userId: String,
    val phone: String,
    val role: UserRole,
    val isOnboarded: Boolean,
    val clientId: String? = null,
    val workerId: String? = null,
    val businessName: String? = null,
    val fullName: String? = null
)

data class AdminProfile(
    val id: String = "admin_01",
    val phone: String = "8275976150",
    val fullName: String = "Super Admin (Owner)",
    val email: String = "admin@krewzo.com",
    val avatarUrl: String? = null,
    val roleTitle: String = "Super Administrator",
    val operatingHub: String = "Pune Operations Hub",
    val isActive: Boolean = true
)

