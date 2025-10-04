package com.example.prog7314_universe

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    // Views
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var switchDarkMode: SwitchMaterial
    private lateinit var spinnerLanguage: Spinner
    private lateinit var seekBarTextSize: SeekBar
    private lateinit var tvTextSizeValue: TextView
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var switchTaskReminders: SwitchMaterial
    private lateinit var switchExamAlerts: SwitchMaterial
    private lateinit var switchHabitReminders: SwitchMaterial
    private lateinit var btnLogout: Button
    private lateinit var btnDeleteAccount: Button
    private lateinit var bottomNavigationView: BottomNavigationView

    private val auth by lazy { FirebaseAuth.getInstance() }
    private lateinit var prefManager: PreferenceManager

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefManager = PreferenceManager(this)

        initializeViews()
        loadUserInfo()
        loadSettings()
        setupListeners()
        setupBottomNavigation()
    }

    private fun initializeViews() {
        // User Info
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)

        // Theme & Display
        switchDarkMode = findViewById(R.id.switchDarkMode)
        spinnerLanguage = findViewById(R.id.spinnerLanguage)
        seekBarTextSize = findViewById(R.id.seekBarTextSize)
        tvTextSizeValue = findViewById(R.id.tvTextSizeValue)

        // Notifications
        switchNotifications = findViewById(R.id.switchNotifications)
        switchTaskReminders = findViewById(R.id.switchTaskReminders)
        switchExamAlerts = findViewById(R.id.switchExamAlerts)
        switchHabitReminders = findViewById(R.id.switchHabitReminders)

        // Account Actions
        btnLogout = findViewById(R.id.btnLogout)
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount)

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
    }

    private fun loadUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            tvUserName.text = user.displayName ?: "User"
            tvUserEmail.text = user.email ?: "No email"
        } else {
            tvUserName.text = "Guest"
            tvUserEmail.text = "Not logged in"
        }
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            // Theme
            prefManager.isDarkMode.collect { isDark ->
                switchDarkMode.isChecked = isDark
            }
        }

        lifecycleScope.launch {
            // Text Size
            prefManager.textScale.collect { scale ->
                seekBarTextSize.progress = ((scale - 0.8f) * 100).toInt()
                tvTextSizeValue.text = "${(scale * 100).toInt()}%"
            }
        }

        lifecycleScope.launch {
            // Notifications
            prefManager.notificationsEnabled.collect { enabled ->
                switchNotifications.isChecked = enabled
                updateNotificationSwitches(enabled)
            }
        }

        lifecycleScope.launch {
            prefManager.taskRemindersEnabled.collect { enabled ->
                switchTaskReminders.isChecked = enabled
            }
        }

        lifecycleScope.launch {
            prefManager.examAlertsEnabled.collect { enabled ->
                switchExamAlerts.isChecked = enabled
            }
        }

        lifecycleScope.launch {
            prefManager.habitRemindersEnabled.collect { enabled ->
                switchHabitReminders.isChecked = enabled
            }
        }
    }

    private fun setupListeners() {
        // Dark Mode Toggle
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                prefManager.setDarkMode(isChecked)
                applyTheme(isChecked)
            }
        }

        // Text Size Slider
        seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = 0.8f + (progress / 100f * 0.4f) // 0.8 to 1.2
                tvTextSizeValue.text = "${(scale * 100).toInt()}%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val scale = 0.8f + (seekBar!!.progress / 100f * 0.4f)
                lifecycleScope.launch {
                    prefManager.setTextScale(scale)
                    Toast.makeText(
                        this@SettingsActivity,
                        "Text size updated. Restart app to apply.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        // Master Notifications Toggle
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                prefManager.setNotificationsEnabled(isChecked)
                updateNotificationSwitches(isChecked)
            }
        }

        // Individual Notification Toggles
        switchTaskReminders.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                prefManager.setTaskRemindersEnabled(isChecked)
            }
        }

        switchExamAlerts.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                prefManager.setExamAlertsEnabled(isChecked)
            }
        }

        switchHabitReminders.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                prefManager.setHabitRemindersEnabled(isChecked)
            }
        }

        // Logout Button
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Delete Account Button
        btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.settings

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.tasks -> {
                    Toast.makeText(this, "Tasks feature coming soon!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.exams -> {
                    startActivity(Intent(this, ExamsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.habits -> {
                    startActivity(Intent(this, HabitListActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.settings -> true
                else -> false
            }
        }
    }

    private fun updateNotificationSwitches(enabled: Boolean) {
        switchTaskReminders.isEnabled = enabled
        switchExamAlerts.isEnabled = enabled
        switchHabitReminders.isEnabled = enabled

        if (!enabled) {
            switchTaskReminders.alpha = 0.5f
            switchExamAlerts.alpha = 0.5f
            switchHabitReminders.alpha = 0.5f
        } else {
            switchTaskReminders.alpha = 1.0f
            switchExamAlerts.alpha = 1.0f
            switchHabitReminders.alpha = 1.0f
        }
    }

    private fun applyTheme(isDark: Boolean) {
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        recreate() // Restart activity to apply theme
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity() // Close all activities
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser
        if (user != null) {
            user.delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to delete account: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}
