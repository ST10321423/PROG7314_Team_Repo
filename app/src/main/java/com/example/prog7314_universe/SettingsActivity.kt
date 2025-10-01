package com.example.prog7314_universe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.example.prog7314_universe.R
import com.example.prog7314_universe.UserPrefs
import com.example.prog7314_universe.UserPrefsKeys
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db   by lazy { FirebaseFirestore.getInstance() }
    private lateinit var prefs: UserPrefs

    // UI
    private lateinit var tvUserEmail: TextView
    private lateinit var rgTheme: RadioGroup
    private lateinit var spLanguage: Spinner
    private lateinit var spTextScale: Spinner
    private lateinit var swReduceMotion: Switch
    private lateinit var swNotif: Switch
    private lateinit var rowReminderTime: LinearLayout
    private lateinit var tvReminderTime: TextView
    private lateinit var swBiometric: Switch
    private lateinit var btnTestNotif: Button
    private lateinit var btnExportData: Button
    private lateinit var btnClearLocal: Button
    private lateinit var btnLogout: Button
    private lateinit var btnDelete: Button

    private val requestNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (!granted) openAppSettings() }

    private val createDocLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) lifecycleScope.launch { writeExport(uri) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = UserPrefs(this)

        bindViews()
        populateDropdowns()
        loadExisting()
        attachListeners()
        ensureNotifChannel()

        tvUserEmail.text = auth.currentUser?.email ?: "Unknown user"
    }

    private fun bindViews() {
        tvUserEmail = findViewById(R.id.tvUserEmail)
        rgTheme     = findViewById(R.id.rgTheme)
        spLanguage  = findViewById(R.id.spLanguage)
        spTextScale = findViewById(R.id.spTextScale)
        swReduceMotion = findViewById(R.id.swReduceMotion)
        swNotif     = findViewById(R.id.swEnableNotifications)
        rowReminderTime = findViewById(R.id.rowReminderTime)
        tvReminderTime = findViewById(R.id.tvReminderTime)
        swBiometric  = findViewById(R.id.swBiometric)
        btnTestNotif = findViewById(R.id.btnSendTestNotification)
        btnExportData= findViewById(R.id.btnExportData)
        btnClearLocal= findViewById(R.id.btnClearLocal)
        btnLogout    = findViewById(R.id.btnLogout)
        btnDelete    = findViewById(R.id.btnDeleteAccount)
    }

    private fun populateDropdowns() {
        spLanguage.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("English (en)", "Afrikaans (af)", "Zulu (zu)")
        )
        spTextScale.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Small (0.9x)", "Default (1.0x)", "Large (1.1x)", "Huge (1.2x)")
        )
    }

    private fun loadExisting() = lifecycleScope.launch {
        val p = prefs.flow.first()

        // Theme
        when (p[UserPrefsKeys.THEME] ?: "system") {
            "system" -> rgTheme.check(R.id.rbThemeSystem)
            "light"  -> rgTheme.check(R.id.rbThemeLight)
            "dark"   -> rgTheme.check(R.id.rbThemeDark)
        }

        // Language
        spLanguage.setSelection(
            when (p[UserPrefsKeys.LANG] ?: "en") {
                "af" -> 1; "zu" -> 2; else -> 0
            }
        )

        // Text scale
        val scale = p[UserPrefsKeys.TEXT_SCALE] ?: 1.0f
        val scaleIdx = when {
            scale <= 0.9f -> 0
            scale < 1.05f -> 1
            scale < 1.15f -> 2
            else -> 3
        }
        spTextScale.setSelection(scaleIdx)

        swReduceMotion.isChecked = p[UserPrefsKeys.REDUCE_MOTION] ?: false
        swNotif.isChecked        = p[UserPrefsKeys.NOTIF_ENABLED] ?: false

        val hour = p[UserPrefsKeys.REMINDER_HOUR] ?: 18
        val min  = p[UserPrefsKeys.REMINDER_MIN] ?: 0
        tvReminderTime.text = formatTime(hour, min)

        swBiometric.isChecked = p[UserPrefsKeys.BIOMETRIC] ?: false
    }

    private fun attachListeners() {
        rgTheme.setOnCheckedChangeListener { _, id ->
            lifecycleScope.launch {
                val value = when(id) {
                    R.id.rbThemeLight -> "light"
                    R.id.rbThemeDark  -> "dark"
                    else              -> "system"
                }
                prefs.setTheme(value)
                applyTheme(value)
            }
        }

        spLanguage.setOnItemSelectedListener { code ->
            lifecycleScope.launch { prefs.setLang(code); applyLocale(code) }
        }

        spTextScale.setOnItemSelectedListener { idx ->
            val scale = listOf(0.9f, 1.0f, 1.1f, 1.2f)[idx]
            lifecycleScope.launch { prefs.setTextScale(scale); applyTextScale(scale) }
        }

        swReduceMotion.setOnCheckedChangeListener { _, checked ->
            lifecycleScope.launch { prefs.setReduceMotion(checked) }
        }

        swNotif.setOnCheckedChangeListener { _, checked ->
            lifecycleScope.launch {
                prefs.setNotificationsEnabled(checked)
                if (checked && Build.VERSION.SDK_INT >= 33) {
                    requestNotifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        rowReminderTime.setOnClickListener { openTimePicker() }
        btnTestNotif.setOnClickListener { sendTestNotification() }

        swBiometric.setOnCheckedChangeListener { _, checked ->
            if (checked) enableBiometricOrExplain() else lifecycleScope.launch { prefs.setBiometric(false) }
        }

        btnExportData.setOnClickListener {
            createDocLauncher.launch("universe-export.json")
        }
        btnClearLocal.setOnClickListener { clearLocalData() }

        btnLogout.setOnClickListener { signOut() }
        btnDelete.setOnClickListener { deleteAccountFlow() }
    }

    /* ---------- APPLY SETTINGS ---------- */

    private fun applyTheme(mode: String) {
        val m = when(mode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark"  -> AppCompatDelegate.MODE_NIGHT_YES
            else    -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(m)
    }

    private fun applyLocale(lang: String) {
        val locales = LocaleListCompat.forLanguageTags(
            when (lang) { "af" -> "af"; "zu" -> "zu"; else -> "en" }
        )
        AppCompatDelegate.setApplicationLocales(locales)
        recreate() // apply to this activity now
    }

    private fun applyTextScale(scale: Float) {
        val conf = resources.configuration
        conf.fontScale = scale
        @Suppress("DEPRECATION")
        resources.updateConfiguration(conf, resources.displayMetrics)
        recreate()
    }

    /* ---------- NOTIFICATIONS ---------- */

    private fun ensureNotifChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel("universe_default", "UNIverse", NotificationManager.IMPORTANCE_DEFAULT)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun sendTestNotification() {
        if (Build.VERSION.SDK_INT >= 33 &&
            !NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            openAppSettings(); return
        }
        val notif = NotificationCompat.Builder(this, "universe_default")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("UNIverse")
            .setContentText("Notifications are enabled 🎉")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(42, notif)
    }

    private fun openTimePicker() {
        val cal = Calendar.getInstance()
        val is24 = DateFormat.is24HourFormat(this)
        val dlg = android.app.TimePickerDialog(this, { _, h, m ->
            lifecycleScope.launch {
                prefs.setReminder(h, m)
                tvReminderTime.text = formatTime(h, m)
                // TODO: schedule your real daily reminder here (AlarmManager/WorkManager)
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24)
        dlg.show()
    }

    private fun formatTime(h: Int, m: Int): String = String.format("%02d:%02d", h, m)

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        })
    }

    /* ---------- BIOMETRIC ---------- */

    private fun enableBiometricOrExplain() {
        val mgr = androidx.biometric.BiometricManager.from(this)
        val can = mgr.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (can == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Enable biometric unlock")
                .setSubtitle("Use your fingerprint/face to unlock UNIverse")
                .setNegativeButtonText("Cancel")
                .build()
            val prompt = androidx.biometric.BiometricPrompt(this,
                mainExecutor,
                object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        lifecycleScope.launch { prefs.setBiometric(true) }
                        Toast.makeText(this@SettingsActivity, "Biometric enabled", Toast.LENGTH_SHORT).show()
                    }
                    override fun onAuthenticationError(code: Int, err: CharSequence) {
                        super.onAuthenticationError(code, err)
                        Toast.makeText(this@SettingsActivity, "$err", Toast.LENGTH_SHORT).show()
                        swBiometric.isChecked = false
                    }
                })
            prompt.authenticate(promptInfo)
        } else {
            Toast.makeText(this, "No biometric enrolled on this device", Toast.LENGTH_LONG).show()
            swBiometric.isChecked = false
        }
    }

    /* ---------- PRIVACY ---------- */

    private suspend fun writeExport(uri: Uri) {
        // TODO: replace with real repositories (tasks/exams/habits/savings)
        val json = """
            {
              "user": {"uid":"${auth.currentUser?.uid}","email":"${auth.currentUser?.email}"},
              "settings": {
                "theme": "${prefs.flow.first()[UserPrefsKeys.THEME] ?: "system"}",
                "language": "${prefs.flow.first()[UserPrefsKeys.LANG] ?: "en"}"
              }
            }
        """.trimIndent()
        contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
        Toast.makeText(this, "Exported to ${uri.lastPathSegment}", Toast.LENGTH_LONG).show()
    }

    private fun clearLocalData() {
        // TODO: clear Room DB tables, caches, etc.
        Toast.makeText(this, "Local data cleared (stub)", Toast.LENGTH_SHORT).show()
    }

    /* ---------- ACCOUNT ---------- */

    private fun signOut() {
        auth.signOut()
        GoogleSignIn.getClient(
            this, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        ).signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    private fun deleteAccountFlow() {
        // Re-auth with Google then delete Firebase user and Firestore doc
        val acct = GoogleSignIn.getLastSignedInAccount(this)
        if (acct == null) { Toast.makeText(this, "Please sign in again", Toast.LENGTH_LONG).show(); return }
        val cred = com.google.firebase.auth.GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.currentUser?.reauthenticate(cred)?.addOnSuccessListener {
            val uid = auth.currentUser!!.uid
            db.collection("users").document(uid).delete().addOnCompleteListener {
                auth.currentUser!!.delete().addOnCompleteListener {
                    Toast.makeText(this, "Account deleted", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, LoginActivity::class.java)); finishAffinity()
                }
            }
        }?.addOnFailureListener { e ->
            Toast.makeText(this, "Re-auth failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /* ---------- Small helpers for Spinner callbacks ---------- */
    private fun Spinner.setOnItemSelectedListener(onSelect: (index: Int) -> Unit) {
        this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, v: android.view.View?, pos: Int, id: Long) {
                onSelect(pos)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }
    private fun Spinner.setOnItemSelectedListener(mapToLang: (langCode: String) -> Unit) { /* overloaded not used */ }
    private fun Spinner.setOnItemSelectedListenerLang(onLang: (String) -> Unit) {
        this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, v: android.view.View?, pos: Int, id: Long) {
                val lang = when(pos){ 1->"af"; 2->"zu"; else->"en" }; onLang(lang)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }
    }
}
