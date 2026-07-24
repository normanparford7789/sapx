package com.vcam.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vcam.data.SubscriptionManager
import com.vcam.databinding.ActivityAdminDashboardBinding
import kotlinx.coroutines.launch

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.cardRequests.setOnClickListener {
            startActivity(Intent(this, AdminSubscriptionRequestsActivity::class.java))
        }
        binding.cardUsers.setOnClickListener {
            startActivity(Intent(this, AdminUsersActivity::class.java))
        }
        binding.cardPayments.setOnClickListener {
            startActivity(Intent(this, AdminPaymentMethodsActivity::class.java))
        }
        binding.cardStats.setOnClickListener {
            startActivity(Intent(this, AdminStatsActivity::class.java))
        }

        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun loadStats() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val stats = SubscriptionManager.getAdminStats()
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                binding.tvTotalUsers.text = stats.totalUsers.toString()
                binding.tvActiveSubs.text = stats.activeSubscriptions.toString()
                binding.tvPendingRequests.text = stats.pendingRequests.toString()
                binding.tvTotalRevenue.text = "$%.2f".format(stats.totalRevenue)
            }
        }
    }
}
