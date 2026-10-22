package ru.mirea.barnatovich.soilsensor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var sensorService: SoilSensorService? = null
    private var collectJob: Job? = null

    private var connected by mutableStateOf(false)
    private var moisture by mutableStateOf<Int?>(null)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as SoilSensorService.LocalBinder).getService()
            sensorService = service
            connected = true
            collectJob = lifecycleScope.launch {
                service.moisture.collect { moisture = it }
            }
        }

        // Вызывается только если процесс сервиса неожиданно упал
        override fun onServiceDisconnected(name: ComponentName?) {
            collectJob?.cancel()
            sensorService = null
            connected = false
        }
    }

    private fun connect() {
        bindService(Intent(this, SoilSensorService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    private fun disconnect() {
        if (!connected) return
        collectJob?.cancel()
        unbindService(connection)
        sensorService = null
        connected = false
        moisture = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Датчик влажности почвы", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (connected) "Подключено к сервису" else "Нет подключения",
                            color = if (connected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(32.dp))
                        val value = moisture
                        Text(
                            if (value != null) "$value %" else "—",
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (value ?: 0) / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            when {
                                value == null -> "Показания не поступают"
                                value < 30 -> "Сухо — пора полить"
                                value > 80 -> "Почва переувлажнена"
                                else -> "Влажность в норме"
                            }
                        )
                        Spacer(Modifier.height(32.dp))
                        Row {
                            Button(onClick = { connect() }, enabled = !connected) { Text("Подключиться") }
                            Spacer(Modifier.width(16.dp))
                            OutlinedButton(onClick = { disconnect() }, enabled = connected) { Text("Отключиться") }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        disconnect()
        super.onStop()
    }
}
