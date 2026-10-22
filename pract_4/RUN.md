# Запуск (задание 4, лента рецептов)

Открыть папку `pract_4` в Android Studio и нажать Run, или:
```bash
./gradlew installDebug
adb shell am start -n ru.mirea.barnatovich.recipefeed/.MainActivity
```
Что показать:
1. Сразу после запуска карточки появляются в состоянии «Загрузка» и по одной становятся «Готово» или «Ошибка» (аватарка 0.3–2.5 с, отзывы 0.5–4 с, сбой ~15 % на каждую часть).
2. При ошибке вместо аватарки — иконка-предупреждение, вместо отзывов — «Отзывы не загрузились».
3. Нажать «Обновить» посреди загрузки: старые загрузки отменяются (`Job.cancel()`), всё грузится заново, счётчик обновлений растёт. Ошибки случайные, после обновления выпадут на других постах.

Код: `MainActivity.kt` (rememberCoroutineScope, async/awaitAll, Job.cancel), `FeedRepository.kt` (supervisorScope, try-catch внутри async, withContext(Dispatchers.IO)).
