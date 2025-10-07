package com.example.prog7314_universe

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.Adapters.TaskAdapter
import com.example.prog7314_universe.Models.Task
import com.example.prog7314_universe.viewmodel.TaskViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class TaskActivity : AppCompatActivity() {

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private val taskList = mutableListOf<Task>()

    private val vm: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks)

        // Recycler + Adapter
        recyclerView = findViewById(R.id.recyclerViewTasks)
        taskAdapter = TaskAdapter(this, taskList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = taskAdapter

        // FAB -> AddTaskActivity (we'll read the result and call vm.addTask)
        val fab: FloatingActionButton = findViewById(R.id.fabAddTask)
        fab.setOnClickListener {
            val intent = Intent(this, AddTaskActivity::class.java)
            startActivityForResult(intent, REQ_ADD_TASK)
        }

        // Swipe-to-delete
        attachSwipeToDelete()

        // Observe ViewModel
        vm.tasks.observe(this) { list ->
            // Replace adapter data with latest from Firestore
            taskList.clear()
            taskList.addAll(list)
            taskAdapter.notifyDataSetChanged()
        }

        vm.error.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }

        // Ensure we are signed in (Anonymous for dev) before loading
        ensureSignedIn {
            vm.refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        // Optional: refresh when coming back to the screen
        if (Firebase.auth.currentUser != null) {
            vm.refresh()
        }
    }

    // Handle result from AddTaskActivity and create via ViewModel/Firestore
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_ADD_TASK && resultCode == RESULT_OK) {
            val title = data?.getStringExtra("title")?.trim().orEmpty()
            val description = data?.getStringExtra("description")?.trim().orEmpty()
            if (title.isNotEmpty()) {
                vm.addTask(title, description.ifEmpty { null }, /* dueIso = */ null)
            } else {
                Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ensureSignedIn(onReady: () -> Unit) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            onReady()
            return
        }
        Firebase.auth.signInAnonymously()
            .addOnSuccessListener { onReady() }
            .addOnFailureListener {
                Toast.makeText(this, "Sign-in failed: ${it.message}", Toast.LENGTH_LONG).show()
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
                val item = taskList.getOrNull(pos)
                if (item != null && item.id.isNotBlank()) {
                    vm.remove(item.id)
                } else {
                    // nothing to remove; just restore UI
                    taskAdapter.notifyItemChanged(pos)
                }
            }
        })
        touchHelper.attachToRecyclerView(recyclerView)
    }

    companion object {
        private const val REQ_ADD_TASK = 1
    }
}
