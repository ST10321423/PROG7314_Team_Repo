package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*

class CreateHabitActivity : AppCompatActivity() {

    private lateinit var habitNameEditText: EditText
    private lateinit var dayViews: List<TextView>
    private lateinit var endDateButton: Button
    private lateinit var difficultySpinner: Spinner
    private lateinit var addHabitButton: Button
    private lateinit var reminderSwitch: SwitchMaterial

    private val selectedDays = mutableSetOf<Int>()
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_habit)

        initializeViews()
        setupDaySelection()
        setupSpinner()
        setupClickListeners()
    }

    private fun initializeViews() {
        habitNameEditText = findViewById(R.id.habitNameEditText)
        endDateButton = findViewById(R.id.endDateButton)
        difficultySpinner = findViewById(R.id.difficultySpinner)
        addHabitButton = findViewById(R.id.addHabitButton)
        reminderSwitch = findViewById(R.id.reminderSwitch)

        // Initialize day views
        dayViews = listOf(
            findViewById(R.id.sunday),
            findViewById(R.id.monday),
            findViewById(R.id.tuesday),
            findViewById(R.id.wednesday),
            findViewById(R.id.thursday),
            findViewById(R.id.friday),
            findViewById(R.id.saturday)
        )
    }

    private fun setupDaySelection() {
        dayViews.forEachIndexed { index, textView ->
            textView.setOnClickListener {
                toggleDaySelection(index, textView)
            }
        }
    }

    private fun toggleDaySelection(dayIndex: Int, dayView: TextView) {
        if (selectedDays.contains(dayIndex)) {
            selectedDays.remove(dayIndex)
            dayView.setBackgroundResource(R.drawable.day_unselected)
            dayView.setTextColor(resources.getColor(android.R.color.black, theme))
        } else {
            selectedDays.add(dayIndex)
            dayView.setBackgroundResource(R.drawable.day_selected)
            dayView.setTextColor(resources.getColor(android.R.color.white, theme))
        }
    }

    private fun setupSpinner() {
        val difficulties = arrayOf("Easy", "Medium", "Hard")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        difficultySpinner.adapter = adapter
    }

    private fun setupClickListeners() {
        endDateButton.setOnClickListener {
            showDatePicker()
        }

        addHabitButton.setOnClickListener {
            createHabit()
        }

        // Set default selections
        findViewById<RadioButton>(R.id.regularHabit).isChecked = true
        findViewById<RadioButton>(R.id.daily).isChecked = true
        findViewById<RadioButton>(R.id.morning).isChecked = true


    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }
                endDateButton.text = String.format(
                    "%02d/%02d/%d",
                    selectedMonth + 1,
                    selectedDay,
                    selectedYear
                )
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun createHabit() {
        val habitName = habitNameEditText.text.toString().trim()

        if (habitName.isEmpty()) {
            Toast.makeText(this, "Please enter a habit name", Toast.LENGTH_SHORT).show()
            return
        }

        // Get selected values
        val habitType = if (findViewById<RadioButton>(R.id.regularHabit).isChecked) "Regular" else "OneTime"
        val repeat = when {
            findViewById<RadioButton>(R.id.daily).isChecked -> "Daily"
            findViewById<RadioButton>(R.id.weekly).isChecked -> "Weekly"
            findViewById<RadioButton>(R.id.monthly).isChecked -> "Monthly"
            else -> "Daily"
        }
        val timeOfDay = when {
            findViewById<RadioButton>(R.id.morning).isChecked -> "Morning"
            findViewById<RadioButton>(R.id.afternoon).isChecked -> "Afternoon"
            findViewById<RadioButton>(R.id.night).isChecked -> "Night"
            else -> "Morning"
        }
        val difficulty = difficultySpinner.selectedItem as String
        val hasReminder = reminderSwitch.isChecked
        val endDate = endDateButton.text.toString()

        // Here you would save the habit to your database
        val habit = Habit(
            name = habitName,
            type = habitType,
            repeat = repeat,
            selectedDays = selectedDays.toList(),
            timeOfDay = timeOfDay,
            difficulty = difficulty,
            endDate = endDate,
            hasReminder = hasReminder
        )

        Toast.makeText(this, "Habit created successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }
}

// Data class for habit
data class Habit(
    val name: String,
    val type: String,
    val repeat: String,
    val selectedDays: List<Int>,
    val timeOfDay: String,
    val difficulty: String,
    val endDate: String,
    val hasReminder: Boolean
)