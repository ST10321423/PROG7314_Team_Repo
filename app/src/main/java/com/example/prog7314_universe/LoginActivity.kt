package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.prog7314_universe.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken ?: throw IllegalStateException("No ID token")
                val credential = GoogleAuthProvider.getCredential(idToken, null)

                auth.signInWithCredential(credential)
                    .addOnSuccessListener { res ->
                        val isNew = res.additionalUserInfo?.isNewUser == true
                        val user = auth.currentUser!!
                        if (isNew) {
                            // First sign-in = registration -> create profile doc
                            val profile = hashMapOf(
                                "uid" to user.uid,
                                "email" to (user.email ?: ""),
                                "name" to (user.displayName ?: ""),
                                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                                "createdAt" to System.currentTimeMillis()
                            )
                            db.collection("users")
                                .document(user.uid)
                                .set(profile)
                                .addOnSuccessListener { goToMain() }
                                .addOnFailureListener {

                                    goToMain()
                                }
                        } else {
                            goToMain()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Firebase sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }

            } catch (e: Exception) {
                Toast.makeText(this, "Google sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser != null) { goToMain(); return }

        setContentView(R.layout.activity_login)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // This string comes from google-services.json
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(this, gso)

        findViewById<Button>(R.id.googleSignInButton).setOnClickListener {
            signInLauncher.launch(googleClient.signInIntent)
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
