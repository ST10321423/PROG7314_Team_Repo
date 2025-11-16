package com.example.prog7314_universe.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.prog7314_universe.Models.MoodEntry
import com.example.prog7314_universe.Models.MoodScale
import com.example.prog7314_universe.repo.MoodRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for Mood Tracker with Firebase integration
 * Manages mood entries and provides data to UI
 */
class MoodViewModel(
    private val repository: MoodRepository = MoodRepository()
) : ViewModel() {

    private val _moodEntries = MutableLiveData<List<MoodEntry>>()
    val moodEntries: LiveData<List<MoodEntry>> = _moodEntries

    private val _selectedMonth = MutableLiveData<Date>(Date())
    val selectedMonth: LiveData<Date> = _selectedMonth

    private val _weeklyMoodStats = MutableLiveData<Map<MoodScale, Int>>()
    val weeklyMoodStats: LiveData<Map<MoodScale, Int>> = _weeklyMoodStats

    private val _currentMood = MutableLiveData<MoodEntry?>()
    val currentMood: LiveData<MoodEntry?> = _currentMood

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        observeMoodEntries()
        loadWeeklyStats()
        loadTodaysMood()
    }

    fun refreshWeeklyStats() {
        loadWeeklyStats()
    }

    /**
     * Observe mood entries from Firebase Flow
     */
    private fun observeMoodEntries() {
        viewModelScope.launch {
            repository.getAllMoods().collect { moods ->
                _moodEntries.value = moods
            }
        }
    }

    /**
     * Load weekly mood statistics
     */
    private fun loadWeeklyStats() {
        viewModelScope.launch {
            try {
                val weeklyMoods = repository.getMoodsForCurrentWeek()

                val stats = mutableMapOf<MoodScale, Int>()
                MoodScale.values().forEach { scale ->
                    stats[scale] = weeklyMoods.count { it.getMoodScale() == scale }
                }

                _weeklyMoodStats.value = stats
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load weekly stats: ${e.message}"
            }
        }
    }

    /**
     * Load today's mood entry
     */
    private fun loadTodaysMood() {
        viewModelScope.launch {
            try {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val mood = repository.getMoodByDate(today)
                _currentMood.value = mood
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load today's mood: ${e.message}"
            }
        }
    }

    /**
     * Create or update mood entry for specific date
     */
    fun saveMoodEntry(date: Date = Date(), scale: MoodScale, note: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.saveMood(date, scale, note)

                result.onSuccess {
                    loadTodaysMood()
                    loadWeeklyStats()
                    _errorMessage.value = null
                }.onFailure { error ->
                    _errorMessage.value = "Failed to save mood: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save mood: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Get mood entry for specific date
     */
    fun getMoodForDate(date: Date): MoodEntry? {
        return moodEntries.value?.find { entry ->
            isSameDay(entry.date.toDate(), date)
        }
    }

    /**
     * Delete mood entry
     */
    fun deleteMood(moodId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.deleteMood(moodId)

                result.onSuccess {
                    loadTodaysMood()
                    loadWeeklyStats()
                    _errorMessage.value = null
                }.onFailure { error ->
                    _errorMessage.value = "Failed to delete mood: ${error.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete mood: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Navigate to previous month
     */
    fun previousMonth() {
        _selectedMonth.value?.let { current ->
            val calendar = Calendar.getInstance()
            calendar.time = current
            calendar.add(Calendar.MONTH, -1)
            _selectedMonth.value = calendar.time
        }
    }

    /**
     * Navigate to next month
     */
    fun nextMonth() {
        _selectedMonth.value?.let { current ->
            val calendar = Calendar.getInstance()
            calendar.time = current
            calendar.add(Calendar.MONTH, 1)
            _selectedMonth.value = calendar.time
        }
    }

    /**
     * Get all dates in current month
     */
    fun getDatesInMonth(): List<Date> {
        val calendar = Calendar.getInstance()
        _selectedMonth.value?.let { calendar.time = it }

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dates = mutableListOf<Date>()
        for (day in 1..maxDay) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            dates.add(calendar.time)
        }

        return dates
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
}
