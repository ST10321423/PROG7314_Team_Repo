package com.example.prog7314_universe.Adapters

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.R
import com.example.prog7314_universe.Models.Task

class TaskAdapter(
    private val context: Context,
    private val taskList: MutableList<Task>
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.taskTitle)
        val description: TextView = itemView.findViewById(R.id.taskDescription)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditTask)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.title.text = task.title
        holder.description.text = task.description

        // Edit button
        holder.btnEdit.setOnClickListener {
            showEditDialog(task, position)
        }

        // Delete button
        holder.btnDelete.setOnClickListener {
            taskList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, taskList.size)
        }
    }

    override fun getItemCount() = taskList.size

    fun addTask(task: Task) {
        taskList.add(task)
        notifyItemInserted(taskList.size - 1)
    }

    private fun showEditDialog(task: Task, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_task, null)
        val editTitle = dialogView.findViewById<EditText>(R.id.editTaskTitle)
        val editDescription = dialogView.findViewById<EditText>(R.id.editTaskDescription)

        editTitle.setText(task.title)
        editDescription.setText(task.description)

        AlertDialog.Builder(context)
            .setTitle("Edit Task")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val updatedTask = task.copy(
                    title = editTitle.text.toString(),
                    description = editDescription.text.toString()
                )
                taskList[position] = updatedTask
                notifyItemChanged(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
