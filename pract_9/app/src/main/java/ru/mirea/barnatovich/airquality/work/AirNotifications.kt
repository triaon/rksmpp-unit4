package ru.mirea.barnatovich.airquality.work

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ForegroundInfo
import ru.mirea.barnatovich.airquality.MainActivity

object AirNotifications {
    private const val CHANNEL_ID = "air_report"
    const val PROGRESS_ID = 1
    private const val RESULT_ID = 2

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Отчёт о качестве воздуха", NotificationManager.IMPORTANCE_LOW)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )

    // Постоянное (ongoing) уведомление с прогрессом по городам
    fun progress(context: Context, text: String, done: Int): Notification =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Качество воздуха")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setProgress(AirKeys.cities.size, done, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Без этого Android 12+ откладывает показ уведомления foreground-сервиса на ~10 секунд
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp(context))
            .build()

    fun foregroundInfo(context: Context, text: String, done: Int): ForegroundInfo {
        val notification = progress(context, text, done)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(PROGRESS_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(PROGRESS_ID, notification)
        }
    }

    // Показываем сразу после нажатия кнопки, ещё до старта Worker'ов
    fun showStarted(context: Context) {
        notify(context, RESULT_ID, null)
        notify(context, PROGRESS_ID, progress(context, AirProgress.text(), 0))
    }

    // Итоговое уведомление (уже не ongoing) — живёт после остановки foreground-сервиса
    fun showResult(context: Context, title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(openApp(context))
            .build()
        notify(context, RESULT_ID, notification)
    }

    private fun notify(context: Context, id: Int, notification: Notification?) {
        val manager = NotificationManagerCompat.from(context)
        if (notification == null) {
            manager.cancel(id)
            return
        }
        val granted = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) manager.notify(id, notification)
    }
}
