package ru.mirea.barnatovich.flowers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import java.util.Calendar

// Постановка и отмена ежедневного точного будильника на 20:00
object ReminderScheduler {
    const val HOUR = 20
    const val MINUTE = 0
    private const val PREFS = "reminder"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    private fun setEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_ENABLED, enabled) }

    // Ближайшие 20:00: сегодня, если время ещё не прошло, иначе завтра
    fun nextTrigger(now: Calendar = Calendar.getInstance()): Calendar =
        (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, HOUR)
            set(Calendar.MINUTE, MINUTE)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Возвращает false, если нет разрешения на точные будильники
    fun schedule(context: Context): Boolean {
        if (!canScheduleExact(context)) return false
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTrigger().timeInMillis,
            pendingIntent(context)
        )
        setEnabled(context, true)
        return true
    }

    fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context))
        setEnabled(context, false)
    }
}
