package ru.mirea.barnatovich.compass

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Surface as ViewSurface
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val Background = Color(0xFF0D0F14)
private val Disk = Color(0xFF1B1E26)
private val DiskBorder = Color(0xFF3A3F4B)
private val NorthRed = Color(0xFFE53935)
private val SouthDark = Color(0xFF07080B)
private val TextLight = Color(0xFFE6E8EE)

class MainActivity : ComponentActivity(), SensorEventListener {

    private val vm: CompassViewModel by viewModels()
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SensorManager::class.java)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (accelerometer == null || magnetometer == null) vm.onSensorMissing()

        // Тёмный фон — светлые значки в статус-баре
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Background, surface = Background)) {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    CompassScreen(vm)
                }
            }
        }
    }

    // Слушаем датчики только пока экран виден — экономим батарею
    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Простой фильтр низких частот, чтобы стрелка не дрожала
        val alpha = 0.2f
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lowPass(event.values, gravity, if (hasGravity) alpha else 1f)
                hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lowPass(event.values, geomagnetic, if (hasGeomagnetic) alpha else 1f)
                hasGeomagnetic = true
            }
        }
        if (!hasGravity || !hasGeomagnetic) return

        val r = FloatArray(9)
        if (!SensorManager.getRotationMatrix(r, null, gravity, geomagnetic)) return

        // Учитываем ориентацию экрана, чтобы азимут был верным и в альбомном режиме
        val remapped = FloatArray(9)
        when (display?.rotation ?: ViewSurface.ROTATION_0) {
            ViewSurface.ROTATION_90 -> SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, remapped)
            ViewSurface.ROTATION_180 -> SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, remapped)
            ViewSurface.ROTATION_270 -> SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, remapped)
            else -> r.copyInto(remapped)
        }

        val orientation = FloatArray(3)
        SensorManager.getOrientation(remapped, orientation)
        val degrees = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
        vm.onAzimuth(degrees)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun lowPass(input: FloatArray, output: FloatArray, alpha: Float) {
        for (i in input.indices.take(3)) output[i] = output[i] + alpha * (input[i] - output[i])
    }
}

fun directionName(azimuth: Int): String {
    val names = listOf("север", "северо-восток", "восток", "юго-восток", "юг", "юго-запад", "запад", "северо-запад")
    return names[((azimuth + 22) % 360) / 45]
}

@Composable
fun CompassScreen(vm: CompassViewModel) {
    val azimuth by vm.azimuth.collectAsState()
    val rotation by vm.rotation.collectAsState()
    val sensorMissing by vm.sensorMissing.collectAsState()

    // Плавный поворот без резких скачков
    val animatedRotation by animateFloatAsState(
        targetValue = -rotation,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessVeryLow),
        label = "needle"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        val landscape = maxWidth > maxHeight
        // Диск ~75% ширины экрана; в альбомной ориентации ограничиваемся высотой
        val diameter = if (landscape) maxHeight * 0.6f else min(maxWidth * 0.75f, maxHeight * 0.55f)

        val title = @Composable {
            Text("Компас", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TextLight)
        }
        val azimuthText = @Composable {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Азимут: $azimuth°", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = TextLight)
                Text(directionName(azimuth), fontSize = 20.sp, color = Color(0xFF9AA0AE))
            }
        }

        when {
            sensorMissing -> Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                title()
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Устройство не поддерживает датчик ориентации",
                        color = NorthRed,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            landscape -> Row(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompassDial(diameter, animatedRotation)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    title()
                    azimuthText()
                }
            }
            else -> Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                title()
                CompassDial(diameter, animatedRotation)
                azimuthText()
            }
        }
    }
}

@Composable
private fun CompassDial(diameter: Dp, rotation: Float) {
    val letterSpace = 52.dp
    Box(
        modifier = Modifier.requiredSize(diameter + letterSpace * 2).rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        // Буква «N» над северным концом стрелки, поворачивается вместе с ней
        Text(
            "N",
            modifier = Modifier.align(Alignment.TopCenter),
            color = NorthRed,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Canvas(modifier = Modifier.size(diameter)) {
            val radius = size.minDimension / 2
            val c = center

            drawCircle(Disk, radius, c)
            drawCircle(DiskBorder, radius, c, style = Stroke(width = 3.dp.toPx()))

            // Деления через 10°, длинные — через 30°
            for (deg in 0 until 360 step 10) {
                val long = deg % 30 == 0
                val angle = Math.toRadians(deg.toDouble() - 90)
                val outer = radius - 6.dp.toPx()
                val inner = outer - if (long) 14.dp.toPx() else 7.dp.toPx()
                drawLine(
                    color = if (deg == 0) NorthRed else Color(0xFF6B7180),
                    start = Offset(c.x + inner * cos(angle).toFloat(), c.y + inner * sin(angle).toFloat()),
                    end = Offset(c.x + outer * cos(angle).toFloat(), c.y + outer * sin(angle).toFloat()),
                    strokeWidth = if (long) 3.dp.toPx() else 1.5.dp.toPx()
                )
            }

            // Стрелка ~80% диаметра: красная половина на север, тёмная на юг
            val half = radius * 0.8f
            val width = radius * 0.12f
            val north = Path().apply {
                moveTo(c.x, c.y - half)
                lineTo(c.x + width, c.y)
                lineTo(c.x - width, c.y)
                close()
            }
            val south = Path().apply {
                moveTo(c.x, c.y + half)
                lineTo(c.x + width, c.y)
                lineTo(c.x - width, c.y)
                close()
            }
            drawPath(north, NorthRed)
            drawPath(south, SouthDark)
            drawPath(south, Color(0xFF8A90A0), style = Stroke(width = 1.5.dp.toPx()))
            drawCircle(Color(0xFFCFD3DC), width * 0.6f, c)
        }
    }
}
