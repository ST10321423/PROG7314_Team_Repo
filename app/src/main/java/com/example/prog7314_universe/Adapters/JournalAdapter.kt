package com.example.prog7314_universe.Adapters

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.prog7314_universe.R
import com.example.prog7314_universe.Models.JournalEntry
import com.example.prog7314_universe.databinding.ItemJournalEntryBinding

/**
 * Adapter for journal entries list
 * Displays journal entries with preview and actions
 */
class JournalAdapter(
    private val onItemClick: (JournalEntry) -> Unit,
    private val onDeleteClick: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = ItemJournalEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return JournalViewHolder(binding, onItemClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class JournalViewHolder(
        private val binding: ItemJournalEntryBinding,
        private val onItemClick: (JournalEntry) -> Unit,
        private val onDeleteClick: (JournalEntry) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: JournalEntry) {
            binding.apply {
                // Set title
                tvTitle.text = entry.title

                // Set preview text
                tvPreview.text = entry.getPreviewText()

                // Set date
                tvDate.text = entry.getFormattedDate()

                // Show image if exists
                if (entry.imageUri != null) {
                    ivThumbnail.visibility = View.VISIBLE
                    Glide.with(root.context)
                        .load(Uri.parse(entry.imageUri))
                        .centerCrop()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(ivThumbnail)
                } else {
                    ivThumbnail.visibility = View.GONE
                }

                // Click listeners
                root.setOnClickListener {
                    onItemClick(entry)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(entry)
                }

                btnEdit.setOnClickListener {
                    onItemClick(entry)
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem.entryId == newItem.entryId
        }

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry): Boolean {
            return oldItem == newItem
        }
    }
}




