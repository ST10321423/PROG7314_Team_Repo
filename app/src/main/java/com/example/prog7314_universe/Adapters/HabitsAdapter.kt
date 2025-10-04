package com.example.prog7314_universe.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.databinding.ItemHabitBinding
import com.example.prog7314_universe.Models.Habit

data class HabitUi(
    val habit: Habit,
    val isDueToday: Boolean,
    val completedToday: Boolean,
    val currentStreak: Int
)

class HabitAdapter(
    private val onToggle: (Habit, Boolean) -> Unit,
    private val onEdit: (Habit) -> Unit
) : ListAdapter<HabitUi, HabitAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<HabitUi>() {
            override fun areItemsTheSame(oldItem: HabitUi, newItem: HabitUi) =
                oldItem.habit.habitId == newItem.habit.habitId
            override fun areContentsTheSame(old: HabitUi, new: HabitUi) = old == new
        }
    }

    inner class VH(val b: ItemHabitBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)

        // Habit name
        h.b.tvName.text = item.habit.name

        // Streak
        h.b.tvStreak.text = " Streak: ${item.currentStreak} days"

        // Checkbox
        h.b.cbComplete.setOnCheckedChangeListener(null)
        h.b.cbComplete.isChecked = item.completedToday
        h.b.cbComplete.isEnabled = item.isDueToday

        // Due badge
        if (item.isDueToday) {
            h.b.tvDueBadge.text = "Due Today"
            h.b.tvDueBadge.setBackgroundColor(Color.parseColor("#E8F5E9"))
            h.b.tvDueBadge.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            h.b.tvDueBadge.text = "Not Due"
            h.b.tvDueBadge.setBackgroundColor(Color.parseColor("#F5F5F5"))
            h.b.tvDueBadge.setTextColor(Color.parseColor("#666666"))
        }

        // Time of day
        h.b.tvTimeOfDay.text = item.habit.timeOfDay?.capitalize() ?: "Anytime"

        // Difficulty
        val difficultyEmoji = when (item.habit.difficulty?.lowercase()) {
            "easy" -> "😊 Easy"
            "medium" -> "💪 Medium"
            "hard" -> "🔥 Hard"
            else -> "Medium"
        }
        h.b.tvDifficulty.text = difficultyEmoji

        // Applies strikethrough if completed
        if (item.completedToday) {
            h.b.tvName.paintFlags = h.b.tvName.paintFlags or
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
            h.b.tvName.alpha = 0.6f
        } else {
            h.b.tvName.paintFlags = h.b.tvName.paintFlags and
                    android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
            h.b.tvName.alpha = 1.0f
        }

        // Click listeners
        h.b.cbComplete.setOnCheckedChangeListener { _, checked ->
            onToggle(item.habit, checked)
        }

        h.b.btnEdit.setOnClickListener {
            onEdit(item.habit)
        }
    }
}
