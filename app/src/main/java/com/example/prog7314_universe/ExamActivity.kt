package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.Adapters.ExamAdapter
import com.example.prog7314_universe.Models.Exam
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ExamsActivity : AppCompatActivity() {

    private lateinit var examAdapter: ExamAdapter
    private lateinit var recyclerView: RecyclerView
    private val examList = mutableListOf<Exam>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exams)

        recyclerView = findViewById(R.id.recyclerViewExams)
        examAdapter = ExamAdapter(this, examList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = examAdapter

        val fab: FloatingActionButton = findViewById(R.id.fabAddExam)
        fab.setOnClickListener {
            val intent = Intent(this, AddExamActivity::class.java)
            startActivityForResult(intent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1 && resultCode == RESULT_OK) {
            val subject = data?.getStringExtra("subject") ?: ""
            val date = data?.getStringExtra("date") ?: ""
            val description = data?.getStringExtra("description") ?: ""
            val exam = Exam(examList.size + 1, subject, date, description)
            examAdapter.addExam(exam)
        }
    }
}
