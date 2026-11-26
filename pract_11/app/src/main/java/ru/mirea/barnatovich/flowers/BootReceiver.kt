package ru.mirea.barnatovich.flowers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// После перезагрузки все будильники AlarmManager сбрасываются — восстанавливаем, если напоминание было включено
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && ReminderScheduler.isEnabled(context)) {
            ReminderScheduler.schedule(context)
        }
    }
}
