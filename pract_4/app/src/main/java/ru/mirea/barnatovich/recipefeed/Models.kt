package ru.mirea.barnatovich.recipefeed

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Int,
    val userId: Int,
    val author: String,
    val avatarColor: String,
    val title: String,
    val body: String
)

@Serializable
data class Comment(val postId: Int, val id: Int, val name: String, val body: String)

// Состояние карточки поста
sealed class CardState {
    object Loading : CardState()
    data class Ready(val avatarColor: String, val comments: List<Comment>) : CardState()
    // avatarColor / comments == null — эта часть не загрузилась
    data class Error(val avatarColor: String?, val comments: List<Comment>?, val message: String) : CardState()
}

data class PostUi(val post: Post, val state: CardState)
