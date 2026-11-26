package ru.mirea.barnatovich.booksearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val DEBOUNCE_MS = 500L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                BookSearchScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchScreen() {
    val context = LocalContext.current
    val repository = remember { BookRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchedQuery by remember { mutableStateOf<String?>(null) }
    var cancelledCount by remember { mutableIntStateOf(0) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun onQueryChange(text: String) {
        query = text
        // Новый ввод — отменяем предыдущий поиск (или ожидание debounce)
        searchJob?.cancel()
        if (text.isBlank()) {
            results = emptyList()
            searchedQuery = null
            isLoading = false
            return
        }
        searchJob = scope.launch {
            try {
                delay(DEBOUNCE_MS) // debounce: ждём, пока пользователь перестанет печатать
                isLoading = true
                results = repository.search(text)
                searchedQuery = text
            } catch (e: CancellationException) {
                cancelledCount++
                throw e
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Поиск книг в библиотеке") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = ::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Название, автор или жанр") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(8.dp))
                    Text("Ищем «$query»...")
                } else {
                    val status = when (searchedQuery) {
                        null -> "Введите название, автора или жанр"
                        else -> "Найдено: ${results.size} по запросу «$searchedQuery»"
                    }
                    Text(status)
                }
            }
            Text(
                "Отменено устаревших запросов: $cancelledCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(8.dp))
            if (isLoading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            if (searchedQuery != null && results.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ничего не найдено")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(results, key = { it.id }) { book -> BookItem(book) }
                }
            }
        }
    }
}

@Composable
fun BookItem(book: Book) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${book.author}, ${book.year}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(book.description, style = MaterialTheme.typography.bodySmall)
            SuggestionChip(onClick = {}, label = { Text(book.genre) })
        }
    }
}
