package com.example.prog7314_universe

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_universe.R
import com.example.prog7314_universe.Adapters.HabitAdapter
import com.example.prog7314_universe.Adapters.HabitUi
import com.example.prog7314_universe.databinding.ActivityHabitListBinding
import com.example.prog7314_universe.Models.DateKeys
import com.example.prog7314_universe.Models.Habit
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.DayOfWeek
import java.time.LocalDate


class HabitListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHabitListBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val adapter by lazy {
        HabitAdapter(
            onToggle = { habit, checked -> toggleCompletion(habit, checked) },
            onEdit = { habit -> AddEditHabitActivity.start(this, habit.habitId) }
        )
    }

    private var timeFilter: String? = null // null | morning | afternoon | evening

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHabitListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.fabAdd.setOnClickListener { AddEditHabitActivity.start(this, null) }

        // Spinner for time-of-day filter
        val items = listOf("All", "Morning", "Afternoon", "Evening")
        binding.spinnerTime.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        binding.spinnerTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) { timeFilter = null; observeHabits() }
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                timeFilter = when (pos) { 1 -> "morning"; 2 -> "afternoon"; 3 -> "evening"; else -> null }
                observeHabits()
            }
        }
    }


    override fun onStart() {
        super.onStart()
        observeHabits()
    }


    private fun observeHabits() {
        val uid = auth.currentUser?.uid ?: return
        val col = db.collection("users").document(uid).collection("habits")

        col.orderBy("name").addSnapshotListener(this) { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener

            val today = LocalDate.now()
            val mon1 = DayOfWeek.from(today).value
            val todayKey = DateKeys.todayKey()

            val base = snap.documents.mapNotNull { it.toObject(Habit::class.java)?.apply { habitId = it.id } }
                .filter { timeFilter == null || it.timeOfDay == timeFilter }
                .map { h ->
                    val due = Habit.isDueToday(h.daysMask, mon1)
                    HabitUi(habit = h, isDueToday = due, completedToday = false, currentStreak = 0)
                }
                .toMutableList()

            if (base.isEmpty()) { adapter.submitList(emptyList()); return@addSnapshotListener }

            var pending = base.size
            base.forEachIndexed { idx, ui ->
                val logRef = col.document(ui.habit.habitId).collection("logs").document(todayKey)
                logRef.get().addOnSuccessListener { logSnap ->
                    val completed = logSnap.getBoolean("completed") == true
                    computeStreak(col.document(ui.habit.habitId)) { streak ->
                        base[idx] = ui.copy(completedToday = completed, currentStreak = streak)
                        if (--pending == 0) adapter.submitList(base)
                    }
                }.addOnFailureListener {
                    if (--pending == 0) adapter.submitList(base)
                }
            }
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
                    .map { it.id } // "yyyy-MM-dd"
                    .toSet()

                var streak = 0
                var cur = today
                while (completedDays.contains(cur.toString())) { streak++; cur = cur.minusDays(1) }
                done(streak)
            }
            .addOnFailureListener { done(0) }
    }
}
