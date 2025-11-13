package com.example.prog7314_universe.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prog7314_universe.Models.JournalEntry
import com.example.prog7314_universe.repo.JournalRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for Journal with Firebase integration
 * Manages journal entries and provides data to UI
 */
class JournalViewModel(
    private val repository: JournalRepository = JournalRepository()
) : ViewModel() {

    private val _journalEntries = MutableLiveData<List<JournalEntry>>()
    val journalEntries: LiveData<List<JournalEntry>> = _journalEntries

    private val _selectedDate = MutableLiveData<Date>(Date())
    val selectedDate: LiveData<Date> = _selectedDate

    private val _viewMode = MutableLiveData<ViewMode>(ViewMode.DAILY)
    val viewMode: LiveData<ViewMode> = _viewMode

    private val _currentEntry = MutableLiveData<JournalEntry?>()
    val currentEntry: LiveData<JournalEntry?> = _currentEntry

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage

    init {
        observeJournalEntries()
    }

    /**
     * Observe journal entries from Firebase Flow
     */
    private fun observeJournalEntries() {
        viewModelScope.launch {
            repository.getAllEntries().collect { entries ->
                _journalEntries.value = entries
            }
        }
    }

    /**
     * Create new journal entry
     */
    fun createJournalEntry(title: String, content: String, imageUri: Uri? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.createEntry(title, content, imageUri)

                result.onSuccess {
                    _successMessage.value = "Journal entry created!"
                    _errorMessage.value = null
                }.onFailure { error ->
                    _errorMessage.value = "Failed to create entry: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create entry: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update existing journal entry
     */
    fun updateJournalEntry(
        entryId: String,
        title: String,
        content: String,
        imageUri: Uri? = null,
        keepExistingImage: Boolean = false
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.updateEntry(entryId, title, content, imageUri, keepExistingImage)

                result.onSuccess {
                    _successMessage.value = "Journal entry updated!"
                    _errorMessage.value = null
                }.onFailure { error ->
                    _errorMessage.value = "Failed to update entry: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update entry: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete journal entry
     */
    fun deleteJournalEntry(entryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.deleteEntry(entryId)

                result.onSuccess {
                    _successMessage.value = "Journal entry deleted!"
                    _errorMessage.value = null
                }.onFailure { error ->
                    _errorMessage.value = "Failed to delete entry: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete entry: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load entry by ID
     */
    fun loadEntryById(entryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entry = repository.getEntryById(entryId)
                _currentEntry.value = entry
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load entry: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get entries for specific date
     */
    fun getEntriesForDate(date: Date): List<JournalEntry> {
        return journalEntries.value?.filter { entry ->
            entry.createdAt?.toDate()?.let { createdDate ->
                isSameDay(createdDate, date)
            } ?: false
        } ?: emptyList()
    }

    /**
     * Get entries for current week
     */
    fun getEntriesForWeek(): List<JournalEntry> {
        val calendar = Calendar.getInstance()
        val startOfWeek = calendar.apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        val endOfWeek = calendar.apply {
            add(Calendar.DAY_OF_WEEK, 7)
        }.time

        return journalEntries.value?.filter { entry ->
            entry.createdAt?.toDate()?.let { createdDate ->
                createdDate.after(startOfWeek) && createdDate.before(endOfWeek)
            } ?: false
        } ?: emptyList()
    }

    /**
     * Set view mode (daily or weekly)
     */
    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    /**
     * Navigate to previous day/week
     */
    fun navigatePrevious() {
        _selectedDate.value?.let { current ->
            val calendar = Calendar.getInstance()
            calendar.time = current

            when (_viewMode.value) {
                ViewMode.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, -1)
                ViewMode.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, -1)
                else -> return
            }

            _selectedDate.value = calendar.time
        }
    }

    /**
     * Navigate to next day/week
     */
    fun navigateNext() {
        _selectedDate.value?.let { current ->
            val calendar = Calendar.getInstance()
            calendar.time = current

            when (_viewMode.value) {
                ViewMode.DAILY -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                ViewMode.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                else -> return
            }

            _selectedDate.value = calendar.time
        }
    }

    /**
     * Search journal entries by title or content
     */
    fun searchEntries(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = repository.searchEntries(query)
                // You can store results in a separate LiveData if needed
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Check if two dates are on the same day
     */
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _successMessage.value = null
    }
}

/**
 * Enum for journal view modes
 */
enum class ViewMode {
    DAILY,
    WEEKLY
}
