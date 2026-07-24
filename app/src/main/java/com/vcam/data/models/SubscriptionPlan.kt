package com.vcam.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionPlan(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("name_ar") val nameAr: String? = null,
    @SerialName("duration_days") val durationDays: Int? = null, // null = permanent
    @SerialName("price") val price: Double,
    @SerialName("currency") val currency: String = "USD",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)
