package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val next = if (FirebaseAuth.getInstance().currentUser != null)
            MainActivity::class.java else LoginActivity::class.java

        startActivity(Intent(this, next))
        finish()
    }
}
