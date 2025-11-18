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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_universe.Adapters.MoodEntryAdapter
import com.example.prog7314_universe.viewmodel.ViewMode

/**
 * Fragment for Mood Tracker dashboard
 * Shows monthly mood calendar and weekly statistics
 */
class MoodTrackerFragment : Fragment() {

    private var _binding: FragmentMoodtrackerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoodViewModel by viewModels()
    private lateinit var moodCalendarAdapter: MoodCalendarAdapter
    private lateinit var moodEntryAdapter: MoodEntryAdapter

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

        setupCalendar()
        setupMoodList()
        setupObservers()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshWeeklyStats()
    }

    private fun setupCalendar() {
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

      //  binding.rvMoodCalendar.apply {
          //  layoutManager = GridLayoutManager(requireContext(), 7) // 7 days per week
  //          adapter = moodCalendarAdapter
    //    }
    }

    private fun setupMoodList() {
        moodEntryAdapter = MoodEntryAdapter()
        binding.rvMoodEntries.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = moodEntryAdapter
        }
    }

    private fun setupObservers() {
        // Observe selected month
     //   viewModel.selectedMonth.observe(viewLifecycleOwner) { month ->
        //    updateMonthDisplay(month)
  //          updateCalendar()
    //    }

        viewModel.selectedDate.observe(viewLifecycleOwner) { date ->
            updateDateDisplay(date)
            updateMoodList()
        }

        viewModel.viewMode.observe(viewLifecycleOwner) { mode ->
            updateViewModeUI(mode)
            viewModel.selectedDate.value?.let { updateDateDisplay(it) }
            updateMoodList()
        }

        // Observe mood entries
        viewModel.moodEntries.observe(viewLifecycleOwner) { entries ->
            updateCalendar()
            viewModel.refreshWeeklyStats()
            updateMoodList()
        }

        // Observe weekly stats
        viewModel.weeklyMoodStats.observe(viewLifecycleOwner) { stats ->
            updateWeeklyStats(stats)
        }
    }

    private fun setupClickListeners() {
        // Previous month button
     //   binding.btnPreviousMonth.setOnClickListener {
    //        viewModel.previousMonth()
    //    }

        // Next month button
   //     binding.btnNextMonth.setOnClickListener {
     //       viewModel.nextMonth()
     //   }

        binding.chipDaily.setOnClickListener {
            viewModel.setViewMode(ViewMode.DAILY)
        }

        binding.chipWeekly.setOnClickListener {
            viewModel.setViewMode(ViewMode.WEEKLY)
        }

        binding.btnPrevious.setOnClickListener {
            viewModel.navigatePrevious()
        }

        binding.btnNext.setOnClickListener {
            viewModel.navigateNext()
        }

        // Today's mood button
        binding.btnTodaysMood.setOnClickListener {
            findNavController().navigate(
                R.id.action_moodTrackerFragment_to_createMoodFragment
            )
        }

        // Edit mood button
        binding.btnEditMood.setOnClickListener {
            val args = Bundle().apply {
                putLong("selected_date", Date().time)
            }
            findNavController().navigate(
                R.id.action_moodTrackerFragment_to_createMoodFragment,
                args
            )
        }
    }

   // private fun updateMonthDisplay(month: Date) {
   //     val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
   //     binding.tvMonthYear.text = dateFormat.format(month)
   // }

    private fun updateDateDisplay(date: Date) {
        val mode = viewModel.viewMode.value
        if (mode == ViewMode.WEEKLY) {
            val cal = Calendar.getInstance().apply { time = date; set(Calendar.DAY_OF_WEEK, firstDayOfWeek) }
            val start = SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
            cal.add(Calendar.DAY_OF_WEEK, 6)
            val end = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(cal.time)
            binding.tvDateRange.text = "$start - $end"
        } else {
            val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            binding.tvDateRange.text = format.format(date)
        }
    }

    private fun updateCalendar() {
        val dates = viewModel.getDatesInMonth()
        val calendarItems = dates.map { date ->
            MoodCalendarItem(date, viewModel.getMoodForDate(date))
        }

        moodCalendarAdapter.submitList(calendarItems)
    }

    private fun updateViewModeUI(mode: ViewMode?) {
        binding.chipDaily.isChecked = mode != ViewMode.WEEKLY
        binding.chipWeekly.isChecked = mode == ViewMode.WEEKLY
    }

    private fun updateMoodList() {
        val entries = when (viewModel.viewMode.value) {
            ViewMode.WEEKLY -> viewModel.getEntriesForWeek()
            else -> viewModel.selectedDate.value?.let { viewModel.getEntriesForDate(it) } ?: emptyList()
        }

        moodEntryAdapter.submitList(entries)
        if (entries.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvMoodEntries.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvMoodEntries.visibility = View.VISIBLE
        }
    }

    private fun updateWeeklyStats(stats: Map<MoodScale, Int>) {
        // Calculate total moods logged
        val total = stats.values.sum()

        val weekRange = getCurrentWeekRange()

        binding.tvWeekRange.text = getString(
            R.string.mood_week_range,
            weekRange.first,
            weekRange.second
        )
        binding.tvTotalMoods.text = resources.getQuantityString(
            R.plurals.mood_total_format,
            total,
            total
        )

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

private fun getCurrentWeekRange(): Pair<String, String> {
    val calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
    }

    val start = calendar.time
    calendar.add(Calendar.DAY_OF_WEEK, 6)
    val end = calendar.time

    val format = SimpleDateFormat("MMM d", Locale.getDefault())
    return format.format(start) to format.format(end)
}

/**
 * Data class for calendar items
 */
data class MoodCalendarItem(
    val date: Date,
    val moodEntry: com.example.prog7314_universe.Models.MoodEntry?
)




