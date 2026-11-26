package ru.mirea.barnatovich.teatimer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// Обычный (background) сервис: ждёт заданное время, показывает уведомление и останавливается
class TeaTimerService : Service() {

    companion object {
        const val EXTRA_SECONDS = "seconds"
        const val CHANNEL_ID = "tea_timer"
        const val NOTIFICATION_ID = 2

        // Сколько секунд осталось (null — таймер не запущен)
        val remaining = MutableStateFlow<Int?>(null)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val seconds = intent?.getIntExtra(EXTRA_SECONDS, 0) ?: 0
        scope.launch {
            for (left in seconds downTo 1) {
                remaining.value = left
                delay(1000)
            }
            remaining.value = null
            showDoneNotification()
            stopSelf(startId) // сервис сам себя останавливает
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        remaining.value = null
        super.onDestroy()
    }

    private fun showDoneNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Таймер заварки", NotificationManager.IMPORTANCE_HIGH)
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Чай готов!")
            .setContentText("Время вышло, можно пить")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
