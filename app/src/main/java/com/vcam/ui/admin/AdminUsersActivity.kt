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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vcam.R
import com.vcam.data.SubscriptionManager
import com.vcam.data.models.SubscriptionPlan
import com.vcam.data.models.UserProfile
import com.vcam.databinding.ActivityAdminUsersBinding
import kotlinx.coroutines.launch

class AdminUsersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminUsersBinding
    private val users = mutableListOf<UserProfile>()
    private lateinit var adapter: UsersAdapter
    private var plans = listOf<SubscriptionPlan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminUsersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        adapter = UsersAdapter(users,
            onBan = { user, ban -> doToggleBan(user, ban) },
            onSetAdmin = { user, isAdmin -> doSetAdmin(user, isAdmin) },
            onActivateSub = { user -> showActivateSubDialog(user) }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(this)
        binding.rvUsers.adapter = adapter

        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            users.clear()
            users.addAll(SubscriptionManager.getAllUsers())
            plans = SubscriptionManager.getAllPlans()
            runOnUiThread {
                binding.progressBar.visibility = View.GONE
                adapter.notifyDataSetChanged()
                binding.tvEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun doToggleBan(user: UserProfile, ban: Boolean) {
        val msg = if (ban) getString(R.string.confirm_ban, user.email)
        else getString(R.string.confirm_unban, user.email)
        AlertDialog.Builder(this)
            .setTitle(if (ban) getString(R.string.ban_user) else getString(R.string.unban_user))
            .setMessage(msg)
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                lifecycleScope.launch {
                    val result = SubscriptionManager.banUser(user.id, ban)
                    runOnUiThread {
                        result.fold(
                            onSuccess = { loadData() },
                            onFailure = { Toast.makeText(this@AdminUsersActivity, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun doSetAdmin(user: UserProfile, isAdmin: Boolean) {
        lifecycleScope.launch {
            val result = SubscriptionManager.setAdmin(user.id, isAdmin)
            runOnUiThread {
                result.fold(
                    onSuccess = { loadData() },
                    onFailure = { Toast.makeText(this@AdminUsersActivity, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    private fun showActivateSubDialog(user: UserProfile) {
        if (plans.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_plans_available), Toast.LENGTH_SHORT).show()
            return
        }
        val planNames = plans.map { it.nameAr ?: it.name }.toTypedArray()
        var selectedPlanIdx = 0
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.activate_subscription))
            .setSingleChoiceItems(planNames, 0) { _, which -> selectedPlanIdx = which }
            .setPositiveButton(getString(R.string.activate)) { _, _ ->
                val plan = plans[selectedPlanIdx]
                lifecycleScope.launch {
                    val result = SubscriptionManager.activateSubscriptionManually(user.id, plan.id, plan.durationDays)
                    runOnUiThread {
                        result.fold(
                            onSuccess = { Toast.makeText(this@AdminUsersActivity, getString(R.string.subscription_activated), Toast.LENGTH_SHORT).show() },
                            onFailure = { Toast.makeText(this@AdminUsersActivity, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    inner class UsersAdapter(
        private val items: List<UserProfile>,
        private val onBan: (UserProfile, Boolean) -> Unit,
        private val onSetAdmin: (UserProfile, Boolean) -> Unit,
        private val onActivateSub: (UserProfile) -> Unit
    ) : RecyclerView.Adapter<UsersAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvEmail: TextView = v.findViewById(R.id.tvEmail)
            val tvName: TextView = v.findViewById(R.id.tvName)
            val chipBanned: Chip = v.findViewById(R.id.chipBanned)
            val chipAdmin: Chip = v.findViewById(R.id.chipAdmin)
            val btnBan: MaterialButton = v.findViewById(R.id.btnBan)
            val btnAdmin: MaterialButton = v.findViewById(R.id.btnAdmin)
            val btnActivateSub: MaterialButton = v.findViewById(R.id.btnActivateSub)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val user = items[position]
            holder.tvEmail.text = user.email
            holder.tvName.text = user.fullName ?: getString(R.string.no_name)
            holder.chipBanned.visibility = if (user.isBanned) View.VISIBLE else View.GONE
            holder.chipAdmin.visibility = if (user.isAdmin) View.VISIBLE else View.GONE

            holder.btnBan.text = if (user.isBanned) getString(R.string.unban_user) else getString(R.string.ban_user)
            holder.btnAdmin.text = if (user.isAdmin) getString(R.string.remove_admin) else getString(R.string.make_admin)

            holder.btnBan.setOnClickListener { onBan(user, !user.isBanned) }
            holder.btnAdmin.setOnClickListener { onSetAdmin(user, !user.isAdmin) }
            holder.btnActivateSub.setOnClickListener { onActivateSub(user) }
        }

        override fun getItemCount() = items.size
    }
}
