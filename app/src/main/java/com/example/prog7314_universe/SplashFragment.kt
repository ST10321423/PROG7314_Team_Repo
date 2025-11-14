package com.example.prog7314_universe

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prog7314_universe.utils.navigator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment(R.layout.activity_splash) {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val splashDelay: Long = 1500

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            delay(splashDelay)
            val next = if (auth.currentUser != null) {
                DashboardFragment()
            } else {
                LoginFragment()
            }
            navigator().openFragment(next, addToBackStack = false, clearBackStack = true)
        }
    }
}