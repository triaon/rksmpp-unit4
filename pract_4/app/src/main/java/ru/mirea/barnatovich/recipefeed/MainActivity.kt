package ru.mirea.barnatovich.recipefeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                FeedScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen() {
    val context = LocalContext.current
    val repository = remember { FeedRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    val posts = remember { mutableStateListOf<PostUi>() }
    var loadJob by remember { mutableStateOf<Job?>(null) }
    var status by remember { mutableStateOf("Загружаем ленту...") }
    var refreshCount by remember { mutableIntStateOf(0) }

    fun refresh() {
        // Отмена родительской Job отменяет все дочерние загрузки аватарок и отзывов
        loadJob?.cancel()
        refreshCount++
        loadJob = scope.launch {
            status = "Загружаем ленту..."
            posts.clear()
            val list = repository.loadPosts()
            posts.addAll(list.map { PostUi(it, CardState.Loading) })
            status = "Загружаем аватарки и отзывы..."
            val start = System.currentTimeMillis()
            // Каждый пост грузится в своей корутине и обновляется сразу, как готов
            list.map { post ->
                async {
                    val state = repository.loadPostDetails(post)
                    val index = posts.indexOfFirst { it.post.id == post.id }
                    if (index >= 0) posts[index] = posts[index].copy(state = state)
                }
            }.awaitAll()
            val errors = posts.count { it.state is CardState.Error }
            status = "Готово за ${System.currentTimeMillis() - start} мс, с ошибками: $errors"
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Лента рецептов") },
                actions = {
                    TextButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Обновить")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val ready = posts.count { it.state !is CardState.Loading }
            Text(
                "$status  ($ready из ${posts.size}, обновлений: $refreshCount)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            if (posts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(posts, key = { it.post.id }) { PostCard(it) }
                }
            }
        }
    }
}

@Composable
fun PostCard(item: PostUi) {
    val state = item.state
    val avatarColor = when (state) {
        is CardState.Ready -> state.avatarColor
        is CardState.Error -> state.avatarColor
        CardState.Loading -> null
    }
    val comments = when (state) {
        is CardState.Ready -> state.comments
        is CardState.Error -> state.comments
        CardState.Loading -> null
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(state, avatarColor, item.post.author)
                Spacer(Modifier.width(10.dp))
                Text(
                    item.post.author,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall
                )
                StateLabel(state)
            }
            Spacer(Modifier.height(8.dp))
            Text(item.post.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(item.post.body, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))
            when {
                state is CardState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Загружаем отзывы...", style = MaterialTheme.typography.bodySmall)
                }
                comments == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Отзывы не загрузились",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                comments.isEmpty() -> Text("Отзывов пока нет", style = MaterialTheme.typography.bodySmall)
                else -> {
                    Text("Отзывы (${comments.size}):", style = MaterialTheme.typography.labelLarge)
                    comments.forEach {
                        Text("${it.name}: ${it.body}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (state is CardState.Error) {
                Text(
                    "Ошибка: ${state.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun Avatar(state: CardState, color: String?, author: String) {
    val modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
    when {
        state is CardState.Loading -> Box(
            modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        color == null -> Box(
            modifier.background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            // placeholder вместо аватарки
            Icon(Icons.Default.Warning, contentDescription = "Ошибка", tint = MaterialTheme.colorScheme.error)
        }
        else -> Box(
            modifier.background(Color(android.graphics.Color.parseColor(color))),
            contentAlignment = Alignment.Center
        ) {
            Text(author.take(1), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StateLabel(state: CardState) {
    val (text, color) = when (state) {
        CardState.Loading -> "Загрузка" to MaterialTheme.colorScheme.outline
        is CardState.Ready -> "Готово" to Color(0xFF2E7D32)
        is CardState.Error -> "Ошибка" to MaterialTheme.colorScheme.error
    }
    Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
}
