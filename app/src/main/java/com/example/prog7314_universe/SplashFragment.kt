package com.example.prog7314_universe

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.activity_splash) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val splashDelay: Long = 1500

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(splashDelay)
            val destination = if (auth.currentUser != null) {
                R.id.homeFragment
            } else {
                R.id.loginFragment
            }
            val options = navOptions {
                popUpTo(R.id.splashFragment) { inclusive = true }
            }
            findNavController().navigate(destination, null, options)
        }
    }
}