package ru.mirea.barnatovich.flowers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

// Срабатывание будильника: показываем уведомление и ставим будильник на следующий день
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        showNotification(context)
        // Точный будильник одноразовый — переставляем на завтра
        if (ReminderScheduler.isEnabled(context)) ReminderScheduler.schedule(context)
    }

    private fun showNotification(context: Context) {
        createChannel(context)
        val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return

        val openApp = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flower)
            .setContentTitle("Время полить цветы!")
            .setContentText("Не забудьте полить домашние растения")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_REMIND = "ru.mirea.barnatovich.flowers.REMIND"
        private const val CHANNEL_ID = "watering"
        private const val NOTIFICATION_ID = 20

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Полив цветов", NotificationManager.IMPORTANCE_HIGH)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
