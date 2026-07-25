package com.vcam.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vcam.R
import com.vcam.data.AuthManager
import com.vcam.databinding.ActivityRegisterBinding
import com.vcam.ui.MainActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { doRegister() }
        binding.tvGoLogin.setOnClickListener { finish() }
    }

    private fun doRegister() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        when {
            name.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                showError(getString(R.string.fill_all_fields))
                return
            }
            password != confirm -> {
                showError(getString(R.string.passwords_not_match))
                return
            }
            password.length < 6 -> {
                showError(getString(R.string.password_too_short))
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                showError(getString(R.string.invalid_email))
                return
            }
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = AuthManager.signUp(email, password, name)
            setLoading(false)
            result.fold(
                onSuccess = {
                    Toast.makeText(this@RegisterActivity, getString(R.string.account_created), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                },
                onFailure = {
                    showError(it.message ?: getString(R.string.register_failed))
                }
            )
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
        binding.etFullName.isEnabled = !loading
        binding.etEmail.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.etConfirmPassword.isEnabled = !loading
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }
}
