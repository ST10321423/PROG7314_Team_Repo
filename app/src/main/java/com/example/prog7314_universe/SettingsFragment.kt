package com.example.prog7314_universe

import android.annotation.SuppressLint
import android.os.Bundle
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
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: ActivitySettingsBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var prefManager: PrefManager
    private var biometricSupported = false
    private var updatingBiometricSwitch = false

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
        lifecycleScope.launch {
            prefManager.isDarkMode.collect { isDark ->
                binding.switchDarkMode.isChecked = isDark
            }
        }

        lifecycleScope.launch {
            prefManager.notificationsEnabled.collect { enabled ->
                binding.switchNotifications.isChecked = enabled
            }
        }

        lifecycleScope.launch {
            prefManager.textScale.collect { scale ->
                val progress = (scale * 100).toInt().coerceIn(80, 120)
                binding.seekBarFontSize.progress = progress
                binding.tvFontSizeValue.text = "${progress}%"
            }
        }

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
            lifecycleScope.launch {
                prefManager.setNotificationsEnabled(isChecked)
            }
            Toast.makeText(
                requireContext(),
                if (isChecked) "Notifications enabled" else "Notifications disabled",
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

    // ---------------- Dialogs ----------------

    private fun showClearCacheDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear Cache")
            .setMessage("Are you sure you want to clear all cached data?")
            .setPositiveButton("Clear") { _, _ ->
                requireContext().cacheDir.deleteRecursively()
                Toast.makeText(requireContext(), "Cache cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("This action cannot be undone. Are you sure you want to delete your account?")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        user?.delete()
            ?.addOnSuccessListener {
                Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                requireActivity().finish()
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Failed to delete account: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}