package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.prog7314_universe.databinding.FragmentBiometricGateBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class BiometricGateFragment : Fragment() {

    private var _binding: FragmentBiometricGateBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private var biometricPrompt: BiometricPrompt? = null
    private var promptInfo: BiometricPrompt.PromptInfo? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBiometricGateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        setupBiometricPrompt()
        binding.btnAuthenticate.setOnClickListener { tryAuth() }
        binding.btnLogout.setOnClickListener { logout() }
        tryAuth()
    }

    private fun setupBiometricPrompt() {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        val manager = BiometricManager.from(requireContext())
        val status = manager.canAuthenticate(authenticators)
        if (status != BiometricManager.BIOMETRIC_SUCCESS &&
            status != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
        ) {
            Snackbar.make(binding.root, getString(R.string.biometric_not_available), Snackbar.LENGTH_LONG).show()
            navigateHome()
            return
        }

        val executor = ContextCompat.getMainExecutor(requireContext())
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                navigateHome()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Snackbar.make(binding.root, errString, Snackbar.LENGTH_SHORT).show()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))
            .setNegativeButtonText(getString(R.string.biometric_prompt_negative))
            .setAllowedAuthenticators(authenticators)
            .build()
    }

    private fun tryAuth() {
        val prompt = biometricPrompt
        val info = promptInfo
        if (prompt != null && info != null) {
            prompt.authenticate(info)
        }
    }

    private fun navigateHome() {
        val options = navOptions {
            popUpTo(R.id.biometricGateFragment) { inclusive = true }
        }
        findNavController().navigate(R.id.homeFragment, null, options)
    }

    private fun navigateToLogin() {
        val options = navOptions {
            popUpTo(R.id.biometricGateFragment) { inclusive = true }
        }
        findNavController().navigate(R.id.loginFragment, null, options)
    }

    private fun logout() {
        auth.signOut()
        navigateToLogin()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}