package com.example.prog7314_universe

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.prog7314_universe.repo.TaskRepository
import kotlinx.coroutines.launch

class AddTaskActivity : AppCompatActivity() {

    private lateinit var titleInput: EditText
    private lateinit var descInput: EditText
    private lateinit var btnSave: Button
    private lateinit var progress: ProgressBar

    private val repository = TaskRepository()
    private var isSaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        titleInput = findViewById(R.id.editTextTaskTitle)
        descInput = findViewById(R.id.editTextTaskDescription)
        btnSave = findViewById(R.id.btnSaveTask)
        progress = findViewById(R.id.progressSaveTask)

        btnSave.setOnClickListener {
            if (isSaving) return@setOnClickListener

            val title = titleInput.text.toString().trim()
            val desc  = descInput.text.toString().trim()

            if (title.isEmpty()) {
                Toast.makeText(this, R.string.task_title_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                setSaving(true)
                try {
                    val created = repository.add(title, desc.ifBlank { null }, /* dueIso = */ null)
                    val result = Intent().apply {
                        putExtra(EXTRA_TASK_ID, created.id)
                        putExtra(EXTRA_TITLE, created.title)
                        putExtra(EXTRA_DESCRIPTION, created.description)
                        // If you add a due date later, also putExtra(EXTRA_DUE_ISO, created.dueDate)
                    }
                    setResult(Activity.RESULT_OK, result)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@AddTaskActivity,
                        getString(R.string.task_save_failed, e.message ?: getString(R.string.task_unknown_error)),
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    setSaving(false)
                }
            }
        }
    }

    private fun setSaving(inProgress: Boolean) {
        isSaving = inProgress
        progress.isVisible = inProgress
        btnSave.isEnabled = !inProgress
    }

    companion object {
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_DUE_ISO = "dueIso" // optional, for future use
    }
}


