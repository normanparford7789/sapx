package com.vcam.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("is_admin") val isAdmin: Boolean = false,
    @SerialName("is_banned") val isBanned: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)
