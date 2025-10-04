package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val splashDelay: Long = 1500 // 1.5 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Check authentication after a short delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkAuthentication()
        }, splashDelay)
    }

    private fun checkAuthentication() {
        val currentUser = auth.currentUser

        val nextActivity = if (currentUser != null) {
            // User is logged in, go to MainActivity
            MainActivity::class.java
        } else {
            // User is not logged in, go to LoginActivity
            LoginActivity::class.java
        }

        startActivity(Intent(this, nextActivity))
        finish()
    }
}
