package ru.mirea.barnatovich.airquality.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

// Загружает текущий индекс AQI для одного города (Open-Meteo Air Quality API)
class CityAqiWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val city = inputData.getString(AirKeys.KEY_CITY) ?: return Result.failure()
        val lat = inputData.getDouble(AirKeys.KEY_LAT, 0.0)
        val lon = inputData.getDouble(AirKeys.KEY_LON, 0.0)

        updateNotification()
        return try {
            // Имитируем «долгую» загрузку, каждую секунду обновляем общее уведомление
            val seconds = Random.nextInt(4, 13)
            repeat(seconds) {
                delay(1000)
                updateNotification()
            }
            val aqi = loadAqi(lat, lon)
            AirProgress.markDone(city, aqi)
            updateNotification()
            Result.success(workDataOf(AirKeys.KEY_CITY to city, AirKeys.KEY_AQI to aqi))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val message = "Не удалось загрузить данные для города $city"
            AirNotifications.showResult(applicationContext, "Ошибка загрузки", message)
            Result.failure(workDataOf(AirKeys.KEY_ERROR to message))
        }
    }

    private suspend fun updateNotification() {
        setForeground(AirNotifications.foregroundInfo(applicationContext, AirProgress.text(), AirProgress.doneCount()))
    }

    private suspend fun loadAqi(lat: Double, lon: Double): Int = withContext(Dispatchers.IO) {
        val url = URL("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        try {
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(body).getJSONObject("current").getInt("us_aqi")
        } finally {
            connection.disconnect()
        }
    }
}
