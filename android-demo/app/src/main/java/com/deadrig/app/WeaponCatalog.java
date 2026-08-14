package com.deadrig.app;

/** Баланс и названия ручного арсенала оператора. */
public final class WeaponCatalog {
    public static final int COUNT = 10;
    public static final String[] NAMES = {
            "Пистолет", "Автомат", "Дробовик", "Рельсотрон", "Снайперская винтовка",
            "Гранатомёт", "Огнемёт", "Крио-пушка", "Тесла-винтовка", "Кислотомёт"
    };
    public static final double[] DAMAGE = {14, 9, 24, 65, 82, 48, 7, 11, 17, 13};
    public static final int[] MAGAZINE = {12, 30, 6, 3, 5, 4, 55, 18, 14, 20};
    public static final double[] INTERVAL = {.32, .11, .62, .90, 1.05, .82, .075, .24, .30, .20};
    public static final double[] RELOAD = {1.15, 1.25, 1.45, 1.80, 1.65, 1.75, 1.60, 1.35, 1.40, 1.35};
    public static final double[] HEAT = {.12, .045, .22, .46, .34, .30, .025, .07, .10, .065};
    public static final String[] ROLES = {
            "Усиленный точный выстрел", "Высокий темп огня", "Урон по группе", "Пробивание брони",
            "Максимальный урон в голову", "Взрыв большого радиуса", "Поджигает цель", "Замедляет и замораживает",
            "Цепной электрический разряд", "Снижает броню врагов"
    };
    public static final String[] RARITY = {"Обычное", "Редкое", "Эпическое", "Легендарное"};
    public static final double[] RARITY_MULT = {1.0, 1.18, 1.42, 1.75};

    private WeaponCatalog() { }

    public static int clamp(int type) { return Math.max(0, Math.min(COUNT - 1, type)); }
}
