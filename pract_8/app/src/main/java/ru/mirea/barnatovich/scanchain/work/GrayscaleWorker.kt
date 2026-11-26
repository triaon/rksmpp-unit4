package ru.mirea.barnatovich.scanchain.work

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ru.mirea.barnatovich.scanchain.R
import java.io.File
import java.io.IOException

// Шаг 1: переводим исходный скан (ресурс приложения) в оттенки серого
class GrayscaleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            reportProgress(0, 40)
            val source = BitmapFactory.decodeResource(applicationContext.resources, R.drawable.scan_source)
                ?: return Result.failure(workDataOf(ScanKeys.KEY_ERROR to "Не удалось открыть исходный скан"))

            val gray = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
            }
            Canvas(gray).drawBitmap(source, 0f, 0f, paint)

            val file = File(ScanKeys.scanDir(applicationContext), "скан_чб.jpg")
            ScanKeys.saveJpeg(gray, file)
            reportProgress(50, 100)

            Result.success(workDataOf(ScanKeys.KEY_FILE to file.absolutePath))
        } catch (e: IOException) {
            Result.failure(workDataOf(ScanKeys.KEY_ERROR to "Ошибка перевода в ч/б: не удалось сохранить файл, нет доступа к папке"))
        } catch (e: Exception) {
            Result.failure(workDataOf(ScanKeys.KEY_ERROR to "Ошибка перевода в ч/б: непредвиденная ошибка"))
        }
    }
}
