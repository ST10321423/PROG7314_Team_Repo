package com.example.prog7314_universe

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.prog7314_universe.utils.NotificationHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Permission launcher for notifications
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(
                this,
                "Notifications enabled! You'll receive reminders for exams and tasks.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Notification permission denied. You won't receive reminders.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize notification channels
        NotificationHelper(this)

        // Request notification permission for Android 13+
        requestNotificationPermission()

        setupViews()
        setupNavigation()
        setupDrawerToggle()
        setupBottomNavigation()

        // Handle deep linking from notifications
        handleNotificationIntent()
    }

    private fun setupViews() {
        toolbar = findViewById(R.id.toolbar)
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        bottomNav = findViewById(R.id.bottomNavigationView)

        setSupportActionBar(toolbar)

        // Populate navigation drawer header with the signed-in user info
        navigationView.getHeaderView(0)?.let { header ->
            val nameView = header.findViewById<android.widget.TextView>(R.id.nav_user_name)
            val emailView = header.findViewById<android.widget.TextView>(R.id.nav_user_email)
            val user = auth.currentUser
            nameView?.text = user?.displayName
                ?: user?.email?.substringBefore("@")
                        ?: getString(R.string.settings_user_fallback_name)
            emailView?.text = user?.email ?: getString(R.string.settings_user_fallback_email)
        }
    }

    private fun setupNavigation() {
        // Get the NavHostFragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Defines top-level destinations
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.moodTrackerFragment,
                R.id.journalListFragment,
                R.id.tasksListFragment,
                R.id.habitListFragment
            ),
            drawerLayout
        )

        // Setups ActionBar with NavController
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Setups NavigationView with NavController
        navigationView.setupWithNavController(navController)

        // Handles navigation item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            handleNavigationItemSelected(menuItem)
        }
    }

    private fun setupDrawerToggle() {
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun setupBottomNavigation() {
        // Setup bottom navigation with nav controller
        bottomNav.setupWithNavController(navController)

        // Hide bottom nav on certain destinations
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.homeFragment,
                R.id.moodTrackerFragment,
                R.id.journalListFragment,
                R.id.tasksListFragment,
                R.id.habitListFragment -> bottomNav.visibility = View.VISIBLE
                    else -> bottomNav.visibility = View.GONE
                }
            }
        }

    private fun handleNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.homeFragment -> navController.navigate(R.id.homeFragment)
            R.id.moodTrackerFragment -> navController.navigate(R.id.moodTrackerFragment)
            R.id.journalListFragment -> navController.navigate(R.id.journalListFragment)
            R.id.tasksListFragment -> navController.navigate(R.id.tasksListFragment)
            R.id.habitListFragment -> navController.navigate(R.id.habitListFragment)
            R.id.savingGoalFragment -> navController.navigate(R.id.savingGoalFragment)
            R.id.fridgeFragment -> navController.navigate(R.id.fridgeFragment)
            R.id.profileFragment -> navController.navigate(R.id.profileFragment)
            R.id.settingsFragment -> navController.navigate(R.id.settingsFragment)
            R.id.nav_logout -> handleLogout()
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun handleLogout() {
        auth.signOut()
        finish()
    }

    /**
     * Request notification permission for Android 13 (API 33) and above
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation to user
                    Toast.makeText(
                        this,
                        "Notifications help you stay on top of your exams and assignments",
                        Toast.LENGTH_LONG
                    ).show()
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission
                    requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    /**
     * Handle notification tap to navigate to specific item
     */
    private fun handleNotificationIntent() {
        intent?.extras?.let { extras ->
            val itemId = extras.getString("itemId")
            val type = extras.getString("type")

            when (type) {
                NotificationHelper.NOTIFICATION_TYPE_EXAM -> {
                    // Navigate to exams screen
                    navController.navigate(R.id.examsFragment)
                }
                NotificationHelper.NOTIFICATION_TYPE_TASK -> {
                    // Navigate to tasks screen
                    navController.navigate(R.id.tasksListFragment)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}