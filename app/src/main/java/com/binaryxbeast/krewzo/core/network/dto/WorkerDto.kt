package com.binaryxbeast.krewzo.core.network.dto

import com.binaryxbeast.krewzo.core.model.SkillCategory
import com.binaryxbeast.krewzo.core.model.Worker
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkerDto(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("phone") val phone: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("age") val age: Int? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("home_address") val homeAddress: String? = null,
    @SerialName("is_available") val isAvailable: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("total_jobs_completed") val totalJobsCompleted: Int = 0,
    @SerialName("total_earnings") val totalEarnings: Double = 0.0,
    @SerialName("rating") val rating: Double? = 5.0,
    @SerialName("skills") val skills: List<String> = emptyList(),
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    @SerialName("admin_notes") val adminNotes: String? = null
) {
    fun toDomain(): Worker {
        return Worker(
            id = id ?: "",
            userId = userId,
            phone = phone,
            fullName = fullName,
            age = age,
            dateOfBirth = dateOfBirth,
            photoUrl = photoUrl,
            homeAddress = homeAddress,
            isAvailable = isAvailable,
            isActive = isActive,
            totalJobsCompleted = totalJobsCompleted,
            totalEarnings = totalEarnings,
            rating = rating,
            skills = skills.map { SkillCategory.fromString(it) },
            emergencyContact = emergencyContact,
            adminNotes = adminNotes
        )
    }

    companion object {
        fun fromDomain(worker: Worker): WorkerDto {
            return WorkerDto(
                id = worker.id.ifBlank { null },
                userId = worker.userId,
                phone = worker.phone,
                fullName = worker.fullName,
                age = worker.age,
                dateOfBirth = worker.dateOfBirth,
                photoUrl = worker.photoUrl,
                homeAddress = worker.homeAddress,
                isAvailable = worker.isAvailable,
                isActive = worker.isActive,
                totalJobsCompleted = worker.totalJobsCompleted,
                totalEarnings = worker.totalEarnings,
                rating = worker.rating,
                skills = worker.skills.map { it.name },
                emergencyContact = worker.emergencyContact,
                adminNotes = worker.adminNotes
            )
        }
    }
}
