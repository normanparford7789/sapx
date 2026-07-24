package com.vcam.data.models

data class AdminStats(
    val totalUsers: Int = 0,
    val activeSubscriptions: Int = 0,
    val pendingRequests: Int = 0,
    val totalRevenue: Double = 0.0,
    val bannedUsers: Int = 0,
    val totalRequests: Int = 0
)
