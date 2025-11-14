package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.prog7314_universe.utils.PrefManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    // Views
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var addTaskButton: Button
    private lateinit var profileImage: ImageView
    private lateinit var userNameText: TextView
    private lateinit var dateText: TextView
    private lateinit var tasksCompletedText: TextView
    private lateinit var studyHoursText: TextView
    private lateinit var coursesCountText: TextView

    // Cards (clickable)
    private lateinit var cardTasksCompleted: CardView
    private lateinit var cardStudyHours: CardView
    private lateinit var cardCourses: CardView

    // "See all" links
    private lateinit var tvSeeAllAssignments: TextView
    private lateinit var tvSeeAllCourses: TextView
    private lateinit var tvSeeAllSchedule: TextView

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var prefManager: PrefManager
    private var hasInitializedUi = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginFragment::class.java))
            finish()
            return
        }
        prefManager = PrefManager(applicationContext)

        lifecycleScope.launch {
            val biometricEnabled = withContext(Dispatchers.IO) {
                prefManager.biometricEnabled.first()
            }

            if (biometricEnabled) {
                if (!canAuthenticateWithBiometrics()) {
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.biometric_not_available),
                        Toast.LENGTH_SHORT
                    ).show()
                    withContext(Dispatchers.IO) {
                        prefManager.setBiometricEnabled(false)
                    }
                } else {
                    showBiometricPrompt(
                        onSuccess = { initializeDashboard() },
                        onError = { _, errString ->
                            Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.biometric_required),
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    )
                    return@launch
                }
            }

            initializeDashboard()
        }
    }

    private fun initializeDashboard() {
        if (hasInitializedUi) return
        hasInitializedUi = true

        setContentView(R.layout.activity_dashboard)

        initializeViews()
        setupClickListeners()
        setupBottomNavigation()
        loadUserData()
        loadDashboardStats()
    }

    private fun initializeViews() {
        // Bottom nav and buttons
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        addTaskButton = findViewById(R.id.addTaskButton)

        // Header views
        profileImage = findViewById(R.id.ivProfile)
        userNameText = findViewById(R.id.tvUserName)
        dateText = findViewById(R.id.tvDate)

        // Stats
        tasksCompletedText = findViewById(R.id.tvTasksCompleted)
        studyHoursText = findViewById(R.id.tvStudyHours)
        coursesCountText = findViewById(R.id.tvCoursesCount)

        // Cards
        cardTasksCompleted = findViewById(R.id.cardTasksCompleted)
        cardStudyHours = findViewById(R.id.cardStudyHours)
        cardCourses = findViewById(R.id.cardCourses)

        // "See all" links
        tvSeeAllAssignments = findViewById(R.id.tvSeeAllAssignments)
        tvSeeAllCourses = findViewById(R.id.tvSeeAllCourses)
        tvSeeAllSchedule = findViewById(R.id.tvSeeAllSchedule)
    }

    private fun setupClickListeners() {
        // Add Task button
        addTaskButton.setOnClickListener {
            startActivity(Intent(this, AddTaskFragment::class.java))
        }

        // Profile image - go to settings or profile
        profileImage.setOnClickListener {
            startActivity(Intent(this, SettingsFragment::class.java))
        }

        // Stats cards - navigate to relevant sections
        cardTasksCompleted.setOnClickListener {
            startActivity(Intent(this, TasksFragment::class.java))
        }

        cardStudyHours.setOnClickListener {
            // Could navigate to a study hours tracking screen
            Toast.makeText(this, "Study hours tracking coming soon", Toast.LENGTH_SHORT).show()
        }

        cardCourses.setOnClickListener {
            // Could navigate to courses screen
            Toast.makeText(this, "Courses screen coming soon", Toast.LENGTH_SHORT).show()
        }

        // "See all" links
        tvSeeAllAssignments.setOnClickListener {
            startActivity(Intent(this, TasksFragment::class.java))
        }

        tvSeeAllCourses.setOnClickListener {
            Toast.makeText(this, "Courses screen coming soon", Toast.LENGTH_SHORT).show()
        }

        tvSeeAllSchedule.setOnClickListener {
            Toast.makeText(this, "Schedule screen coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        // Set dashboard as selected
        bottomNavigationView.selectedItemId = R.id.dashboard

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> {
                    // Already on dashboard
                    true
                }
                R.id.tasks -> {
                    startActivity(Intent(this, TasksFragment::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }

                R.id.exams -> {
                    startActivity(Intent(this, ExamsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.habits -> {
                    startActivity(Intent(this, HabitListFragment::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.settings -> {
                    startActivity(Intent(this, SettingsFragment::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun canAuthenticateWithBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt(
        onSuccess: () -> Unit,
        onError: (Int, CharSequence) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.biometric_required),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_prompt_negative))
            .build()

        prompt.authenticate(promptInfo)
    }


    private fun loadUserData() {
        val user = auth.currentUser

        // Update user name
        val displayName = user?.displayName ?: "User"
        userNameText.text = "Hello, $displayName!"

        // Update date
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        dateText.text = currentDate
    }

    private fun loadDashboardStats() {
        val uid = auth.currentUser?.uid ?: return

        lifecycleScope.launch {
            try {
                // Load Tasks Stats
                loadTasksStats(uid)

                // Load Exams Stats (as proxy for courses)
                loadExamsStats(uid)

                // Load Habits Stats (as proxy for study hours)
                loadHabitsStats(uid)

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error loading stats: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun loadTasksStats(uid: String) {
        try {
            val tasksSnapshot = db.collection("users")
                .document(uid)
                .collection("tasks")
                .get()
                .await()

            val totalTasks = tasksSnapshot.documents.size
            val completedTasks = tasksSnapshot.documents.count {
                it.getBoolean("isCompleted") == true
            }

            tasksCompletedText.text = "$completedTasks/$totalTasks"
        } catch (e: Exception) {
            tasksCompletedText.text = "0/0"
        }
    }

    private suspend fun loadExamsStats(uid: String) {
        try {
            val examsSnapshot = db.collection("users")
                .document(uid)
                .collection("exams")
                .get()
                .await()

            val totalExams = examsSnapshot.documents.size
            coursesCountText.text = totalExams.toString()
        } catch (e: Exception) {
            coursesCountText.text = "0"
        }
    }

    private suspend fun loadHabitsStats(uid: String) {
        try {
            // Get today's completed habits
            val habitsSnapshot = db.collection("users")
                .document(uid)
                .collection("habits")
                .get()
                .await()

            var totalCompletedToday = 0
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            for (habitDoc in habitsSnapshot.documents) {
                val logDoc = habitDoc.reference
                    .collection("logs")
                    .document(today)
                    .get()
                    .await()

                if (logDoc.getBoolean("completed") == true) {
                    totalCompletedToday++
                }
            }

            // Calculate approximate study hours (30 min per completed habit)
            val studyHours = totalCompletedToday * 0.5
            studyHoursText.text = "${studyHours}h"

        } catch (e: Exception) {
            studyHoursText.text = "0h"
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload stats when returning to this activity
        loadDashboardStats()

        // Make sure dashboard is selected in bottom nav
        bottomNavigationView.selectedItemId = R.id.dashboard
    }
}