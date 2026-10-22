package ru.mirea.barnatovich.spacefacts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private val SpaceColors = darkColorScheme(
    primary = Color(0xFF9FA8FF),
    background = Color(0xFF0B1026),
    surface = Color(0xFF0B1026),
    surfaceVariant = Color(0xFF1C2452)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Тёмный фон — светлые значки в статус-баре
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MaterialTheme(colorScheme = SpaceColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FactsScreen()
                }
            }
        }
    }
}

@Composable
fun FactsScreen(vm: FactsViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Факты о космосе", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 260.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                state.isLoading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(56.dp))
                    Text("Ищем факт в глубинах космоса…", modifier = Modifier.padding(top = 16.dp))
                }
                state.fact == null -> Text(
                    "Нажмите «Новый факт!», чтобы узнать что-нибудь интересное о космосе",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                // Новый ключ на каждый факт — карточка каждый раз появляется с анимацией
                else -> key(state.factNumber) {
                    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                    androidx.compose.animation.AnimatedVisibility(
                        visibleState = visible,
                        enter = fadeIn(tween(700)) + scaleIn(tween(700), initialScale = 0.8f) +
                            slideInVertically(tween(700)) { it / 3 }
                    ) {
                        FactCard(state.factNumber, state.fact ?: "")
                    }
                }
            }
        }

        Button(
            onClick = { vm.onNewFactClick() },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Новый факт!", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun FactCard(number: Int, fact: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Факт №$number", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(fact, style = MaterialTheme.typography.titleLarge)
        }
    }
}
