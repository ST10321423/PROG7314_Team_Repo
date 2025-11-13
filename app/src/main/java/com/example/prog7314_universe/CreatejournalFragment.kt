package com.example.prog7314_universe

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.prog7314_universe.databinding.FragmentCreatejournalBinding
import com.example.prog7314_universe.viewmodel.JournalViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for creating/editing journal entry
 * Allows users to write entries with optional images
 */
class CreateJournalFragment : Fragment() {

    private var _binding: FragmentCreatejournalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JournalViewModel by viewModels()
    private var currentEntryId: String? = null
    private var selectedImageUri: Uri? = null

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                displayImage(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatejournalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get entry ID from arguments if editing
        currentEntryId = arguments?.getString("entry_id")

        setupUI()
        setupClickListeners()
        loadExistingEntry()
    }

    private fun setupUI() {
        // Display current date/time
        val dateFormat = SimpleDateFormat("MMM d, yyyy - h:mm a", Locale.getDefault())
        binding.tvDate.text = dateFormat.format(Date())

        // Set title based on mode
        binding.tvTitle.text = if (currentEntryId != null) {
            "Edit Journal Entry"
        } else {
            "New Journal Entry"
        }
    }

    private fun setupClickListeners() {
        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // Upload image button
        binding.btnUploadImage.setOnClickListener {
            openImagePicker()
        }

        // Remove image button
        binding.btnRemoveImage.setOnClickListener {
            removeImage()
        }

        // Create/Update entry button
        binding.btnCreateEntry.setOnClickListener {
            saveEntry()
        }
    }

    private fun loadExistingEntry() {
        currentEntryId?.let { entryId ->
            // Find entry in ViewModel
            viewModel.journalEntries.value?.find { it.entryId == entryId }?.let { entry ->
                binding.apply {
                    etTitle.setText(entry.title)
                    etContent.setText(entry.content)

                    entry.imageUri?.let { uri ->
                        selectedImageUri = Uri.parse(uri)
                        displayImage(selectedImageUri!!)
                    }

                    btnCreateEntry.text = "Update Entry"
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private fun displayImage(uri: Uri) {
        binding.apply {
            ivJournalImage.visibility = View.VISIBLE
            btnRemoveImage.visibility = View.VISIBLE

            Glide.with(requireContext())
                .load(uri)
                .centerCrop()
                .into(ivJournalImage)
        }
    }

    private fun removeImage() {
        selectedImageUri = null
        binding.apply {
            ivJournalImage.visibility = View.GONE
            btnRemoveImage.visibility = View.GONE
        }
    }

    private fun saveEntry() {
        val title = binding.etTitle.text.toString().trim()
        val content = binding.etContent.text.toString().trim()

        // Validate input
        if (title.isEmpty()) {
            binding.etTitle.error = "Title is required"
            return
        }

        if (content.isEmpty()) {
            binding.etContent.error = "Content is required"
            return
        }

        // Save or update entry
        val imageUriString = selectedImageUri?.toString()

        if (currentEntryId != null) {
            // Update existing entry
            viewModel.updateJournalEntry(currentEntryId!!, title, content, imageUriString as Uri?)
            android.widget.Toast.makeText(
                requireContext(),
                "Journal entry updated!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            // Create new entry
            viewModel.createJournalEntry(title, content, imageUriString as Uri?)
            android.widget.Toast.makeText(
                requireContext(),
                "Journal entry created!",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        // Navigate back
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}




