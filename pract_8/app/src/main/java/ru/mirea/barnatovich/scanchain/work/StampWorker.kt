package ru.mirea.barnatovich.scanchain.work

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Шаг 2: ставим на скан штамп с датой и подписью
class StampWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val inputPath = inputData.getString(ScanKeys.KEY_FILE)
        if (inputPath == null || !File(inputPath).exists()) {
            return Result.failure(workDataOf(ScanKeys.KEY_ERROR to "Нет файла после шага «ч/б»"))
        }
        return try {
            reportProgress(0, 40)
            val bitmap = BitmapFactory.decodeFile(inputPath)
                .copy(android.graphics.Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(bitmap)

            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()
            val rect = RectF(w * 0.42f, h * 0.70f, w * 0.90f, h * 0.84f)
            val stampColor = Color.rgb(25, 70, 200)

            val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 6f
                color = stampColor
            }
            canvas.save()
            canvas.rotate(-8f, rect.centerX(), rect.centerY())
            canvas.drawRoundRect(rect, 20f, 20f, frame)

            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = stampColor
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
                textSize = rect.height() * 0.22f
            }
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
            canvas.drawText("ПРОВЕРЕНО", rect.centerX(), rect.top + rect.height() * 0.32f, text)
            text.isFakeBoldText = false
            text.textSize = rect.height() * 0.17f
            canvas.drawText(date, rect.centerX(), rect.top + rect.height() * 0.58f, text)
            canvas.drawText("Барнатович И.В.", rect.centerX(), rect.top + rect.height() * 0.84f, text)
            canvas.restore()

            val file = File(ScanKeys.scanDir(applicationContext), "скан_со_штампом.jpg")
            ScanKeys.saveJpeg(bitmap, file)
            reportProgress(50, 100)

            Result.success(workDataOf(ScanKeys.KEY_FILE to file.absolutePath))
        } catch (e: IOException) {
            Result.failure(workDataOf(ScanKeys.KEY_ERROR to "Ошибка при наложении штампа: не удалось сохранить файл"))
        } catch (e: Exception) {
            Result.failure(workDataOf(ScanKeys.KEY_ERROR to "Ошибка при наложении штампа: непредвиденная ошибка"))
        }
    }
}
