# Архитектура — Zombie Miner: Crypto Fortress

## 1. Стек

- **Движок:** Unity 6 LTS (6000.x), C#.
- **Цель:** Android (позже возможно iOS).
- **Рендер:** 2D (пиксель-арт или мульт — решить на этапе арта).
- **Сейвы:** локальный JSON (`Application.persistentDataPath`), версия внутри сейва для миграций.

## 2. Структура проекта

```
Assets/
├── Scenes/            # Main.unity (создаётся через меню ZombieMiner → Создать сцену)
├── Scripts/
│   ├── Core/          # Ядро: GameManager, EconomyManager, WaveManager, SaveManager, MetaProgression, Bootstrap
│   ├── UI/            # HUD, экраны (появится на фазе UI)
│   ├── Game/          # Геймплей: враги, турели, герои (появится на фазе 1)
│   └── Data/          # ScriptableObject-конфиги (появятся позже)
├── Editor/            # Редакторные утилиты (SetupScene)
└── Art/               # Спрайты, анимации (пока пусто)
```

## 3. Системы и ответственность

| Класс | Ответственность |
|-------|----------------|
| `GameManager` | Точка входа, связывает системы, тик, автосейв при паузе/выходе |
| `EconomyManager` | ВСЯ экономика: доход, траты, апгрейды, оффлайн-доход. Единственная точка для валюты (токен-ready) |
| `WaveManager` | Волны: номер, число врагов, HP, награды |
| `SaveManager` | JSON-сейв/лоад, версия, защита от повреждения |
| `MetaProgression` | Престиж и множители |
| `Bootstrap` | Автосоздание GameManager в сцене |

## 4. Планируемые интеграции (фаза 2)

- **Реклама:** Unity Ads / LevelPlay (ironSource) или AdMob — rewarded + interstitial.
- **IAP:** Unity IAP / Play Billing.
- **Аналитика:** Firebase Analytics + Remote Config (тюнинг баланса без обновлений).
- **CI:** GitHub Actions — сборка APK/AAB.

## 5. Токен-ready (Путь C, фаза 3)

- Интерфейс `IWalletAdapter` поверх `EconomyManager`: сейчас `LocalWalletAdapter` (обычный счётчик), позже `OnchainWalletAdapter` (кошелёк/смарт-контракт).
- Ончейн-слой живёт только в отдельном дистрибутиве (web/APK), **не** в версии Google Play.

## 6. Правила кода

- Комментарии и документация — на русском, идентификаторы — на английском.
- Каждая система — чистый C# без зависимости от сцены (логика отделена от Unity-объектов, где возможно).
- Баланс-числа выносить в конфиги (Remote Config на фазе 2).
