package com.deadrig.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Полноценные нативные экраны управления вместо непрозрачных кнопок-автоматизаций. */
public final class ControlPanel {
    private static final int BG = Color.rgb(6, 18, 22);
    private static final int CARD = Color.rgb(14, 38, 42);
    private static final int CYAN = Color.rgb(64, 231, 225);
    private static final int ORANGE = Color.rgb(234, 132, 40);
    private static final int TEXT = Color.rgb(226, 239, 238);
    private static final int MUTED = Color.rgb(145, 174, 174);

    public interface OnChanged { void run(); }

    private ControlPanel() { }

    public static void showTutorial(Activity activity, OnChanged completed) {
        Dialog dialog = create(activity, "ДОБРО ПОЖАЛОВАТЬ В DEADRIG", "Краткий инструктаж оператора");
        LinearLayout list = content(dialog);
        addCard(activity, list, "1. Стреляйте сами", "Нажмите на зомби для усиленного одиночного выстрела. Удерживайте палец на враге для автоматического огня.", null, false, null);
        addCard(activity, list, "2. Защищайте ядро", "Зомби идут по четырём дорогам. Башни стреляют автоматически, но маршрут и состав обороны выбираете вы.", null, false, null);
        addCard(activity, list, "3. Развивайте экономику", "Ферма добывает хеши. За очищенные волны выдаётся лом — главный материал науки и производства.", null, false, null);
        addCard(activity, list, "4. Исследуйте и создавайте", "Откройте экран Науки, выберите технологию, затем создайте конкретную башню в Цехе.", null, false, null);
        addCard(activity, list, "5. Устанавливайте башни", "Готовая башня попадает в резерв. Закройте меню и нажмите на светящуюся площадку рядом с дорогой.", null, false, null);
        addCard(activity, list, "6. Управляйте каждой башней", "Нажмите на установленную башню: её можно улучшить, переместить или продать.", "НАЧАТЬ ЗАЩИТУ", true, v -> {
            feedback(); completed.run(); dialog.dismiss();
        });
        dialog.setCancelable(false);
        dialog.show(); fit(dialog);
    }

    public static void showFarm(Activity activity, GameState state, OnChanged changed) {
        Dialog dialog = create(activity, "МАЙНИНГОВАЯ ФЕРМА", "Пассивный доход узла");
        LinearLayout list = content(dialog);
        addMetric(activity, list, "УРОВЕНЬ", String.valueOf(state.minerLevel));
        addMetric(activity, list, "ДОХОД", String.format(Locale.US, "%.1f хеш/с", state.miningRate()));
        addCard(activity, list, "Расширить ферму",
                "Новый ASIC-модуль увеличивает постоянную добычу.\nСтоимость: " + GameState.fmt(state.minerUpgradeCost()) + " хешей",
                "УЛУЧШИТЬ", true, v -> result(activity, state.tryUpgradeMiner(), changed, dialog));
        addCard(activity, list, "Ежедневный контракт", state.dailyStatus() + "\nНаграда: 5 кристаллов и 75 лома",
                state.dailyReady() ? "ЗАБРАТЬ НАГРАДУ" : "В ПРОЦЕССЕ", state.dailyReady(),
                v -> result(activity, state.claimDailyReward(), changed, dialog));
        addCard(activity, list, "Переезд на новый узел  //  престиж " + state.prestigeLevel(),
                "Сбрасывает текущую экономику и башни, сохраняя исследования и открытое оружие. Каждый престиж даёт +50% дохода навсегда.\nТребуется: "
                        + GameState.fmt(state.prestigeRequirement()) + " хешей",
                "ЗАПУСТИТЬ ПРЕСТИЖ", true, v -> result(activity, state.tryPrestige(), changed, dialog));
        addHint(activity, list, "Доход продолжает начисляться вне игры: 50%, максимум 8 часов.");
        dialog.show(); fit(dialog);
    }

    public static void showDefense(Activity activity, GameState state, OnChanged changed) {
        Dialog dialog = create(activity, "КОНТУР ОБОРОНЫ", "Башни и боевые модификаторы");
        LinearLayout list = content(dialog);
        addMetric(activity, list, "ОБЩИЙ УРОВЕНЬ", String.valueOf(state.turretLevel));
        addMetric(activity, list, "УСТАНОВЛЕНО / РЕЗЕРВ", state.turretCount() + " / " + state.pendingTowerCount());
        addMetric(activity, list, "ДОРОЖНЫЕ МИНЫ", String.valueOf(state.mineCharges()));
        addMetric(activity, list, "БОНУСЫ", "+" + percent(state.turretDamageBonus()) + " урон  •  +"
                + percent(state.turretFireRateBonus()) + " темп  •  +" + String.format(Locale.US, "%.1f", state.turretRangeBonus()) + " дальность");
        addMetric(activity, list, "РУЧНОЕ ОРУЖИЕ", state.manualWeaponName() + "  ур." + state.manualWeaponLevel()
                + "  •  " + GameState.fmt(state.manualWeaponDamage()) + " урона");
        addMetric(activity, list, "МАГАЗИН / НАГРЕВ", state.manualAmmo() + "/" + state.manualMagazineSize()
                + "  •  " + Math.round(state.manualHeat() * 100) + "%");
        addCard(activity, list, "Перезарядить оружие", "Ручная перезарядка до начала следующей атаки.",
                "ПЕРЕЗАРЯДИТЬ", true, v -> result(activity, state.startManualReload(), changed, dialog));
        addCard(activity, list, "Улучшить ручное оружие",
                "Повышает урон выстрелов игрока на 35%.\nСтоимость: " + GameState.fmt(state.manualUpgradeCost()) + " хешей",
                "УЛУЧШИТЬ ОРУЖИЕ", true, v -> result(activity, state.tryUpgradeManualWeapon(), changed, dialog));
        for (int type = 0; type < WeaponCatalog.COUNT; type++) {
            if (!state.ownsWeapon(type)) continue;
            final int weaponType = type;
            String weaponInfo = state.weaponRarityName(type) + "  •  " + WeaponCatalog.ROLES[type]
                    + (type == state.manualWeaponType() ? "\nСейчас экипировано" : "");
            addCard(activity, list, WeaponCatalog.NAMES[type], weaponInfo,
                    type == state.manualWeaponType() ? null : "ЭКИПИРОВАТЬ", true,
                    v -> result(activity, state.equipManualWeapon(weaponType), changed, dialog));
        }
        addCard(activity, list, "Модуль ствола  ур." + state.damageModuleLevel(), "+12% урона за уровень", "УЛУЧШИТЬ", true,
                v -> result(activity, state.upgradeWeaponModule(0), changed, dialog));
        addCard(activity, list, "Модуль охлаждения  ур." + state.coolingModuleLevel(), "Быстрее снимает перегрев", "УЛУЧШИТЬ", true,
                v -> result(activity, state.upgradeWeaponModule(1), changed, dialog));
        addCard(activity, list, "Модуль магазина  ур." + state.magazineModuleLevel(), "+15% ёмкости за уровень", "УЛУЧШИТЬ", true,
                v -> result(activity, state.upgradeWeaponModule(2), changed, dialog));
        if (state.specialAmmoUnlocked()) {
            String[] ammo = {"Обычные патроны", "Бронебойные", "Зажигательные", "Электрические"};
            for (int i = 0; i < ammo.length; i++) {
                final int ammoType = i;
                addCard(activity, list, ammo[i], i == state.manualAmmoType() ? "Сейчас заряжены" : "Сменить тип боеприпасов",
                        i == state.manualAmmoType() ? null : "ВЫБРАТЬ", true,
                        v -> result(activity, state.selectAmmoType(ammoType), changed, dialog));
            }
        }
        if (state.combatDroneUnlocked()) addMetric(activity, list, "БОЕВОЙ ДРОН", "АКТИВЕН");
        addCard(activity, list, "Усилить все башни",
                "Повышает базовый урон всего оборонного контура.\nСтоимость: " + GameState.fmt(state.turretUpgradeCost()) + " хешей",
                "УСИЛИТЬ", true, v -> result(activity, state.tryUpgradeTurret(), changed, dialog));

        String[] names = {"", "Пулемётная", "Лазерная", "Тесла", "Крио", "Ракетная", "Поддержка"};
        for (int i = 0; i < GameState.TOWER_SLOTS.length; i++) {
            int type = state.towerTypeAt(i);
            if (type != 0) addCard(activity, list, "Площадка " + (i + 1) + "  //  " + names[type],
                    towerDescription(type),
                    null, false, null);
        }
        addHint(activity, list, "Чтобы установить башню из резерва, закройте экран и нажмите на светящуюся свободную площадку у дороги.");
        dialog.show(); fit(dialog);
    }

    public static void showTower(Activity activity, GameState state, int slot, OnChanged changed) {
        int type = state.towerTypeAt(slot);
        if (type == 0) return;
        String[] names = {"", "ПУЛЕМЁТНАЯ БАШНЯ", "ЛАЗЕРНАЯ БАШНЯ", "ТЕСЛА-БАШНЯ", "КРИО-БАШНЯ", "РАКЕТНАЯ БАШНЯ", "БАШНЯ ПОДДЕРЖКИ"};
        Dialog dialog = create(activity, names[type], "Монтажная площадка " + (slot + 1));
        LinearLayout list = content(dialog);
        addMetric(activity, list, "ЛОКАЛЬНЫЙ УРОВЕНЬ", String.valueOf(state.towerLevelAt(slot)));
        addMetric(activity, list, "ГЛОБАЛЬНЫЙ УРОВЕНЬ", String.valueOf(state.turretLevel));
        addMetric(activity, list, "РАДИУС", String.format(Locale.US, "%.1f", state.towerRangeAt(slot)));
        addMetric(activity, list, "СПЕЦИАЛИЗАЦИЯ", state.towerBranchName(slot));
        addCard(activity, list, "Приоритет цели", state.towerPriorityName(slot), "СМЕНИТЬ ПРИОРИТЕТ", true,
                v -> result(activity, state.cycleTowerPriority(slot), changed, dialog));
        if (state.towerBranchAt(slot) == 0) {
            addCard(activity, list, "Ветка: урон", "+45% урона", "ВЫБРАТЬ", true,
                    v -> result(activity, state.chooseTowerBranch(slot, 1), changed, dialog));
            addCard(activity, list, "Ветка: дальность", "+1.5 к радиусу", "ВЫБРАТЬ", true,
                    v -> result(activity, state.chooseTowerBranch(slot, 2), changed, dialog));
            addCard(activity, list, "Ветка: темп", "+54% к темпу огня", "ВЫБРАТЬ", true,
                    v -> result(activity, state.chooseTowerBranch(slot, 3), changed, dialog));
        }
        if (state.towerLevelAt(slot) >= 5 && state.towerEvolutionAt(slot) == 0 && type <= 4) {
            addCard(activity, list, evolutionName(type, 1), evolutionDescription(type, 1), "ЭВОЛЮЦИЯ A", true,
                    v -> result(activity, state.evolveTower(slot, 1), changed, dialog));
            addCard(activity, list, evolutionName(type, 2), evolutionDescription(type, 2), "ЭВОЛЮЦИЯ B", true,
                    v -> result(activity, state.evolveTower(slot, 2), changed, dialog));
        }
        addCard(activity, list, "Локальное усиление",
                "Увеличивает урон только этой башни.\nСтоимость: " + GameState.fmt(state.towerUpgradeCost(slot)) + " хешей",
                "УЛУЧШИТЬ", true, v -> result(activity, state.tryUpgradeTower(slot), changed, dialog));
        addCard(activity, list, "Переместить",
                "Башня сохранит тип и локальный уровень. После подтверждения нажмите на свободную площадку.",
                "ВЫБРАТЬ НОВОЕ МЕСТО", true, v -> result(activity, state.beginMoveTower(slot), changed, dialog));
        addCard(activity, list, "Демонтаж",
                "Возвращает часть лома. Последнюю и стартовую башню демонтировать нельзя.",
                "ПРОДАТЬ", true, v -> result(activity, state.trySellTower(slot), changed, dialog));
        dialog.show(); fit(dialog);
    }

    public static void showResearch(Activity activity, GameState state, OnChanged changed) {
        Dialog dialog = create(activity, "НАУЧНЫЙ КОМПЛЕКС", "Дерево технологий DeadRig");
        LinearLayout list = content(dialog);
        if (state.activeResearchId != null) {
            Defs.ResearchDef active = Defs.findResearch(state.activeResearchId);
            addCard(activity, list, "В РАБОТЕ  //  " + active.name,
                    "Осталось примерно " + (int) Math.ceil(state.activeResearchSeconds()) + " сек.", null, false, null);
        }
        for (Defs.ResearchDef def : Defs.RESEARCH) {
            boolean done = state.doneResearch.contains(def.id);
            boolean unlocked = def.requiresId == null || state.doneResearch.contains(def.requiresId);
            String requirement = "";
            if (!unlocked) {
                Defs.ResearchDef req = Defs.findResearch(def.requiresId);
                requirement = "\nТребуется: " + (req != null ? req.name : def.requiresId);
            }
            String body = def.desc + "\nЦена: " + GameState.fmt(def.costHashes) + " хешей + "
                    + GameState.fmt(def.costScrap) + " лома  •  " + (int) def.durationSec + " сек." + requirement;
            addCard(activity, list, (done ? "ИЗУЧЕНО  //  " : "") + def.name, body,
                    done ? null : unlocked ? "ИССЛЕДОВАТЬ" : "ЗАКРЫТО", unlocked && !done,
                    v -> result(activity, state.tryStartResearch(def.id), changed, dialog));
        }
        dialog.show(); fit(dialog);
    }

    public static void showWorkshop(Activity activity, GameState state, OnChanged changed) {
        Dialog dialog = create(activity, "ПРОИЗВОДСТВЕННЫЙ ЦЕХ", "Выберите конкретный рецепт");
        LinearLayout list = content(dialog);
        if (state.activeCraftId != null) {
            Defs.RecipeDef active = Defs.findRecipe(state.activeCraftId);
            addCard(activity, list, "ПРОИЗВОДСТВО  //  " + active.name,
                    "Осталось примерно " + (int) Math.ceil(state.activeCraftSeconds()) + " сек.", null, false, null);
        }
        for (Defs.RecipeDef recipe : Defs.RECIPES) {
            boolean unlocked = state.isUnlocked(recipe);
            String requirement = "";
            if (!unlocked) {
                Defs.ResearchDef req = Defs.findResearch(recipe.requiresResearchId);
                requirement = "\nТребуется: " + (req != null ? req.name : recipe.requiresResearchId);
            }
            String description = recipeDescription(recipe.outItem) + "\nЦена: " + GameState.fmt(recipe.costHashes)
                    + " хешей + " + GameState.fmt(recipe.costScrap) + " лома  •  " + (int) recipe.durationSec + " сек." + requirement;
            addCard(activity, list, recipe.name, description, unlocked ? "СОЗДАТЬ" : "ЗАКРЫТО", unlocked,
                    v -> result(activity, state.tryStartCraft(recipe.id), changed, dialog));
        }
        addHint(activity, list, "Готовая башня попадёт в резерв. Нажмите на свободную площадку у дороги, чтобы установить её.");
        dialog.show(); fit(dialog);
    }

    private static String evolutionName(int type, int variant) {
        if (type == 1) return variant == 1 ? "Миниган" : "Бронебойная пушка";
        if (type == 2) return variant == 1 ? "Дальний луч" : "Прожигающий лазер";
        if (type == 3) return variant == 1 ? "Цепной каскад" : "Электрический шторм";
        return variant == 1 ? "Глубокая заморозка" : "Хрупкий лёд";
    }

    private static String evolutionDescription(int type, int variant) {
        if (type == 1) return variant == 1 ? "Резко повышает темп" : "Пробивает броню";
        if (type == 2) return variant == 1 ? "Увеличивает дальность" : "Поджигает цель";
        if (type == 3) return variant == 1 ? "До пяти цепей" : "Урон всем рядом";
        return variant == 1 ? "Замедление длится 6 секунд" : "Цель получает больше урона";
    }

    private static String towerDescription(int type) {
        if (type == 1) return "Высокий темп, стандартный урон";
        if (type == 2) return "Двойной урон и повышенная дальность";
        if (type == 3) return "Цепная молния по группе";
        if (type == 4) return "Замедляет врагов крио-зарядами";
        if (type == 5) return "Ракеты с большим радиусом взрыва";
        return "Усиливает соседние башни на 18%";
    }

    private static String recipeDescription(String item) {
        if ("turret_basic".equals(item)) return "Быстрая универсальная башня";
        if ("turret_laser".equals(item)) return "Двойной урон и увеличенная дальность";
        if ("turret_tesla".equals(item)) return "Цепная молния поражает до трёх целей";
        if ("turret_cryo".equals(item)) return "Замедляет врага на 52% на 3.2 секунды";
        if ("turret_module".equals(item)) return "+1 к общему уровню всех башен";
        if ("turret_rocket".equals(item)) return "Ракетный взрыв поражает большую группу";
        if ("turret_support".equals(item)) return "+18% урона соседним башням";
        if ("road_mines".equals(item)) return "Три автоматические мины на маршрутах";
        if ("wall".equals(item)) return "+20 к максимальному HP базы и ремонт";
        if ("weapon_auto".equals(item)) return "Высокий темп ручного огня при удержании";
        if ("weapon_shotgun".equals(item)) return "Дробь наносит урон группе зомби";
        if ("weapon_rail".equals(item)) return "Мощный бронебойный выстрел";
        if ("weapon_sniper".equals(item)) return "Максимальный множитель попадания в голову";
        if ("weapon_grenade".equals(item)) return "Взрыв поражает большую группу";
        if ("weapon_flame".equals(item)) return "Поджигает цель продолжительным уроном";
        if ("weapon_cryo".equals(item)) return "Замедляет и визуально замораживает";
        if ("weapon_tesla".equals(item)) return "Цепной разряд по нескольким врагам";
        if ("weapon_acid".equals(item)) return "Коррозия повышает последующий урон";
        if ("combat_drone".equals(item)) return "Автоматически атакует ближайших врагов";
        if ("special_ammo".equals(item)) return "Открывает три типа специальных патронов";
        return "Производственный проект";
    }

    private static Dialog create(Activity activity, String title, String subtitle) {
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(activity, 18), dp(activity, 16), dp(activity, 18), dp(activity, 12));
        root.setBackgroundColor(BG);
        TextView heading = text(activity, title, 20, CYAN); heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD); heading.setLetterSpacing(.08f);
        root.addView(heading);
        TextView sub = text(activity, subtitle, 12, MUTED); LinearLayout.LayoutParams sp = wrap(); sp.bottomMargin = dp(activity, 12); root.addView(sub, sp);
        ScrollView scroll = new ScrollView(activity); scroll.setFillViewport(true);
        LinearLayout list = new LinearLayout(activity); list.setId(android.R.id.list); list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Button close = action(activity, "ЗАКРЫТЬ", false); close.setOnClickListener(v -> dialog.dismiss());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 48)); cp.topMargin = dp(activity, 10); root.addView(close, cp);
        dialog.setContentView(root);
        return dialog;
    }

    private static LinearLayout content(Dialog dialog) { return dialog.findViewById(android.R.id.list); }

    private static void fit(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window == null) return;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        window.setDimAmount(.72f); window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    private static void addMetric(Activity a, LinearLayout list, String label, String value) {
        LinearLayout row = new LinearLayout(a); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(dp(a, 10), dp(a, 7), dp(a, 10), dp(a, 7));
        TextView left = text(a, label, 11, MUTED); TextView right = text(a, value, 13, TEXT); right.setGravity(Gravity.END); right.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(right, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f)); list.addView(row);
    }

    private static void addCard(Activity a, LinearLayout list, String title, String body, String action,
                                boolean enabled, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(a); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(a, 14), dp(a, 12), dp(a, 14), dp(a, 12));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{CARD, Color.rgb(9, 29, 33)});
        bg.setCornerRadius(dp(a, 9)); bg.setStroke(dp(a, 1), enabled ? Color.rgb(35, 114, 114) : Color.rgb(43, 63, 65)); card.setBackground(bg);
        TextView heading = text(a, title, 15, enabled ? TEXT : MUTED); heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD); card.addView(heading);
        TextView description = text(a, body, 12, MUTED); LinearLayout.LayoutParams dp = wrap(); dp.topMargin = dp(a, 5); card.addView(description, dp);
        if (action != null) {
            Button button = action(a, action, enabled); button.setEnabled(enabled); if (listener != null) button.setOnClickListener(listener);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(a, 45)); bp.topMargin = dp(a, 10); card.addView(button, bp);
        }
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); cp.bottomMargin = dp(a, 9); list.addView(card, cp);
    }

    private static void addHint(Activity a, LinearLayout list, String value) {
        TextView hint = text(a, value, 12, ORANGE); hint.setGravity(Gravity.CENTER); hint.setPadding(dp(a, 10), dp(a, 8), dp(a, 10), dp(a, 12)); list.addView(hint);
    }

    private static Button action(Activity a, String label, boolean accent) {
        Button button = new Button(a); button.setAllCaps(false); button.setText(label); button.setTextSize(12); button.setTextColor(Color.WHITE); button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                accent ? new int[]{Color.rgb(33, 133, 132), Color.rgb(15, 78, 83)} : new int[]{Color.rgb(49, 65, 67), Color.rgb(26, 39, 42)});
        bg.setCornerRadius(dp(a, 7)); bg.setStroke(dp(a, 1), accent ? CYAN : Color.rgb(78, 97, 98)); button.setBackground(bg);
        button.setHapticFeedbackEnabled(true);
        return button;
    }

    private static TextView text(Activity a, String value, int size, int color) {
        TextView text = new TextView(a); text.setText(value); text.setTextSize(size); text.setTextColor(color); text.setLineSpacing(0, 1.12f); return text;
    }

    private static void result(Activity a, String message, OnChanged changed, Dialog dialog) {
        feedback();
        Toast.makeText(a, message, Toast.LENGTH_SHORT).show(); changed.run(); dialog.dismiss();
    }

    private static void feedback() {
        final ToneGenerator tone = new ToneGenerator(AudioManager.STREAM_MUSIC, 28);
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 70);
        new Handler(Looper.getMainLooper()).postDelayed(tone::release, 120);
    }

    private static String percent(double value) { return Math.round(value * 100) + "%"; }
    private static int dp(Activity a, int value) { return (int) (value * a.getResources().getDisplayMetrics().density + .5f); }
    private static LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
}
