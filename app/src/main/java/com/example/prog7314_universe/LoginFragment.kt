package com.example.prog7314_universe

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.prog7314_universe.utils.navigator
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment(R.layout.activity_login) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)

                val idToken = account.idToken
                if (idToken.isNullOrBlank()) {
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
                                    goToMain()
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
                val code = e.statusCode
                val codeName = GoogleSignInStatusCodes.getStatusCodeString(code)
                Log.e(TAG, "Google sign-in ApiException: code=$code ($codeName)", e)

                if (code == GoogleSignInStatusCodes.DEVELOPER_ERROR) {
                    toast(
                        "Sign-in failed (10). Fix Firebase config: add your SHA-1/SHA-256, " +
                                "download a fresh google-services.json, and ensure default_web_client_id matches."
                    )
                    Log.e(
                        TAG,
                        """
                        ========= CONFIG CHECK =========
                        packageName: ${requireContext().packageName}
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (auth.currentUser != null) {
            goToMain()
            return
        }

        val apiStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext())
        if (apiStatus != ConnectionResult.SUCCESS) {
            val human =
                GoogleApiAvailability.getInstance().getErrorString(apiStatus) ?: "$apiStatus"
            toast("Google Play services not available: $human")
            Log.e(TAG, "Play services status=$apiStatus ($human)")
        }

        val clientId = safeClientId()
        if (!clientId.endsWith(".apps.googleusercontent.com")) {
            toast("Invalid default_web_client_id. Re-download google-services.json.")
            Log.e(TAG, "default_web_client_id looks invalid: $clientId")
        } else {
            Log.d(TAG, "Using clientId: $clientId")
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(requireContext(), gso)

        view.findViewById<Button>(R.id.googleSignInButton).setOnClickListener {
            googleClient.revokeAccess().addOnCompleteListener {
                googleClient.signOut().addOnCompleteListener {
                    signInLauncher.launch(googleClient.signInIntent)
                }
            }
        }
    }

    private fun goToMain() {
        navigator().openFragment(DashboardFragment(), addToBackStack = false, clearBackStack = true)
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    private fun safeClientId(): String = try {
        getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        Log.e(TAG, "default_web_client_id missing", e)
        ""
    }

    private fun playServicesStatusString(): String {
        val s = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext())
        val human = GoogleApiAvailability.getInstance().getErrorString(s)
        return "$s ($human)"
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}