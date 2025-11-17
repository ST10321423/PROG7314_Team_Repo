package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf

import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.prog7314_universe.Adapters.ContributionsAdapter
import com.example.prog7314_universe.Models.SavingsGoal
import com.example.prog7314_universe.databinding.ActivitySavingsGoalBinding
import com.example.prog7314_universe.AddContributionFragment
import com.example.prog7314_universe.viewmodel.SavingsContributionViewModel
import com.example.prog7314_universe.viewmodel.SavingsGoalViewModel
import com.google.firebase.auth.FirebaseAuth

class SavingsGoalFragment : Fragment() {

    private var _binding: ActivitySavingsGoalBinding? = null
    private val binding get() = _binding!!

    private lateinit var vm: SavingsGoalViewModel
    private lateinit var contribVm: SavingsContributionViewModel
    private lateinit var adapter: ContributionsAdapter

    private var userId: String = ""
    private var goals: List<SavingsGoal> = emptyList()
    private var selectedGoalId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivitySavingsGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm = ViewModelProvider(this)[SavingsGoalViewModel::class.java]
        contribVm = ViewModelProvider(this)[SavingsContributionViewModel::class.java]
        userId = arguments?.getString(ARG_USER_ID) ?: FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (userId.isBlank()) {
            Toast.makeText(requireContext(), "Invalid user session", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        adapter = ContributionsAdapter(emptyList())
        binding.contributionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.contributionsRecyclerView.adapter = adapter

        vm.getSavingsGoals(userId).observe(viewLifecycleOwner) { list ->
            goals = list
            if (goals.isEmpty()) {
                selectedGoalId = ""
                binding.goalTypeSpinner.isEnabled = false
                binding.tvGoalName.text = getString(R.string.no_savings_goals)
                binding.txtAmount.text = getString(R.string.default_currency_zero)
                binding.txtProgress.text = getString(R.string.no_savings_progress)
                binding.progressCircle.setProgressCompat(0, false)
                binding.btnAdd.text = getString(R.string.create_savings_goal)
                binding.btnAdd.setOnClickListener { openCreateGoal() }
                adapter.updateList(emptyList())
                return@observe
            }
            selectedGoalId = goals.first().id
            binding.goalTypeSpinner.isEnabled = true
            binding.btnAdd.text = getString(R.string.add_contribution)


            val names = goals.map { it.goalName }
            binding.goalTypeSpinner.adapter = ArrayAdapter(
                requireContext(), android.R.layout.simple_spinner_item, names
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            binding.goalTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                    val g = goals[pos]
                    selectedGoalId = g.id
                    binding.tvGoalName.text = g.goalName
                    updateProgress(g)
                    loadContributions(g.id)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            if (binding.goalTypeSpinner.selectedItemPosition == AdapterView.INVALID_POSITION) {
                binding.goalTypeSpinner.setSelection(0)
                goals.firstOrNull()?.let { first ->
                    binding.tvGoalName.text = first.goalName
                    updateProgress(first)
                    loadContributions(first.id)
                }
            }
        }

        binding.btnStatistic.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAdd.setOnClickListener { openContribution() }
        binding.btnNewGoal.setOnClickListener { openCreateGoal() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateProgress(goal: SavingsGoal) {
        contribVm.getContributions(userId, goal.id).observe(viewLifecycleOwner) { list ->
            val total = list.sumOf { it.amount }
            val progress = if (goal.targetAmount > 0) ((total / goal.targetAmount) * 100).toInt() else 0
            binding.txtAmount.text = "R%.2f".format(total)
            binding.txtProgress.text = "You've saved $progress% of your goal!"
            binding.progressCircle.setProgressCompat(progress.coerceIn(0, 100), true)
        }
    }

    private fun loadContributions(goalId: String) {
        contribVm.getContributions(userId, goalId).observe(viewLifecycleOwner) { contributions ->
            adapter.updateList(contributions)
            binding.contributionsRecyclerView.isVisible = contributions.isNotEmpty()
            binding.tvEmptyState.isVisible = contributions.isEmpty()
        }
    }

    private fun openCreateGoal() {
        val args = Bundle().apply { putString(ARG_USER_ID, userId) }
        findNavController().navigate(R.id.createSavingsGoalFragment, args)
    }

    private fun openContribution() {
        if (selectedGoalId.isBlank()) {
            if (goals.isNotEmpty()) {
                selectedGoalId = goals.first().id
            }
            if (selectedGoalId.isBlank()) {
                openCreateGoal()
                return
            }
        }
        val args = AddContributionFragment.createArgs(userId, selectedGoalId)
        findNavController().navigate(R.id.addContributionFragment, args)
    }


    companion object {
        private const val ARG_USER_ID = "user_id"
        fun newInstance(userId: String): SavingsGoalFragment = SavingsGoalFragment().apply {
            arguments = bundleOf(ARG_USER_ID to userId)
        }
    }
}