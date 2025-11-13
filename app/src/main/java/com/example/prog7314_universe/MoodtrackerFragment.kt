package com.example.prog7314_universe


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.prog7314_universe.Adapters.MoodCalendarAdapter
import com.example.prog7314_universe.R
import com.example.prog7314_universe.databinding.FragmentMoodtrackerBinding
import com.example.prog7314_universe.Models.MoodScale
import com.example.prog7314_universe.viewmodel.MoodViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for Mood Tracker dashboard
 * Shows monthly mood calendar and weekly statistics
 */
class MoodTrackerFragment : Fragment() {

    private var _binding: FragmentMoodtrackerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoodViewModel by viewModels()
    private lateinit var moodCalendarAdapter: MoodCalendarAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoodtrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        moodCalendarAdapter = MoodCalendarAdapter { date ->
            // Navigate to edit mood for selected date
            val bundle = Bundle().apply {
                putLong("selected_date", date.time)
            }
            findNavController().navigate(
                R.id.action_moodTrackerFragment_to_createMoodFragment,
                bundle
            )
        }

        binding.rvMoodCalendar.apply {
            layoutManager = GridLayoutManager(requireContext(), 7) // 7 days per week
            adapter = moodCalendarAdapter
        }
    }

    private fun setupObservers() {
        // Observe selected month
        viewModel.selectedMonth.observe(viewLifecycleOwner) { month ->
            updateMonthDisplay(month)
            updateCalendar()
        }

        // Observe mood entries
        viewModel.moodEntries.observe(viewLifecycleOwner) { entries ->
            updateCalendar()
        }

        // Observe weekly stats
        viewModel.weeklyMoodStats.observe(viewLifecycleOwner) { stats ->
            updateWeeklyStats(stats)
        }
    }

    private fun setupClickListeners() {
        // Previous month button
        binding.btnPreviousMonth.setOnClickListener {
            viewModel.previousMonth()
        }

        // Next month button
        binding.btnNextMonth.setOnClickListener {
            viewModel.nextMonth()
        }

        // Today's mood button
        binding.btnTodaysMood.setOnClickListener {
            findNavController().navigate(
                R.id.action_moodTrackerFragment_to_createMoodFragment
            )
        }

        // Edit mood button
        binding.btnEditMood.setOnClickListener {
            findNavController().navigate(
                R.id.action_moodTrackerFragment_to_createMoodFragment
            )
        }
    }

    private fun updateMonthDisplay(month: Date) {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvMonthYear.text = dateFormat.format(month)
    }

    private fun updateCalendar() {
        val dates = viewModel.getDatesInMonth()
        val moodMap = viewModel.moodEntries.value?.associateBy { it.date } ?: emptyMap()

        val calendarItems = dates.map { date ->
            MoodCalendarItem(date, viewModel.getMoodForDate(date))
        }

        moodCalendarAdapter.submitList(calendarItems)
    }

    private fun updateWeeklyStats(stats: Map<MoodScale, Int>) {
        // Calculate total moods logged
        val total = stats.values.sum()

        // Update stat cards
        binding.apply {
            // Happy
            val happyCount = stats[MoodScale.HAPPY] ?: 0
            tvHappyCount.text = happyCount.toString()
            progressHappy.progress = if (total > 0) (happyCount * 100 / total) else 0

            // Sad
            val sadCount = stats[MoodScale.SAD] ?: 0
            tvSadCount.text = sadCount.toString()
            progressSad.progress = if (total > 0) (sadCount * 100 / total) else 0

            // Angry
            val angryCount = stats[MoodScale.ANGRY] ?: 0
            tvAngryCount.text = angryCount.toString()
            progressAngry.progress = if (total > 0) (angryCount * 100 / total) else 0

            // Fear
            val fearCount = stats[MoodScale.FEAR] ?: 0
            tvFearCount.text = fearCount.toString()
            progressFear.progress = if (total > 0) (fearCount * 100 / total) else 0

            // Disgust
            val disgustCount = stats[MoodScale.DISGUST] ?: 0
            tvDisgustCount.text = disgustCount.toString()
            progressDisgust.progress = if (total > 0) (disgustCount * 100 / total) else 0
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Data class for calendar items
 */
data class MoodCalendarItem(
    val date: Date,
    val moodEntry: com.example.prog7314_universe.Models.MoodEntry?
)




