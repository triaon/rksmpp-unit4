# Запуск (задание 3, поиск книг)

Открыть папку `pract_3` в Android Studio и нажать Run, или из терминала:
```bash
./gradlew installDebug
adb shell am start -n ru.mirea.barnatovich.booksearch/.MainActivity
```
Что показать:
1. Начать печатать в поле, например «фантастика», «Толстой», «Пушкин» (поиск по названию, автору и жанру без учёта регистра).
2. Пока печатаете, поиск не стартует (debounce 500 мс), счётчик «Отменено устаревших запросов» растёт.
3. После паузы появляется индикатор загрузки (~1 с), затем список книг в LazyColumn.
4. Если продолжить печатать во время поиска, старый поиск отменяется через `Job.cancel()`.

Код: `MainActivity.kt` (debounce, rememberCoroutineScope), `BookRepository.kt` (withContext, async, delay), данные — `app/src/main/assets/books.json`.
