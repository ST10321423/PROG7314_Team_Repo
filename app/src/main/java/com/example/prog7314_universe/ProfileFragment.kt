package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.prog7314_universe.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * ProfileFragment - User profile and account information
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val user = auth.currentUser
        if (user != null) {
            binding.tvUserName.text = user.displayName ?: "Student Name"
            binding.tvUserEmail.text = user.email ?: "email@example.com"

            // Load profile image if available
            // You can use Glide to load user.photoUrl if it exists
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}