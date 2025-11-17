package com.example.prog7314_universe.utils

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.prog7314_universe.workers.ReminderWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val notificationHelper = NotificationHelper(context)

    /**
     * Send immediate notification for mood creation
     */
    fun sendMoodCreatedNotification(moodName: String, moodEmoji: String) {
        notificationHelper.sendNotification(
            title = "$moodEmoji Mood Logged",
            message = "You're feeling $moodName today. Keep track of your emotional journey!",
            type = NotificationHelper.NOTIFICATION_TYPE_GENERAL,
            itemId = "",
            notificationId = System.currentTimeMillis().toInt()
        )
    }

    /**
     * Send immediate notification for journal creation
     */
    fun sendJournalCreatedNotification(journalTitle: String, isUpdate: Boolean = false) {
        val action = if (isUpdate) "updated" else "created"
        notificationHelper.sendNotification(
            title = "📝 Journal Entry ${action.capitalize()}",
            message = "\"$journalTitle\" has been successfully saved!",
            type = NotificationHelper.NOTIFICATION_TYPE_GENERAL,
            itemId = "",
            notificationId = System.currentTimeMillis().toInt()
        )
    }

    /**
     * Send immediate notification for task creation
     */
    fun sendTaskCreatedNotification(taskTitle: String) {
        notificationHelper.sendNotification(
            title = "✅ New Task Added",
            message = "\"$taskTitle\" has been added to your task list!",
            type = NotificationHelper.NOTIFICATION_TYPE_GENERAL,
            itemId = "",
            notificationId = System.currentTimeMillis().toInt()
        )
    }

    /**
     * Send immediate notification for habit creation
     */
    fun sendHabitCreatedNotification(habitName: String) {
        notificationHelper.sendNotification(
            title = "🎯 New Habit Created",
            message = "Start building your new habit: $habitName",
            type = NotificationHelper.NOTIFICATION_TYPE_GENERAL,
            itemId = "",
            notificationId = System.currentTimeMillis().toInt()
        )
    }

    /**
     * Schedule exam reminders
     * @param examId The unique ID of the exam
     * @param examSubject The subject name
     * @param examDate Date string in format "EEEE, MMMM dd, yyyy"
     * @param examStartTime Time string in format "hh:mm a"
     * @param reminderDaysBefore List of days before to remind (e.g., [7, 3, 1])
     */
    fun scheduleExamReminder(
        examId: String,
        examSubject: String,
        examDate: String,
        examStartTime: String,
        reminderDaysBefore: List<Int> = listOf(7, 3, 1)
    ) {
        // Parse the exam date and time
        val examDateTime = parseExamDateTime(examDate, examStartTime)
        if (examDateTime == null) {
            return // Invalid date/time format
        }

        reminderDaysBefore.forEach { daysBefore ->
            val reminderTime = examDateTime - (daysBefore * 24 * 60 * 60 * 1000L)
            val delay = reminderTime - System.currentTimeMillis()

            // Only schedule if the reminder is in the future
            if (delay > 0) {
                val message = when (daysBefore) {
                    1 -> "Your exam for $examSubject is tomorrow!"
                    else -> "Your exam for $examSubject is in $daysBefore days!"
                }

                scheduleNotification(
                    uniqueTag = "exam_${examId}_${daysBefore}d",
                    notificationId = generateExamNotificationId(examId, daysBefore),
                    title = "📚 Exam Reminder",
                    message = message,
                    type = NotificationHelper.NOTIFICATION_TYPE_EXAM,
                    itemId = examId,
                    delay = delay
                )
            }
        }

        // Also schedule a notification on the day of the exam (2 hours before)
        val twoHoursBefore = examDateTime - (2 * 60 * 60 * 1000L)
        val delayForDayOf = twoHoursBefore - System.currentTimeMillis()

        if (delayForDayOf > 0) {
            scheduleNotification(
                uniqueTag = "exam_${examId}_today",
                notificationId = generateExamNotificationId(examId, 0),
                title = "📚 Exam Today!",
                message = "Your exam for $examSubject starts soon. Good luck! 🍀",
                type = NotificationHelper.NOTIFICATION_TYPE_EXAM,
                itemId = examId,
                delay = delayForDayOf
            )
        }
    }

    /**
     * Schedule task reminders
     * @param taskId The unique ID of the task
     * @param taskTitle The task title
     * @param dueDate ISO date string
     * @param reminderHoursBefore List of hours before to remind (e.g., [24, 6])
     */
    fun scheduleTaskReminder(
        taskId: String,
        taskTitle: String,
        dueDate: String,
        reminderHoursBefore: List<Int> = listOf(24, 6, 1)
    ) {
        // Parse ISO date string
        val dueDateTime = try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                .parse(dueDate)?.time ?: return
        } catch (e: Exception) {
            return // Invalid date format
        }

        reminderHoursBefore.forEach { hoursBefore ->
            val reminderTime = dueDateTime - (hoursBefore * 60 * 60 * 1000L)
            val delay = reminderTime - System.currentTimeMillis()

            if (delay > 0) {
                val message = when (hoursBefore) {
                    1 -> "Task '$taskTitle' is due in 1 hour!"
                    else -> "Task '$taskTitle' is due in $hoursBefore hours!"
                }

                scheduleNotification(
                    uniqueTag = "task_${taskId}_${hoursBefore}h",
                    notificationId = generateTaskNotificationId(taskId, hoursBefore),
                    title = "✅ Task Reminder",
                    message = message,
                    type = NotificationHelper.NOTIFICATION_TYPE_TASK,
                    itemId = taskId,
                    delay = delay
                )
            }
        }
    }

    /**
     * Cancel all reminders for a specific exam
     */
    fun cancelExamReminders(examId: String) {
        listOf(7, 3, 1, 0).forEach { day ->
            workManager.cancelAllWorkByTag("exam_${examId}_${day}d")
        }
        workManager.cancelAllWorkByTag("exam_${examId}_today")
    }

    /**
     * Cancel all reminders for a specific task
     */
    fun cancelTaskReminders(taskId: String) {
        listOf(24, 6, 1).forEach { hour ->
            workManager.cancelAllWorkByTag("task_${taskId}_${hour}h")
        }
    }

    private fun scheduleNotification(
        uniqueTag: String,
        notificationId: Int,
        title: String,
        message: String,
        type: String,
        itemId: String,
        delay: Long
    ) {
        val data = Data.Builder()
            .putString("title", title)
            .putString("message", message)
            .putString("type", type)
            .putString("itemId", itemId)
            .putInt("notificationId", notificationId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(uniqueTag)
            .build()

        workManager.enqueueUniqueWork(
            uniqueTag,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Parse exam date and time strings to milliseconds
     */
    private fun parseExamDateTime(dateStr: String, timeStr: String): Long? {
        return try {
            // Date format: "EEEE, MMMM dd, yyyy" e.g., "Monday, January 15, 2024"
            // Time format: "hh:mm a" e.g., "02:30 PM"
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy hh:mm a", Locale.getDefault())
            val combinedString = "$dateStr $timeStr"
            dateFormat.parse(combinedString)?.time
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate unique notification ID for exams
     */
    private fun generateExamNotificationId(examId: String, daysBefore: Int): Int {
        return NotificationHelper.EXAM_BASE_ID + examId.hashCode() + daysBefore
    }

    /**
     * Generate unique notification ID for tasks
     */
    private fun generateTaskNotificationId(taskId: String, hoursBefore: Int): Int {
        return NotificationHelper.TASK_BASE_ID + taskId.hashCode() + hoursBefore
    }
}