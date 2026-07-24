package com.vcam.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethod(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("name_ar") val nameAr: String? = null,
    @SerialName("address") val address: String,
    @SerialName("instructions") val instructions: String? = null,
    @SerialName("instructions_ar") val instructionsAr: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)
