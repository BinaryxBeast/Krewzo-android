package com.binaryxbeast.krewzo.core.model

data class Client(
    val id: String,
    val businessName: String,
    val contactPerson: String,
    val phone: String,
    val email: String? = null,
    val employeeCount: Int = 10,
    val address: String,
    val city: String = "Pune",
    val pincode: String = "411001",
    val latitude: Double? = 18.5204,
    val longitude: Double? = 73.8567,
    val gstin: String? = null,
    val isVerified: Boolean = true,
    val isActive: Boolean = true,
    val totalBookings: Int = 0,
    val notes: String? = null,
    val contactPersonRole: String? = null,
    val logoUrl: String? = null
)

