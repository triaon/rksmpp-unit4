package ru.mirea.barnatovich.spacefacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlin.random.Random

data class FactsUiState(
    val fact: String? = null,
    val factNumber: Int = 0,
    val isLoading: Boolean = false
)

class FactsViewModel : ViewModel() {

    private val facts = listOf(
        "Один день на Венере длится дольше, чем венерианский год: оборот вокруг оси занимает 243 земных дня, а вокруг Солнца — 225.",
        "На Луне до сих пор видны следы астронавтов «Аполлона»: ветра там нет, поэтому отпечатки могут сохраниться миллионы лет.",
        "Солнце составляет около 99,86% всей массы Солнечной системы.",
        "Свету от Солнца нужно примерно 8 минут 20 секунд, чтобы долететь до Земли.",
        "На Марсе находится самая высокая гора Солнечной системы — вулкан Олимп высотой около 22 км.",
        "Большое Красное Пятно на Юпитере — это шторм, который больше Земли и бушует уже как минимум 350 лет.",
        "Сатурн настолько лёгкий по плотности, что в огромном океане он бы не утонул.",
        "Нейтронная звезда так плотна, что чайная ложка её вещества весила бы около миллиарда тонн.",
        "МКС пролетает вокруг Земли примерно за 90 минут — экипаж видит 16 восходов в сутки.",
        "Первым человеком в космосе стал Юрий Гагарин: 12 апреля 1961 года он облетел Землю за 108 минут.",
        "В космосе металлы могут «свариваться» сами при касании — это называется холодной сваркой.",
        "Галактика Андромеды приближается к Млечному Пути и примерно через 4,5 млрд лет столкнётся с ним.",
        "На Уране времена года длятся около 21 земного года, потому что планета «лежит на боку».",
        "Звёзд во Вселенной больше, чем песчинок на всех пляжах Земли.",
        "Самая холодная известная область Вселенной — туманность Бумеранг: около −272 °C.",
        "Аппарат «Вояджер-1» — самый далёкий рукотворный объект: он уже за пределами гелиосферы."
    )

    private val _state = MutableStateFlow(FactsUiState())
    val state: StateFlow<FactsUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    // Холодный Flow: код внутри flow { } выполняется заново при каждом collect
    fun getRandomFact(): Flow<String> = flow {
        delay(Random.nextLong(1500, 3001))
        var fact = facts.random()
        // чтобы подряд не выпадал один и тот же факт
        while (fact == _state.value.fact) fact = facts.random()
        emit(fact)
    }

    // Факт хранится во ViewModel, поэтому при повороте экрана он не генерируется заново
    fun onNewFactClick() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getRandomFact()
                .onStart { _state.value = _state.value.copy(isLoading = true) }
                .onCompletion { _state.value = _state.value.copy(isLoading = false) }
                .collect { fact ->
                    _state.value = FactsUiState(fact = fact, factNumber = _state.value.factNumber + 1)
                }
        }
    }
}
