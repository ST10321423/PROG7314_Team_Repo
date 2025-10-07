package com.example.prog7314_universe

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var descInput: EditText
    private lateinit var dateInput: EditText
    private lateinit var btnSave: Button

    private val calendar = Calendar.getInstance()
    private var selectedDateDisplay: String = ""
    private var selectedDateIso: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        titleInput = findViewById(R.id.etTaskTitle)
        descInput = findViewById(R.id.editTextTaskDescription)
        dateInput = findViewById(R.id.editTextExamDate)
        btnSave = findViewById(R.id.btnSaveTask)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Open Date Picker when user clicks the date field
        dateInput.setOnClickListener {
            showDatePicker()
        }

        btnSave.setOnClickListener {
            saveTask()
        }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                // Normalized to start-of-day so API receives a consistent instant
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)

                val displayFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                selectedDateDisplay = displayFormat.format(calendar.time)
                selectedDateIso = calendar.toInstant().toString()
                dateInput.setText(selectedDateDisplay)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun saveTask() {
        val title = titleInput.text.toString().trim()
        val desc = descInput.text.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            titleInput.requestFocus()
            return
        }

        if (selectedDateIso.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        val result = Intent().apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, desc)
            putExtra(EXTRA_DUE_ISO, selectedDateIso) // send the selected date
        }

        setResult(Activity.RESULT_OK, result)
        finish()
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_DUE_ISO = "dueIso" // selected date in string format
    }
}
