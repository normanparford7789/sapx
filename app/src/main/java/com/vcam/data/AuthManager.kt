package com.vcam.data

import android.content.Context
import com.vcam.data.models.UserProfile
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AuthManager {

    private val client get() = SupabaseClientProvider.client

    suspend fun signIn(email: String, password: String): Result<UserInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
                client.auth.currentUserOrNull() ?: error("Login failed")
            }
        }

    suspend fun signUp(email: String, password: String, fullName: String): Result<UserInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                    data = kotlinx.serialization.json.buildJsonObject {
                        put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
                    }
                }
                // Create profile record
                val user = client.auth.currentUserOrNull() ?: error("Signup failed")
                runCatching {
                    client.from("profiles").insert(
                        UserProfile(
                            id = user.id,
                            email = email,
                            fullName = fullName
                        )
                    )
                }
                user
            }
        }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        runCatching { client.auth.signOut() }
    }

    fun currentUser(): UserInfo? = client.auth.currentUserOrNull()

    fun isLoggedIn(): Boolean = client.auth.currentUserOrNull() != null

    suspend fun getUserProfile(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        runCatching {
            client.from("profiles").select {
                filter { eq("id", userId) }
            }.decodeSingle<UserProfile>()
        }.getOrNull()
    }

    suspend fun getCurrentProfile(): UserProfile? {
        val user = currentUser() ?: return null
        return getUserProfile(user.id)
    }

    suspend fun isAdmin(): Boolean {
        return getCurrentProfile()?.isAdmin ?: false
    }

    // Save session for offline reloads
    fun saveSession(context: Context) {
        val prefs = context.getSharedPreferences("vcam_auth", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("logged_in", isLoggedIn()).apply()
    }
}
