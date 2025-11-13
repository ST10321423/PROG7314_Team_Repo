package com.example.prog7314_universe

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prog7314_universe.databinding.ActivitySettingsBinding
import com.example.prog7314_universe.utils.PrefManager
import com.example.prog7314_universe.utils.navigator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var prefManager: PrefManager

    @SuppressLint("RestrictedApi")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivitySettingsBinding.inflate(inflater, container, false)
        prefManager = PrefManager(requireContext().applicationContext)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadUserInfo()
        loadSettings()
        setupListeners()
        setupBottomNavigation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            binding.tvUserName.text = user.displayName ?: "User"
            binding.tvUserEmail.text = user.email ?: "No email"
        } else {
            binding.tvUserName.text = "Guest"
            binding.tvUserEmail.text = "Not logged in"
        }
    }

    private fun loadSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.isDarkMode.collect { isDark ->
                binding.switchDarkMode.isChecked = isDark
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.textScale.collect { scale ->
                binding.seekBarTextSize.progress = ((scale - 0.8f) * 100).toInt()
                binding.tvTextSizeValue.text = "${(scale * 100).toInt()}%"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.notificationsEnabled.collect { enabled ->
                binding.switchNotifications.isChecked = enabled
                updateNotificationSwitches(enabled)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.taskRemindersEnabled.collect { enabled ->
                binding.switchTaskReminders.isChecked = enabled
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.examAlertsEnabled.collect { enabled ->
                binding.switchExamAlerts.isChecked = enabled
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.habitRemindersEnabled.collect { enabled ->
                binding.switchHabitReminders.isChecked = enabled
            }
        }
    }

    private fun setupListeners() {
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                prefManager.setDarkMode(isChecked)
                applyTheme(isChecked)
            }
        }

        binding.seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = 0.8f + (progress / 100f * 0.4f)
                binding.tvTextSizeValue.text = "${(scale * 100).toInt()}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val scale = 0.8f + (seekBar!!.progress / 100f * 0.4f)
                viewLifecycleOwner.lifecycleScope.launch {
                    prefManager.setTextScale(scale)
                    Toast.makeText(requireContext(), "Text size updated. Restart app to apply.", Toast.LENGTH_SHORT).show()
                }
            }
        })

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                prefManager.setNotificationsEnabled(isChecked)
                updateNotificationSwitches(isChecked)
            }
        }

        binding.switchTaskReminders.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch { prefManager.setTaskRemindersEnabled(isChecked) }
        }

        binding.switchExamAlerts.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch { prefManager.setExamAlertsEnabled(isChecked) }
        }

        binding.switchHabitReminders.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch { prefManager.setHabitRemindersEnabled(isChecked) }
        }

        binding.btnLogout.setOnClickListener { showLogoutDialog() }
        binding.btnDeleteAccount.setOnClickListener { showDeleteAccountDialog() }
    }

    private fun setupBottomNavigation() = with(binding.bottomNavigationView) {
        selectedItemId = R.id.settings
        setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> {
                    navigator().openFragment(DashboardFragment(), addToBackStack = false, clearBackStack = true)
                    true
                }
                R.id.tasks -> {
                    navigator().openFragment(TasksFragment(), addToBackStack = false, clearBackStack = true)
                    true
                }
                R.id.exams -> {
                    navigator().openFragment(ExamsFragment(), addToBackStack = false, clearBackStack = true)
                    true
                }
                R.id.habits -> {
                    navigator().openFragment(HabitListFragment(), addToBackStack = false, clearBackStack = true)
                    true
                }
                R.id.settings -> true
                else -> false
            }
        }
    }

    private fun updateNotificationSwitches(enabled: Boolean) {
        binding.switchTaskReminders.isEnabled = enabled
        binding.switchExamAlerts.isEnabled = enabled
        binding.switchHabitReminders.isEnabled = enabled

        val alpha = if (enabled) 1f else 0.5f
        binding.switchTaskReminders.alpha = alpha
        binding.switchExamAlerts.alpha = alpha
        binding.switchHabitReminders.alpha = alpha
    }

    private fun applyTheme(isDark: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
        requireActivity().recreate()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                navigator().openFragment(LoginFragment(), addToBackStack = false, clearBackStack = true)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        if (user != null) {
            user.delete()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                    navigator().openFragment(LoginFragment(), addToBackStack = false, clearBackStack = true)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        requireContext(),
                        "Failed to delete account: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}