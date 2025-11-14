package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.Adapters.ExamAdapter
import com.example.prog7314_universe.Models.Exam
import com.example.prog7314_universe.viewmodel.ExamViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ExamsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var examAdapter: ExamAdapter
    private lateinit var fabAddExam: FloatingActionButton
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var tvFilterAll: TextView
    private lateinit var tvFilterCompleted: TextView
    private lateinit var tvSearch: TextView

    private val vm: ExamViewModel by viewModels()
    private var allExams = listOf<Exam>()
    private var showingCompleted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exams)

        initializeViews()
        setupRecyclerView()
        setupClickListeners()
        setupBottomNavigation()
        observeViewModel()
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewExams)
        fabAddExam = findViewById(R.id.fabAddExam)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvFilterAll = findViewById(R.id.tvFilterAll)
        tvFilterCompleted = findViewById(R.id.tvFilterCompleted)
        tvSearch = findViewById(R.id.tvSearch)
    }

    private fun setupRecyclerView() {
        examAdapter = ExamAdapter(
            examList = emptyList(),
            onEdit = { exam -> editExam(exam) },
            onDelete = { exam -> showDeleteDialog(exam) },
            onToggleComplete = { exam, isCompleted ->
                vm.toggleCompleted(exam.id, isCompleted)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ExamsActivity)
            adapter = examAdapter
        }
    }

    private fun setupClickListeners() {
        // FAB - Add Exam
        fabAddExam.setOnClickListener {
            startActivityForResult(
                Intent(this, `AddExamFragment.kt`::class.java),
                REQ_ADD_EXAM
            )
        }

        // Filter tabs
        tvFilterAll.setOnClickListener {
            showingCompleted = false
            updateFilterTabs()
            filterExams()
        }

        tvFilterCompleted.setOnClickListener {
            showingCompleted = true
            updateFilterTabs()
            filterExams()
        }

        // Search
        tvSearch.setOnClickListener {
            Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.exams

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.tasks -> {
                    startActivity(Intent(this, TasksFragment::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.exams -> {
                    // Already here
                    true
                }
                R.id.habits -> {
                    startActivity(Intent(this, HabitListFragment::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.settings -> {
                    startActivity(Intent(this, SettingsFragment::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        // Exams
        vm.exams.observe(this) { exams ->
            allExams = exams
            filterExams()
            updateEmptyState()
            updateFilterCounts(exams)
        }

        // Loading
        vm.loading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Error
        vm.error.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrBlank()) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        // Operation Success
        vm.operationSuccess.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "Operation successful", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterExams() {
        val filtered = if (showingCompleted) {
            allExams.filter { it.isCompleted }
        } else {
            allExams
        }
        examAdapter.updateList(filtered)
    }

    private fun updateFilterTabs() {
        if (showingCompleted) {
            tvFilterAll.apply {
                setTextColor(getColor(android.R.color.darker_gray))
                setBackgroundResource(0)
            }
            tvFilterCompleted.apply {
                setTextColor(getColor(android.R.color.black))
                setBackgroundResource(R.drawable.tab_active_background)
            }
        } else {
            tvFilterAll.apply {
                setTextColor(getColor(android.R.color.black))
                setBackgroundResource(R.drawable.tab_active_background)
            }
            tvFilterCompleted.apply {
                setTextColor(getColor(android.R.color.darker_gray))
                setBackgroundResource(0)
            }
        }
    }

    private fun updateFilterCounts(exams: List<Exam>) {
        val completedCount = exams.count { it.isCompleted }
        val totalCount = exams.size

        tvFilterAll.text = "All ($totalCount)"
        tvFilterCompleted.text = "Completed ($completedCount)"
    }

    private fun updateEmptyState() {
        val isEmpty = if (showingCompleted) {
            allExams.none { it.isCompleted }
        } else {
            allExams.isEmpty()
        }

        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun editExam(exam: Exam) {
        val intent = Intent(this, `AddExamFragment.kt`::class.java).apply {
            putExtra(EXTRA_EXAM_ID, exam.id)
            putExtra(EXTRA_EXAM_SUBJECT, exam.subject)
            putExtra(EXTRA_EXAM_MODULE, exam.module)
            putExtra(EXTRA_EXAM_DATE, exam.date)
            putExtra(EXTRA_EXAM_START_TIME, exam.startTime)
            putExtra(EXTRA_EXAM_END_TIME, exam.endTime)
            putExtra(EXTRA_EXAM_DESCRIPTION, exam.description)
        }
        startActivityForResult(intent, REQ_EDIT_EXAM)
    }

    private fun showDeleteDialog(exam: Exam) {
        AlertDialog.Builder(this)
            .setTitle("Delete Exam")
            .setMessage("Are you sure you want to delete '${exam.subject}'?")
            .setPositiveButton("Delete") { _, _ ->
                vm.deleteExam(exam.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                REQ_ADD_EXAM -> {
                    val exam = extractExamFromIntent(data)
                    vm.addExam(exam)
                }
                REQ_EDIT_EXAM -> {
                    val examId = data.getStringExtra(EXTRA_EXAM_ID) ?: return
                    val exam = extractExamFromIntent(data).copy(id = examId)
                    vm.updateExam(exam)
                }
            }
        }
    }

    private fun extractExamFromIntent(data: Intent): Exam {
        return Exam(
            id = data.getStringExtra(EXTRA_EXAM_ID) ?: "",
            subject = data.getStringExtra(EXTRA_EXAM_SUBJECT) ?: "",
            module = data.getStringExtra(EXTRA_EXAM_MODULE) ?: "",
            date = data.getStringExtra(EXTRA_EXAM_DATE) ?: "",
            startTime = data.getStringExtra(EXTRA_EXAM_START_TIME) ?: "",
            endTime = data.getStringExtra(EXTRA_EXAM_END_TIME) ?: "",
            description = data.getStringExtra(EXTRA_EXAM_DESCRIPTION) ?: ""
        )
    }

    companion object {
        private const val REQ_ADD_EXAM = 1
        private const val REQ_EDIT_EXAM = 2

        const val EXTRA_EXAM_ID = "exam_id"
        const val EXTRA_EXAM_SUBJECT = "exam_subject"
        const val EXTRA_EXAM_MODULE = "exam_module"
        const val EXTRA_EXAM_DATE = "exam_date"
        const val EXTRA_EXAM_START_TIME = "exam_start_time"
        const val EXTRA_EXAM_END_TIME = "exam_end_time"
        const val EXTRA_EXAM_DESCRIPTION = "exam_description"
    }
}