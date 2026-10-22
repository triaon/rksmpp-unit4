package ru.mirea.barnatovich.flowers

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val vm: ReminderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderReceiver.createChannel(this)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ReminderScreen(vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
    }
}

private val Green = Color(0xFF2E7D32)

@Composable
fun ReminderScreen(vm: ReminderViewModel) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    // Сначала спрашиваем POST_NOTIFICATIONS (Android 13+), потом ставим будильник
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.enable()
    }

    // На Android 12+ точные будильники нужно разрешить в отдельном экране настроек
    LaunchedEffect(state.needExactAlarmPermission) {
        if (state.needExactAlarmPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            vm.permissionHintShown()
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            )
        }
    }

    fun onEnableClick() {
        val needAsk = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
        if (needAsk) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.enable()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.LocalFlorist,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = if (state.enabled) Green else Color.Gray
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Напоминание полить цветы",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "%02d:%02d".format(ReminderScheduler.HOUR, ReminderScheduler.MINUTE),
            fontSize = 64.sp,
            fontWeight = FontWeight.Light
        )
        Text("каждый день", style = MaterialTheme.typography.bodyLarge)

        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(20.dp)
                    .background(if (state.enabled) Green else Color.Gray, CircleShape)
            )
            Text(
                if (state.enabled) "  Включено" else "  Выключено",
                style = MaterialTheme.typography.titleLarge,
                color = if (state.enabled) Green else Color.Gray
            )
        }
        if (state.enabled) {
            Spacer(Modifier.height(8.dp))
            Text(state.nextText, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { if (state.enabled) vm.disable() else onEnableClick() },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = if (state.enabled) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors(containerColor = Green)
            }
        ) {
            Text(
                if (state.enabled) "Выключить напоминание" else "Включить напоминание",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
