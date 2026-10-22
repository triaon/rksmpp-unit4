package ru.mirea.barnatovich.scanchain.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Шаг 3: «отправляем» документ в облако (имитация загрузки)
class UploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val inputPath = inputData.getString(ScanKeys.KEY_FILE)
        val file = inputPath?.let { File(it) }
        if (file == null || !file.exists()) {
            return Result.failure(workDataOf(ScanKeys.KEY_ERROR to "Нет файла со штампом для отправки"))
        }
        // загрузка «идёт» дольше остальных шагов
        reportProgress(0, 100, stepDelay = 450)

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        // Путь документа в «облаке»
        val cloudUrl = "Документы/скан_$stamp.jpg"
        return Result.success(
            workDataOf(
                ScanKeys.KEY_FILE to file.absolutePath,
                ScanKeys.KEY_CLOUD_URL to cloudUrl
            )
        )
    }
}
