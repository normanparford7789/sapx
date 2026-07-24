package com.vcam.ui.account

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
import com.google.android.material.card.MaterialCardView
import com.vcam.R
import com.vcam.data.AuthManager
import com.vcam.data.SubscriptionManager
import com.vcam.data.models.PaymentMethod
import com.vcam.data.models.SubscriptionRequest
import com.vcam.databinding.ActivityPaymentBinding
import kotlinx.coroutines.launch

class PaymentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentBinding
    private val methods = mutableListOf<PaymentMethod>()
    private lateinit var adapter: MethodsAdapter
    private var selectedMethod: PaymentMethod? = null
    private var planId = 0
    private var planName = ""
    private var planPrice = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        planId = intent.getIntExtra("plan_id", 0)
        planName = intent.getStringExtra("plan_name") ?: ""
        planPrice = intent.getDoubleExtra("plan_price", 0.0)
        val planDuration = intent.getIntExtra("plan_duration", -1)

        binding.tvSelectedPlan.text = planName
        binding.tvSelectedPrice.text = getString(R.string.price_format, planPrice, "USD")
        binding.tvSelectedDuration.text = if (planDuration == -1)
            getString(R.string.permanent)
        else
            getString(R.string.days_format, planDuration)

        adapter = MethodsAdapter(methods) { method ->
            selectedMethod = method
            showPaymentDetails(method)
            binding.cardPaymentDetails.visibility = View.VISIBLE
        }
        binding.rvMethods.layoutManager = LinearLayoutManager(this)
        binding.rvMethods.adapter = adapter

        binding.btnSubmit.setOnClickListener { submitRequest() }

        loadMethods()
    }

    private fun showPaymentDetails(method: PaymentMethod) {
        binding.tvPaymentAddress.text = method.address
        binding.tvPaymentInstructions.text = method.instructionsAr
            ?: method.instructions
            ?: getString(R.string.transfer_instructions_default)
        binding.tvPaymentAmount.text = getString(R.string.price_format, planPrice, "USD")
    }

    private fun loadMethods() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = SubscriptionManager.getActivePaymentMethods()
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                methods.clear()
                methods.addAll(result)
                adapter.notifyDataSetChanged()
                if (result.isEmpty()) {
                    binding.tvNoMethods.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun submitRequest() {
        val method = selectedMethod
        if (method == null) {
            Toast.makeText(this, getString(R.string.select_payment_method), Toast.LENGTH_SHORT).show()
            return
        }
        val txNumber = binding.etTransactionNumber.text.toString().trim()
        if (txNumber.isEmpty()) {
            binding.etTransactionNumber.error = getString(R.string.enter_transaction_number)
            return
        }

        val userId = AuthManager.currentUser()?.id ?: run {
            Toast.makeText(this, getString(R.string.login_required), Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.confirm_request))
            .setMessage(getString(R.string.confirm_request_msg, planName, txNumber))
            .setPositiveButton(getString(R.string.confirm)) { _, _ -> doSubmit(userId, method, txNumber) }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun doSubmit(userId: String, method: PaymentMethod, txNumber: String) {
        binding.btnSubmit.isEnabled = false
        binding.progressSubmit.visibility = View.VISIBLE
        lifecycleScope.launch {
            val request = SubscriptionRequest(
                userId = userId,
                planId = planId,
                paymentMethodId = method.id,
                transactionNumber = txNumber,
                amount = planPrice
            )
            val result = SubscriptionManager.submitRequest(request)
            runOnUiThread {
                binding.btnSubmit.isEnabled = true
                binding.progressSubmit.visibility = View.GONE
                result.fold(
                    onSuccess = {
                        AlertDialog.Builder(this@PaymentActivity)
                            .setTitle(getString(R.string.request_sent))
                            .setMessage(getString(R.string.request_sent_msg))
                            .setPositiveButton(getString(R.string.ok)) { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                    },
                    onFailure = {
                        Toast.makeText(this@PaymentActivity, getString(R.string.error_sending_request), Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    inner class MethodsAdapter(
        private val items: List<PaymentMethod>,
        private val onSelect: (PaymentMethod) -> Unit
    ) : RecyclerView.Adapter<MethodsAdapter.VH>() {

        private var selectedPos = -1

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val card: MaterialCardView = itemView.findViewById(R.id.cardPaymentMethod)
            val tvName: TextView = itemView.findViewById(R.id.tvMethodName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_payment_method, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val method = items[position]
            holder.tvName.text = method.nameAr ?: method.name
            holder.card.isChecked = position == selectedPos
            holder.card.setOnClickListener {
                val prev = selectedPos
                selectedPos = holder.adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedPos)
                onSelect(method)
            }
        }

        override fun getItemCount() = items.size
    }
}
