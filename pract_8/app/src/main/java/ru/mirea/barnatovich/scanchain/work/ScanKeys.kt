package ru.mirea.barnatovich.scanchain.work

import android.content.Context
import android.graphics.Bitmap
import androidx.work.CoroutineWorker
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import java.io.File

// Ключи Data и теги, общие для всех шагов цепочки
object ScanKeys {
    const val CHAIN_NAME = "scan_chain"

    const val TAG_GRAY = "step_gray"
    const val TAG_STAMP = "step_stamp"
    const val TAG_UPLOAD = "step_upload"

    const val KEY_PROGRESS = "progress"
    const val KEY_FILE = "file_path"
    const val KEY_CLOUD_URL = "cloud_url"
    const val KEY_ERROR = "error"

    fun scanDir(context: Context): File = File(context.filesDir, "scans").apply { mkdirs() }

    fun saveJpeg(bitmap: Bitmap, file: File) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    }
}

// Имитация прогресса 0..100% для шага: обновляем setProgress и немного ждём
suspend fun CoroutineWorker.reportProgress(from: Int, to: Int, stepDelay: Long = 300) {
    for (p in from..to step 10) {
        setProgress(workDataOf(ScanKeys.KEY_PROGRESS to p))
        delay(stepDelay)
    }
}
