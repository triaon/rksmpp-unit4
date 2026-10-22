package ru.mirea.barnatovich.parking

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParkingSpot(val address: String, val lat: Double, val lng: Double, val savedAt: String)

data class ParkingUiState(
    val isLoading: Boolean = false,
    val spot: ParkingSpot? = null,
    val error: String? = null,
    val showSettingsButton: Boolean = false
)

class ParkingViewModel(app: Application) : AndroidViewModel(app) {

    private val locator = ParkingLocator(app)
    private val prefs = app.getSharedPreferences("parking", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(ParkingUiState(spot = loadSpot()))
    val state: StateFlow<ParkingUiState> = _state.asStateFlow()

    fun onPermissionDenied(permanently: Boolean) {
        _state.value = _state.value.copy(
            isLoading = false,
            error = "Нет доступа к геолокации. Разрешите доступ, чтобы запомнить место парковки.",
            showSettingsButton = permanently
        )
    }

    fun rememberParking(precise: Boolean) {
        if (!locator.isLocationEnabled()) {
            _state.value = _state.value.copy(
                error = "Геолокация (GPS) выключена. Включите её в настройках устройства.",
                showSettingsButton = false
            )
            return
        }
        _state.value = _state.value.copy(isLoading = true, error = null, showSettingsButton = false)

        viewModelScope.launch {
            try {
                val location = locator.getCurrentLocation(precise)
                if (location == null) {
                    _state.value = _state.value.copy(isLoading = false, error = "Не удалось определить координаты. Попробуйте ещё раз на открытом месте.")
                    return@launch
                }
                // Для адреса нужен интернет: без него сохраняем только координаты
                val hasInternet = locator.hasInternet()
                val address = if (hasInternet) {
                    try {
                        locator.getAddress(location.latitude, location.longitude)
                    } catch (e: IOException) {
                        null
                    }
                } else null
                val time = SimpleDateFormat("dd.MM.yyyy в HH:mm", Locale("ru")).format(Date())
                val spot = ParkingSpot(
                    address = address ?: "Адрес не определён",
                    lat = location.latitude,
                    lng = location.longitude,
                    savedAt = time
                )
                saveSpot(spot)
                _state.value = ParkingUiState(
                    spot = spot,
                    error = when {
                        address != null -> null
                        !hasInternet -> "Нет подключения к интернету — адрес не определён, сохранены координаты."
                        else -> "Сервис геокодирования не ответил — сохранены только координаты."
                    }
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Ошибка геолокации: ${e.message}")
            }
        }
    }

    private fun saveSpot(spot: ParkingSpot) = prefs.edit {
        putString("address", spot.address)
        putString("lat", spot.lat.toString())
        putString("lng", spot.lng.toString())
        putString("time", spot.savedAt)
    }

    private fun loadSpot(): ParkingSpot? {
        val address = prefs.getString("address", null) ?: return null
        return ParkingSpot(
            address = address,
            lat = prefs.getString("lat", "0")!!.toDouble(),
            lng = prefs.getString("lng", "0")!!.toDouble(),
            savedAt = prefs.getString("time", "") ?: ""
        )
    }
}
