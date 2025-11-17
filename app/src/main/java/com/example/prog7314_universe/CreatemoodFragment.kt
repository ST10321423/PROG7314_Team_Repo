package com.example.prog7314_universe

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.prog7314_universe.databinding.FragmentCreatemoodBinding
import com.example.prog7314_universe.Models.MoodScale
import com.example.prog7314_universe.utils.PrefManager
import com.example.prog7314_universe.utils.ReminderScheduler
import com.example.prog7314_universe.viewmodel.MoodViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    private lateinit var prefManager: PrefManager
    private lateinit var reminderScheduler: ReminderScheduler
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatemoodBinding.inflate(inflater, container, false)
        prefManager = PrefManager(requireContext())
        reminderScheduler = ReminderScheduler(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get selected date from arguments
        arguments?.let { args ->
            if (args.containsKey("selected_date")) {
                val timestamp = args.getLong("selected_date")
                if (timestamp > 0) {
                    selectedDate = Date(timestamp)
                }
            }
        }

        setupUI()
        setupClickListeners()
        loadExistingMood()

        viewModel.moodEntries.observe(viewLifecycleOwner) {
            // No need to reload here as we handle it in loadExistingMood
        }
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
            isEditMode = true
            selectedMood = mood.getMoodScale()
            binding.etNote.setText(mood.note)
            updateMoodDisplay()

            // Update button text to indicate edit mode
            binding.btnSaveMood.text = "Update Mood"
        }
    }

    private fun selectMood(mood: MoodScale) {
        selectedMood = mood
        updateMoodDisplay()

        // Add haptic feedback (optional)
        binding.root.performHapticFeedback(
            android.view.HapticFeedbackConstants.VIRTUAL_KEY
        )
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

                // Enable save button
                btnSaveMood.isEnabled = true
                btnSaveMood.alpha = 1.0f
            }
        } ?: run {
            binding.apply {
                tvMoodEmoji.text = "😊"
                tvMoodName.text = "Select your mood"
                cardMoodDisplay.setCardBackgroundColor(
                    android.graphics.Color.parseColor("#E8F5E9")
                )

                // Disable save button until mood is selected
                btnSaveMood.isEnabled = false
                btnSaveMood.alpha = 0.5f
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
        button.animate()
            .alpha(1.0f)
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .start()
    }

    private fun saveMood() {
        selectedMood?.let { mood ->
            val note = binding.etNote.text.toString().trim()

            // Disable button to prevent double-click
            binding.btnSaveMood.isEnabled = false

            // Save the mood entry
            viewModel.saveMoodEntry(
                date = selectedDate,
                scale = mood,
                note = note.ifEmpty { null }
            )

            // Check if notifications are enabled and send notification
            lifecycleScope.launch {
                val notificationsEnabled = prefManager.notificationsEnabled.first()

                if (notificationsEnabled) {
                    // Send notification
                    reminderScheduler.sendMoodCreatedNotification(
                        moodName = mood.displayName,
                        moodEmoji = mood.emoji
                    )
                }

                // Show success message
                val message = if (isEditMode) {
                    "Mood updated successfully!"
                } else {
                    "Mood saved successfully!"
                }

                android.widget.Toast.makeText(
                    requireContext(),
                    message,
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                // Navigate back with a slight delay for better UX
                binding.root.postDelayed({
                    findNavController().navigateUp()
                }, 300)
            }
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