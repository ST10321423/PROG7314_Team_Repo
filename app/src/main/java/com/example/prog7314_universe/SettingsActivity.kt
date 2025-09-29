package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.example.prog7314_universe.R

class SettingsActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            // Firebase out
            auth.signOut()
            // Google client sign out (optional but recommended)
            GoogleSignIn.getClient(
                this,
                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
            ).signOut()

            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}