package com.example.prog7314_universe.Adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.Models.MoodEntry
import com.example.prog7314_universe.databinding.ItemMoodEntryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class MoodEntryAdapter : ListAdapter<MoodEntry, MoodEntryAdapter.MoodEntryViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodEntryViewHolder {
        val binding = ItemMoodEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MoodEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MoodEntryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MoodEntryViewHolder(
        private val binding: ItemMoodEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun bind(entry: MoodEntry) {
            val scale = entry.getMoodScale()
            binding.tvMoodEmoji.text = scale.emoji
            binding.tvMoodTitle.text = scale.displayName
            binding.tvMoodDate.text = dateFormat.format(entry.date.toDate())
            binding.tvMoodNote.text = entry.note?.takeIf { it.isNotBlank() } ?: "No note"
        }
    }

    private object Diff : DiffUtil.ItemCallback<MoodEntry>() {
        override fun areItemsTheSame(oldItem: MoodEntry, newItem: MoodEntry): Boolean = oldItem.moodId == newItem.moodId
        override fun areContentsTheSame(oldItem: MoodEntry, newItem: MoodEntry): Boolean = oldItem == newItem
    }
}