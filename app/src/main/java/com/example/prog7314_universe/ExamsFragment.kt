package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_universe.Adapters.ExamAdapter
import com.example.prog7314_universe.Models.Exam
import com.example.prog7314_universe.databinding.ActivityExamsBinding
import com.example.prog7314_universe.utils.navigator
import com.example.prog7314_universe.viewmodel.ExamViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ExamsFragment : Fragment() {

    private var _binding: ActivityExamsBinding? = null
    private val binding get() = _binding!!

    private lateinit var examAdapter: ExamAdapter
    private val vm: ExamViewModel by activityViewModels()

    private var allExams = listOf<Exam>()
    private var showingCompleted = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityExamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupBottomNavigation()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        examAdapter = ExamAdapter(
            examList = emptyList(),
            onEdit = { exam -> editExam(exam) },
            onDelete = { exam -> showDeleteDialog(exam) },
            onToggleComplete = { exam, isCompleted ->
                vm.toggleCompleted(exam.id, isCompleted)
            }
        )

        binding.recyclerViewExams.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = examAdapter
        }
    }

    private fun setupClickListeners() = with(binding) {
        fabAddExam.setOnClickListener {
            navigator().openFragment(AddExamFragment.newInstance(null))
        }

        tvFilterAll.setOnClickListener {
            showingCompleted = false
            updateFilterTabs()
            filterExams()
        }

        tvFilterCompleted.setOnClickListener {
            showingCompleted = true
            updateFilterTabs()
            filterExams()
        }

        tvSearch.setOnClickListener {
            Toast.makeText(requireContext(), "Search coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() = with(binding.bottomNavigationView) {
        selectedItemId = R.id.exams

        setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> {
                    navigator().openFragment(DashboardFragment(), addToBackStack = false, clearBackStack = true)
                    true
                }
                R.id.tasks -> {
                    navigator().openFragment(TasksFragment(), addToBackStack = false, clearBackStack = true)
                    true
                }
                R.id.exams -> true
                R.id.habits -> {
                    navigator().openFragment(HabitListFragment(), addToBackStack = false, clearBackStack = true)
                    true
                }
                R.id.settings -> {
                    navigator().openFragment(SettingsFragment(), addToBackStack = false, clearBackStack = true)
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        vm.exams.observe(viewLifecycleOwner) { exams ->
            allExams = exams
            filterExams()
            updateEmptyState()
            updateFilterCounts(exams)
        }

        vm.loading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        vm.error.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrBlank()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        vm.operationSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Operation successful", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterExams() {
        val filtered = if (showingCompleted) {
            allExams.filter { it.isCompleted }
        } else {
            allExams
        }
        examAdapter.updateList(filtered)
    }

    private fun updateFilterTabs() = with(binding) {
        if (showingCompleted) {
            tvFilterAll.apply {
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                setBackgroundResource(0)
            }
            tvFilterCompleted.apply {
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                setBackgroundResource(R.drawable.tab_active_background)
            }
        } else {
            tvFilterAll.apply {
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                setBackgroundResource(R.drawable.tab_active_background)
            }
            tvFilterCompleted.apply {
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
                setBackgroundResource(0)
            }
        }
    }

    private fun updateFilterCounts(exams: List<Exam>) = with(binding) {
        val completedCount = exams.count { it.isCompleted }
        val totalCount = exams.size
        tvFilterAll.text = "All ($totalCount)"
        tvFilterCompleted.text = "Completed ($completedCount)"
    }

    private fun updateEmptyState() = with(binding) {
        val isEmpty = if (showingCompleted) {
            allExams.none { it.isCompleted }
        } else {
            allExams.isEmpty()
        }

        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerViewExams.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun editExam(exam: Exam) {
        navigator().openFragment(AddExamFragment.newInstance(exam))
    }

    private fun showDeleteDialog(exam: Exam) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Exam")
            .setMessage("Are you sure you want to delete '${exam.subject}'?")
            .setPositiveButton("Delete") { _, _ ->
                vm.deleteExam(exam.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}