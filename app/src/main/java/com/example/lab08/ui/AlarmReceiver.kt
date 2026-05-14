package com.example.lab08.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.lab08.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskDescription = intent.getStringExtra(EXTRA_TASK_DESCRIPTION) ?: "Tienes una tarea pendiente"
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)

        showNotification(context, taskDescription, taskId)
    }

    private fun showNotification(context: Context, description: String, taskId: Int) {
        val channelId = NotificationHelper.CHANNEL_ID

        // Asegurar que el canal existe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de tareas",
                NotificationManager.IMPORTANCE_HIGH  // HIGH = aparece en bandeja con sonido
            ).apply {
                this.description = "Notificaciones programadas de tareas"
                enableVibration(true)
                enableLights(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⏰ Recordatorio de tarea")
            .setContentText(description)
            .setStyle(NotificationCompat.BigTextStyle().bigText(description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // Alta prioridad = bandeja prominente
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(taskId, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_DESCRIPTION = "task_description"
    }
}
