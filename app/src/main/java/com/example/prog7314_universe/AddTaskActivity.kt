package com.example.prog7314_universe

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddTaskActivity : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var descInput: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        titleInput = findViewById(R.id.editTextTaskTitle)
        descInput = findViewById(R.id.editTextTaskDescription)
        btnSave = findViewById(R.id.btnSaveTask)

        btnSave.setOnClickListener {
            val title = titleInput.text.toString().trim()
            val desc  = descInput.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val result = Intent().apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_DESCRIPTION, desc)
                // If you add a due date later, also putExtra(EXTRA_DUE_ISO, isoString)
            }
            setResult(Activity.RESULT_OK, result)
            finish()
        }
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_DUE_ISO = "dueIso" // optional, for future use
    }
}

