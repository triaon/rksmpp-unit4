package ru.mirea.barnatovich.booksearch

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class BookRepository(private val context: Context) {

    private var cache: List<Book>? = null

    // Читаем books.json из assets один раз, в фоновом потоке
    suspend fun loadBooks(): List<Book> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("books.json").bufferedReader().use { it.readText() }
            Json.decodeFromString<List<Book>>(text)
        }.also { cache = it }
    }

    // «Поиск в библиотеке»: имитация сетевой задержки + фильтрация в фоне
    suspend fun search(query: String): List<Book> = coroutineScope {
        val books = loadBooks()
        delay(1000)
        val q = query.trim().lowercase()
        val byTitle = async(Dispatchers.Default) { books.filter { matches(it.title, q) } }
        val byOther = async(Dispatchers.Default) {
            books.filter { matches(it.author, q) || matches(it.genre, q) }
        }
        (byTitle.await() + byOther.await()).distinctBy { it.id }
    }

    private fun matches(text: String, q: String): Boolean = text.lowercase().contains(q)
}
