package com.vcam.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.vcam.R
import com.vcam.data.SubscriptionManager
import com.vcam.data.models.SubscriptionPlan
import com.vcam.databinding.ActivitySubscriptionPlansBinding
import kotlinx.coroutines.launch

class SubscriptionPlansActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionPlansBinding
    private val plans = mutableListOf<SubscriptionPlan>()
    private lateinit var adapter: PlansAdapter
    private var selectedPlan: SubscriptionPlan? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionPlansBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = PlansAdapter(plans) { plan ->
            selectedPlan = plan
            binding.btnContinue.isEnabled = true
        }
        binding.rvPlans.layoutManager = LinearLayoutManager(this)
        binding.rvPlans.adapter = adapter

        binding.btnContinue.isEnabled = false
        binding.btnContinue.setOnClickListener {
            val plan = selectedPlan ?: return@setOnClickListener
            val intent = Intent(this, PaymentActivity::class.java)
            intent.putExtra("plan_id", plan.id)
            intent.putExtra("plan_name", plan.nameAr ?: plan.name)
            intent.putExtra("plan_price", plan.price)
            intent.putExtra("plan_duration", plan.durationDays ?: -1)
            startActivity(intent)
        }

        loadPlans()
    }

    private fun loadPlans() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = SubscriptionManager.getActivePlans()
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                if (result.isEmpty()) {
                    binding.tvEmpty.visibility = View.VISIBLE
                } else {
                    binding.tvEmpty.visibility = View.GONE
                    plans.clear()
                    plans.addAll(result)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    inner class PlansAdapter(
        private val items: List<SubscriptionPlan>,
        private val onSelect: (SubscriptionPlan) -> Unit
    ) : RecyclerView.Adapter<PlansAdapter.VH>() {

        private var selectedPos = -1

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val card: MaterialCardView = itemView.findViewById(R.id.cardPlan)
            val tvName: TextView = itemView.findViewById(R.id.tvPlanName)
            val tvDuration: TextView = itemView.findViewById(R.id.tvPlanDuration)
            val tvPrice: TextView = itemView.findViewById(R.id.tvPlanPrice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_plan_card, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val plan = items[position]
            holder.tvName.text = plan.nameAr ?: plan.name
            holder.tvDuration.text = if (plan.durationDays == null)
                getString(R.string.permanent)
            else
                getString(R.string.days_format, plan.durationDays)
            holder.tvPrice.text = getString(R.string.price_format, plan.price, plan.currency)
            holder.card.isChecked = position == selectedPos
            holder.card.setOnClickListener {
                val prev = selectedPos
                selectedPos = holder.adapterPosition
                notifyItemChanged(prev)
                notifyItemChanged(selectedPos)
                onSelect(plan)
            }
        }

        override fun getItemCount() = items.size
    }
}
