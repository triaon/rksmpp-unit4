# Запуск (задание 7, датчик влажности почвы)

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
