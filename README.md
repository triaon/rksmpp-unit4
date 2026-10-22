# РКСМП, Модуль 4 – Выполнение задач в фоновом режиме

Барнатович И.В., ИКБО-65-23. Каждая папка `pract_*` — отдельный проект.

Перед запуском:

- Android-проекты открывать в Android Studio: **File → Open → папка практики** (например, `pract_3`),
  дождаться синхронизации Gradle и нажать **Run** на эмуляторе (Pixel, Android 14+).
- Из терминала: в папке практики `./gradlew assembleDebug`, затем `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
- Нужен JDK 17+ (подходит встроенный в Android Studio):
  `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

## `pract_1_2` (задания 1 и 2)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_1_2

Консольный Kotlin/JVM-проект, нужен JDK 17+ (подойдёт JDK из Android Studio). Открыть папку в IntelliJ IDEA или запускать из терминала.

Задание 1 — кинотеатр, три параллельные загрузки. Аргумент — seed случайного сбоя (без аргумента сбой случайный):
```bash
./gradlew -q runTask1 --args="1"   # все данные загружены, ~2.5 с
./gradlew -q runTask1 --args="4"   # падает загрузка продаж билетов
```

Задание 2 — поиск дубликатов json в `test_dir`. Аргументы: папка и таймаут в мс:
```bash
./gradlew -q runTask2 --args="test_dir 5000"   # найдутся 3 группы дубликатов
./gradlew -q runTask2 --args="test_dir 300"    # «Поиск прерван по таймауту»
find test_dir | sort                           # что лежит в тестовой папке
```

## `pract_3` (задание 3, поиск книг)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_3

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

## `pract_4` (задание 4, лента рецептов)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_4

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

## `pract_5` (задание 5, секундомер тренировки)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_5

Открыть папку `pract_5` в Android Studio и нажать Run, или:
```bash
./gradlew installDebug
adb shell am start -n ru.mirea.barnatovich.workouttimer/.MainActivity
```
Что показать:
1. При первом запуске разрешить уведомления (Allow).
2. Нажать «Старт» — счётчик крупным шрифтом растёт каждую секунду.
3. Опустить шторку: уведомление «Тренировка идёт: X секунд», оно обновляется. Можно свернуть приложение — счёт продолжается.
4. Нажать «Стоп» — сервис останавливается, уведомление пропадает.

Код: `WorkoutService.kt` (foreground-сервис, тип specialUse), `MainActivity.kt`.

## `pract_6` (задание 6, таймер заварки чая)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_6

Открыть папку `pract_6` в Android Studio и нажать Run, или:
```bash
./gradlew installDebug
adb shell am start -n ru.mirea.barnatovich.teatimer/.MainActivity
```
Что показать:
1. Разрешить уведомления при первом запуске.
2. Ввести число секунд (например, `15`) и нажать «Запустить таймер» — на экране идёт обратный отсчёт.
3. Через 15 секунд приходит уведомление «Чай готов! / Время вышло, можно пить», сервис сам вызывает `stopSelf()`.
4. Проверить, что сервис остановлен: `adb shell dumpsys activity services ru.mirea.barnatovich.teatimer` — пусто.

Сервис обычный (не foreground), поэтому Android 8+ может остановить его, если приложение надолго уйдёт в фон. Для демонстрации держите приложение открытым.

## `pract_7` (задание 7, датчик влажности почвы)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_7

Открыть папку `pract_7` в Android Studio и нажать Run, или:
```bash
./gradlew installDebug
adb shell am start -n ru.mirea.barnatovich.soilsensor/.MainActivity
```
Что показать:
1. Сначала на экране «—» и «Нет подключения».
2. Нажать «Подключиться» — `bindService`, значение влажности меняется каждую секунду.
3. Нажать «Отключиться» — `unbindService`, сервис уничтожается (к нему больше никто не привязан), показания пропадают.

Код: `SoilSensorService.kt` (LocalBinder, корутина с delay(1000)), `MainActivity.kt` (ServiceConnection).

## `pract_8` — Задание 8 (WorkManager, последовательная цепочка)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_8

Открыть папку `pract_8` в Android Studio или собрать из консоли:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.mirea.barnatovich.scanchain/.MainActivity
```

Что показать: нажать «Начать обработку и отправку». Статус меняется «Переводим в ч/б…» → «Ставим штамп…» → «Отправляем…»,
у каждого шага свой прогресс 0–100%, превью скана становится чёрно-белым, затем на нём появляется синий штамп.
В конце — «Готово! Документ отправлен» и путь к файлу.

Проверка ошибки (необязательно): запретить запись в папку сканов и запустить ещё раз — первый шаг упадёт, остальные отменятся:

```bash
adb shell run-as ru.mirea.barnatovich.scanchain sh -c 'rm files/scans/*; chmod 555 files/scans'
# после проверки вернуть права:
adb shell run-as ru.mirea.barnatovich.scanchain chmod 700 files/scans
```

## `pract_9` — Задание 9 (WorkManager, параллельные Worker'ы + уведомление)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_9

Нужен интернет (данные AQI берутся с air-quality-api.open-meteo.com, ключ не нужен).

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.mirea.barnatovich.airquality/.MainActivity
```

Что показать: нажать «Собрать отчёт», разрешить уведомления. Карточки городов переходят из «загрузка…» в «AQI N»
в разном порядке (параллельно). Опустить шторку — там постоянное уведомление с прогрессом
(«Готово: …, … в процессе…», затем «формируем отчёт…»). В конце — карточка «Отчёт готов! Средний AQI N»
и такое же уведомление в шторке.

```bash
adb shell cmd statusbar expand-notifications   # открыть шторку
```

## `pract_10` — Задание 10 (геолокация + Geocoder)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_10

Нужен эмулятор с Google APIs (Play Services) и интернет (для Geocoder).

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.mirea.barnatovich.parking/.MainActivity
adb emu geo fix 37.4795 55.6700   # координаты: Москва, проспект Вернадского, 78
```

Что показать: нажать «Запомнить место парковки», разрешить доступ к геолокации («While using the app»).
Появится индикатор, затем карточка с адресом и координатами. Если индикатор висит долго — ещё раз выполнить `geo fix`.

Ошибки: отказ в разрешении — нажать «Don't allow»; без интернета — `adb shell svc wifi disable; adb shell svc data disable`;
без GPS — `adb shell cmd location set-location-enabled false` (потом вернуть `enable`/`true`).

## `pract_11` — Задание 11 (AlarmManager, напоминание в 20:00)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_11

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.mirea.barnatovich.flowers/.MainActivity
```

Что показать: нажать «Включить напоминание», разрешить уведомления, на экране «Alarms & reminders» включить переключатель
и вернуться назад — напоминание включится само («Следующее напоминание: сегодня/завтра в 20:00»).

Проверить срабатывание, не дожидаясь вечера (эмулятор Google APIs, нужен root adb):

```bash
adb root
adb shell settings put global auto_time 0
adb shell date 102219592026.45        # MMDDhhmmYYYY.ss — за 15 секунд до 20:00
# через ~15 секунд в шторке появится «Время полить цветы!»
adb shell settings put global auto_time 1   # вернуть время
```

Проверить восстановление после перезагрузки: `adb reboot`, затем
`adb shell dumpsys alarm | grep -A2 "barnatovich.flowers}"` — будильник на 20:00 снова стоит.

## `pract_12` — Задание 12 (cold Flow)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_12

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.mirea.barnatovich.spacefacts/.MainActivity
```

Что показать: нажать «Новый факт!» — 1,5–3 секунды крутится индикатор, затем карточка с фактом плавно появляется.
Повернуть экран (в эмуляторе кнопка поворота или команды ниже) — на экране остаётся тот же «Факт №N».

```bash
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1   # альбомная; 0 — вернуть портретную
```

## `pract_13` — Задание 13 (StateFlow)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_13

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.mirea.barnatovich.eurorate/.MainActivity
```

Что показать: каждые 5 секунд курс меняется сам, рядом зелёная стрелка вверх (рост) или красная вниз (падение).
«Обновить сейчас» сразу даёт новый курс. Поворот экрана не сбрасывает курс и время «Обновлено в …»:

```bash
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 1   # альбомная; 0 — вернуть портретную
```

## `pract_14` — Задание 14 (Компас)

Код: https://github.com/triaon/rksmpp-unit4/tree/main/pract_14

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n ru.mirea.barnatovich.compass/.MainActivity
```

На эмуляторе направление задаётся виртуальными сенсорами (телефон лежит экраном вверх):

```bash
adb emu sensor set acceleration 0:0:9.81
adb emu sensor set magnetic-field 0:30:-40           # азимут 0° (север)
adb emu sensor set magnetic-field -18.5:-23.6:-40    # азимут ~142°
adb emu sensor set magnetic-field 17.3:10:-40        # азимут ~300°
```

Что показать: после каждой команды стрелка плавно поворачивается, текст «Азимут: N°» меняется.
В Android Studio то же самое удобно делать в Extended Controls → Virtual sensors.
