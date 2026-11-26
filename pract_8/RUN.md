# Запуск: Задание 8 (WorkManager, последовательная цепочка)

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
