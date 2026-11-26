package ru.mirea.barnatovich.recipefeed

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.random.Random

class FeedRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private var allComments: List<Comment>? = null

    private suspend fun readAsset(name: String): String = withContext(Dispatchers.IO) {
        context.assets.open(name).bufferedReader().use { it.readText() }
    }

    suspend fun loadPosts(): List<Post> {
        delay(600) // имитация сети
        return json.decodeFromString(readAsset("recipes_posts.json"))
    }

    // Аватарка автора: имитация загрузки, результат — строка-цвет
    private suspend fun loadAvatar(post: Post): String {
        delay(Random.nextLong(300, 2500))
        if (Random.nextInt(100) < 15) throw IOException("аватарка недоступна")
        return post.avatarColor
    }

    // Отзывы к рецепту: читаем comments.json и фильтруем по postId
    private suspend fun loadComments(postId: Int): List<Comment> {
        delay(Random.nextLong(500, 4000))
        if (Random.nextInt(100) < 15) throw IOException("сервер отзывов не ответил")
        val comments = allComments
            ?: json.decodeFromString<List<Comment>>(readAsset("comments.json")).also { allComments = it }
        return comments.filter { it.postId == postId }
    }

    // Аватарка и отзывы грузятся параллельно; ошибка одной части не отменяет другую
    suspend fun loadPostDetails(post: Post): CardState = supervisorScope {
        val avatar = async {
            try {
                Result.success(loadAvatar(post))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        val comments = async {
            try {
                Result.success(loadComments(post.id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
        val avatarResult = avatar.await()
        val commentsResult = comments.await()

        if (avatarResult.isSuccess && commentsResult.isSuccess) {
            CardState.Ready(avatarResult.getOrThrow(), commentsResult.getOrThrow())
        } else {
            val error = avatarResult.exceptionOrNull() ?: commentsResult.exceptionOrNull()
            CardState.Error(
                avatarColor = avatarResult.getOrNull(),
                comments = commentsResult.getOrNull(),
                message = error?.message ?: "ошибка загрузки"
            )
        }
    }
}
