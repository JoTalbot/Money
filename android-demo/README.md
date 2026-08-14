# DeadRig — Android-демо (нативный порт)

Играбельный порт прототипа DeadRig на нативный Android (Java, без движка).
Собирается в APK без Unity — для быстрого теста геймплея.

## Готовые APK

- `releases/DeadRig-v0.1.0-debug.apk` — debug-сборка (подписана debug-ключом).
- `releases/DeadRig-v0.1.0-release.apk` — release-сборка (подписана keystore, minSdk 21 / targetSdk 33).

Установка: разрешить установку из неизвестных источников → открыть APK.

## Как собрать

```bash
export JAVA_HOME=/usr/lib/jvm/jdk-11
export ANDROID_HOME=/path/to/android-sdk   # platforms;android-33, build-tools;33.0.2
export GRADLE_USER_HOME=/tmp/gradle-home   # чтобы не захламлять проект
gradle assembleDebug     # или assembleRelease
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Что внутри (порт Unity-ядра 1:1)

- Майнинг «хешей» + «лом» с зомби + «кристаллы».
- Волны зомби (авто-старт, награда за зачистку).
- Турели: автоприцел, урон × апгрейд × бонусы исследований.
- Исследования (4 технологии, время в реальном времени).
- Крафт (3 рецепта: турель базовая, турель лазерная, стена).
- Сейв в SharedPreferences + оффлайн-доход (50%, кап 8ч).
- Экран поражения + рестарт.

## Отличия от Unity-версии

- Престиж не включён в демо (есть в Unity-ядре).
- Арт — примитивы (круги/квадраты), в Unity — low-poly 3D.
