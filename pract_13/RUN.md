# Запуск: Задание 13 (StateFlow)

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
