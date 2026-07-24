package com.vcam.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionRequest(
    @SerialName("id") val id: Int = 0,
    @SerialName("user_id") val userId: String,
    @SerialName("plan_id") val planId: Int,
    @SerialName("payment_method_id") val paymentMethodId: Int,
    @SerialName("transaction_number") val transactionNumber: String,
    @SerialName("amount") val amount: Double,
    @SerialName("status") val status: String = "pending",
    @SerialName("admin_note") val adminNote: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class SubscriptionRequestDetail(
    @SerialName("id") val id: Int,
    @SerialName("user_id") val userId: String,
    @SerialName("plan_id") val planId: Int,
    @SerialName("payment_method_id") val paymentMethodId: Int,
    @SerialName("transaction_number") val transactionNumber: String,
    @SerialName("amount") val amount: Double,
    @SerialName("status") val status: String,
    @SerialName("admin_note") val adminNote: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("profiles") val profile: ProfileRef? = null,
    @SerialName("subscription_plans") val plan: PlanRef? = null,
    @SerialName("payment_methods") val paymentMethod: PaymentMethodRef? = null
)

@Serializable
data class ProfileRef(
    @SerialName("email") val email: String,
    @SerialName("full_name") val fullName: String? = null
)

@Serializable
data class PlanRef(
    @SerialName("name") val name: String,
    @SerialName("name_ar") val nameAr: String? = null
)

@Serializable
data class PaymentMethodRef(
    @SerialName("name") val name: String,
    @SerialName("name_ar") val nameAr: String? = null
)
