package com.deadrig.app;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
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

    public static void showFarm(Activity activity, GameState state, OnChanged changed) {
        Dialog dialog = create(activity, "МАЙНИНГОВАЯ ФЕРМА", "Пассивный доход узла");
        LinearLayout list = content(dialog);
        addMetric(activity, list, "УРОВЕНЬ", String.valueOf(state.minerLevel));
        addMetric(activity, list, "ДОХОД", String.format(Locale.US, "%.1f хеш/с", state.miningRate()));
        addCard(activity, list, "Расширить ферму",
                "Новый ASIC-модуль увеличивает постоянную добычу.\nСтоимость: " + GameState.fmt(state.minerUpgradeCost()) + " хешей",
                "УЛУЧШИТЬ", true, v -> result(activity, state.tryUpgradeMiner(), changed, dialog));
        addHint(activity, list, "Доход продолжает начисляться вне игры: 50%, максимум 8 часов.");
        dialog.show(); fit(dialog);
    }

    public static void showDefense(Activity activity, GameState state, OnChanged changed) {
        Dialog dialog = create(activity, "КОНТУР ОБОРОНЫ", "Башни и боевые модификаторы");
        LinearLayout list = content(dialog);
        addMetric(activity, list, "ОБЩИЙ УРОВЕНЬ", String.valueOf(state.turretLevel));
        addMetric(activity, list, "УСТАНОВЛЕНО / РЕЗЕРВ", state.turretCount() + " / " + state.pendingTowerCount());
        addMetric(activity, list, "БОНУСЫ", "+" + percent(state.turretDamageBonus()) + " урон  •  +"
                + percent(state.turretFireRateBonus()) + " темп  •  +" + String.format(Locale.US, "%.1f", state.turretRangeBonus()) + " дальность");
        addCard(activity, list, "Усилить все башни",
                "Повышает базовый урон всего оборонного контура.\nСтоимость: " + GameState.fmt(state.turretUpgradeCost()) + " хешей",
                "УСИЛИТЬ", true, v -> result(activity, state.tryUpgradeTurret(), changed, dialog));

        String[] names = {"", "Пулемётная", "Лазерная", "Тесла"};
        for (int i = 0; i < GameState.TOWER_SLOTS.length; i++) {
            int type = state.towerTypeAt(i);
            if (type != 0) addCard(activity, list, "Площадка " + (i + 1) + "  //  " + names[type],
                    type == 1 ? "Высокий темп, стандартный урон" : type == 2 ? "Двойной урон и повышенная дальность" : "Тяжёлый энергетический импульс",
                    null, false, null);
        }
        addHint(activity, list, "Чтобы установить башню из резерва, закройте экран и нажмите на светящуюся свободную площадку у дороги.");
        dialog.show(); fit(dialog);
    }

    public static void showTower(Activity activity, GameState state, int slot, OnChanged changed) {
        int type = state.towerTypeAt(slot);
        if (type == 0) return;
        String[] names = {"", "ПУЛЕМЁТНАЯ БАШНЯ", "ЛАЗЕРНАЯ БАШНЯ", "ТЕСЛА-БАШНЯ"};
        Dialog dialog = create(activity, names[type], "Монтажная площадка " + (slot + 1));
        LinearLayout list = content(dialog);
        addMetric(activity, list, "ЛОКАЛЬНЫЙ УРОВЕНЬ", String.valueOf(state.towerLevelAt(slot)));
        addMetric(activity, list, "ГЛОБАЛЬНЫЙ УРОВЕНЬ", String.valueOf(state.turretLevel));
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

    private static String recipeDescription(String item) {
        if ("turret_basic".equals(item)) return "Быстрая универсальная башня";
        if ("turret_laser".equals(item)) return "Двойной урон и увеличенная дальность";
        if ("turret_tesla".equals(item)) return "Тяжёлый энергетический разряд";
        if ("turret_module".equals(item)) return "+1 к общему уровню всех башен";
        if ("wall".equals(item)) return "+20 к максимальному HP базы и ремонт";
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
        bg.setCornerRadius(dp(a, 7)); bg.setStroke(dp(a, 1), accent ? CYAN : Color.rgb(78, 97, 98)); button.setBackground(bg); return button;
    }

    private static TextView text(Activity a, String value, int size, int color) {
        TextView text = new TextView(a); text.setText(value); text.setTextSize(size); text.setTextColor(color); text.setLineSpacing(0, 1.12f); return text;
    }

    private static void result(Activity a, String message, OnChanged changed, Dialog dialog) {
        Toast.makeText(a, message, Toast.LENGTH_SHORT).show(); changed.run(); dialog.dismiss();
    }

    private static String percent(double value) { return Math.round(value * 100) + "%"; }
    private static int dp(Activity a, int value) { return (int) (value * a.getResources().getDisplayMetrics().density + .5f); }
    private static LinearLayout.LayoutParams wrap() { return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); }
}
