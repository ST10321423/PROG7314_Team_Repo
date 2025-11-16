package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.prog7314_universe.Models.SavingsGoal
import com.example.prog7314_universe.databinding.ActivityAddSavingsGoalBinding
import androidx.navigation.fragment.findNavController
import com.example.prog7314_universe.viewmodel.SavingsGoalViewModel
import com.google.firebase.Timestamp
import java.util.Calendar

class CreateSavingsGoalFragment : Fragment() {

    private var _binding: ActivityAddSavingsGoalBinding? = null
    private val binding get() = _binding!!

    private lateinit var vm: SavingsGoalViewModel
    private var userId: String = ""
    private var selectedDeadline: Timestamp = Timestamp.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAddSavingsGoalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm = ViewModelProvider(this)[SavingsGoalViewModel::class.java]
        userId = arguments?.getString(ARG_USER_ID) ?: ""
        if (userId.isBlank()) {
            Toast.makeText(requireContext(), "Invalid user session", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }

        binding.edtDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                cal.set(y, m, d)
                selectedDeadline = Timestamp(cal.time)
                binding.edtDate.setText("$d/${m + 1}/$y")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnCreateGoal.setOnClickListener {
            val name = binding.edtName.text.toString().trim()
            val target = binding.edtAmount.text.toString().toDoubleOrNull()
            val dateTxt = binding.edtDate.text.toString().trim()

            if (name.isBlank() || target == null || dateTxt.isBlank()) {
                Toast.makeText(requireContext(), "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val goal = SavingsGoal(
                userOwnerId = userId,
                goalName = name,
                targetAmount = target,
                deadline = selectedDeadline,
                createdAt = Timestamp.now()
            )
            vm.insertSavingsGoal(goal) { ok ->
                Toast.makeText(requireContext(), if (ok) "Goal created!" else "Failed to create goal", Toast.LENGTH_SHORT).show()
                if (ok) findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_USER_ID = "user_id"
        fun newInstance(userId: String): CreateSavingsGoalFragment = CreateSavingsGoalFragment().apply {
            arguments = bundleOf(ARG_USER_ID to userId)
        }
    }
}