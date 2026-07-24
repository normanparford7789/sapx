package com.vcam.ui.admin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vcam.data.SubscriptionManager
import com.vcam.databinding.ActivityAdminStatsBinding
import kotlinx.coroutines.launch

class AdminStatsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminStatsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminStatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnRefresh.setOnClickListener { loadStats() }
        loadStats()
    }

    private fun loadStats() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        lifecycleScope.launch {
            val stats = SubscriptionManager.getAdminStats()
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE

                binding.tvTotalUsers.text = stats.totalUsers.toString()
                binding.tvActiveSubs.text = stats.activeSubscriptions.toString()
                binding.tvPendingRequests.text = stats.pendingRequests.toString()
                binding.tvTotalRevenue.text = "$%.2f".format(stats.totalRevenue)
                binding.tvBannedUsers.text = stats.bannedUsers.toString()
                binding.tvTotalRequests.text = stats.totalRequests.toString()
            }
        }
    }
}
