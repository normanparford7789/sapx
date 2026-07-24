package com.vcam.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActiveSubscription(
    @SerialName("id") val id: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("plan_id") val planId: Int,
    @SerialName("status") val status: String,
    @SerialName("starts_at") val startsAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("subscription_plans") val plan: SubscriptionPlan? = null
)

@Serializable
data class NewSubscription(
    @SerialName("user_id") val userId: String,
    @SerialName("plan_id") val planId: Int,
    @SerialName("status") val status: String = "active",
    @SerialName("starts_at") val startsAt: String,
    @SerialName("expires_at") val expiresAt: String? = null
)
