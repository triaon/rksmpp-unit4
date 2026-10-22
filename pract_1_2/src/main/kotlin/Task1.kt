import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@Serializable
data class Film(val id: Int, val title: String, val genre: String, val duration: Int)

@Serializable
data class TicketItem(val film: String, val sold: Int, val revenue: Int)

@Serializable
data class TicketsReport(val date: String, val items: List<TicketItem>)

@Serializable
data class CinemaSchedule(val cinema: String, val sessions: Int, val halls: Int)

// Вероятность сбоя каждой операции (в процентах)
const val FAIL_CHANCE = 25

val json = Json { ignoreUnknownKeys = true }

// Читаем файл из resources в фоновом потоке
suspend fun readResource(name: String): String = withContext(Dispatchers.IO) {
    val stream = object {}.javaClass.getResourceAsStream("/$name")
        ?: throw IllegalStateException("файл $name не найден")
    stream.bufferedReader().use { it.readText() }
}

suspend fun loadFilms(fail: Boolean): List<String> {
    delay(1800)
    if (fail) throw RuntimeException("сервер проката не отвечает")
    return json.decodeFromString<List<Film>>(readResource("films.json")).map { it.title }
}

suspend fun loadTicketSales(fail: Boolean): Map<String, Int> {
    delay(1200)
    if (fail) throw RuntimeException("касса вернула пустой отчёт")
    val report = json.decodeFromString<TicketsReport>(readResource("tickets.json"))
    return report.items.associate { it.film to it.sold }
}

suspend fun loadCinemaSessions(fail: Boolean): List<String> {
    delay(2500)
    if (fail) throw RuntimeException("нет связи с кинотеатрами")
    return json.decodeFromString<List<CinemaSchedule>>(readResource("cinemas.json"))
        .map { "${it.cinema}: ${it.sessions} ${sessionsWord(it.sessions)}" }
}

fun sessionsWord(n: Int): String = when {
    n % 100 in 11..14 -> "сеансов"
    n % 10 == 1 -> "сеанс"
    n % 10 in 2..4 -> "сеанса"
    else -> "сеансов"
}

fun main(args: Array<String>) = runBlocking {
    // Если передан аргумент — используем его как seed, чтобы сбой воспроизводился
    val seed = args.firstOrNull()?.toIntOrNull()
    val random = if (seed != null) Random(seed) else Random.Default
    val failFilms = random.nextInt(100) < FAIL_CHANCE
    val failTickets = random.nextInt(100) < FAIL_CHANCE
    val failSessions = random.nextInt(100) < FAIL_CHANCE

    println("Кинотеатр: загрузка данных за день" + (seed?.let { " (seed = $it)" } ?: ""))
    println("Запускаем три операции параллельно...")

    val time = measureTimeMillis {
        // try-catch внутри async: ошибка одной задачи не роняет остальные
        val films = async {
            try {
                Result.success(loadFilms(failFilms))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        val tickets = async {
            try {
                Result.success(loadTicketSales(failTickets))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        val sessions = async {
            try {
                Result.success(loadCinemaSessions(failSessions))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        // Ждём все три результата и только потом выводим
        val filmsResult = films.await()
        val ticketsResult = tickets.await()
        val sessionsResult = sessions.await()

        println()
        filmsResult.onSuccess { list ->
            println("Фильмы в прокате (${list.size}):")
            list.forEach { println("  - $it") }
        }.onFailure { println("Ошибка загрузки списка фильмов: ${it.message}") }

        ticketsResult.onSuccess { map ->
            println("Продано билетов за день:")
            map.forEach { (film, sold) -> println("  $film — $sold шт.") }
            println("  Всего: ${map.values.sum()} шт.")
        }.onFailure { println("Ошибка загрузки продаж билетов: ${it.message}") }

        sessionsResult.onSuccess { list ->
            println("Сеансы в кинотеатрах:")
            list.forEach { println("  $it") }
        }.onFailure { println("Ошибка загрузки сеансов: ${it.message}") }

        val failed = listOf(filmsResult, ticketsResult, sessionsResult).count { it.isFailure }
        println()
        if (failed == 0) {
            println("Все данные загружены успешно")
        } else {
            println("Не удалось загрузить часть данных: упало задач — $failed из 3")
        }
    }
    println("Общее время выполнения: $time мс")
}
