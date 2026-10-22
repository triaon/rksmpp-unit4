# Запуск: Задание 12 (cold Flow)

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
