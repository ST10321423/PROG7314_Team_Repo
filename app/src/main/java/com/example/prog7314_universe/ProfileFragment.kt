package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.prog7314_universe.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.prog7314_universe.viewmodel.JournalViewModel
import com.example.prog7314_universe.viewmodel.MoodViewModel
import com.example.prog7314_universe.viewmodel.TaskViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * ProfileFragment - User profile and account information
 */
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val moodViewModel: MoodViewModel by viewModels()
    private val journalViewModel: JournalViewModel by viewModels()
    private val taskViewModel: TaskViewModel by activityViewModels()

    private var habitsListener: ListenerRegistration? = null

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
        observeStats()
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

    private fun observeStats() {
        taskViewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            binding.tvTotalTasks.text = tasks.count { it.isCompleted }.toString()
        }
        taskViewModel.refresh()

        moodViewModel.moodEntries.observe(viewLifecycleOwner) { moods ->
            binding.tvTotalMoods.text = moods.size.toString()
        }

        journalViewModel.journalEntries.observe(viewLifecycleOwner) { entries ->
            binding.tvTotalJournals.text = entries.size.toString()
        }

        val uid = auth.currentUser?.uid ?: return
        habitsListener?.remove()
        habitsListener = db.collection("users")
            .document(uid)
            .collection("habits")
            .addSnapshotListener { snapshot, _ ->
                binding.tvTotalHabits.text = snapshot?.size()?.toString() ?: "0"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        habitsListener?.remove()
        habitsListener = null
        _binding = null
    }
}