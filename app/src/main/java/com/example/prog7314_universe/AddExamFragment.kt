package com.example.prog7314_universe

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.prog7314_universe.Models.Exam
import com.example.prog7314_universe.databinding.ActivityAddExamBinding
import androidx.navigation.fragment.findNavController
import com.example.prog7314_universe.viewmodel.ExamViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExamFragment : Fragment() {

    private var _binding: ActivityAddExamBinding? = null
    private val binding get() = _binding!!

    private val calendar = Calendar.getInstance()
    private val vm: ExamViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }

    private var examId: String? = null
    private var selectedDate: String = ""
    private var selectedStartTime: String = ""
    private var selectedEndTime: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityAddExamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadExistingExam()
        setupClickListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadExistingExam() {
        examId = arguments?.getString(ARG_EXAM_ID)

        if (examId != null) {
            binding.btnSaveExam.text = getString(R.string.update_exam)
            binding.btnCancel.text = getString(R.string.cancel)

            binding.editTextExamSubject.setText(arguments?.getString(ARG_EXAM_SUBJECT))
            binding.editTextExamModule.setText(arguments?.getString(ARG_EXAM_MODULE))

            selectedDate = arguments?.getString(ARG_EXAM_DATE) ?: ""
            binding.editTextExamDate.setText(selectedDate)

            selectedStartTime = arguments?.getString(ARG_EXAM_START_TIME) ?: ""
            binding.editTextStartTime.setText(selectedStartTime)

            selectedEndTime = arguments?.getString(ARG_EXAM_END_TIME) ?: ""
            binding.editTextEndTime.setText(selectedEndTime)

            binding.editTextExamDescription.setText(arguments?.getString(ARG_EXAM_DESCRIPTION))
        } else {
            binding.btnSaveExam.text = getString(R.string.save_exam)
        }
    }

    private fun setupClickListeners() = with(binding) {
        editTextExamDate.setOnClickListener { showDatePicker() }
        editTextStartTime.setOnClickListener { showTimePicker(isStartTime = true) }
        editTextEndTime.setOnClickListener { showTimePicker(isStartTime = false) }
        btnSaveExam.setOnClickListener { saveExam() }
        btnCancel.setOnClickListener { findNavController().popBackStack() }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)

                val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(calendar.time)
                binding.editTextExamDate.setText(selectedDate)
            },
            year,
            month,
            day
        ).show()
    }

    private fun showTimePicker(isStartTime: Boolean) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)

                val timeString = timeFormat.format(calendar.time)

                if (isStartTime) {
                    selectedStartTime = timeString
                    binding.editTextStartTime.setText(timeString)
                } else {
                    selectedEndTime = timeString
                    binding.editTextEndTime.setText(timeString)
                }
            },
            hour,
            minute,
            false
        ).show()
    }

    private fun saveExam() {
        val subject = binding.editTextExamSubject.text.toString().trim()
        val module = binding.editTextExamModule.text.toString().trim()
        val description = binding.editTextExamDescription.text.toString().trim()

        if (subject.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a subject", Toast.LENGTH_SHORT).show()
            binding.editTextExamSubject.requestFocus()
            return
        }

        if (module.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a module", Toast.LENGTH_SHORT).show()
            binding.editTextExamModule.requestFocus()
            return
        }

        if (selectedDate.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedStartTime.isEmpty()) {
            Toast.makeText(requireContext(), "Please select a start time", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedEndTime.isEmpty()) {
            Toast.makeText(requireContext(), "Please select an end time", Toast.LENGTH_SHORT).show()
            return
        }

        val exam = Exam(
            id = examId ?: "",
            subject = subject,
            module = module,
            date = selectedDate,
            startTime = selectedStartTime,
            endTime = selectedEndTime,
            description = description
        )

        if (examId == null) {
            vm.addExam(exam)
        } else {
            vm.updateExam(exam)
        }
        findNavController().popBackStack()
    }

    companion object {
        private const val ARG_EXAM_ID = "exam_id"
        private const val ARG_EXAM_SUBJECT = "exam_subject"
        private const val ARG_EXAM_MODULE = "exam_module"
        private const val ARG_EXAM_DATE = "exam_date"
        private const val ARG_EXAM_START_TIME = "exam_start_time"
        private const val ARG_EXAM_END_TIME = "exam_end_time"
        private const val ARG_EXAM_DESCRIPTION = "exam_description"

        fun newInstance(exam: Exam?): AddExamFragment = AddExamFragment().apply {
            arguments = createArgs(exam)
        }

        fun createArgs(exam: Exam?): Bundle = Bundle().apply {
            putString(ARG_EXAM_ID, exam?.id)
            putString(ARG_EXAM_SUBJECT, exam?.subject)
            putString(ARG_EXAM_MODULE, exam?.module)
            putString(ARG_EXAM_DATE, exam?.date)
            putString(ARG_EXAM_START_TIME, exam?.startTime)
            putString(ARG_EXAM_END_TIME, exam?.endTime)
            putString(ARG_EXAM_DESCRIPTION, exam?.description)
        }
    }
}