package com.vcam.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.vcam.BuildConfig
import com.vcam.R
import com.vcam.data.AuthManager
import com.vcam.data.SupabaseClientProvider
import com.vcam.databinding.ActivityLoginBinding
import com.vcam.ui.MainActivity
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        credentialManager = CredentialManager.create(this)

        // If already logged in, go to main
        if (AuthManager.isLoggedIn()) {
            goToMain()
            return
        }

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.btnGoogleSignIn.setOnClickListener { doGoogleSignIn() }
        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_LONG).show()
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            val result = AuthManager.signIn(email, password)
            setLoading(false)
            result.fold(
                onSuccess = { goToMain() },
                onFailure = {
                    showError(it.message ?: getString(R.string.login_failed))
                }
            )
        }
    }

    private fun doGoogleSignIn() {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isEmpty()) {
            Toast.makeText(this, "Google Sign-In غير مفعّل", Toast.LENGTH_SHORT).show()
            return
        }
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        setLoading(true)
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                SupabaseClientProvider.client.auth.signInWith(IDToken) {
                    this.idToken = googleIdToken
                    this.provider = Google
                }
                val user = SupabaseClientProvider.client.auth.currentUserOrNull()
                if (user != null) {
                    // Ensure profile exists
                    runCatching {
                        com.vcam.data.SubscriptionManager // load object
                    }
                    goToMain()
                } else {
                    setLoading(false)
                    showError(getString(R.string.login_failed))
                }
            } catch (e: GetCredentialException) {
                setLoading(false)
                showError("Google Sign-In فشل: ${e.message}")
            } catch (e: Exception) {
                setLoading(false)
                showError(e.message ?: getString(R.string.login_failed))
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
        binding.btnGoogleSignIn.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
