package com.vcam.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vcam.R
import com.vcam.data.AuthManager
import com.vcam.data.SubscriptionManager
import com.vcam.databinding.ActivityAccountBinding
import com.vcam.ui.admin.AdminDashboardActivity
import com.vcam.ui.auth.LoginActivity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AccountActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAccountBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSubscribe.setOnClickListener {
            startActivity(Intent(this, SubscriptionPlansActivity::class.java))
        }
        binding.btnLogout.setOnClickListener { confirmLogout() }
        binding.btnAdminPanel.setOnClickListener {
            startActivity(Intent(this, AdminDashboardActivity::class.java))
        }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        setLoading(true)
        lifecycleScope.launch {
            val user = AuthManager.currentUser()
            if (user == null) {
                startActivity(Intent(this@AccountActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
                return@launch
            }

            val profile = AuthManager.getUserProfile(user.id)
            val sub = SubscriptionManager.getActiveSubscription(user.id)

            runOnUiThread {
                setLoading(false)

                // User info
                binding.tvEmail.text = profile?.email ?: user.email ?: "—"
                binding.tvName.text = profile?.fullName?.ifEmpty { null } ?: getString(R.string.no_name)
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())
                binding.tvJoined.text = profile?.createdAt?.let {
                    runCatching { formatter.format(Instant.parse(it)) }.getOrDefault("—")
                } ?: "—"

                // Admin panel visibility
                binding.btnAdminPanel.visibility = if (profile?.isAdmin == true) View.VISIBLE else View.GONE
                binding.adminDivider.visibility = binding.btnAdminPanel.visibility

                // Subscription status
                if (sub != null) {
                    binding.cardSubStatus.setCardBackgroundColor(getColor(R.color.color_success_bg))
                    binding.tvSubStatus.text = getString(R.string.subscription_active)
                    binding.tvSubStatus.setTextColor(getColor(R.color.color_success))
                    binding.tvPlanName.text = sub.plan?.nameAr ?: sub.plan?.name ?: "—"
                    binding.tvSubExpiry.text = if (sub.expiresAt == null) {
                        getString(R.string.subscription_permanent)
                    } else {
                        getString(R.string.expires_on, formatter.format(Instant.parse(sub.expiresAt)))
                    }
                    binding.btnSubscribe.text = getString(R.string.renew_subscription)
                } else {
                    binding.cardSubStatus.setCardBackgroundColor(getColor(R.color.color_warning_bg))
                    binding.tvSubStatus.text = getString(R.string.no_active_subscription)
                    binding.tvSubStatus.setTextColor(getColor(R.color.color_warning))
                    binding.tvPlanName.text = "—"
                    binding.tvSubExpiry.text = getString(R.string.subscribe_to_use)
                    binding.btnSubscribe.text = getString(R.string.subscribe_now)
                }
            }
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.logout))
            .setMessage(getString(R.string.logout_confirm))
            .setPositiveButton(getString(R.string.yes)) { _, _ -> doLogout() }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun doLogout() {
        lifecycleScope.launch {
            AuthManager.signOut()
            startActivity(Intent(this@AccountActivity, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.contentScroll.visibility = if (loading) View.GONE else View.VISIBLE
    }
}
