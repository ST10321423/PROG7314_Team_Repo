package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.prog7314_universe.databinding.ActivityDashboardBinding
import com.example.prog7314_universe.utils.getWithOfflineFallback
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: ActivityDashboardBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (auth.currentUser == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        setupClickListeners()
        loadUserData()
        loadDashboardStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupClickListeners() = with(binding) {
        addTaskButton.setOnClickListener {
            findNavController().navigate(R.id.addTaskFragment)
        }

        ivProfile.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        cardTasksCompleted.setOnClickListener {
            findNavController().navigate(R.id.tasksListFragment)
        }

        cardStudyHours.setOnClickListener {
            Toast.makeText(requireContext(), "Study hours tracking coming soon", Toast.LENGTH_SHORT).show()
        }

        cardCourses.setOnClickListener {
            Toast.makeText(requireContext(), "Courses screen coming soon", Toast.LENGTH_SHORT).show()
        }

        tvSeeAllAssignments.setOnClickListener {
            findNavController().navigate(R.id.tasksListFragment)
        }

        tvSeeAllCourses.setOnClickListener {
            Toast.makeText(requireContext(), "Courses screen coming soon", Toast.LENGTH_SHORT).show()
        }

        tvSeeAllSchedule.setOnClickListener {
            Toast.makeText(requireContext(), "Schedule screen coming soon", Toast.LENGTH_SHORT).show()
        }
    }



    private fun loadUserData() {
        val user = auth.currentUser
        val displayName = user?.displayName ?: "User"
        binding.tvUserName.text = "Hello, $displayName!"

        val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        binding.tvDate.text = currentDate
    }

    private fun loadDashboardStats() {
        val uid = auth.currentUser?.uid ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                loadTasksStats(uid)
                loadExamsStats(uid)
                loadHabitsStats(uid)
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "Error loading stats: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun loadTasksStats(uid: String) {
        val tasksRef = db.collection("users").document(uid).collection("tasks")
        val snapshot = tasksRef.getWithOfflineFallback()
        val tasksCompleted = snapshot.documents.count { it.getBoolean("completed") == true }
        binding.tvTasksCompleted.text = "$tasksCompleted"
    }

    private suspend fun loadExamsStats(uid: String) {
        val examsRef = db.collection("users").document(uid).collection("exams")
        val snapshot = examsRef.getWithOfflineFallback()
        val coursesCount = snapshot.size()
        binding.tvCoursesCount.text = "$coursesCount"
    }

    private suspend fun loadHabitsStats(uid: String) {
        val habitsRef = db.collection("users").document(uid).collection("habits")
        val snapshot = habitsRef.getWithOfflineFallback()
        val studyHours = snapshot.size() * 2
        binding.tvStudyHours.text = "$studyHours"
    }
}