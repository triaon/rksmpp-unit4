package ru.mirea.barnatovich.eurorate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

private val Up = Color(0xFF2E7D32)
private val Down = Color(0xFFC62828)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RateScreen()
                }
            }
        }
    }
}

@Composable
fun RateScreen(vm: RateViewModel = viewModel()) {
    val rate by vm.rate.collectAsState()
    val previous by vm.previousRate.collectAsState()
    val updatedAt by vm.updatedAt.collectAsState()

    val diff = previous?.let { rate - it }
    val color by animateColorAsState(
        targetValue = when {
            diff == null -> Color.Gray
            diff > 0 -> Up
            else -> Down
        },
        label = "rateColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Курс евро к рублю", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Евро / рубль · обновляется каждые 5 секунд", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("1 € =", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        String.format(Locale("ru"), "%.2f ₽", rate),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    if (diff != null) {
                        Icon(
                            imageVector = if (diff > 0) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                            contentDescription = if (diff > 0) "Рост" else "Падение",
                            tint = color,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
                if (diff != null) {
                    Text(
                        String.format(Locale("ru"), "%+.2f ₽ (%s)", diff, if (diff > 0) "рост" else "падение"),
                        style = MaterialTheme.typography.titleMedium,
                        color = color
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Обновлено в $updatedAt", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = { vm.refreshNow() }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = null)
            Text("  Обновить сейчас", style = MaterialTheme.typography.titleMedium)
        }
    }
}
