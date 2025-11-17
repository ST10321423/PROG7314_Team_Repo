package com.example.prog7314_universe.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.prog7314_universe.MainActivity
import com.example.prog7314_universe.R

class NotificationHelper(private val context: Context) {

    companion object {
        // Notification channels
        const val CHANNEL_EXAM = "exam_reminders"
        const val CHANNEL_TASK = "task_reminders"
        const val CHANNEL_GENERAL = "general_notifications"

        // Notification types
        const val NOTIFICATION_TYPE_EXAM = "exam"
        const val NOTIFICATION_TYPE_TASK = "task"
        const val NOTIFICATION_TYPE_GENERAL = "general"

        // Notification ID bases (to avoid conflicts)
        const val EXAM_BASE_ID = 1000
        const val TASK_BASE_ID = 2000
        const val GENERAL_BASE_ID = 3000
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Create notification channels for different types of notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Exam reminders channel
            val examChannel = NotificationChannel(
                CHANNEL_EXAM,
                "Exam Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming exams"
                enableVibration(true)
            }

            // Task reminders channel
            val taskChannel = NotificationChannel(
                CHANNEL_TASK,
                "Task Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for task deadlines"
                enableVibration(true)
            }

            // General notifications channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General app notifications for mood, journal, and habit updates"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(examChannel)
            notificationManager.createNotificationChannel(taskChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }

    /**
     * Send a notification
     * @param title Notification title
     * @param message Notification message
     * @param type Type of notification (exam, task, or general)
     * @param itemId ID of the related item (for deep linking)
     * @param notificationId Unique notification ID
     */
    fun sendNotification(
        title: String,
        message: String,
        type: String,
        itemId: String,
        notificationId: Int
    ) {
        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", type)
            putExtra("itemId", itemId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Select channel based on type
        val channelId = when (type) {
            NOTIFICATION_TYPE_EXAM -> CHANNEL_EXAM
            NOTIFICATION_TYPE_TASK -> CHANNEL_TASK
            else -> CHANNEL_GENERAL
        }

        // Build notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        // Send notification
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Cancel a specific notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}