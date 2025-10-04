package com.example.prog7314_universe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.prog7314_universe.databinding.ActivityAddEditHabitBinding
import com.example.prog7314_universe.Models.Habit
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddEditHabitActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditHabitBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private var habitId: String? = null
    private var current: Habit? = null

    companion object {
        fun start(ctx: Context, habitId: String?) {
            ctx.startActivity(Intent(ctx, AddEditHabitActivity::class.java).apply {
                putExtra("habitId", habitId)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditHabitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        habitId = intent.getStringExtra("habitId")

        setupUi()
        if (habitId != null) loadHabit()
    }

    private fun setupUi() {
        binding.spinnerTime.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            arrayOf("None", "Morning", "Afternoon", "Evening")
        )

        binding.btnSave.setOnClickListener { saveHabit() }
        binding.btnDelete.setOnClickListener { deleteHabit() }
        binding.btnDelete.isEnabled = habitId != null
    }

    private fun loadHabit() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("habits").document(habitId!!)
            .get()
            .addOnSuccessListener { d ->
                current = d.toObject(Habit::class.java)?.apply { habitId = d.id }
                current?.let { fillForm(it) }
            }
    }

    private fun fillForm(h: Habit) {
        binding.etName.setText(h.name)
        binding.etColor.setText(h.colorHex ?: "#4CAF50")
        binding.spinnerTime.setSelection(when (h.timeOfDay?.lowercase()) {
            "morning" -> 1; "afternoon" -> 2; "evening" -> 3; else -> 0
        })
        when (h.difficulty) {
            "easy" -> binding.rbEasy.isChecked = true
            "medium" -> binding.rbMedium.isChecked = true
            else -> binding.rbHard.isChecked = true
        }
        setDayChecked(h.daysMask)
    }

    private fun setDayChecked(mask: Int) {
        fun bit(mon1: Int) = Habit.dayToBit(mon1)
        binding.cbMon.isChecked = (mask and bit(1)) != 0
        binding.cbTue.isChecked = (mask and bit(2)) != 0
        binding.cbWed.isChecked = (mask and bit(3)) != 0
        binding.cbThu.isChecked = (mask and bit(4)) != 0
        binding.cbFri.isChecked = (mask and bit(5)) != 0
        binding.cbSat.isChecked = (mask and bit(6)) != 0
        binding.cbSun.isChecked = (mask and bit(7)) != 0
    }

    private fun readMaskFromUi(): Int {
        val days = mutableSetOf<Int>()
        if (binding.cbMon.isChecked) days += 1
        if (binding.cbTue.isChecked) days += 2
        if (binding.cbWed.isChecked) days += 3
        if (binding.cbThu.isChecked) days += 4
        if (binding.cbFri.isChecked) days += 5
        if (binding.cbSat.isChecked) days += 6
        if (binding.cbSun.isChecked) days += 7
        return Habit.maskFor(days)
    }

    private fun saveHabit() {
        val uid = auth.currentUser?.uid ?: return
        val name = binding.etName.text.toString().trim()
        if (name.isEmpty()) { Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show(); return }

        val color = binding.etColor.text.toString().ifBlank { "#4CAF50" }
        val timeOfDay = when (binding.spinnerTime.selectedItemPosition) { 1 -> "morning"; 2 -> "afternoon"; 3 -> "evening"; else -> null }
        val difficulty = when {
            binding.rbEasy.isChecked -> "easy"
            binding.rbMedium.isChecked -> "medium"
            else -> "hard"
        }
        val daysMask = readMaskFromUi()
        val cadence = if (daysMask == 0) "daily" else "weekly"

        val now = Timestamp.now()
        val data = hashMapOf(
            "name" to name,
            "iconName" to current?.iconName,
            "colorHex" to color,
            "cadence" to cadence,
            "daysMask" to daysMask,
            "timeOfDay" to timeOfDay,
            "difficulty" to difficulty,
            "updatedAt" to now
        )

        val habits = db.collection("users").document(uid).collection("habits")
        if (habitId == null) {
            data["createdAt"] = now
            habits.add(data).addOnSuccessListener { finish() }
        } else {
            habits.document(habitId!!).update(data as Map<String, Any>).addOnSuccessListener { finish() }
        }
    }

    private fun deleteHabit() {
        val uid = auth.currentUser?.uid ?: return
        habitId?.let {
            db.collection("users").document(uid).collection("habits")
                .document(it).delete().addOnSuccessListener { finish() }
        }
    }


}
