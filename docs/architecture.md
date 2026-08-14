# Архитектура — DeadRig

## 1. Стек

- **Движок:** Unity 6 LTS (6000.x), C#.
- **Цель:** Android (позже возможно iOS).
- **Рендер:** 3D, **low-poly** стиль (утверждено владельцем).
- **Сейвы:** локальный JSON (`Application.persistentDataPath`), версия внутри сейва (`SaveData.version`, сейчас 2) + `Sanitize()` для миграций.

## 2. Структура проекта

```
Assets/
├── Scenes/            # Main.unity (создаётся через меню DeadRig → Создать сцену)
├── Scripts/
│   ├── Core/          # Ядро: GameManager, EconomyManager, WaveManager, SaveManager,
│   │                  # MetaProgression, ResearchManager, CraftingManager,
│   │                  # ResearchCatalog, CraftingCatalog, Bootstrap
│   ├── UI/            # HUD, экраны (появится на фазе UI)
│   ├── Game/          # Геймплей: враги, турели, герои (появится на фазе 1)
│   └── Data/          # ScriptableObject-конфиги (появятся позже)
├── Editor/            # Редакторные утилиты (SetupScene)
└── Art/               # Спрайты, анимации (пока пусто)
```

## 3. Системы и ответственность

| Класс | Ответственность |
|-------|----------------|
| `GameManager` | Точка входа, связывает системы, тик, награды за волны, автосейв |
| `EconomyManager` | ВСЯ экономика: доход, траты, апгрейды, оффлайн-доход, лом. Единственная точка для валюты (токен-ready) |
| `WaveManager` | Волны: номер, число врагов, HP, событие очистки |
| `ResearchManager` | Исследования: слоты, старт/завершение проектов, применение эффектов |
| `ResearchCatalog` | Дерево технологий (статические данные) |
| `CraftingManager` | Крафт: рецепты, цеха, очередь, инвентарь |
| `CraftingCatalog` | Рецепты (статические данные) |
| `SaveManager` | JSON-сейв/лоад, версия, защита от повреждения, миграции |
| `MetaProgression` | Престиж и множители |
| `Bootstrap` | Автосоздание GameManager в сцене |

## 4. Поток времени (оффлайн-прогресс)

- Майнинг: при загрузке `GrantOfflineEarnings()` (50% скорости, кап 8 ч).
- Исследования и крафт: хранят `finishUtc`, поэтому **идут в реальном времени оффлайн** — `Tick()` после загрузки мгновенно завершает готовое.

## 5. Планируемые интеграции (фаза 2)

- **Реклама:** Unity Ads / LevelPlay (ironSource) или AdMob — rewarded + interstitial.
- **IAP:** Unity IAP / Play Billing.
- **Аналитика:** Firebase Analytics + Remote Config (тюнинг баланса без обновлений).
- **CI:** GitHub Actions — сборка APK/AAB.

## 6. Токен-ready (Путь C, фаза 3)

- Интерфейс `IWalletAdapter` поверх `EconomyManager`: сейчас `LocalWalletAdapter` (обычный счётчик), позже `OnchainWalletAdapter` (кошелёк/смарт-контракт).
- Ончейн-слой живёт только в отдельном дистрибутиве (web/APK), **не** в версии Google Play.

## 7. Правила кода

- Комментарии и документация — на русском, идентификаторы — на английском.
- Каждая система — чистый C# без зависимости от сцены (логика отделена от Unity-объектов, где возможно).
- Статические данные (исследования, рецепты) — в `*Catalog`-классах, позже можно вынести в ScriptableObject/Remote Config.
- Баланс-числа выносить в конфиги (Remote Config на фазе 2).
