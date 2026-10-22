# Запуск: Задание 10 (геолокация + Geocoder)

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
