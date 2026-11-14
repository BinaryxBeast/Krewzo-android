package com.binaryxbeast.krewzo.core.common

sealed interface Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>
    data class Error(val message: String, val cause: Throwable? = null) : Resource<Nothing>
    data object Loading : Resource<Nothing>
    data object Idle : Resource<Nothing>
}

sealed interface UiEvent {
    data class ShowSnackbar(val message: String, val isError: Boolean = false) : UiEvent
    data class Navigate(val route: String, val popUpToRoute: String? = null, val inclusive: Boolean = false) : UiEvent
    data object NavigateUp : UiEvent
}

object PuneLocationDefaults {
    const val PUNE_LATITUDE = 18.5204
    const val PUNE_LONGITUDE = 73.8567
    const val PUNE_CITY_NAME = "Pune"
    const val PUNE_DEFAULT_PINCODE = "411001"
}
