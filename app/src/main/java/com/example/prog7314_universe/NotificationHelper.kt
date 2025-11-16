package com.example.prog7314_universe.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.prog7314_universe.MainActivity
import com.example.prog7314_universe.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_EXAMS = "exam_reminders"
        const val CHANNEL_ID_TASKS = "task_reminders"
        const val CHANNEL_ID_GENERAL = "general_reminders"

        const val NOTIFICATION_TYPE_EXAM = "exam"
        const val NOTIFICATION_TYPE_TASK = "task"

        // Notification IDs
        const val EXAM_BASE_ID = 1000
        const val TASK_BASE_ID = 2000
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val examChannel = NotificationChannel(
                CHANNEL_ID_EXAMS,
                "Exam Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for upcoming exams"
                enableVibration(true)
                setShowBadge(true)
            }

            val taskChannel = NotificationChannel(
                CHANNEL_ID_TASKS,
                "Task & Assignment Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for task due dates"
                enableVibration(true)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(examChannel)
            notificationManager?.createNotificationChannel(taskChannel)
            notificationManager?.createNotificationChannel(generalChannel)
        }
    }

    fun sendNotification(
        title: String,
        message: String,
        type: String,
        itemId: String,
        notificationId: Int
    ) {
        val channelId = when(type) {
            NOTIFICATION_TYPE_EXAM -> CHANNEL_ID_EXAMS
            NOTIFICATION_TYPE_TASK -> CHANNEL_ID_TASKS
            else -> CHANNEL_ID_GENERAL
        }

        // Create intent to open the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("itemId", itemId)
            putExtra("type", type)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Make sure to add this icon
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}