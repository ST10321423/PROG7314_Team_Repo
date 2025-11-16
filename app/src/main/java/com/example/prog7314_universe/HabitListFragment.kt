package com.example.prog7314_universe

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_universe.Adapters.HabitAdapter
import com.example.prog7314_universe.Adapters.HabitUi
import com.example.prog7314_universe.Models.DateKeys
import com.example.prog7314_universe.Models.Habit
import com.example.prog7314_universe.databinding.ActivityHabitListBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDate

class HabitListFragment : Fragment() {

    private var _binding: ActivityHabitListBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var habitsListener: ListenerRegistration? = null

    private val adapter by lazy {
        HabitAdapter(
            onToggle = { habit, checked -> toggleCompletion(habit, checked) },
            onEdit = { habit -> navigateToHabitEditor(habit.habitId) }
        )
    }

    private var timeFilter: String? = null
    private var statusFilter: StatusFilter = StatusFilter.ALL
    private var allHabits = listOf<HabitUi>()

    enum class StatusFilter {
        ALL, DUE_TODAY, COMPLETED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityHabitListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSpinner()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
    }

    private fun setupSpinner() {
        val items = listOf("All Times", "Morning", "Afternoon", "Evening")
        binding.spinnerTime.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            items
        )

        binding.spinnerTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                timeFilter = null
                observeHabits()
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                timeFilter = when (pos) {
                    1 -> "morning"
                    2 -> "afternoon"
                    3 -> "evening"
                    else -> null
                }
                observeHabits()
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAdd.setOnClickListener {
            navigateToHabitEditor(null)
        }

        binding.tvSearch.setOnClickListener {
            Toast.makeText(requireContext(), "Search coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.tvFilterAll.setOnClickListener {
            statusFilter = StatusFilter.ALL
            updateFilterTabs()
            filterHabits()
        }

        binding.tvFilterDueToday.setOnClickListener {
            statusFilter = StatusFilter.DUE_TODAY
            updateFilterTabs()
            filterHabits()
        }

        binding.tvFilterCompleted.setOnClickListener {
            statusFilter = StatusFilter.COMPLETED
            updateFilterTabs()
            filterHabits()
        }
    }

    override fun onStart() {
        super.onStart()
        observeHabits()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeHabits() {
        val uid = auth.currentUser?.uid ?: return
        val col = db.collection("users").document(uid).collection("habits")

        // Remove previous listener if exists
        habitsListener?.remove()

        binding.progressBar.isVisible = true
        habitsListener = col.orderBy("name").addSnapshotListener { snap, err ->
            binding.progressBar.isVisible = false
            if (err != null || snap == null) {
                Toast.makeText(requireContext(), "Error loading habits", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            val now = System.currentTimeMillis()
            allHabits = snap.documents.mapNotNull { doc ->
                doc.toObject(Habit::class.java)?.let { habit ->
                    HabitUi.fromHabit(habit.copy(habitId = doc.id), now)
                }
            }
            filterHabits()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun filterHabits() {
        var filtered = allHabits

        timeFilter?.let { filter ->
            filtered = filtered.filter { it.timeOfDay.equals(filter, true) == true}
        }

        filtered = when (statusFilter) {
            StatusFilter.ALL -> filtered
            StatusFilter.DUE_TODAY -> filtered.filter { habit ->
                val today = LocalDate.now().dayOfWeek
                val bit = DateKeys.dayOfWeekToMask(today)
                habit.daysMask and bit != 0 && !habit.isCompleted
            }
            StatusFilter.COMPLETED -> filtered.filter { it.isCompleted }
        }

        adapter.submitList(filtered)
        binding.tvEmptyState.isVisible = filtered.isEmpty()
        updateFilterCounts()
    }


    private fun navigateToHabitEditor(habitId: String?) {
        findNavController().navigate(
            R.id.action_habitListFragment_to_addEditHabitFragment,
            bundleOf("habitId" to habitId)
        )
    }

    private fun updateFilterTabs() = with(binding) {
            tvFilterAll.isSelected = statusFilter == StatusFilter.ALL
            tvFilterDueToday.isSelected = statusFilter == StatusFilter.DUE_TODAY
            tvFilterCompleted.isSelected = statusFilter == StatusFilter.COMPLETED
    }

    private fun updateFilterCounts() = with(binding) {
        tvFilterAll.text = "All (${allHabits.size})"
        val todayMask = DateKeys.dayOfWeekToMask(LocalDate.now().dayOfWeek)
        val dueCount = allHabits.count { habit ->
            habit.daysMask and todayMask != 0 && !habit.isCompleted
        }
        val completedCount = allHabits.count { it.isCompleted }
        tvFilterDueToday.text = "Due Today ($dueCount)"
        tvFilterCompleted.text = "Completed ($completedCount)"
    }

    private fun toggleCompletion(habit: HabitUi, checked: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val doc = db.collection("users").document(uid)
            .collection("habits").document(habit.habitId)

        val now = Timestamp.now()
        val lastCompletedValue = if (checked) now else null
        val streakValue: Any = if (checked) FieldValue.increment(1) else 0

        doc.update(
            "lastCompleted", lastCompletedValue,
            "isCompleted", checked,
            "updatedAt", now,
            "streak", streakValue
        ).addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to update habit", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        habitsListener?.remove()
        _binding = null
    }
}