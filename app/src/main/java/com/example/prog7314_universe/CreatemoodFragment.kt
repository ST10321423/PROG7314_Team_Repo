package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.prog7314_universe.databinding.FragmentCreatemoodBinding
import com.example.prog7314_universe.Models.MoodScale
import com.example.prog7314_universe.viewmodel.MoodViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for creating/editing mood entry
 * Allows users to select mood and add optional notes
 */
class CreateMoodFragment : Fragment() {

    private var _binding: FragmentCreatemoodBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoodViewModel by viewModels()
    private var selectedMood: MoodScale? = null
    private var selectedDate: Date = Date()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatemoodBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get selected date from arguments
        arguments?.getLong("selected_date")?.let { timestamp ->
            selectedDate = Date(timestamp)
        }

        setupUI()
        setupClickListeners()
        loadExistingMood()
    }

    private fun setupUI() {
        // Display date
        val dateFormat = SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(selectedDate)

        // Set initial mood display
        updateMoodDisplay()
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Mood selection buttons
        binding.btnHappy.setOnClickListener {
            selectMood(MoodScale.HAPPY)
        }

        binding.btnSad.setOnClickListener {
            selectMood(MoodScale.SAD)
        }

        binding.btnAngry.setOnClickListener {
            selectMood(MoodScale.ANGRY)
        }

        binding.btnFear.setOnClickListener {
            selectMood(MoodScale.FEAR)
        }

        binding.btnDisgust.setOnClickListener {
            selectMood(MoodScale.DISGUST)
        }

        // Save button
        binding.btnSaveMood.setOnClickListener {
            saveMood()
        }
    }

    private fun loadExistingMood() {
        val existingMood = viewModel.getMoodForDate(selectedDate)
        existingMood?.let { mood ->
            selectedMood = mood.getMoodScale()
            binding.etNote.setText(mood.note)
            updateMoodDisplay()
        }
    }

    private fun selectMood(mood: MoodScale) {
        selectedMood = mood
        updateMoodDisplay()
    }

    private fun updateMoodDisplay() {
        selectedMood?.let { mood ->
            binding.apply {
                tvMoodEmoji.text = mood.emoji
                tvMoodName.text = mood.displayName

                // Update mood display card background color
                cardMoodDisplay.setCardBackgroundColor(
                    android.graphics.Color.parseColor(mood.colorHex)
                )

                // Highlight selected button
                resetMoodButtonStyles()
                when (mood) {
                    MoodScale.HAPPY -> highlightButton(btnHappy)
                    MoodScale.SAD -> highlightButton(btnSad)
                    MoodScale.ANGRY -> highlightButton(btnAngry)
                    MoodScale.FEAR -> highlightButton(btnFear)
                    MoodScale.DISGUST -> highlightButton(btnDisgust)
                }
            }
        } ?: run {
            binding.apply {
                tvMoodEmoji.text = "😊"
                tvMoodName.text = "Select your mood"
                cardMoodDisplay.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#E8F5E9")
                )
            }
        }
    }

    private fun resetMoodButtonStyles() {
        binding.apply {
            listOf(btnHappy, btnSad, btnAngry, btnFear, btnDisgust).forEach { button ->
                button.alpha = 0.5f
                button.scaleX = 1.0f
                button.scaleY = 1.0f
            }
        }
    }

    private fun highlightButton(button: View) {
        button.alpha = 1.0f
        button.scaleX = 1.1f
        button.scaleY = 1.1f
    }

    private fun saveMood() {
        selectedMood?.let { mood ->
            val note = binding.etNote.text.toString().trim()
            viewModel.saveMoodEntry(
                date = selectedDate,
                scale = mood,
                note = note.ifEmpty { null }
            )

            // Show success message
            android.widget.Toast.makeText(
                requireContext(),
                "Mood saved successfully!",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            // Navigate back
            findNavController().navigateUp()
        } ?: run {
            // Show error - no mood selected
            android.widget.Toast.makeText(
                requireContext(),
                "Please select a mood",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




