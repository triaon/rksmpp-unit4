# Запуск: Задание 11 (AlarmManager, напоминание в 20:00)

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
