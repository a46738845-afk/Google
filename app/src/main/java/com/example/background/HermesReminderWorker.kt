package com.example.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.HermesApplication

class HermesReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "Hermes Reminder"
        val promptAction = inputData.getString("prompt") ?: "You have a scheduled reminder."
        val taskId = inputData.getLong("task_id", -1L)

        // Show notification to user
        showNotification(title, promptAction)

        // Mark completed in database if repository is available
        if (taskId != -1L) {
            try {
                HermesApplication.instance.memoryRepository.markTaskCompleted(taskId)
            } catch (e: Exception) {
                // Non-fatal
            }
        }

        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        val channelId = "hermes_reminders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hermes Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders and scheduled actions from Hermes Agent"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
