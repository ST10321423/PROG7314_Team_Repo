package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
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

                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
                    // Most common when default_web_client_id is wrong/missing (config issue)
                    toast("Google returned no ID token. Check default_web_client_id / Firebase config.")
                    Log.e(TAG, "Null/blank idToken. ClientId=${safeClientId()}, acct=${account.email}")
                    return@registerForActivityResult
                }

                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { res ->
                        val isNewUser = res.additionalUserInfo?.isNewUser == true
                        val user = auth.currentUser
                        if (user == null) {
                            toast("Signed in but user is null.")
                            Log.e(TAG, "FirebaseAuth returned null user after signInWithCredential.")
                            return@addOnSuccessListener
                        }

                        if (isNewUser) {
                            val profile = hashMapOf(
                                "uid" to user.uid,
                                "email" to (user.email ?: ""),
                                "name" to (user.displayName ?: ""),
                                "photoUrl" to (user.photoUrl?.toString() ?: ""),
                                "createdAt" to System.currentTimeMillis(),
                                "lastLogin" to System.currentTimeMillis()
                            )
                            db.collection("users").document(user.uid)
                                .set(profile)
                                .addOnSuccessListener {
                                    toast("Welcome! Account created 🎉")
                                    goToMain()
                                }
                                .addOnFailureListener { e ->
                                    toast("Profile creation failed: ${e.message}")
                                    Log.e(TAG, "Firestore create profile failed", e)
                                    goToMain() // proceed anyway
                                }
                        } else {
                            db.collection("users").document(user.uid)
                                .update("lastLogin", System.currentTimeMillis())
                                .addOnSuccessListener {
                                    toast("Welcome back, ${user.displayName ?: ""}!")
                                    goToMain()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Update lastLogin failed", e)
                                    toast("Welcome back!")
                                    goToMain()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        toast("Authentication failed: ${e.message}")
                        Log.e(TAG, "Firebase signInWithCredential failed", e)
                    }

            } catch (e: ApiException) {
                // Surface precise reason; code 10 => DEVELOPER_ERROR (usually SHA/client mismatch)
                val code = e.statusCode
                val codeName = GoogleSignInStatusCodes.getStatusCodeString(code)
                Log.e(TAG, "Google sign-in ApiException: code=$code ($codeName)", e)

                if (code == GoogleSignInStatusCodes.DEVELOPER_ERROR) {
                    // Very explicit guidance for config mismatch
                    toast(
                        "Sign-in failed (10). Fix Firebase config: add your SHA-1/SHA-256, " +
                                "download a fresh google-services.json, and ensure default_web_client_id matches."
                    )
                    Log.e(
                        TAG,
                        """
                        ========= CONFIG CHECK =========
                        packageName: $packageName
                        default_web_client_id: ${safeClientId()}
                        Is Play Services OK? ${playServicesStatusString()}
                        Tip: Run `gradlew signingReport` and add SHA-1 & SHA-256 for *this* build variant to Firebase,
                        then re-download google-services.json.
                        =================================
                        """.trimIndent()
                    )
                } else {
                    toast("Google sign-in failed: $code ($codeName)")
                }

            } catch (e: Exception) {
                toast("Google sign-in failed: ${e.message}")
                Log.e(TAG, "Google sign-in unexpected exception", e)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If already logged in, skip login
        if (auth.currentUser != null) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        // Check Google Play services (Play-Store build of emulator/device)
        val apiStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (apiStatus != ConnectionResult.SUCCESS) {
            val human =
                GoogleApiAvailability.getInstance().getErrorString(apiStatus) ?: "$apiStatus"
            toast("Google Play services not available: $human")
            Log.e(TAG, "Play services status=$apiStatus ($human)")
        }

        // Build GoogleSignInOptions from the auto-generated string resource
        val clientId = safeClientId()
        if (!clientId.endsWith(".apps.googleusercontent.com")) {
            // This is the fastest way to catch a wrong resource/client
            toast("Invalid default_web_client_id. Re-download google-services.json.")
            Log.e(TAG, "default_web_client_id looks invalid: $clientId")
        } else {
            Log.d(TAG, "Using clientId: $clientId")
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId) // must come from google-services.json -> strings.xml
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(this, gso)

        // Ensure account picker shows (avoid cached sessions)
        findViewById<Button>(R.id.googleSignInButton).setOnClickListener {
            // Optional: revoke + signOut gives the cleanest picker when testing multiple accounts
            googleClient.revokeAccess().addOnCompleteListener {
                googleClient.signOut().addOnCompleteListener {
                    signInLauncher.launch(googleClient.signInIntent)
                }
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun safeClientId(): String = try {
        getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        Log.e(TAG, "default_web_client_id missing", e)
        ""
    }

    private fun playServicesStatusString(): String {
        val s = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        val human = GoogleApiAvailability.getInstance().getErrorString(s)
        return "$s ($human)"
    }

    companion object {
        private const val TAG = "LoginActivity"
    }
}

