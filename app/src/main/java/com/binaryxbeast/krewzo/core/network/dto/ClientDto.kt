package com.binaryxbeast.krewzo.core.network.dto

import com.binaryxbeast.krewzo.core.model.Client
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientDto(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("business_name") val businessName: String,
    @SerialName("contact_person") val contactPerson: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("employee_count") val employeeCount: Int? = 10,
    @SerialName("address") val address: String? = null,
    @SerialName("city") val city: String? = "Pune",
    @SerialName("pincode") val pincode: String? = "411001",
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("gst_number") val gstNumber: String? = null,
    @SerialName("billing_address") val billingAddress: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = true,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("notes") val notes: String? = null,
    @SerialName("contact_person_role") val contactPersonRole: String? = null,
    @SerialName("logo_url") val logoUrl: String? = null
) {
    fun toDomain(): Client {
        return Client(
            id = id ?: "",
            businessName = businessName,
            contactPerson = contactPerson ?: "",
            phone = phone ?: "",
            email = email,
            employeeCount = employeeCount ?: 10,
            address = address ?: billingAddress ?: "",
            city = city ?: "Pune",
            pincode = pincode ?: "411001",
            latitude = latitude ?: 18.5204,
            longitude = longitude ?: 73.8567,
            gstin = gstNumber,
            isVerified = isVerified,
            isActive = isActive,
            totalBookings = 0,
            notes = notes,
            contactPersonRole = contactPersonRole,
            logoUrl = logoUrl
        )
    }

    companion object {
        fun fromDomain(client: Client): ClientDto {
            return ClientDto(
                id = client.id.ifBlank { null },
                userId = null,
                businessName = client.businessName,
                contactPerson = client.contactPerson,
                phone = client.phone,
                email = client.email,
                employeeCount = client.employeeCount,
                address = client.address,
                billingAddress = client.address,
                city = client.city,
                pincode = client.pincode,
                latitude = client.latitude,
                longitude = client.longitude,
                gstNumber = client.gstin,
                isVerified = client.isVerified,
                isActive = client.isActive,
                notes = client.notes,
                contactPersonRole = client.contactPersonRole,
                logoUrl = client.logoUrl
            )
        }
    }
}

