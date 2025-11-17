package com.example.prog7314_universe

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.biometric.BiometricManager
import androidx.lifecycle.lifecycleScope
import com.example.prog7314_universe.databinding.ActivitySettingsBinding
import com.example.prog7314_universe.utils.PrefManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var prefManager: PrefManager
    private var biometricSupported = false
    private var updatingBiometricSwitch = false
    private var updatingNotificationSwitch = false

    // Language support
    private val supportedLanguageCodes = listOf("en", "af", "zu")
    private var isLanguageSpinnerInitialized = false
    private var currentLanguageCode: String = supportedLanguageCodes.first()

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
        setupBiometricCapability()
        loadSettings()
        setupLanguageSpinner()
        setupListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ---------------- User info ----------------

    private fun loadUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            binding.tvUserName.text =
                user.displayName ?: getString(R.string.settings_user_fallback_name)
            binding.tvUserEmail.text =
                user.email ?: getString(R.string.settings_user_fallback_email)
        }
    }

    // ---------------- Load settings from SharedPreferences ----------------

    private fun loadSettings() {
        // Load dark mode setting
        lifecycleScope.launch {
            prefManager.isDarkMode.collect { isDark ->
                binding.switchDarkMode.isChecked = isDark
            }
        }

        // Load notification setting
        lifecycleScope.launch {
            prefManager.notificationsEnabled.collect { enabled ->
                updatingNotificationSwitch = true
                binding.switchNotifications.isChecked = enabled
                updatingNotificationSwitch = false
                updateNotificationSwitchAppearance(enabled)
            }
        }

        // Load text scale setting
        lifecycleScope.launch {
            prefManager.textScale.collect { scale ->
                val progress = (scale * 100).toInt().coerceIn(80, 120)
                binding.seekBarFontSize.progress = progress
                binding.tvFontSizeValue.text = "${progress}%"
            }
        }

        // Load biometric setting
        lifecycleScope.launch {
            prefManager.biometricEnabled.collect { enabled ->
                updatingBiometricSwitch = true
                binding.switchBiometricLock.isChecked = enabled && biometricSupported
                updatingBiometricSwitch = false
            }
        }
    }

    // ---------------- Language Spinner ----------------

    @SuppressLint("RestrictedApi")
    private fun setupLanguageSpinner() {
        // Get currently selected locale
        val appLocales = AppCompatDelegate.getApplicationLocales()
        currentLanguageCode = if (!appLocales.isEmpty) {
            appLocales[0]?.language ?: supportedLanguageCodes.first()
        } else {
            supportedLanguageCodes.first()
        }

        // Pre-select the current language
        val currentIndex = supportedLanguageCodes.indexOf(currentLanguageCode)
        if (currentIndex >= 0) {
            binding.spinnerLanguage.setSelection(currentIndex)
        }
        var hasHandledInitialSelection = false

        binding.spinnerLanguage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                @SuppressLint("RestrictedApi")
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (!hasHandledInitialSelection) {
                        hasHandledInitialSelection = true
                        return
                    }

                    val selectedCode = supportedLanguageCodes[position]
                    if (selectedCode != currentLanguageCode) {
                        currentLanguageCode = selectedCode
                        val locales = LocaleListCompat.forLanguageTags(selectedCode)
                        AppCompatDelegate.setApplicationLocales(locales)
                        lifecycleScope.launch { prefManager.setLanguage(selectedCode) }
                        requireActivity().recreate()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    // ---------------- Listeners ----------------

    private fun setupListeners() {
        // Dark mode toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                prefManager.setDarkMode(isChecked)
            }
            val mode =
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        // Notifications toggle
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (updatingNotificationSwitch) return@setOnCheckedChangeListener

            // Check if system notifications are enabled for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (isChecked && !areSystemNotificationsEnabled()) {
                    // Show dialog to guide user to system settings
                    showSystemNotificationSettingsDialog()

                    // Revert the switch
                    updatingNotificationSwitch = true
                    binding.switchNotifications.isChecked = false
                    updatingNotificationSwitch = false
                    return@setOnCheckedChangeListener
                }
            }

            // Save preference
            lifecycleScope.launch {
                prefManager.setNotificationsEnabled(isChecked)
            }

            // Update UI
            updateNotificationSwitchAppearance(isChecked)

            // Show feedback
            Toast.makeText(
                requireContext(),
                if (isChecked)
                    "✅ Notifications enabled! You'll receive updates for moods, journals, and more."
                else
                    "🔕 Notifications disabled. You won't receive any updates.",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Font size seekbar
        binding.seekBarFontSize.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvFontSizeValue.text = "$progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val progress = (seekBar?.progress ?: 100).coerceIn(80, 120)
                val scale = progress / 100f
                lifecycleScope.launch {
                    prefManager.setTextScale(scale)
                }
                Toast.makeText(
                    requireContext(),
                    "Text size updated to $progress%",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })

        // Clear cache button
        binding.btnClearCache.setOnClickListener {
            showClearCacheDialog()
        }

        // Delete account button
        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }

        // Biometric lock toggle
        binding.switchBiometricLock.setOnCheckedChangeListener { _, isChecked ->
            if (updatingBiometricSwitch) return@setOnCheckedChangeListener

            if (!biometricSupported && isChecked) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.biometric_not_available),
                    Toast.LENGTH_SHORT
                ).show()
                binding.switchBiometricLock.isChecked = false
                return@setOnCheckedChangeListener
            }

            val user = auth.currentUser
            if (user == null && isChecked) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.settings_not_logged_in),
                    Toast.LENGTH_SHORT
                ).show()
                binding.switchBiometricLock.isChecked = false
                return@setOnCheckedChangeListener
            }

            lifecycleScope.launch {
                prefManager.setBiometricEnabled(isChecked)
            }

            Toast.makeText(
                requireContext(),
                if (isChecked)
                    getString(R.string.biometric_enable_success)
                else
                    getString(R.string.biometric_disable_success),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupBiometricCapability() {
        val manager = BiometricManager.from(requireContext())
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        val status = manager.canAuthenticate(authenticators)
        biometricSupported = status == BiometricManager.BIOMETRIC_SUCCESS ||
                status == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED

        binding.switchBiometricLock.isEnabled = biometricSupported
        binding.switchBiometricLock.alpha = if (biometricSupported) 1f else 0.5f
        if (status == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            binding.switchBiometricLock.text = getString(R.string.unlock_with_biometrics)
        }
    }

    // ---------------- Notification Helper Methods ----------------

    /**
     * Check if system notifications are enabled for the app (Android 13+)
     */
    private fun areSystemNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val notificationManager = requireContext().getSystemService(android.app.NotificationManager::class.java)
            notificationManager?.areNotificationsEnabled() ?: false
        } else {
            true // Pre-Android N always returns true
        }
    }

    /**
     * Show dialog to guide user to system notification settings
     */
    private fun showSystemNotificationSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Enable Notifications")
            .setMessage("To receive notifications, you need to enable them in your device settings. Would you like to open settings now?")
            .setPositiveButton("Open Settings") { _, _ ->
                openSystemNotificationSettings()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Open system notification settings for this app
     */
    private fun openSystemNotificationSettings() {
        val intent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:${requireContext().packageName}")
            }
        }
        startActivity(intent)
    }

    /**
     * Update the visual appearance of notification switch based on state
     */
    private fun updateNotificationSwitchAppearance(enabled: Boolean) {
        // Optional: Add visual feedback
        binding.switchNotifications.alpha = if (enabled) 1.0f else 0.7f
    }

    // ---------------- Dialogs ----------------

    private fun showClearCacheDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Cache")
            .setMessage("Are you sure you want to clear all cached data? This will remove temporary files and may improve performance.")
            .setPositiveButton("Clear") { _, _ ->
                clearCache()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun clearCache() {
        try {
            val cacheSize = requireContext().cacheDir.walkTopDown().sumOf { it.length() }
            val cacheSizeMB = cacheSize / (1024 * 1024)

            requireContext().cacheDir.deleteRecursively()

            Toast.makeText(
                requireContext(),
                "Cache cleared! Freed up ${cacheSizeMB}MB",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to clear cache: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Delete Account")
            .setMessage("This action cannot be undone. All your data including moods, journals, tasks, and habits will be permanently deleted. Are you absolutely sure?")
            .setPositiveButton("Delete Forever") { _, _ ->
                showFinalConfirmationDialog()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFinalConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Final Confirmation")
            .setMessage("Type 'DELETE' to confirm account deletion")
            .setView(
                android.widget.EditText(requireContext()).apply {
                    hint = "Type DELETE here"
                }
            )
            .setPositiveButton("Confirm") { dialog, _ ->
                val editText = (dialog as AlertDialog).findViewById<android.widget.EditText>(android.R.id.edit)
                val input = editText?.text.toString()

                if (input == "DELETE") {
                    deleteAccount()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Account deletion cancelled - text did not match",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(
                requireContext(),
                "No user is currently signed in",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        user.delete()
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Account deleted successfully",
                    Toast.LENGTH_SHORT
                ).show()



                // Close the app
                requireActivity().finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to delete account: ${e.message}\n\nYou may need to sign in again to delete your account.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Refresh notification status when returning from system settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                val appPreference = prefManager.notificationsEnabled.first()
                val systemEnabled = areSystemNotificationsEnabled()

                // If app preference is on but system notifications are off, update the switch
                if (appPreference && !systemEnabled) {
                    updatingNotificationSwitch = true
                    binding.switchNotifications.isChecked = false
                    updatingNotificationSwitch = false

                    // Update the preference to match system state
                    prefManager.setNotificationsEnabled(false)
                }
            }
        }
    }
}