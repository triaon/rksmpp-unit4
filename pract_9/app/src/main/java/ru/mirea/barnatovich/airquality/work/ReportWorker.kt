package ru.mirea.barnatovich.airquality.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Финальный Worker: объединяет результаты всех городов (ArrayCreatingInputMerger) в отчёт
class ReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cities = inputData.getStringArray(AirKeys.KEY_CITY) ?: emptyArray()
        val values = inputData.getIntArray(AirKeys.KEY_AQI) ?: IntArray(0)
        if (values.isEmpty()) return Result.failure(workDataOf(AirKeys.KEY_ERROR to "Нет данных для отчёта"))

        // Если приложение уже в фоне, система может не дать снова поднять foreground-сервис — тогда просто считаем отчёт
        runCatching {
            setForeground(
                AirNotifications.foregroundInfo(applicationContext, "Все данные получены, формируем отчёт…", cities.size)
            )
        }
        delay(3000)

        val avg = values.average().roundToInt()
        AirNotifications.showResult(applicationContext, "Качество воздуха", "Отчёт готов! Средний AQI $avg")
        return Result.success(workDataOf(AirKeys.KEY_AVG to avg))
    }
}
