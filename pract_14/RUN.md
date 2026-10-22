# Запуск: Задание 14 (Компас)

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
