package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prog7314_universe.databinding.FragmentHomeBinding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.prog7314_universe.viewmodel.JournalViewModel
import com.example.prog7314_universe.viewmodel.MoodViewModel
import com.example.prog7314_universe.viewmodel.TaskViewModel
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
    private val moodViewModel: MoodViewModel by viewModels()
    private val journalViewModel: JournalViewModel by viewModels()
    private val taskViewModel: TaskViewModel by activityViewModels()

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
        observeDashboardData()
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
            findNavController().navigate(R.id.action_homeFragment_to_moodTrackerFragment)
        }

        binding.cardJournal.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_journalListFragment)
        }

        binding.cardTasks.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_tasksListFragment)
        }

        binding.cardHabits.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_habitListFragment)
        }
    }

    private fun observeDashboardData() {
        val userId = auth.currentUser?.uid ?: return

        if (_binding != null) {
            // Tasks
            taskViewModel.tasks.observe(viewLifecycleOwner) { tasks ->
                binding.tvTaskCount.text = tasks.count { !it.isCompleted }.toString()
            }
            taskViewModel.refresh()

            // Moods
            moodViewModel.moodEntries.observe(viewLifecycleOwner) { moods ->
                val todayMood = moods.firstOrNull { isSameDay(it.date.toDate(), Date()) }
                binding.tvMoodStatus.text = todayMood?.let { entry ->
                    "Logged: ${entry.getMoodScale().displayName}"
                } ?: "Not logged today"
            }

            // Load active habits
            db.collection("users").document(userId)
                .collection("habits")
                .addSnapshotListener { snapshot, _ ->
                    binding.tvHabitCount.text = snapshot?.size()?.toString() ?: "0"
                }

            journalViewModel.journalEntries.observe(viewLifecycleOwner) { entries ->
                val startOfWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val weekCount = entries.count { it.createdAt?.toDate()?.after(startOfWeek) == true }
                binding.tvJournalCount.text = "$weekCount this week"
            }
        }
    }

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = d1 }
        val c2 = Calendar.getInstance().apply { time = d2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}