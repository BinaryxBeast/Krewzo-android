package com.binaryxbeast.krewzo.core.model

enum class SkillCategory(val displayName: String) {
    LOADING_UNLOADING("Loading & Unloading"),
    CLEANING("Cleaning & Housekeeping"),
    SECURITY("Security Guard"),
    HOSPITALITY("Hospitality & Catering"),
    CONSTRUCTION("Construction & Masonry"),
    DELIVERY("Delivery & Logistics"),
    PACKAGING("Packaging & Assembly"),
    EVENT_STAFF("Event Management Staff"),
    OTHER("General Labor");

    companion object {
        fun fromString(value: String?): SkillCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

data class Worker(
    val id: String,
    val userId: String? = null,
    val phone: String,
    val fullName: String,
    val age: Int? = null,
    val dateOfBirth: String? = null,
    val photoUrl: String? = null,
    val homeAddress: String? = null,
    val isAvailable: Boolean = false,
    val isActive: Boolean = true,
    val totalJobsCompleted: Int = 0,
    val totalEarnings: Double = 0.0,
    val rating: Double? = 5.0,
    val skills: List<SkillCategory> = emptyList(),
    val emergencyContact: String? = null,
    val adminNotes: String? = null
) {
    /**
     * Resolves the worker's effective age, calculating from [dateOfBirth]
     * if [age] is null or unpopulated.
     */
    fun effectiveAge(): Int? {
        return age ?: calculateAgeFromDob(dateOfBirth)
    }

    companion object {
        fun calculateAgeFromDob(dobString: String?): Int? {
            if (dobString.isNullOrBlank()) return null
            return try {
                val parts = dobString.trim().split("-")
                if (parts.size == 3) {
                    val year = parts[0].toIntOrNull() ?: return null
                    val month = parts[1].toIntOrNull() ?: return null
                    val day = parts[2].toIntOrNull() ?: return null
                    val cal = java.util.Calendar.getInstance()
                    val curYear = cal.get(java.util.Calendar.YEAR)
                    val curMonth = cal.get(java.util.Calendar.MONTH) + 1
                    val curDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                    var calculated = curYear - year
                    if (curMonth < month || (curMonth == month && curDay < day)) {
                        calculated--
                    }
                    if (calculated in 0..120) calculated else null
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }
}
