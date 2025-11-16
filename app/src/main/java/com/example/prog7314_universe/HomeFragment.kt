package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_universe.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

/**
 * HomeFragment - Dashboard showing overview of all app features
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupClickListeners()
        loadDashboardData()
    }

    private fun setupUI() {
        // Set welcome message with user name
        val user = auth.currentUser
        val name = user?.displayName?.split(" ")?.firstOrNull() ?: "Student"
        binding.tvWelcome.text = "Welcome back, $name!"

        // Set current date
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date())
    }

    private fun setupClickListeners() {
        // Navigation to different sections
        binding.cardMood.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_mood)
        }

        binding.cardJournal.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_journal)
        }

        binding.cardTasks.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_tasks)
        }

        binding.cardHabits.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_habits)
        }
    }

    private fun loadDashboardData() {
        val userId = auth.currentUser?.uid ?: return

        // Load task count
        db.collection("users").document(userId)
            .collection("tasks")
            .whereEqualTo("isCompleted", false)
            .get()
            .addOnSuccessListener { documents ->
                binding.tvTaskCount.text = "${documents.size()}"
            }

        // Load today's mood
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        db.collection("users").document(userId)
            .collection("moods")
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    binding.tvMoodStatus.text = "Not logged today"
                } else {
                    binding.tvMoodStatus.text = "Logged today ✓"
                }
            }

        // Load active habits
        db.collection("users").document(userId)
            .collection("habits")
            .get()
            .addOnSuccessListener { documents ->
                binding.tvHabitCount.text = "${documents.size()}"
            }

        // Load journal entries this week
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        db.collection("users").document(userId)
            .collection("journal_entries")
            .whereGreaterThanOrEqualTo("timestamp", startOfWeek)
            .get()
            .addOnSuccessListener { documents ->
                binding.tvJournalCount.text = "${documents.size()} this week"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}