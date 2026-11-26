package ru.mirea.barnatovich.booksearch

import kotlinx.serialization.Serializable

@Serializable
data class Book(
    val id: Int,
    val title: String,
    val author: String,
    val description: String,
    val year: Int,
    val genre: String
)
