package com.example.prog7314_universe

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import androidx.biometric.BiometricManager
import com.example.prog7314_universe.utils.PrefManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.activity_login) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private lateinit var prefManager: PrefManager
    private var biometricPrompt: BiometricPrompt? = null
    private var biometricPromptInfo: BiometricPrompt.PromptInfo? = null
    private var canUseBiometrics: Boolean = false
    private var biometricsEnabledByUser: Boolean = false
    private var hasAutoPrompted = false
    private var biometricButton: Button? = null

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
                                    handleSignedInState()
                                }
                                .addOnFailureListener { e ->
                                    toast("Profile creation failed: ${e.message}")
                                    Log.e(TAG, "Firestore create profile failed", e)
                                    handleSignedInState()
                                }
                        } else {
                            db.collection("users").document(user.uid)
                                .update("lastLogin", System.currentTimeMillis())
                                .addOnSuccessListener {
                                    toast("Welcome back, ${user.displayName ?: ""}!")
                                    handleSignedInState()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Update lastLogin failed", e)
                                    toast("Welcome back!")
                                    handleSignedInState()
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

        prefManager = PrefManager(requireContext().applicationContext)
        val biometricButton = view.findViewById<Button>(R.id.biometricButton)
        this.biometricButton = biometricButton
        setupBiometric(biometricButton)

        viewLifecycleOwner.lifecycleScope.launch {
            prefManager.biometricEnabled.collect { enabled ->
                biometricsEnabledByUser = enabled
                updateBiometricButtonState()
                handleSignedInState()
            }
        }

        if (auth.currentUser == null) {
            updateBiometricButtonState()
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

    override fun onResume() {
        super.onResume()
        handleSignedInState()
    }

    private fun setupBiometric(button: Button) {
        val manager = BiometricManager.from(requireContext())
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        canUseBiometrics = manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
        button.isVisible = false
        if (!canUseBiometrics) {
            return
        }

        val executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                toast("Authentication successful")
                goToMain()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    toast(errString.toString())
                }
            }
        })

        biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_prompt_negative))
            .setAllowedAuthenticators(authenticators)
            .build()

        button.setOnClickListener {
            if (auth.currentUser == null) {
                toast("Sign in with Google first to enable biometrics")
            } else {
                biometricPrompt?.authenticate(biometricPromptInfo!!)
            }
        }

        updateBiometricButtonState()
    }

    private fun updateBiometricButtonState() {
        val button = biometricButton ?: return
        val signedIn = auth.currentUser != null
        val shouldShow = canUseBiometrics && biometricsEnabledByUser && signedIn
        button.isVisible = shouldShow
        button.isEnabled = shouldShow
        button.alpha = if (shouldShow) 1f else 0.5f
    }

    private fun maybeAutoPrompt() {
        if (hasAutoPrompted) return
        if (auth.currentUser == null) return
        if (!canUseBiometrics || !biometricsEnabledByUser) {
            if (!biometricsEnabledByUser) {
                hasAutoPrompted = false
            }
            if (!canUseBiometrics) {
                biometricButton?.isVisible = false
            }
            return
        }

        val prompt = biometricPrompt ?: return
        val promptInfo = biometricPromptInfo ?: return
        hasAutoPrompted = true
        prompt.authenticate(promptInfo)
    }

    private fun handleSignedInState() {
        if (auth.currentUser == null) return
        val shouldGateWithBiometrics =
            canUseBiometrics && biometricsEnabledByUser && biometricPromptInfo != null
        if (shouldGateWithBiometrics) {
            maybeAutoPrompt()
        } else {
            goToMain()
        }
    }

    private fun goToMain() {
        val options = navOptions {
            popUpTo(R.id.loginFragment) { inclusive = true }
        }
        findNavController().navigate(R.id.homeFragment, null, options)
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