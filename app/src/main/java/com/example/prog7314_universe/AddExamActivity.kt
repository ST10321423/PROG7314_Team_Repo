package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AddExamActivity : AppCompatActivity() {

    private lateinit var tvTitle: TextView
    private lateinit var editSubject: EditText
    private lateinit var editModule: EditText
    private lateinit var editDate: EditText
    private lateinit var editStartTime: EditText
    private lateinit var editEndTime: EditText
    private lateinit var editDescription: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private var examId: String? = null
    private var selectedDate: String = ""
    private var selectedStartTime: String = ""
    private var selectedEndTime: String = ""

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_exam)

        initializeViews()
        loadExistingExam()
        setupClickListeners()
    }

    private fun initializeViews() {
        tvTitle = findViewById(R.id.tvTitle)
        editSubject = findViewById(R.id.editTextExamSubject)
        editModule = findViewById(R.id.editTextExamModule)
        editDate = findViewById(R.id.editTextExamDate)
        editStartTime = findViewById(R.id.editTextStartTime)
        editEndTime = findViewById(R.id.editTextEndTime)
        editDescription = findViewById(R.id.editTextExamDescription)
        btnSave = findViewById(R.id.btnSaveExam)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun loadExistingExam() {
        examId = intent.getStringExtra(ExamsActivity.EXTRA_EXAM_ID)

        if (examId != null) {
            // Edit mode
            tvTitle.text = "Edit Exam"
            btnSave.text = "Update Exam"

            editSubject.setText(intent.getStringExtra(ExamsActivity.EXTRA_EXAM_SUBJECT))
            editModule.setText(intent.getStringExtra(ExamsActivity.EXTRA_EXAM_MODULE))

            selectedDate = intent.getStringExtra(ExamsActivity.EXTRA_EXAM_DATE) ?: ""
            editDate.setText(selectedDate)

            selectedStartTime = intent.getStringExtra(ExamsActivity.EXTRA_EXAM_START_TIME) ?: ""
            editStartTime.setText(selectedStartTime)

            selectedEndTime = intent.getStringExtra(ExamsActivity.EXTRA_EXAM_END_TIME) ?: ""
            editEndTime.setText(selectedEndTime)

            editDescription.setText(intent.getStringExtra(ExamsActivity.EXTRA_EXAM_DESCRIPTION))
        } else {
            // Add mode
            tvTitle.text = "Add Exam"
            btnSave.text = "Save Exam"
        }
    }

    private fun setupClickListeners() {
        // Date picker
        editDate.setOnClickListener {
            showDatePicker()
        }

        // Start time picker
        editStartTime.setOnClickListener {
            showTimePicker(true)
        }

        // End time picker
        editEndTime.setOnClickListener {
            showTimePicker(false)
        }

        // Save button
        btnSave.setOnClickListener {
            saveExam()
        }

        // Cancel button
        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)

                // Format: "Saturday, November 28, 2025"
                val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)
                editDate.setText(selectedDate)
            },
            year,
            month,
            day
        ).show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                // Format: "02:00 PM"
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)

                val timeString = timeFormat.format(calendar.time)

                if (isStartTime) {
                    selectedStartTime = timeString
                    editStartTime.setText(timeString)
                } else {
                    selectedEndTime = timeString
                    editEndTime.setText(timeString)
                }
            },
            hour,
            minute,
            false // 12-hour format
        ).show()
    }

    private fun saveExam() {
        val subject = editSubject.text.toString().trim()
        val module = editModule.text.toString().trim()
        val description = editDescription.text.toString().trim()

        // Validation
        if (subject.isEmpty()) {
            Toast.makeText(this, "Please enter a subject", Toast.LENGTH_SHORT).show()
            editSubject.requestFocus()
            return
        }

        if (module.isEmpty()) {
            Toast.makeText(this, "Please enter a module", Toast.LENGTH_SHORT).show()
            editModule.requestFocus()
            return
        }

        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedStartTime.isEmpty()) {
            Toast.makeText(this, "Please select a start time", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedEndTime.isEmpty()) {
            Toast.makeText(this, "Please select an end time", Toast.LENGTH_SHORT).show()
            return
        }

        // Create result intent
        val resultIntent = Intent().apply {
            putExtra(ExamsActivity.EXTRA_EXAM_ID, examId ?: "")
            putExtra(ExamsActivity.EXTRA_EXAM_SUBJECT, subject)
            putExtra(ExamsActivity.EXTRA_EXAM_MODULE, module)
            putExtra(ExamsActivity.EXTRA_EXAM_DATE, selectedDate)
            putExtra(ExamsActivity.EXTRA_EXAM_START_TIME, selectedStartTime)
            putExtra(ExamsActivity.EXTRA_EXAM_END_TIME, selectedEndTime)
            putExtra(ExamsActivity.EXTRA_EXAM_DESCRIPTION, description)
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
