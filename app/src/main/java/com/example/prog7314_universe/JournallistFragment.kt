package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_universe.Adapters.JournalAdapter
import com.example.prog7314_universe.R
import com.example.prog7314_universe.databinding.FragmentJournallistBinding
import com.example.prog7314_universe.viewmodel.JournalViewModel
import com.example.prog7314_universe.viewmodel.ViewMode
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for Journal list view
 * Shows daily or weekly journal entries
 */
class JournalListFragment : Fragment() {

    private var _binding: FragmentJournallistBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels()
    private lateinit var journalAdapter: JournalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournallistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        journalAdapter = JournalAdapter(
            onItemClick = { entry ->
                // Navigate to view/edit entry
                val bundle = Bundle().apply {
                    putString("entry_id", entry.entryId)
                }
                findNavController().navigate(
                    R.id.action_journalListFragment_to_createJournalFragment,
                    bundle
                )
            },
            onDeleteClick = { entry ->
                viewModel.deleteJournalEntry(entry.entryId)
            }
        )

        binding.rvJournalEntries.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = journalAdapter
        }
    }

    private fun setupObservers() {
        // Observe selected date
        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            updateDateDisplay(date)
            updateEntries()
        }

        // Observe view mode
        viewModel.viewMode.observe(viewLifecycleOwner) { mode ->
            updateViewModeUI(mode)
            updateEntries()
        }

        // Observe journal entries
        viewModel.journalEntries.observe(viewLifecycleOwner) {
            updateEntries()
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // View mode toggle
        binding.chipDaily.setOnClickListener {
            viewModel.setViewMode(ViewMode.DAILY)
        }

        binding.chipWeekly.setOnClickListener {
            viewModel.setViewMode(ViewMode.WEEKLY)
        }

        // Navigation buttons
        binding.btnPrevious.setOnClickListener {
            viewModel.navigatePrevious()
        }

        binding.btnNext.setOnClickListener {
            viewModel.navigateNext()
        }

        // Add new entry button
        binding.fabAddEntry.setOnClickListener {
            findNavController().navigate(
                R.id.action_journalListFragment_to_createJournalFragment
            )
        }
    }

    private fun updateDateDisplay(date: Date) {
        val mode = viewModel.viewMode.value
        if (mode == ViewMode.WEEKLY) {
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            }

            val startDate = SimpleDateFormat("MMM d", Locale.getDefault()).format(calendar.time)

            calendar.add(Calendar.DAY_OF_WEEK, 6)
            val endDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(calendar.time)

            binding.tvDateRange.text = "$startDate - $endDate"
        } else {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            binding.tvDateRange.text = dateFormat.format(date)
        }
    }

    private fun updateViewModeUI(mode: ViewMode) {
        binding.apply {
            when (mode) {
                ViewMode.DAILY -> {
                    chipDaily.isChecked = true
                    chipWeekly.isChecked = false
                }
                ViewMode.WEEKLY -> {
                    chipDaily.isChecked = false
                    chipWeekly.isChecked = true
                }
            }
        }
    }

    private fun updateEntries() {
        val entries = when (viewModel.viewMode.value) {
            ViewMode.DAILY -> {
                viewModel.selectedDate.value?.let { date ->
                    viewModel.getEntriesForDate(date)
                } ?: emptyList()
            }
            ViewMode.WEEKLY -> viewModel.getEntriesForWeek()
            else -> emptyList()
        }

        journalAdapter.submitList(entries)

        // Show/hide empty state
        if (entries.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvJournalEntries.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvJournalEntries.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




