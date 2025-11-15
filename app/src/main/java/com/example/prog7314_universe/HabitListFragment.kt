package com.example.prog7314_universe

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.Adapters.HabitAdapter
import com.example.prog7314_universe.Adapters.HabitUi
import com.example.prog7314_universe.Models.DateKeys
import com.example.prog7314_universe.Models.Habit
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.LocalDate

class HabitListFragment : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var spinnerTime: Spinner
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var tvSearch: TextView
    private lateinit var tvFilterAll: TextView
    private lateinit var tvFilterDueToday: TextView
    private lateinit var tvFilterCompleted: TextView

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val adapter by lazy {
        HabitAdapter(
            onToggle = { habit, checked -> toggleCompletion(habit, checked) },
            onEdit = { habit -> AddEditHabitActivity.start(this, habit.habitId) }
        )
    }

    private var timeFilter: String? = null // null | morning | afternoon | evening
    private var statusFilter: StatusFilter = StatusFilter.ALL
    private var allHabits = listOf<HabitUi>()

    enum class StatusFilter {
        ALL, DUE_TODAY, COMPLETED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_habit_list)

        initializeViews()
        setupRecyclerView()
        setupSpinner()
        setupClickListeners()
        setupBottomNavigation()
    }

    private fun initializeViews() {
        recycler = findViewById(R.id.recycler)
        fabAdd = findViewById(R.id.fabAdd)
        spinnerTime = findViewById(R.id.spinnerTime)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvSearch = findViewById(R.id.tvSearch)
        tvFilterAll = findViewById(R.id.tvFilterAll)
        tvFilterDueToday = findViewById(R.id.tvFilterDueToday)
        tvFilterCompleted = findViewById(R.id.tvFilterCompleted)
    }

    private fun setupRecyclerView() {
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    private fun setupSpinner() {
        val items = listOf("All Times", "Morning", "Afternoon", "Evening")
        spinnerTime.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            items
        )

        spinnerTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                timeFilter = null
                observeHabits()
            }

            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                timeFilter = when (pos) {
                    1 -> "morning"
                    2 -> "afternoon"
                    3 -> "evening"
                    else -> null
                }
                observeHabits()
            }
        }
    }

    private fun setupClickListeners() {
        fabAdd.setOnClickListener {
            AddEditHabitActivity.start(this, null)
        }

        tvSearch.setOnClickListener {
            Toast.makeText(this, "Search coming soon", Toast.LENGTH_SHORT).show()
        }

        // Filter tabs
        tvFilterAll.setOnClickListener {
            statusFilter = StatusFilter.ALL
            updateFilterTabs()
            filterHabits()
        }

        tvFilterDueToday.setOnClickListener {
            statusFilter = StatusFilter.DUE_TODAY
            updateFilterTabs()
            filterHabits()
        }

        tvFilterCompleted.setOnClickListener {
            statusFilter = StatusFilter.COMPLETED
            updateFilterTabs()
            filterHabits()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.habits

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.dashboard -> {
                    startActivity(Intent(this, RootActivity::class.java))
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
                    startActivity(Intent(this, ExamsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.habits -> {
                    // Already here
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

    override fun onStart() {
        super.onStart()
        observeHabits()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun observeHabits() {
        val uid = auth.currentUser?.uid ?: return
        val col = db.collection("users").document(uid).collection("habits")

        progressBar.visibility = View.VISIBLE

        col.orderBy("name").addSnapshotListener(this) { snap, err ->
            progressBar.visibility = View.GONE

            if (err != null || snap == null) {
                Toast.makeText(this, "Error loading habits", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            val today = LocalDate.now()
            val mon1 = DayOfWeek.from(today).value
            val todayKey = DateKeys.todayKey()

            val base = snap.documents
                .mapNotNull { it.toObject(Habit::class.java)?.apply { habitId = it.id } }
                .filter { timeFilter == null || it.timeOfDay == timeFilter }
                .map { h ->
                    val due = Habit.isDueToday(h.daysMask, mon1)
                    HabitUi(habit = h, isDueToday = due, completedToday = false, currentStreak = 0)
                }
                .toMutableList()

            if (base.isEmpty()) {
                allHabits = emptyList()
                filterHabits()
                updateEmptyState()
                return@addSnapshotListener
            }

            var pending = base.size
            base.forEachIndexed { idx, ui ->
                val logRef = col.document(ui.habit.habitId).collection("logs").document(todayKey)
                logRef.get().addOnSuccessListener { logSnap ->
                    val completed = logSnap.getBoolean("completed") == true
                    computeStreak(col.document(ui.habit.habitId)) { streak ->
                        base[idx] = ui.copy(completedToday = completed, currentStreak = streak)
                        if (--pending == 0) {
                            allHabits = base
                            filterHabits()
                            updateEmptyState()
                            updateFilterCounts()
                        }
                    }
                }.addOnFailureListener {
                    if (--pending == 0) {
                        allHabits = base
                        filterHabits()
                        updateEmptyState()
                        updateFilterCounts()
                    }
                }
            }
        }
    }

    private fun filterHabits() {
        val filtered = when (statusFilter) {
            StatusFilter.ALL -> allHabits
            StatusFilter.DUE_TODAY -> allHabits.filter { it.isDueToday }
            StatusFilter.COMPLETED -> allHabits.filter { it.completedToday }
        }
        adapter.submitList(filtered)
    }

    private fun updateFilterTabs() {
        // Reset all
        tvFilterAll.apply {
            setTextColor(getColor(android.R.color.darker_gray))
            setBackgroundResource(0)
        }
        tvFilterDueToday.apply {
            setTextColor(getColor(android.R.color.darker_gray))
            setBackgroundResource(0)
        }
        tvFilterCompleted.apply {
            setTextColor(getColor(android.R.color.darker_gray))
            setBackgroundResource(0)
        }

        // Set active
        when (statusFilter) {
            StatusFilter.ALL -> tvFilterAll.apply {
                setTextColor(getColor(android.R.color.black))
                setBackgroundResource(R.drawable.tab_active_background)
            }
            StatusFilter.DUE_TODAY -> tvFilterDueToday.apply {
                setTextColor(getColor(android.R.color.black))
                setBackgroundResource(R.drawable.tab_active_background)
            }
            StatusFilter.COMPLETED -> tvFilterCompleted.apply {
                setTextColor(getColor(android.R.color.black))
                setBackgroundResource(R.drawable.tab_active_background)
            }
        }
    }

    private fun updateFilterCounts() {
        val totalCount = allHabits.size
        val dueCount = allHabits.count { it.isDueToday }
        val completedCount = allHabits.count { it.completedToday }

        tvFilterAll.text = "All ($totalCount)"
        tvFilterDueToday.text = "Due Today ($dueCount)"
        tvFilterCompleted.text = "Completed ($completedCount)"
    }

    private fun updateEmptyState() {
        val isEmpty = when (statusFilter) {
            StatusFilter.ALL -> allHabits.isEmpty()
            StatusFilter.DUE_TODAY -> allHabits.none { it.isDueToday }
            StatusFilter.COMPLETED -> allHabits.none { it.completedToday }
        }

        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE

        tvEmptyState.text = when (statusFilter) {
            StatusFilter.ALL -> "No habits yet\nTap + to add your first habit"
            StatusFilter.DUE_TODAY -> "No habits due today"
            StatusFilter.COMPLETED -> "No completed habits today"
        }
    }

    private fun toggleCompletion(habit: Habit, checked: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        val key = DateKeys.todayKey()
        val logRef = db.collection("users").document(uid)
            .collection("habits").document(habit.habitId)
            .collection("logs").document(key)

        if (checked) {
            logRef.set(mapOf("completed" to true, "completedAt" to Timestamp.now()))
                .addOnSuccessListener {
                    Toast.makeText(this, "Great job! 🎉", Toast.LENGTH_SHORT).show()
                }
        } else {
            logRef.delete()
        }
    }

    private fun computeStreak(habitDoc: com.google.firebase.firestore.DocumentReference, done: (Int) -> Unit) {
        habitDoc.collection("logs")
            .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(30)
            .get()
            .addOnSuccessListener { q ->
                val today = LocalDate.now()
                val completedDays = q.documents
                    .filter { it.getBoolean("completed") == true }
                    .map { it.id }
                    .toSet()

                var streak = 0
                var cur = today
                while (completedDays.contains(cur.toString())) {
                    streak++
                    cur = cur.minusDays(1)
                }
                done(streak)
            }
            .addOnFailureListener { done(0) }
    }


}
