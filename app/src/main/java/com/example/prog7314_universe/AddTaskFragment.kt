package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.prog7314_universe.databinding.ActivityAddTaskBinding
import com.example.prog7314_universe.utils.navigator
import com.example.prog7314_universe.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTaskFragment : Fragment() {

    private var _binding: ActivityAddTaskBinding? = null
    private val binding get() = _binding!!

    private val calendar = Calendar.getInstance()
    private var selectedDateIso: String = ""

    private val vm: TaskViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editTextExamDate.setOnClickListener { showDatePicker() }
        binding.btnSaveTask.setOnClickListener { saveTask() }
        setupBottomNavigation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                val displayFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                val selectedDateDisplay = displayFormat.format(calendar.time)
                selectedDateIso = calendar.toInstant().toString()
                binding.editTextExamDate.setText(selectedDateDisplay)
            },
            year,
            month,
            day
        ).show()
    }

    private fun saveTask() {
        val title = binding.etTaskTitle.text.toString().trim()
        val desc = binding.editTextTaskDescription.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show()
            binding.etTaskTitle.requestFocus()
            return
        }

        if (selectedDateIso.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        vm.addTask(title, desc.ifEmpty { null }, selectedDateIso)
        navigator().popBackStack()
    }

    private fun setupBottomNavigation() = with(binding.bottomNavigationView) {
        selectedItemId = R.id.tasks

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
                R.id.exams -> {
                    navigator().openFragment(ExamsFragment(), addToBackStack = false, clearBackStack = true)
                    true
                }
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
}