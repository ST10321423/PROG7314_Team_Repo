package com.example.prog7314_universe


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class AddTaskActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_task)

        val titleInput: EditText = findViewById(R.id.editTextTaskTitle)
        val descInput: EditText = findViewById(R.id.editTextTaskDescription)
        val btnSave: Button = findViewById(R.id.btnSaveTask)

        btnSave.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("title", titleInput.text.toString())
            resultIntent.putExtra("description", descInput.text.toString())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
