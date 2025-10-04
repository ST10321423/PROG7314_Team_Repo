package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.prog7314_universe.databinding.ActivityCreateHabitBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateHabitActivity : AppCompatActivity() {

    private lateinit var b: ActivityCreateHabitBinding

    private val selectedDays = mutableSetOf<Int>() // 0..6 (Sun..Sat)
    private val calendar = Calendar.getInstance()
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCreateHabitBinding.inflate(layoutInflater)
        setContentView(b.root)

        initializeViews()
        setupDaySelection()
        setupSpinner()
        setupClickListeners()
    }

    private fun initializeViews() {
        // Set defaults
        b.regularHabit.isChecked = true
        b.daily.isChecked = true
        b.morning.isChecked = true


        b.endDateButton.text = "Select Date"
    }

    private fun setupDaySelection() {

        val dayViews: List<TextView> = listOf(
            b.sunday, b.monday, b.tuesday, b.wednesday, b.thursday, b.friday, b.saturday
        )

        dayViews.forEachIndexed { index, tv ->
            tv.setOnClickListener { toggleDaySelection(index, tv) }
        }
    }

    private fun toggleDaySelection(dayIndex: Int, dayView: TextView) {
        val selectedBg = R.drawable.day_selected
        val unselectedBg = R.drawable.day_unselected
        val selectedText = ContextCompat.getColor(this, android.R.color.white)
        val unselectedText = ContextCompat.getColor(this, android.R.color.black)

        if (selectedDays.contains(dayIndex)) {
            selectedDays.remove(dayIndex)
            dayView.setBackgroundResource(unselectedBg)
            dayView.setTextColor(unselectedText)
        } else {
            selectedDays.add(dayIndex)
            dayView.setBackgroundResource(selectedBg)
            dayView.setTextColor(selectedText)
        }
    }

    private fun setupSpinner() {
        val difficulties = listOf("Easy", "Medium", "Hard")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficulties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.difficultySpinner.adapter = adapter
    }

    private fun setupClickListeners() {
        b.endDateButton.setOnClickListener { showDatePicker() }
        b.addHabitButton.setOnClickListener { createHabit() }


        b.repeatGroup.setOnCheckedChangeListener { _, checkedId ->
            val weeklySelected = checkedId == R.id.weekly
            b.sunday.parent?.let { it as? android.view.View }?.visibility =
                if (weeklySelected) android.view.View.VISIBLE else android.view.View.GONE
            if (!weeklySelected) selectedDays.clear()
        }
    }

    private fun showDatePicker() {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH)
        val d = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, yy, mm, dd ->
            calendar.set(yy, mm, dd, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            b.endDateButton.text = dateFmt.format(calendar.time)
        }, y, m, d).show()
    }

    private fun createHabit() {
        val habitName = b.habitNameEditText.text?.toString()?.trim().orEmpty()
        if (habitName.isEmpty()) {
            toast("Please enter a habit name"); return
        }


        val habitType = if (b.regularHabit.isChecked) "Regular" else "OneTime"
        val repeat = when (b.repeatGroup.checkedRadioButtonId) {
            R.id.daily -> "Daily"
            R.id.weekly -> "Weekly"
            R.id.monthly -> "Monthly"
            else -> "Daily"
        }

        if (repeat == "Weekly" && selectedDays.isEmpty()) {
            toast("Select at least one day for a weekly habit"); return
        }

        val timeOfDay = when (b.timeGroup.checkedRadioButtonId) {
            R.id.morning -> "Morning"
            R.id.afternoon -> "Afternoon"
            R.id.night -> "Night"
            else -> "Morning"
        }
        val difficulty = (b.difficultySpinner.selectedItem as? String) ?: "Easy"
        val hasReminder = b.reminderSwitch.isChecked
        val endDate = b.endDateButton.text?.toString()?.takeIf { it.isNotBlank() && it != "Select Date" } ?: ""


        val habit = Habit(
            name = habitName,
            type = habitType,
            repeat = repeat,
            selectedDays = selectedDays.toList().sorted(),
            timeOfDay = timeOfDay,
            difficulty = difficulty,
            endDate = endDate,          // String yyyy-MM-dd or ""
            hasReminder = hasReminder
        )

        // --- Firestore SAVE ---
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { toast("Not signed in"); return }

        val db = Firebase.firestore
        val docRef = db.collection("users").document(uid).collection("habits").document()

        val payload = hashMapOf(
            "id" to docRef.id,
            "name" to habit.name,
            "type" to habit.type,                  // Regular | OneTime
            "repeat" to habit.repeat,              // Daily | Weekly | Monthly
            "selectedDays" to habit.selectedDays,  // [0..6]
            "timeOfDay" to habit.timeOfDay,        // Morning | Afternoon | Night
            "difficulty" to habit.difficulty,      // Easy | Medium | Hard
            "endDate" to habit.endDate,            // String
            "hasReminder" to habit.hasReminder,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )

        b.addHabitButton.isEnabled = false

        docRef.set(payload)
            .addOnSuccessListener {
                toast("Habit created successfully!")
                finish()
            }
            .addOnFailureListener { e ->
                b.addHabitButton.isEnabled = true
                toast("Failed to save: ${e.message}")
            }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// ---- Top-level model ---
data class Habit(
    val name: String,
    val type: String,             // Regular | OneTime
    val repeat: String,           // Daily | Weekly | Monthly
    val selectedDays: List<Int>,  // 0..6 = Sun..Sat (only for Weekly)
    val timeOfDay: String,        // Morning | Afternoon | Night
    val difficulty: String,       // Easy | Medium | Hard
    val endDate: String,          // "yyyy-MM-dd" or ""
    val hasReminder: Boolean
)

