package ru.mirea.barnatovich.compass

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Хранит азимут между пересозданиями Activity (поворот экрана)
class CompassViewModel : ViewModel() {

    // Азимут 0..359
    private val _azimuth = MutableStateFlow(0)
    val azimuth: StateFlow<Int> = _azimuth.asStateFlow()

    // Угол поворота диска «без разрывов»: 359° -> 1° даёт +2°, а не -358°, поэтому стрелка не прыгает
    private val _rotation = MutableStateFlow(0f)
    val rotation: StateFlow<Float> = _rotation.asStateFlow()

    private val _sensorMissing = MutableStateFlow(false)
    val sensorMissing: StateFlow<Boolean> = _sensorMissing.asStateFlow()

    private var lastAzimuth: Float? = null

    fun onAzimuth(degrees: Float) {
        val prev = lastAzimuth
        if (prev == null) {
            _rotation.value = degrees
        } else {
            var delta = degrees - prev
            if (delta > 180) delta -= 360
            if (delta < -180) delta += 360
            _rotation.value += delta
        }
        lastAzimuth = degrees
        _azimuth.value = Math.round(degrees) % 360
    }

    fun onSensorMissing() {
        _sensorMissing.value = true
    }
}
