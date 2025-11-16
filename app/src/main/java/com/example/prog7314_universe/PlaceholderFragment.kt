package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * TasksListFragment - Placeholder for tasks feature
 */
class TasksListFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            text = "Tasks Feature - Coming Soon!"
            textSize = 18f
            setPadding(32, 32, 32, 32)
        }
    }
}

/**
 * SavingsFragment - Placeholder for savings goals feature
 */
class SavingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            text = "Savings Goals Feature - Coming Soon!"
            textSize = 18f
            setPadding(32, 32, 32, 32)
        }
    }
}

/**
 * FridgeFragment - Placeholder for fridge manager feature
 */
class FridgeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            text = "Fridge Manager Feature - Coming Soon!"
            textSize = 18f
            setPadding(32, 32, 32, 32)
        }
    }
}