package com.example.prog7314_universe.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.prog7314_universe.utils.NotificationHelper

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val title = inputData.getString("title") ?: return Result.failure()
            val message = inputData.getString("message") ?: return Result.failure()
            val type = inputData.getString("type") ?: "general"
            val itemId = inputData.getString("itemId") ?: ""
            val notificationId = inputData.getInt("notificationId", 0)

            // Send the notification
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.sendNotification(title, message, type, itemId, notificationId)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}