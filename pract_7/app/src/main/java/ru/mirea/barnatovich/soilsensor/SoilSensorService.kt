package ru.mirea.barnatovich.soilsensor

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

// Привязанный (bound) сервис: «датчик» живёт, пока к нему кто-то подключён
class SoilSensorService : Service() {

    inner class LocalBinder : Binder() {
        fun getService(): SoilSensorService = this@SoilSensorService
    }

    private val binder = LocalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _moisture = MutableStateFlow<Int?>(null)
    val moisture: StateFlow<Int?> = _moisture

    override fun onCreate() {
        super.onCreate()
        // Каждую секунду «снимаем показание» влажности почвы, 0..100 %
        scope.launch {
            while (isActive) {
                _moisture.value = Random.nextInt(0, 101)
                delay(1000)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
