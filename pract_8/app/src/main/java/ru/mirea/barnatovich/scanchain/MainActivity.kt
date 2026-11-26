package ru.mirea.barnatovich.scanchain

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScanScreen()
                }
            }
        }
    }
}

@Composable
fun ScanScreen(vm: ScanViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Подготовка скана к отправке", style = MaterialTheme.typography.titleLarge)

        Card(
            modifier = Modifier.fillMaxWidth().height(330.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            ScanPreview(state.previewPath)
        }

        Text(
            text = state.status,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        state.stepLabel?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge)
        }
        if (state.isRunning) {
            LinearProgressIndicator(
                progress = { state.progress ?: 0f },
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
        }

        Button(
            onClick = { vm.start() },
            enabled = !state.isRunning,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Начать обработку и отправку")
        }

        state.result?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ScanPreview(path: String?) {
    // Пока ни один шаг не завершён — показываем исходный скан из ресурсов
    val bitmap = remember(path) { path?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
    val modifier = Modifier.fillMaxSize().padding(8.dp)
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = "Скан", modifier = modifier, contentScale = ContentScale.Fit)
    } else {
        Image(
            painter = painterResource(R.drawable.scan_source),
            contentDescription = "Исходный скан",
            modifier = modifier,
            contentScale = ContentScale.Fit
        )
    }
}
