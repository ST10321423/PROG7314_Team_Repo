package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.prog7314_universe.Models.SavingsContribution
import com.example.prog7314_universe.Models.SavingsGoal
import com.example.prog7314_universe.databinding.ActivityAddContributionBinding
//new navigation controller added
import androidx.navigation.fragment.findNavController
import com.example.prog7314_universe.viewmodel.SavingsContributionViewModel
import com.example.prog7314_universe.viewmodel.SavingsGoalViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddContributionFragment : Fragment() {

    private var _binding: ActivityAddContributionBinding? = null
    private val binding get() = _binding!!

    private val calendar: Calendar = Calendar.getInstance()
    private lateinit var goalViewModel: SavingsGoalViewModel
    private lateinit var contributionViewModel: SavingsContributionViewModel

    private var userId: String = ""
    private var selectedGoalId: String = ""
    private var selectedDate: Timestamp = Timestamp(calendar.time)
    private var goals: List<SavingsGoal> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAddContributionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        goalViewModel = ViewModelProvider(this)[SavingsGoalViewModel::class.java]
        contributionViewModel = ViewModelProvider(this)[SavingsContributionViewModel::class.java]

        userId = arguments?.getString(ARG_USER_ID)
            ?: FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        if (userId.isBlank()) {
            toast("Unable to determine user. Please sign in again.")
            return
        }
        selectedGoalId = arguments?.getString(ARG_GOAL_ID) ?: ""

        observeGoals()

        binding.etContributionDate.setOnClickListener { showDatePicker() }
        selectedDate = Timestamp(calendar.time)
        binding.etContributionDate.setText(formatDate(calendar.time))

        binding.btnAddContribution.setOnClickListener {
            val amountText = binding.etContributionAmt.text?.toString()?.trim().orEmpty()
            val amount = amountText.replace("R", "").trim().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                toast("Enter a valid amount")
                return@setOnClickListener
            }

            if (selectedGoalId.isBlank()) {
                toast("Select a goal")
                return@setOnClickListener
            }

            val contribution = SavingsContribution(
                goalId = selectedGoalId,
                amount = amount,
                contributionDate = selectedDate
            )

            setLoading(true)
            contributionViewModel.addContribution(userId, selectedGoalId, contribution) { success ->
                setLoading(false)
                if (success) {
                    toast("Contribution saved")
                    findNavController().popBackStack()
                } else {
                    toast("Failed to save contribution")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeGoals() {
        goalViewModel.getSavingsGoals(userId).observe(viewLifecycleOwner) { list ->
            goals = list
            if (list.isNullOrEmpty()) {
                toast("No savings goals found. Create one first.")
                findNavController().popBackStack()
                return@observe
            }

            val names = list.map { it.goalName }
            binding.spinnerContributionGoal.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, names)
            )

            val initialIndex = list.indexOfFirst { it.id == selectedGoalId }
            val resolvedIndex = if (initialIndex >= 0) initialIndex else 0
            selectedGoalId = list.getOrNull(resolvedIndex)?.id.orEmpty()
            binding.spinnerContributionGoal.setText(list.getOrNull(resolvedIndex)?.goalName.orEmpty(), false)

            binding.spinnerContributionGoal.setOnItemClickListener { _, _, position, _ ->
                selectedGoalId = goals.getOrNull(position)?.id.orEmpty()
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day, 0, 0, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val pickedDate = calendar.time
                selectedDate = Timestamp(pickedDate)
                binding.etContributionDate.setText(formatDate(pickedDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(date: Date): String =
        SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(date)

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    private fun setLoading(loading: Boolean) {
        binding.btnAddContribution.isEnabled = !loading
        binding.btnAddContribution.alpha = if (loading) 0.7f else 1f
    }

    companion object {
        private const val ARG_USER_ID = "user_id"
        private const val ARG_GOAL_ID = "goal_id"

        fun newInstance(userId: String, goalId: String?): AddContributionFragment = AddContributionFragment().apply {
            arguments = createArgs(userId, goalId)
        }

        fun createArgs(userId: String, goalId: String?): Bundle = bundleOf(
            ARG_USER_ID to userId,
            ARG_GOAL_ID to goalId
        )
    }
}