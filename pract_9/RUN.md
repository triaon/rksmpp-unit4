# Запуск: Задание 9 (WorkManager, параллельные Worker'ы + уведомление)

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
