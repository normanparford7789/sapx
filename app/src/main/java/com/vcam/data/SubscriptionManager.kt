package com.vcam.data

import com.vcam.data.models.ActiveSubscription
import com.vcam.data.models.AdminStats
import com.vcam.data.models.NewSubscription
import com.vcam.data.models.PaymentMethod
import com.vcam.data.models.SubscriptionPlan
import com.vcam.data.models.SubscriptionRequest
import com.vcam.data.models.SubscriptionRequestDetail
import com.vcam.data.models.UserProfile
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

object SubscriptionManager {

    private val client get() = SupabaseClientProvider.client

    // ── Plans ─────────────────────────────────────────────────────────────

    suspend fun getActivePlans(): List<SubscriptionPlan> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("subscription_plans").select {
                filter { eq("is_active", true) }
                order("price", Order.ASCENDING)
            }.decodeList<SubscriptionPlan>()
        }.getOrDefault(emptyList())
    }

    suspend fun getAllPlans(): List<SubscriptionPlan> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("subscription_plans").select {
                order("created_at", Order.DESCENDING)
            }.decodeList<SubscriptionPlan>()
        }.getOrDefault(emptyList())
    }

    suspend fun createPlan(plan: SubscriptionPlan): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("subscription_plans").insert(plan)
            Unit
        }
    }

    suspend fun updatePlanStatus(planId: Int, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("subscription_plans").update({
                set("is_active", isActive)
            }) {
                filter { eq("id", planId) }
            }
            Unit
        }
    }

    // ── Payment Methods ───────────────────────────────────────────────────

    suspend fun getActivePaymentMethods(): List<PaymentMethod> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("payment_methods").select {
                filter { eq("is_active", true) }
            }.decodeList<PaymentMethod>()
        }.getOrDefault(emptyList())
    }

    suspend fun getAllPaymentMethods(): List<PaymentMethod> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("payment_methods").select().decodeList<PaymentMethod>()
        }.getOrDefault(emptyList())
    }

    suspend fun createPaymentMethod(method: PaymentMethod): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { client.from("payment_methods").insert(method); Unit }
    }

    suspend fun updatePaymentMethod(method: PaymentMethod): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("payment_methods").update({
                set("name", method.name)
                set("name_ar", method.nameAr ?: "")
                set("address", method.address)
                set("instructions_ar", method.instructionsAr ?: "")
                set("is_active", method.isActive)
            }) {
                filter { eq("id", method.id) }
            }
            Unit
        }
    }

    // ── Subscription Requests ─────────────────────────────────────────────

    suspend fun submitRequest(request: SubscriptionRequest): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { client.from("subscription_requests").insert(request); Unit }
    }

    suspend fun getPendingRequests(): List<SubscriptionRequestDetail> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("subscription_requests").select {
                filter { eq("status", "pending") }
                order("created_at", Order.DESCENDING)
            }.decodeList<SubscriptionRequestDetail>()
        }.getOrDefault(emptyList())
    }

    suspend fun getAllRequests(): List<SubscriptionRequestDetail> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("subscription_requests").select {
                order("created_at", Order.DESCENDING)
            }.decodeList<SubscriptionRequestDetail>()
        }.getOrDefault(emptyList())
    }

    suspend fun getUserRequests(userId: String): List<SubscriptionRequest> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("subscription_requests").select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<SubscriptionRequest>()
        }.getOrDefault(emptyList())
    }

    suspend fun approveRequest(requestId: Int, userId: String, planId: Int, durationDays: Int?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Update request status
            client.from("subscription_requests").update({
                set("status", "approved")
                set("updated_at", Instant.now().toString())
            }) { filter { eq("id", requestId) } }

            // Deactivate old subscriptions
            client.from("subscriptions").update({
                set("status", "cancelled")
            }) {
                filter {
                    eq("user_id", userId)
                    eq("status", "active")
                }
            }

            // Create new subscription
            val now = Instant.now()
            val expiresAt = durationDays?.let {
                now.plus(it.toLong(), ChronoUnit.DAYS).toString()
            }
            client.from("subscriptions").insert(
                NewSubscription(
                    userId = userId,
                    planId = planId,
                    startsAt = now.toString(),
                    expiresAt = expiresAt
                )
            )
            Unit
        }
    }

    suspend fun rejectRequest(requestId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("subscription_requests").update({
                set("status", "rejected")
                set("updated_at", Instant.now().toString())
            }) { filter { eq("id", requestId) } }
            Unit
        }
    }

    // ── User Subscriptions ────────────────────────────────────────────────

    suspend fun getActiveSubscription(userId: String): ActiveSubscription? = withContext(Dispatchers.IO) {
        runCatching {
            val results = client.from("subscriptions").select {
                filter {
                    eq("user_id", userId)
                    eq("status", "active")
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }.decodeList<ActiveSubscription>()

            val sub = results.firstOrNull() ?: return@runCatching null

            // Check if expired
            sub.expiresAt?.let { expiresAt ->
                if (Instant.parse(expiresAt).isBefore(Instant.now())) {
                    // Mark as expired
                    client.from("subscriptions").update({
                        set("status", "expired")
                    }) { filter { eq("id", sub.id) } }
                    return@runCatching null
                }
            }
            sub
        }.getOrNull()
    }

    suspend fun hasActiveSubscription(userId: String): Boolean {
        return getActiveSubscription(userId) != null
    }

    // ── Admin: Users ──────────────────────────────────────────────────────

    suspend fun getAllUsers(): List<UserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("profiles").select {
                order("created_at", Order.DESCENDING)
            }.decodeList<UserProfile>()
        }.getOrDefault(emptyList())
    }

    suspend fun banUser(userId: String, ban: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("profiles").update({
                set("is_banned", ban)
            }) { filter { eq("id", userId) } }
            Unit
        }
    }

    suspend fun setAdmin(userId: String, isAdmin: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            client.from("profiles").update({
                set("is_admin", isAdmin)
            }) { filter { eq("id", userId) } }
            Unit
        }
    }

    suspend fun activateSubscriptionManually(userId: String, planId: Int, durationDays: Int?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Deactivate existing
            client.from("subscriptions").update({ set("status", "cancelled") }) {
                filter { eq("user_id", userId); eq("status", "active") }
            }
            val now = Instant.now()
            val expiresAt = durationDays?.let { now.plus(it.toLong(), ChronoUnit.DAYS).toString() }
            client.from("subscriptions").insert(
                NewSubscription(userId = userId, planId = planId, startsAt = now.toString(), expiresAt = expiresAt)
            )
            Unit
        }
    }

    // ── Admin: Stats ──────────────────────────────────────────────────────

    suspend fun getAdminStats(): AdminStats = withContext(Dispatchers.IO) {
        runCatching {
            val users = client.from("profiles").select().decodeList<UserProfile>()
            val subs = client.from("subscriptions").select {
                filter { eq("status", "active") }
            }.decodeList<ActiveSubscription>()
            val pending = client.from("subscription_requests").select {
                filter { eq("status", "pending") }
            }.decodeList<SubscriptionRequest>()
            val allRequests = client.from("subscription_requests").select {
                filter { eq("status", "approved") }
            }.decodeList<SubscriptionRequest>()

            AdminStats(
                totalUsers = users.size,
                activeSubscriptions = subs.size,
                pendingRequests = pending.size,
                totalRevenue = allRequests.sumOf { it.amount },
                bannedUsers = users.count { it.isBanned },
                totalRequests = allRequests.size
            )
        }.getOrDefault(AdminStats())
    }
}
