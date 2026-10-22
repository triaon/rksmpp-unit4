package ru.mirea.barnatovich.airquality.work

import java.util.concurrent.ConcurrentHashMap

data class City(val name: String, val lat: Double, val lon: Double)

object AirKeys {
    const val WORK_NAME = "air_report"
    const val TAG_CITY = "city"
    const val TAG_REPORT = "report"

    const val KEY_CITY = "city"
    const val KEY_LAT = "lat"
    const val KEY_LON = "lon"
    const val KEY_AQI = "aqi"
    const val KEY_AVG = "avg"
    const val KEY_ERROR = "error"

    val cities = listOf(
        City("Москва", 55.7558, 37.6173),
        City("Казань", 55.7964, 49.1089),
        City("Сочи", 43.5855, 39.7231),
        City("Санкт-Петербург", 59.9386, 30.3141)
    )

    fun cityTag(name: String) = "city:$name"
}

// Общий прогресс для уведомления: какие города уже готовы (все Worker'ы в одном процессе)
object AirProgress {
    private val done = ConcurrentHashMap<String, Int>()

    fun reset() = done.clear()

    fun markDone(city: String, aqi: Int) {
        done[city] = aqi
    }

    fun doneCount() = done.size

    // «Готово: Москва и Казань, Сочи в процессе…»
    fun text(): String {
        val ready = AirKeys.cities.map { it.name }.filter { done.containsKey(it) }
        val waiting = AirKeys.cities.map { it.name }.filter { !done.containsKey(it) }
        return when {
            ready.isEmpty() -> "Загружаем данные о воздухе для ${AirKeys.cities.size} городов…"
            waiting.isEmpty() -> "Все данные получены, формируем отчёт…"
            else -> "Готово: ${joinRu(ready)}, ${joinRu(waiting)} в процессе…"
        }
    }

    private fun joinRu(items: List<String>): String =
        if (items.size == 1) items[0]
        else items.dropLast(1).joinToString(", ") + " и " + items.last()
}
