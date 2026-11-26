package ru.mirea.barnatovich.airquality

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ArrayCreatingInputMerger
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.mirea.barnatovich.airquality.work.AirKeys
import ru.mirea.barnatovich.airquality.work.AirNotifications
import ru.mirea.barnatovich.airquality.work.AirProgress
import ru.mirea.barnatovich.airquality.work.CityAqiWorker
import ru.mirea.barnatovich.airquality.work.ReportWorker

enum class CityStatus { WAITING, LOADING, DONE, ERROR }

data class CityRow(val name: String, val status: CityStatus, val aqi: Int? = null)

data class AirUiState(
    val cities: List<CityRow> = AirKeys.cities.map { CityRow(it.name, CityStatus.WAITING) },
    val isRunning: Boolean = false,
    val isBuildingReport: Boolean = false,
    val average: Int? = null,
    val error: String? = null
)

class AirViewModel(private val app: Application) : AndroidViewModel(app) {

    private val workManager = WorkManager.getInstance(app)

    val uiState: StateFlow<AirUiState> = workManager
        .getWorkInfosForUniqueWorkFlow(AirKeys.WORK_NAME)
        .map { toUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AirUiState())

    fun collectReport() {
        AirProgress.reset()
        AirNotifications.showStarted(app)

        // Города загружаются параллельно, отчёт — после всех
        val cityWorks = AirKeys.cities.map { city ->
            OneTimeWorkRequestBuilder<CityAqiWorker>()
                .setInputData(
                    workDataOf(AirKeys.KEY_CITY to city.name, AirKeys.KEY_LAT to city.lat, AirKeys.KEY_LON to city.lon)
                )
                .addTag(AirKeys.TAG_CITY)
                .addTag(AirKeys.cityTag(city.name))
                .build()
        }
        val report = OneTimeWorkRequestBuilder<ReportWorker>()
            .setInputMerger(ArrayCreatingInputMerger::class.java)
            .addTag(AirKeys.TAG_REPORT)
            .build()

        workManager
            .beginUniqueWork(AirKeys.WORK_NAME, ExistingWorkPolicy.REPLACE, cityWorks)
            .then(report)
            .enqueue()
    }

    private fun toUiState(infos: List<WorkInfo>): AirUiState {
        if (infos.isEmpty()) return AirUiState()

        val rows = AirKeys.cities.map { city ->
            val info = infos.find { AirKeys.cityTag(city.name) in it.tags }
            when (info?.state) {
                WorkInfo.State.RUNNING -> CityRow(city.name, CityStatus.LOADING)
                WorkInfo.State.SUCCEEDED -> CityRow(city.name, CityStatus.DONE, info.outputData.getInt(AirKeys.KEY_AQI, 0))
                WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> CityRow(city.name, CityStatus.ERROR)
                else -> CityRow(city.name, CityStatus.WAITING)
            }
        }
        val report = infos.find { AirKeys.TAG_REPORT in it.tags }
        val error = infos.firstNotNullOfOrNull { it.outputData.getString(AirKeys.KEY_ERROR) }
        val finished = infos.all { it.state.isFinished }

        return AirUiState(
            cities = rows,
            isRunning = !finished,
            isBuildingReport = report?.state == WorkInfo.State.RUNNING,
            average = report?.takeIf { it.state == WorkInfo.State.SUCCEEDED }?.outputData?.getInt(AirKeys.KEY_AVG, 0),
            error = error
        )
    }
}
