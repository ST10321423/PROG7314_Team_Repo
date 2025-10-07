package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.Adapters.TaskAdapter
import com.example.prog7314_universe.Models.Task
import com.example.prog7314_universe.viewmodel.TaskViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TaskActivity : AppCompatActivity() {

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView

    private val vm: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)

        // Recycler + Adapter
        recyclerView = findViewById(R.id.recyclerViewTasks)
        taskAdapter = TaskAdapter(
            onEdit = { task -> showEditDialog(task) },
            onDelete = { task -> confirmDelete(task) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter

        // FAB -> AddTaskActivity (it posts to the Render API and returns the new task id)
        val fab: FloatingActionButton = findViewById(R.id.fabAddTask)
        fab.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivityForResult(intent, REQ_ADD_TASK)
        }

        // Swipe-to-delete
        attachSwipeToDelete()

        // Observe ViewModel
        vm.tasks.observe(this) { list -> taskAdapter.submitList(list) }

        vm.error.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        vm.refresh()
    }

    override fun onResume() {
        super.onResume()
        // Optional: refresh when coming back to the screen
        vm.refresh()
    }

    // Handle result from AddTaskActivity and create via ViewModel/API
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_ADD_TASK && resultCode == RESULT_OK) {
            val createdId = data?.getStringExtra(AddTaskActivity.EXTRA_TASK_ID)
            if (!createdId.isNullOrBlank()) {
                vm.refresh()

                val createdTitle = data?.getStringExtra(AddTaskActivity.EXTRA_TITLE)
                if (!createdTitle.isNullOrBlank()) {
                    Toast.makeText(
                        this,
                        getString(R.string.task_created_via_render, createdTitle),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(this, R.string.task_created_generic, Toast.LENGTH_SHORT).show()
                }
                return
            }

            val title = data?.getStringExtra(AddTaskActivity.EXTRA_TITLE)?.trim().orEmpty()
            val description = data?.getStringExtra(AddTaskActivity.EXTRA_DESCRIPTION)?.trim().orEmpty()
            if (title.isNotEmpty()) {
                vm.addTask(title, description.ifEmpty { null }, /* dueIso = */ null)
            } else {
                Toast.makeText(this, R.string.task_title_required, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun attachSwipeToDelete() {
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.bindingAdapterPosition
                val item = taskAdapter.getTaskAt(pos)
                if (item != null && item.id.isNotBlank()) {
                    confirmDelete(item)
                }
                taskAdapter.notifyItemChanged(pos)
            }
        })
        touchHelper.attachToRecyclerView(recyclerView)
    }

    private fun confirmDelete(task: Task) {
        if (task.id.isBlank()) return

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_task_title, task.title))
            .setMessage(R.string.delete_task_message)
            .setPositiveButton(R.string.delete) { _, _ ->
                vm.remove(task.id)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showEditDialog(task: Task) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_task, null)
        val titleInput = dialogView.findViewById<EditText>(R.id.editTaskTitle)
        val descriptionInput = dialogView.findViewById<EditText>(R.id.editTaskDescription)

        titleInput.setText(task.title)
        descriptionInput.setText(task.description)

        AlertDialog.Builder(this)
            .setTitle(R.string.edit_task_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val newTitle = titleInput.text.toString().trim()
                val newDescription = descriptionInput.text.toString().trim()

                if (newTitle.isEmpty()) {
                    Toast.makeText(this, R.string.task_title_required, Toast.LENGTH_SHORT).show()
                } else {
                    vm.updateTask(task, newTitle, newDescription)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        private const val REQ_ADD_TASK = 1
    }
}