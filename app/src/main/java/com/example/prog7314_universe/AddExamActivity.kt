package com.example.prog7314_universe

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class AddExamActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_exam)

        val editSubject: EditText = findViewById(R.id.editTextExamSubject)
        val editDate: EditText = findViewById(R.id.editTextExamDate)
        val editDescription: EditText = findViewById(R.id.editTextExamDescription)
        val btnSave: Button = findViewById(R.id.btnSaveExam)

        btnSave.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("subject", editSubject.text.toString())
            resultIntent.putExtra("date", editDate.text.toString())
            resultIntent.putExtra("description", editDescription.text.toString())
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}
