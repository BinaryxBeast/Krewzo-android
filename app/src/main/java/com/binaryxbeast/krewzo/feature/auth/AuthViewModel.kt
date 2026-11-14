package com.binaryxbeast.krewzo.feature.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binaryxbeast.krewzo.core.common.Resource
import com.binaryxbeast.krewzo.core.model.UserSession
import com.binaryxbeast.krewzo.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _phoneState = MutableStateFlow("")
    val phoneState: StateFlow<String> = _phoneState.asStateFlow()

    private val _otpState = MutableStateFlow("")
    val otpState: StateFlow<String> = _otpState.asStateFlow()

    private val _otpSendResult = MutableStateFlow<Resource<String>>(Resource.Idle)
    val otpSendResult: StateFlow<Resource<String>> = _otpSendResult.asStateFlow()

    private val _authResult = MutableStateFlow<Resource<UserSession>>(Resource.Idle)
    val authResult: StateFlow<Resource<UserSession>> = _authResult.asStateFlow()

    private val _onboardingResult = MutableStateFlow<Resource<UserSession>>(Resource.Idle)
    val onboardingResult: StateFlow<Resource<UserSession>> = _onboardingResult.asStateFlow()

    fun updatePhone(newPhone: String) {
        val filtered = newPhone.filter { it.isDigit() }.take(10)
        _phoneState.value = filtered
    }

    fun updateOtp(newOtp: String) {
        val filtered = newOtp.filter { it.isDigit() }.take(6)
        _otpState.value = filtered
    }

    fun sendOtp(activity: Activity? = null, onSuccess: (String) -> Unit = {}) {
        val phone = _phoneState.value.trim()
        if (phone.length < 10) {
            _otpSendResult.value = Resource.Error("Please enter a valid 10-digit mobile number")
            return
        }

        viewModelScope.launch {
            _otpSendResult.value = Resource.Loading
            val res = authRepository.sendPhoneOtp(phone, activity)
            _otpSendResult.value = res
            if (res is Resource.Success) {
                onSuccess(phone)
            }
        }
    }

    fun verifyOtp(phone: String, onSuccess: (UserSession) -> Unit) {
        val otp = _otpState.value.trim()
        if (otp.length != 6) {
            _authResult.value = Resource.Error("Please enter the 6-digit OTP code")
            return
        }

        viewModelScope.launch {
            _authResult.value = Resource.Loading
            val res = authRepository.verifyOtp(phone, otp)
            _authResult.value = res
            if (res is Resource.Success) {
                onSuccess(res.data)
            }
        }
    }

    fun completeOnboarding(
        businessName: String,
        employeeCount: Int,
        address: String,
        pincode: String,
        latitude: Double? = null,
        longitude: Double? = null,
        onSuccess: () -> Unit
    ) {
        if (businessName.isBlank() || address.isBlank()) {
            _onboardingResult.value = Resource.Error("Please fill in all required business details")
            return
        }

        viewModelScope.launch {
            _onboardingResult.value = Resource.Loading
            val res = authRepository.completeClientOnboarding(
                businessName = businessName,
                employeeCount = employeeCount,
                address = address,
                city = "Pune",
                pincode = pincode,
                latitude = latitude,
                longitude = longitude
            )
            _onboardingResult.value = res
            if (res is Resource.Success) {
                onSuccess()
            }
        }
    }

    fun resetAuthState() {
        _otpSendResult.value = Resource.Idle
        _authResult.value = Resource.Idle
        _onboardingResult.value = Resource.Idle
    }

    fun signOut(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.signOut()
            _phoneState.value = ""
            _otpState.value = ""
            resetAuthState()
            onSuccess()
        }
    }
}
