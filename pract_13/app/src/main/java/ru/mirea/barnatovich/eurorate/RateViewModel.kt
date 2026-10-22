package ru.mirea.barnatovich.eurorate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

class RateViewModel : ViewModel() {

    // Текущий курс евро к рублю. StateFlow хранит последнее значение — при повороте экрана оно не теряется
    private val _rate = MutableStateFlow(100.0)
    val rate: StateFlow<Double> = _rate.asStateFlow()

    // Предыдущее значение — чтобы показать стрелку роста/падения
    private val _previousRate = MutableStateFlow<Double?>(null)
    val previousRate: StateFlow<Double?> = _previousRate.asStateFlow()

    private val _updatedAt = MutableStateFlow(now())
    val updatedAt: StateFlow<String> = _updatedAt.asStateFlow()

    private var timerJob: Job? = null

    init {
        startTimer()
    }

    // Имитация биржи: новый курс каждые 5 секунд
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                emitNewRate()
            }
        }
    }

    // Ручное обновление; отсчёт 5 секунд начинается заново
    fun refreshNow() {
        emitNewRate()
        startTimer()
    }

    private fun emitNewRate() {
        var newRate: Double
        do {
            newRate = ((100.0 + Random.nextDouble(-2.0, 2.0)) * 100).roundToInt() / 100.0
        } while (newRate == _rate.value)
        _previousRate.value = _rate.value
        _rate.value = newRate
        _updatedAt.value = now()
    }

    private fun now() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
}
