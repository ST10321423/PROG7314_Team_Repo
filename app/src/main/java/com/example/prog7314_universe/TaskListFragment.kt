package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.prog7314_universe.Adapters.TaskAdapter
import com.example.prog7314_universe.Models.Task
import com.example.prog7314_universe.databinding.ActivityTasksBinding
import com.example.prog7314_universe.viewmodel.TaskViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth

class TasksListFragment : Fragment() {

    private var _binding: ActivityTasksBinding? = null
    private val binding get() = _binding!!

    private val taskAdapter by lazy { TaskAdapter(requireContext(), taskList) }
    private val taskList = mutableListOf<Task>()

    private val viewModel: TaskViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        attachSwipeToDelete()
        setupFab()
        observeViewModel()
        ensureSignedIn { viewModel.refresh() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecycler() = with(binding.recyclerViewTasks) {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = taskAdapter
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            findNavController().navigate(R.id.action_tasksListFragment_to_addTaskFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.tasks.observe(viewLifecycleOwner) { list ->
            taskList.clear()
            taskList.addAll(list)
            taskAdapter.notifyDataSetChanged()
            binding.tvEmptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Sign-in failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun attachSwipeToDelete() {
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = taskList.getOrNull(position)
                if (item != null && item.id.isNotBlank()) {
                    viewModel.remove(item.id)
                } else {
                    taskAdapter.notifyItemChanged(position)
                }
            }
        })
        touchHelper.attachToRecyclerView(binding.recyclerViewTasks)
    }
}