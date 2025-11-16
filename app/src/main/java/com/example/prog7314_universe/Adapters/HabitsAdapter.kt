package com.example.prog7314_universe.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.databinding.ItemHabitBinding
import com.example.prog7314_universe.Models.DateKeys
import com.example.prog7314_universe.Models.Habit
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

data class HabitUi(
    val habitId: String,
    val name: String,
    val daysMask: Int,
    val timeOfDay: String?,
    val difficulty: String,
    val streak: Int,
    val isDueToday: Boolean,
    val isCompleted: Boolean
){
    companion object {
        fun fromHabit(habit: Habit, nowMillis: Long): HabitUi {
            val todayMask = DateKeys.dayOfWeekToMask(
                Instant.ofEpochMilli(nowMillis)
                    .atZone(ZoneId.systemDefault())
                    .dayOfWeek
            )
            val isDue = habit.daysMask == 0 || (habit.daysMask and todayMask) != 0

            val streak = habit.streak.coerceAtLeast(0)

            return HabitUi(
                habitId = habit.habitId,
                name = habit.name,
                daysMask = habit.daysMask,
                timeOfDay = habit.timeOfDay,
                difficulty = habit.difficulty,
                streak = streak,
                isDueToday = isDue,
                isCompleted = habit.isCompleted
            )
        }
    }
}

class HabitAdapter(
    private val onToggle: (HabitUi, Boolean) -> Unit,
    private val onEdit: (HabitUi) -> Unit
) : ListAdapter<HabitUi, HabitAdapter.VH>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<HabitUi>() {
            override fun areItemsTheSame(oldItem: HabitUi, newItem: HabitUi) =
                oldItem.habitId == newItem.habitId
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
        h.b.tvName.text = item.name

        // Streak
        h.b.tvStreak.text = " Streak: ${item.streak} days"

        // Checkbox
        h.b.cbComplete.setOnCheckedChangeListener(null)
        h.b.cbComplete.isChecked = item.isCompleted
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
        h.b.tvTimeOfDay.text = item.timeOfDay?.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        } ?: "Anytime"

        val difficultyEmoji = when (item.difficulty.lowercase(Locale.getDefault())) {
            "easy" -> "😊 Easy"
            "medium" -> "💪 Medium"
            "hard" -> "🔥 Hard"
            else -> item.difficulty
        }
        h.b.tvDifficulty.text = difficultyEmoji

        // Applies strikethrough if completed
        if (item.isCompleted) {
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
            onToggle(item, checked)
        }

        h.b.btnEdit.setOnClickListener {
            onEdit(item)
        }
    }
}
