package com.example.prog7314_universe.Adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.MoodCalendarItem
import com.example.prog7314_universe.databinding.ItemMoodCalenderBinding
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for mood calendar grid
 * Displays days of the month with mood indicators
 */
class MoodCalendarAdapter(
    private val onDateClick: (Date) -> Unit
) : ListAdapter<MoodCalendarItem, MoodCalendarAdapter.MoodCalendarViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodCalendarViewHolder {
        val binding = ItemMoodCalenderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MoodCalendarViewHolder(binding, onDateClick)
    }

    override fun onBindViewHolder(holder: MoodCalendarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MoodCalendarViewHolder(
        private val binding: ItemMoodCalenderBinding,
        private val onDateClick: (Date) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MoodCalendarItem) {
            val calendar = Calendar.getInstance()
            calendar.time = item.date

            // Display day number
            binding.tvDay.text = calendar.get(Calendar.DAY_OF_MONTH).toString()

            // Show mood if exists
            if (item.moodEntry != null) {
                binding.tvMoodEmoji.visibility = View.VISIBLE

                // Get emoji from MoodScale enum
                val moodScale = item.moodEntry.getMoodScale()
                binding.tvMoodEmoji.text = moodScale.emoji

                // Set background color based on mood
                try {
                    binding.cardDay.setCardBackgroundColor(Color.parseColor(moodScale.colorHex))
                } catch (e: IllegalArgumentException) {
                    // If color parsing fails, use default
                    binding.cardDay.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
                }
            } else {
                binding.tvMoodEmoji.visibility = View.GONE
                binding.cardDay.setCardBackgroundColor(Color.parseColor("#F5F5F5"))
            }

            // Highlight today
            val today = Calendar.getInstance()
            if (calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            ) {
                binding.cardDay.strokeWidth = 4
                binding.cardDay.strokeColor = Color.parseColor("#6200EE")
            } else {
                binding.cardDay.strokeWidth = 0
            }

            // Click listener
            binding.root.setOnClickListener {
                onDateClick(item.date)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MoodCalendarItem>() {
        override fun areItemsTheSame(oldItem: MoodCalendarItem, newItem: MoodCalendarItem): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: MoodCalendarItem, newItem: MoodCalendarItem): Boolean {
            return oldItem == newItem
        }
    }
}


