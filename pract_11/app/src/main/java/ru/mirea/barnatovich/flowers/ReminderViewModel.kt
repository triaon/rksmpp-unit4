package ru.mirea.barnatovich.flowers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class ReminderState(
    val enabled: Boolean = false,
    val nextText: String = "",
    val needExactAlarmPermission: Boolean = false
)

class ReminderViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(ReminderState())
    val state: StateFlow<ReminderState> = _state.asStateFlow()

    // Пользователь нажал «Включить», но ушёл выдавать разрешение на точные будильники
    private var pendingEnable = false

    init {
        refresh()
    }

    // Вызывается при возврате на экран: время могло перейти через 20:00, разрешение могло измениться
    fun refresh() {
        if (pendingEnable && ReminderScheduler.canScheduleExact(app)) {
            pendingEnable = false
            enable()
            return
        }
        _state.value = _state.value.copy(
            enabled = ReminderScheduler.isEnabled(app),
            nextText = nextText()
        )
    }

    fun enable() {
        if (ReminderScheduler.schedule(app)) {
            _state.value = ReminderState(enabled = true, nextText = nextText())
        } else {
            pendingEnable = true
            _state.value = _state.value.copy(needExactAlarmPermission = true)
        }
    }

    fun disable() {
        ReminderScheduler.cancel(app)
        _state.value = ReminderState(enabled = false, nextText = nextText())
    }

    fun permissionHintShown() {
        _state.value = _state.value.copy(needExactAlarmPermission = false)
    }

    private fun nextText(): String {
        val now = Calendar.getInstance()
        val next = ReminderScheduler.nextTrigger(now)
        val day = if (next.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) "сегодня" else "завтра"
        return "Следующее напоминание: $day в %02d:%02d".format(ReminderScheduler.HOUR, ReminderScheduler.MINUTE)
    }
}
