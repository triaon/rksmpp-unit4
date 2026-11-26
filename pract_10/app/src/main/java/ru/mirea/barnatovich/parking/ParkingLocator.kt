package ru.mirea.barnatovich.parking

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// Получение координат (FusedLocationProviderClient) и адреса по ним (Geocoder)
class ParkingLocator(private val context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    fun isLocationEnabled(): Boolean {
        val manager = context.getSystemService(LocationManager::class.java)
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    fun hasInternet(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Разрешение проверяется во ViewModel/UI до вызова
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(precise: Boolean): Location? {
        val priority = if (precise) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val tokenSource = CancellationTokenSource()
        val current = withTimeoutOrNull(15_000) {
            fusedClient.getCurrentLocation(priority, tokenSource.token).await()
        }
        if (current == null) tokenSource.cancel()
        // Если свежих координат нет — берём последние известные
        return current ?: fusedClient.lastLocation.await()
    }

    suspend fun getAddress(lat: Double, lng: Double): String {
        if (!Geocoder.isPresent()) throw IOException("Геокодер недоступен на устройстве")
        val geocoder = Geocoder(context, Locale("ru"))

        val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(result: MutableList<Address>) = cont.resume(result)
                    override fun onError(errorMessage: String?) =
                        cont.resumeWithException(IOException(errorMessage ?: "Ошибка геокодирования"))
                })
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1).orEmpty()
            }
        }
        val address = addresses.firstOrNull() ?: throw IOException("Адрес для этих координат не найден")
        return format(address)
    }

    // «проспект Вернадского, 78, Москва, Россия»
    private fun format(a: Address): String {
        val street = listOfNotNull(a.thoroughfare, a.subThoroughfare).joinToString(", ")
        val parts = listOf(street, a.locality ?: a.subAdminArea ?: a.adminArea, a.countryName)
            .filter { !it.isNullOrBlank() }
        return if (parts.isNotEmpty()) parts.joinToString(", ") else a.getAddressLine(0) ?: "Адрес не определён"
    }
}
