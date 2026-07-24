package com.vcam.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.vcam.R
import com.vcam.data.SubscriptionManager
import com.vcam.data.models.PaymentMethod
import com.vcam.data.models.SubscriptionPlan
import com.vcam.databinding.ActivityAdminPaymentMethodsBinding
import kotlinx.coroutines.launch

class AdminPaymentMethodsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminPaymentMethodsBinding
    private val methods = mutableListOf<PaymentMethod>()
    private val plans = mutableListOf<SubscriptionPlan>()
    private lateinit var methodsAdapter: MethodsAdapter
    private lateinit var plansAdapter: PlansAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPaymentMethodsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        methodsAdapter = MethodsAdapter(methods)
        binding.rvMethods.layoutManager = LinearLayoutManager(this)
        binding.rvMethods.adapter = methodsAdapter

        plansAdapter = PlansAdapter(plans)
        binding.rvPlans.layoutManager = LinearLayoutManager(this)
        binding.rvPlans.adapter = plansAdapter

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        binding.rvMethods.visibility = View.VISIBLE
                        binding.fabAddMethod.visibility = View.VISIBLE
                        binding.rvPlans.visibility = View.GONE
                        binding.fabAddPlan.visibility = View.GONE
                    }
                    1 -> {
                        binding.rvMethods.visibility = View.GONE
                        binding.fabAddMethod.visibility = View.GONE
                        binding.rvPlans.visibility = View.VISIBLE
                        binding.fabAddPlan.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        binding.fabAddMethod.setOnClickListener { showAddMethodDialog() }
        binding.fabAddPlan.setOnClickListener { showAddPlanDialog() }

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val m = SubscriptionManager.getAllPaymentMethods()
            val p = SubscriptionManager.getAllPlans()
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                methods.clear(); methods.addAll(m)
                plans.clear(); plans.addAll(p)
                methodsAdapter.notifyDataSetChanged()
                plansAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun showAddMethodDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_payment_method, null)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_payment_method))
            .setView(view)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val name = view.findViewById<EditText>(R.id.etMethodName).text.toString().trim()
                val nameAr = view.findViewById<EditText>(R.id.etMethodNameAr).text.toString().trim()
                val address = view.findViewById<EditText>(R.id.etMethodAddress).text.toString().trim()
                val instructions = view.findViewById<EditText>(R.id.etMethodInstructions).text.toString().trim()
                if (name.isEmpty() || address.isEmpty()) {
                    Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val result = SubscriptionManager.createPaymentMethod(
                        PaymentMethod(id = 0, name = name, nameAr = nameAr.ifEmpty { null },
                            address = address, instructionsAr = instructions.ifEmpty { null })
                    )
                    runOnUiThread {
                        result.fold(
                            onSuccess = { loadData() },
                            onFailure = { Toast.makeText(this@AdminPaymentMethodsActivity, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showAddPlanDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_plan, null)
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_plan))
            .setView(view)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val name = view.findViewById<EditText>(R.id.etPlanName).text.toString().trim()
                val nameAr = view.findViewById<EditText>(R.id.etPlanNameAr).text.toString().trim()
                val price = view.findViewById<EditText>(R.id.etPlanPrice).text.toString().toDoubleOrNull()
                val durationStr = view.findViewById<EditText>(R.id.etPlanDuration).text.toString().trim()
                val duration = durationStr.toIntOrNull() // blank = permanent

                if (name.isEmpty() || price == null) {
                    Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val result = SubscriptionManager.createPlan(
                        SubscriptionPlan(id = 0, name = name, nameAr = nameAr.ifEmpty { null },
                            price = price, durationDays = duration)
                    )
                    runOnUiThread {
                        result.fold(
                            onSuccess = { loadData() },
                            onFailure = { Toast.makeText(this@AdminPaymentMethodsActivity, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    inner class MethodsAdapter(private val items: List<PaymentMethod>) : RecyclerView.Adapter<MethodsAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvMethodName)
            val tvAddress: TextView = v.findViewById(R.id.tvMethodAddress)
            val chipActive: Chip = v.findViewById(R.id.chipActive)
            val btnToggle: MaterialButton = v.findViewById(R.id.btnToggle)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_payment_method_admin, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val m = items[position]
            holder.tvName.text = m.nameAr ?: m.name
            holder.tvAddress.text = m.address
            holder.chipActive.text = if (m.isActive) getString(R.string.active) else getString(R.string.inactive)
            holder.chipActive.setChipBackgroundColorResource(if (m.isActive) R.color.color_success_bg else R.color.color_error_bg)
            holder.btnToggle.text = if (m.isActive) getString(R.string.deactivate) else getString(R.string.activate_verb)
            holder.btnToggle.setOnClickListener {
                lifecycleScope.launch {
                    SubscriptionManager.updatePaymentMethod(m.copy(isActive = !m.isActive))
                    runOnUiThread { loadData() }
                }
            }
        }
        override fun getItemCount() = items.size
    }

    inner class PlansAdapter(private val items: List<SubscriptionPlan>) : RecyclerView.Adapter<PlansAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvPlanName)
            val tvPrice: TextView = v.findViewById(R.id.tvPlanPrice)
            val tvDuration: TextView = v.findViewById(R.id.tvPlanDuration)
            val chipActive: Chip = v.findViewById(R.id.chipPlanActive)
            val btnToggle: MaterialButton = v.findViewById(R.id.btnTogglePlan)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_plan_admin, parent, false))
        override fun onBindViewHolder(holder: VH, position: Int) {
            val p = items[position]
            holder.tvName.text = p.nameAr ?: p.name
            holder.tvPrice.text = "$%.2f %s".format(p.price, p.currency)
            holder.tvDuration.text = if (p.durationDays == null) getString(R.string.permanent)
            else getString(R.string.days_format, p.durationDays)
            holder.chipActive.text = if (p.isActive) getString(R.string.active) else getString(R.string.inactive)
            holder.chipActive.setChipBackgroundColorResource(if (p.isActive) R.color.color_success_bg else R.color.color_error_bg)
            holder.btnToggle.text = if (p.isActive) getString(R.string.deactivate) else getString(R.string.activate_verb)
            holder.btnToggle.setOnClickListener {
                lifecycleScope.launch {
                    SubscriptionManager.updatePlanStatus(p.id, !p.isActive)
                    runOnUiThread { loadData() }
                }
            }
        }
        override fun getItemCount() = items.size
    }
}
