package ru.mirea.barnatovich.teatimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                TeaTimerScreen()
            }
        }
    }
}

@Composable
fun TeaTimerScreen() {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val remaining by TeaTimerService.remaining.collectAsState()
    var wasRunning by remember { mutableStateOf(false) }

    // Таймер закончился — сервис уже показал уведомление и остановился
    LaunchedEffect(remaining) {
        if (remaining == null && wasRunning) message = "Чай готов! Уведомление отправлено"
        wasRunning = remaining != null
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Таймер заварки чая", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Зелёный — 120 с, чёрный — 240 с, травяной — 300 с",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { text -> input = text.filter { it.isDigit() }.take(4) },
                label = { Text("Время заварки, секунд") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val seconds = input.toIntOrNull()
                    if (seconds == null || seconds <= 0) {
                        message = "Введите число секунд больше нуля"
                    } else {
                        val intent = Intent(context, TeaTimerService::class.java)
                            .putExtra(TeaTimerService.EXTRA_SECONDS, seconds)
                        context.startService(intent)
                        message = "Таймер запущен на $seconds с"
                    }
                },
                enabled = remaining == null,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Запустить таймер") }
            Spacer(Modifier.height(24.dp))
            remaining?.let {
                Text("$it", fontSize = 64.sp, fontWeight = FontWeight.Bold)
                Text("секунд до готовности", color = MaterialTheme.colorScheme.outline)
            }
            if (message.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(message)
            }
        }
    }
}
