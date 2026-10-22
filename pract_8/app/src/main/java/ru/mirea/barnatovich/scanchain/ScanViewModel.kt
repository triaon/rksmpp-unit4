package ru.mirea.barnatovich.scanchain

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.mirea.barnatovich.scanchain.work.GrayscaleWorker
import ru.mirea.barnatovich.scanchain.work.ScanKeys
import ru.mirea.barnatovich.scanchain.work.StampWorker
import ru.mirea.barnatovich.scanchain.work.UploadWorker
import java.io.File

data class ScanUiState(
    val status: String = "Документ готов к обработке",
    val stepLabel: String? = null,
    val progress: Float? = null,
    val isRunning: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val previewPath: String? = null
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val workManager = WorkManager.getInstance(app)

    // Шаги цепочки по порядку: тег -> (номер шага, текст статуса)
    private val steps = listOf(
        ScanKeys.TAG_GRAY to "Переводим в ч/б…",
        ScanKeys.TAG_STAMP to "Ставим штамп…",
        ScanKeys.TAG_UPLOAD to "Отправляем…"
    )

    val uiState: StateFlow<ScanUiState> = workManager
        .getWorkInfosForUniqueWorkFlow(ScanKeys.CHAIN_NAME)
        .map { infos -> toUiState(infos) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanUiState())

    fun start() {
        val gray = OneTimeWorkRequestBuilder<GrayscaleWorker>().addTag(ScanKeys.TAG_GRAY).build()
        val stamp = OneTimeWorkRequestBuilder<StampWorker>().addTag(ScanKeys.TAG_STAMP).build()
        val upload = OneTimeWorkRequestBuilder<UploadWorker>().addTag(ScanKeys.TAG_UPLOAD).build()

        // Последовательная цепочка: выход каждого шага становится входом следующего
        workManager
            .beginUniqueWork(ScanKeys.CHAIN_NAME, ExistingWorkPolicy.REPLACE, gray)
            .then(stamp)
            .then(upload)
            .enqueue()
    }

    private fun toUiState(infos: List<WorkInfo>): ScanUiState {
        if (infos.isEmpty()) return ScanUiState()

        // Файл последнего успешно выполненного шага — для превью
        val lastFile = steps.mapNotNull { (tag, _) ->
            infos.find { tag in it.tags && it.state == WorkInfo.State.SUCCEEDED }
                ?.outputData?.getString(ScanKeys.KEY_FILE)
        }.lastOrNull()

        // Если шаг упал, WorkManager помечает все следующие шаги как FAILED/CANCELLED —
        // цепочка дальше не идёт
        val failed = infos.find { it.state == WorkInfo.State.FAILED && it.outputData.getString(ScanKeys.KEY_ERROR) != null }
        if (failed != null || infos.any { it.state == WorkInfo.State.CANCELLED }) {
            val error = failed?.outputData?.getString(ScanKeys.KEY_ERROR) ?: "Цепочка отменена"
            return ScanUiState(status = "Ошибка", error = "$error. Остальные шаги отменены.", previewPath = lastFile)
        }

        val upload = infos.find { ScanKeys.TAG_UPLOAD in it.tags }
        if (infos.all { it.state == WorkInfo.State.SUCCEEDED } && upload != null) {
            val path = upload.outputData.getString(ScanKeys.KEY_FILE)
            val url = upload.outputData.getString(ScanKeys.KEY_CLOUD_URL)
            return ScanUiState(
                status = "Готово! Документ отправлен",
                result = "Файл: ${path?.let { File(it).name }}\nВ облаке: $url",
                previewPath = path
            )
        }

        // Текущий шаг — первый ещё не завершённый
        val index = steps.indexOfFirst { (tag, _) ->
            infos.find { tag in it.tags }?.state != WorkInfo.State.SUCCEEDED
        }.coerceAtLeast(0)
        val (tag, text) = steps[index]
        val info = infos.find { tag in it.tags }
        val p = if (info?.state == WorkInfo.State.RUNNING) info.progress.getInt(ScanKeys.KEY_PROGRESS, 0) else 0
        return ScanUiState(
            status = text,
            stepLabel = "Шаг ${index + 1} из ${steps.size} · $p%",
            progress = p / 100f,
            isRunning = true,
            previewPath = lastFile
        )
    }
}
