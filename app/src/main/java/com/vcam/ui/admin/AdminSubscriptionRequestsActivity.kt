package com.vcam.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.vcam.R
import com.vcam.data.SubscriptionManager
import com.vcam.data.models.SubscriptionRequestDetail
import com.vcam.databinding.ActivityAdminSubscriptionRequestsBinding
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class AdminSubscriptionRequestsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSubscriptionRequestsBinding
    private val requests = mutableListOf<SubscriptionRequestDetail>()
    private lateinit var adapter: RequestsAdapter
    private var showAll = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSubscriptionRequestsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = RequestsAdapter(requests,
            onApprove = { req -> confirmApprove(req) },
            onReject = { req -> confirmReject(req) }
        )
        binding.rvRequests.layoutManager = LinearLayoutManager(this)
        binding.rvRequests.adapter = adapter

        binding.chipPending.setOnClickListener { showAll = false; loadRequests() }
        binding.chipAll.setOnClickListener { showAll = true; loadRequests() }

        loadRequests()
    }

    override fun onResume() {
        super.onResume()
        loadRequests()
    }

    private fun loadRequests() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = if (showAll) SubscriptionManager.getAllRequests()
            else SubscriptionManager.getPendingRequests()
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                requests.clear()
                requests.addAll(result)
                adapter.notifyDataSetChanged()
                binding.tvEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun confirmApprove(req: SubscriptionRequestDetail) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.approve_request))
            .setMessage(getString(R.string.approve_confirm, req.profile?.email ?: req.userId))
            .setPositiveButton(getString(R.string.approve)) { _, _ -> doApprove(req) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun confirmReject(req: SubscriptionRequestDetail) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.reject_request))
            .setMessage(getString(R.string.reject_confirm, req.profile?.email ?: req.userId))
            .setPositiveButton(getString(R.string.reject)) { _, _ -> doReject(req) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun doApprove(req: SubscriptionRequestDetail) {
        lifecycleScope.launch {
            // Get plan duration
            val plans = SubscriptionManager.getAllPlans()
            val plan = plans.firstOrNull { it.id == req.planId }
            val result = SubscriptionManager.approveRequest(req.id, req.userId, req.planId, plan?.durationDays)
            runOnUiThread {
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@AdminSubscriptionRequestsActivity, getString(R.string.request_approved), Toast.LENGTH_SHORT).show()
                        loadRequests()
                    },
                    onFailure = {
                        Toast.makeText(this@AdminSubscriptionRequestsActivity, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun doReject(req: SubscriptionRequestDetail) {
        lifecycleScope.launch {
            val result = SubscriptionManager.rejectRequest(req.id)
            runOnUiThread {
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@AdminSubscriptionRequestsActivity, getString(R.string.request_rejected), Toast.LENGTH_SHORT).show()
                        loadRequests()
                    },
                    onFailure = {
                        Toast.makeText(this@AdminSubscriptionRequestsActivity, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    inner class RequestsAdapter(
        private val items: List<SubscriptionRequestDetail>,
        private val onApprove: (SubscriptionRequestDetail) -> Unit,
        private val onReject: (SubscriptionRequestDetail) -> Unit
    ) : RecyclerView.Adapter<RequestsAdapter.VH>() {

        private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault())

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvEmail: TextView = v.findViewById(R.id.tvUserEmail)
            val tvPlan: TextView = v.findViewById(R.id.tvPlan)
            val tvMethod: TextView = v.findViewById(R.id.tvPaymentMethod)
            val tvTxNumber: TextView = v.findViewById(R.id.tvTxNumber)
            val tvAmount: TextView = v.findViewById(R.id.tvAmount)
            val tvDate: TextView = v.findViewById(R.id.tvDate)
            val chipStatus: Chip = v.findViewById(R.id.chipStatus)
            val btnApprove: MaterialButton = v.findViewById(R.id.btnApprove)
            val btnReject: MaterialButton = v.findViewById(R.id.btnReject)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_subscription_request, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val req = items[position]
            holder.tvEmail.text = req.profile?.email ?: req.userId
            holder.tvPlan.text = req.plan?.nameAr ?: req.plan?.name ?: "#${req.planId}"
            holder.tvMethod.text = req.paymentMethod?.nameAr ?: req.paymentMethod?.name ?: "#${req.paymentMethodId}"
            holder.tvTxNumber.text = req.transactionNumber
            holder.tvAmount.text = "$%.2f".format(req.amount)
            holder.tvDate.text = req.createdAt?.let {
                runCatching { formatter.format(Instant.parse(it)) }.getOrDefault(it)
            } ?: "—"

            when (req.status) {
                "pending" -> {
                    holder.chipStatus.text = getString(R.string.status_pending)
                    holder.chipStatus.setChipBackgroundColorResource(R.color.color_warning_bg)
                    holder.btnApprove.visibility = View.VISIBLE
                    holder.btnReject.visibility = View.VISIBLE
                }
                "approved" -> {
                    holder.chipStatus.text = getString(R.string.status_approved)
                    holder.chipStatus.setChipBackgroundColorResource(R.color.color_success_bg)
                    holder.btnApprove.visibility = View.GONE
                    holder.btnReject.visibility = View.GONE
                }
                "rejected" -> {
                    holder.chipStatus.text = getString(R.string.status_rejected)
                    holder.chipStatus.setChipBackgroundColorResource(R.color.color_error_bg)
                    holder.btnApprove.visibility = View.GONE
                    holder.btnReject.visibility = View.GONE
                }
            }

            holder.btnApprove.setOnClickListener { onApprove(req) }
            holder.btnReject.setOnClickListener { onReject(req) }
        }

        override fun getItemCount() = items.size
    }
}
