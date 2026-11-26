package ru.mirea.barnatovich.airquality

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.mirea.barnatovich.airquality.work.AirNotifications

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AirNotifications.createChannel(this)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AirScreen()
                }
            }
        }
    }
}

// Категории индекса AQI (шкала US EPA)
fun aqiLevel(aqi: Int): Pair<String, Color> = when {
    aqi <= 50 -> "хорошо" to Color(0xFF2E7D32)
    aqi <= 100 -> "умеренно" to Color(0xFFF9A825)
    aqi <= 150 -> "вредно для чувствительных" to Color(0xFFEF6C00)
    aqi <= 200 -> "вредно" to Color(0xFFC62828)
    else -> "очень вредно" to Color(0xFF6A1B9A)
}

@Composable
fun AirScreen(vm: AirViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val context = LocalContext.current

    // Уведомление показываем только с разрешением POST_NOTIFICATIONS (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.collectReport()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Мониторинг качества воздуха", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Индекс AQI собирается параллельно для 4 городов, ход работы виден в уведомлении",
            style = MaterialTheme.typography.bodyMedium
        )

        Button(
            onClick = {
                val needAsk = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
                if (needAsk) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.collectReport()
            },
            enabled = !state.isRunning,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Собрать отчёт")
        }

        state.cities.forEach { CityCard(it) }

        when {
            state.isBuildingReport -> ReportCard("Все данные получены, формируем отчёт…", null)
            state.average != null -> ReportCard("Отчёт готов! Средний AQI ${state.average}", state.average)
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun CityCard(row: CityRow) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(row.name, style = MaterialTheme.typography.titleMedium)
                val subtitle = when (row.status) {
                    CityStatus.WAITING -> "в очереди"
                    CityStatus.LOADING -> "загрузка…"
                    CityStatus.DONE -> aqiLevel(row.aqi ?: 0).first
                    CityStatus.ERROR -> "ошибка загрузки"
                }
                val color = when (row.status) {
                    CityStatus.DONE -> aqiLevel(row.aqi ?: 0).second
                    CityStatus.ERROR -> MaterialTheme.colorScheme.error
                    else -> Color.Gray
                }
                Text(subtitle, color = color, style = MaterialTheme.typography.bodyMedium)
            }
            when (row.status) {
                CityStatus.LOADING -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                CityStatus.DONE -> {
                    Box(Modifier.size(14.dp).background(aqiLevel(row.aqi ?: 0).second, CircleShape))
                    Text("  AQI ${row.aqi}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun ReportCard(title: String, average: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (average != null) {
                val (label, color) = aqiLevel(average)
                Text("Общая оценка: $label", color = color, fontWeight = FontWeight.SemiBold)
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}
