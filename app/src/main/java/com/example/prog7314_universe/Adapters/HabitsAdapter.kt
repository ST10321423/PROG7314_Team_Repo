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
        h.b.tvName.text = item.habit.name
        h.b.tvStreak.text = "Streak: ${item.currentStreak}"

        h.b.cbComplete.setOnCheckedChangeListener(null)
        h.b.cbComplete.isChecked = item.completedToday
        h.b.cbComplete.isEnabled = item.isDueToday

        h.b.tvDueBadge.text = if (item.isDueToday) "Due Today" else "Not Due"
        h.b.tvDueBadge.setBackgroundColor(Color.parseColor(if (item.isDueToday) "#E8F5E9" else "#F5F5F5"))

        h.b.cbComplete.setOnCheckedChangeListener { _, checked ->
            onToggle(item.habit, checked)
        }
        h.b.btnEdit.setOnClickListener {
            onEdit(item.habit)
        }
    }
}
