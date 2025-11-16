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

    // Language support
    private val supportedLanguageCodes = listOf("en", "af")
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
        loadSettings()
        setupLanguageSpinner()
        setupListeners()
        setupBottomNavigation()
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
                user.email ?: getString(R.string.settings_no_email)
        } else {
            binding.tvUserName.text = getString(R.string.settings_guest_user_name)
            binding.tvUserEmail.text = getString(R.string.settings_not_logged_in)
        }
    }

    // ---------------- Settings loading ----------------

    private fun loadSettings() {
        // Dark mode
        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.isDarkMode.collect { isDark ->
                binding.switchDarkMode.isChecked = isDark
            }
        }

        // Text scale
        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.textScale.collect { scale ->
                binding.seekBarTextSize.progress = ((scale - 0.8f) * 100).toInt()
                binding.tvTextSizeValue.text = "${(scale * 100).toInt()}%"
            }
        }

        // Language
        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.language.collect { language ->
                val code = if (supportedLanguageCodes.contains(language)) {
                    language
                } else {
                    supportedLanguageCodes.first()
                }
                currentLanguageCode = code
                val index = supportedLanguageCodes.indexOf(code)
                if (index != -1 && binding.spinnerLanguage.selectedItemPosition != index) {
                    binding.spinnerLanguage.setSelection(index, false)
                }
            }
        }

        // Notifications
        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.notificationsEnabled.collect { enabled ->
                binding.switchNotifications.isChecked = enabled
                updateNotificationSwitches(enabled)
            }
        }

        // Task reminders
        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.taskRemindersEnabled.collect { enabled ->
                binding.switchTaskReminders.isChecked = enabled
            }
        }

        // Exam alerts
        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.examAlertsEnabled.collect { enabled ->
                binding.switchExamAlerts.isChecked = enabled
            }
        }

        // Habit reminders
        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.habitRemindersEnabled.collect { enabled ->
                binding.switchHabitReminders.isChecked = enabled
            }
        }
    }

    // ---------------- Listeners ----------------

    private fun setupListeners() {
        // Dark mode
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                prefManager.setDarkMode(isChecked)
                applyTheme(isChecked)
            }
        }

        // Text size
        binding.seekBarTextSize.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar?,
                progress: Int,
                fromUser: Boolean
            ) {
                val scale = 0.8f + (progress / 100f * 0.4f)
                binding.tvTextSizeValue.text = "${(scale * 100).toInt()}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val scale = 0.8f + (seekBar!!.progress / 100f * 0.4f)
                viewLifecycleOwner.lifecycleScope.launch {
                    prefManager.setTextScale(scale)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.settings_text_size_updated),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        // Global notifications
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                prefManager.setNotificationsEnabled(isChecked)
                updateNotificationSwitches(isChecked)
            }
        }

        // Individual notification types
        binding.switchTaskReminders.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                prefManager.setTaskRemindersEnabled(isChecked)
            }
        }

        binding.switchExamAlerts.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                prefManager.setExamAlertsEnabled(isChecked)
            }
        }

        binding.switchHabitReminders.setOnCheckedChangeListener { _, isChecked ->
            viewLifecycleOwner.lifecycleScope.launch {
                prefManager.setHabitRemindersEnabled(isChecked)
            }
        }

        // Auth actions
        binding.btnLogout.setOnClickListener { showLogoutDialog() }
        binding.btnDeleteAccount.setOnClickListener { showDeleteAccountDialog() }
    }

    // ---------------- Language spinner ----------------

    private fun setupLanguageSpinner() {
        binding.spinnerLanguage.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // First automatic call from Android – ignore
                    if (!isLanguageSpinnerInitialized) {
                        isLanguageSpinnerInitialized = true
                        return
                    }

                    val selectedCode =
                        supportedLanguageCodes.getOrElse(position) { supportedLanguageCodes.first() }

                    if (selectedCode != currentLanguageCode) {
                        currentLanguageCode = selectedCode
                        viewLifecycleOwner.lifecycleScope.launch {
                            prefManager.setLanguage(selectedCode)
                        }
                        applyLanguage(selectedCode)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    // ---------------- Bottom navigation ----------------

    private fun setupBottomNavigation() = with(binding.bottomNavigationView) {
        selectedItemId = R.id.settings
        setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> {
                    navigator().openFragment(
                        DashboardFragment(),
                        addToBackStack = false,
                        clearBackStack = true
                    )
                    true
                }

                R.id.tasks -> {
                    navigator().openFragment(
                        TasksFragment(),
                        addToBackStack = false,
                        clearBackStack = true
                    )
                    true
                }

                R.id.exams -> {
                    navigator().openFragment(
                        ExamsFragment(),
                        addToBackStack = false,
                        clearBackStack = true
                    )
                    true
                }

                R.id.habits -> {
                    navigator().openFragment(
                        HabitListFragment(),
                        addToBackStack = false,
                        clearBackStack = true
                    )
                    true
                }

                R.id.settings -> true
                else -> false
            }
        }
    }

    // ---------------- Helpers ----------------

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
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        requireActivity().recreate()
    }

    private fun applyLanguage(languageCode: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(localeList)
        requireActivity().recreate()
    }

    // ---------------- Dialogs ----------------

    private fun showLogoutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_logout_title))
            .setMessage(getString(R.string.settings_logout_message))
            .setPositiveButton(getString(R.string.settings_logout)) { _, _ ->
                auth.signOut()
                navigator().openFragment(
                    LoginFragment(),
                    addToBackStack = false,
                    clearBackStack = true
                )
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_delete_account_title))
            .setMessage(getString(R.string.settings_delete_account_message))
            .setPositiveButton(getString(R.string.settings_delete_account_confirm)) { _, _ ->
                deleteAccount()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    // ---------------- Account deletion ----------------

    private fun deleteAccount() {
        val user = auth.currentUser
        if (user != null) {
            user.delete()
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.settings_account_deleted),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigator().openFragment(
                        LoginFragment(),
                        addToBackStack = false,
                        clearBackStack = true
                    )
                }
                .addOnFailureListener { e ->
                    val message = getString(
                        R.string.settings_delete_account_error,
                        e.message ?: getString(R.string.settings_unknown_error)
                    )
                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}
