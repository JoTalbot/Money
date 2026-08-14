package com.deadrig.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Главный экран нативной Android-версии: изометрическое поле и мобильный HUD. */
public class MainActivity extends Activity {

    private static final int CYAN = Color.rgb(63, 232, 226);
    private static final int ORANGE = Color.rgb(232, 126, 35);
    private static final int TEXT = Color.rgb(225, 239, 238);
    private static final int MUTED = Color.rgb(147, 177, 177);

    private GameState gs;
    private SharedPreferences prefs;
    private TextView lblHash, lblScrap, lblCrystal, lblWave, lblBase, lblWeapon, lblResearch, lblCraft;
    private Button abilityButton, ultimateButton;
    private View gameOverPanel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(5, 14, 17));
        getWindow().setNavigationBarColor(Color.rgb(5, 14, 17));
        prefs = getSharedPreferences("deadrig", MODE_PRIVATE);
        gs = new GameState(prefs);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(5, 14, 17));
        root.addView(new GameView(this, gs, slot -> {
            gs.selectTower(slot);
            ControlPanel.showTower(this, gs, slot, this::refreshHud);
        }), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(buildTopPanel(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));
        root.addView(buildBottomPanel(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
        abilityButton = button("СПЕЦ", false, v -> act(gs.activateWeaponAbility()));
        FrameLayout.LayoutParams abilityParams = new FrameLayout.LayoutParams(dp(118), dp(44), Gravity.START | Gravity.BOTTOM);
        abilityParams.leftMargin = dp(10); abilityParams.bottomMargin = dp(76); root.addView(abilityButton, abilityParams);
        ultimateButton = button("УЛЬТА", true, v -> act(gs.activateUltimate()));
        FrameLayout.LayoutParams ultimateParams = new FrameLayout.LayoutParams(dp(118), dp(44), Gravity.END | Gravity.BOTTOM);
        ultimateParams.rightMargin = dp(10); ultimateParams.bottomMargin = dp(76); root.addView(ultimateButton, ultimateParams);
        root.addView(buildGameOver(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        startHudLoop();
        if (!prefs.getBoolean("tutorial_v1_complete", false)) {
            handler.postDelayed(() -> ControlPanel.showTutorial(this, () ->
                    prefs.edit().putBoolean("tutorial_v1_complete", true).apply()), 350);
        }
    }

    private View buildTopPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(9), dp(14), dp(10));
        panel.setBackground(panelBackground(238, false));

        TextView title = tv(11, CYAN, Gravity.CENTER);
        title.setText("DEADRIG  //  УЗЕЛ 07");
        title.setLetterSpacing(.16f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        panel.addView(title, matchWrap());

        LinearLayout resources = row();
        lblHash = tv(15, Color.rgb(255, 193, 91), Gravity.START);
        lblScrap = tv(13, Color.rgb(154, 218, 224), Gravity.CENTER);
        lblCrystal = tv(13, Color.rgb(197, 171, 255), Gravity.END);
        resources.addView(lblHash, weighted()); resources.addView(lblScrap, weighted()); resources.addView(lblCrystal, weighted());
        panel.addView(resources, matchWrap());

        LinearLayout combat = row();
        lblWave = tv(14, TEXT, Gravity.START);
        lblBase = tv(14, CYAN, Gravity.END);
        combat.addView(lblWave, weighted()); combat.addView(lblBase, weighted());
        panel.addView(combat, matchWrap());

        lblWeapon = tv(11, ORANGE, Gravity.CENTER);
        LinearLayout.LayoutParams weaponParams = matchWrap(); weaponParams.topMargin = dp(4);
        panel.addView(lblWeapon, weaponParams);

        LinearLayout progress = row();
        lblResearch = tv(11, MUTED, Gravity.START);
        lblCraft = tv(11, MUTED, Gravity.END);
        progress.addView(lblResearch, weighted()); progress.addView(lblCraft, weighted());
        panel.addView(progress, matchWrap());
        return panel;
    }

    private View buildBottomPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(dp(7), dp(8), dp(7), dp(11));
        panel.setBackground(panelBackground(242, true));
        panel.setClickable(true);
        panel.setFocusable(true);
        panel.setElevation(dp(16));
        panel.addView(button("Ферма", false, v -> ControlPanel.showFarm(this, gs, this::refreshHud)));
        panel.addView(button("Оборона", false, v -> ControlPanel.showDefense(this, gs, this::refreshHud)));
        panel.addView(button("Наука", false, v -> ControlPanel.showResearch(this, gs, this::refreshHud)));
        panel.addView(button("Цех", false, v -> ControlPanel.showWorkshop(this, gs, this::refreshHud)));
        panel.addView(button("Волна", true, v -> act(gs.tryStartWave())));
        return panel;
    }

    private View buildGameOver() {
        LinearLayout over = new LinearLayout(this);
        over.setOrientation(LinearLayout.VERTICAL);
        over.setGravity(Gravity.CENTER);
        over.setPadding(dp(24), dp(24), dp(24), dp(24));
        over.setBackgroundColor(Color.argb(238, 3, 9, 11));

        TextView code = tv(11, ORANGE, Gravity.CENTER);
        code.setLetterSpacing(.2f); code.setText("КРИТИЧЕСКАЯ ОШИБКА");
        over.addView(code);
        TextView title = tv(27, Color.rgb(255, 75, 79), Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD); title.setText("БАЗА УНИЧТОЖЕНА");
        LinearLayout.LayoutParams titleParams = matchWrap(); titleParams.topMargin = dp(8); over.addView(title, titleParams);
        TextView hint = tv(13, MUTED, Gravity.CENTER);
        hint.setText("Перезапустите узел и укрепите оборонный контур");
        LinearLayout.LayoutParams hintParams = matchWrap(); hintParams.topMargin = dp(10); over.addView(hint, hintParams);

        Button restart = button("ПЕРЕЗАПУСТИТЬ", true, v -> {
            gs.reset();
            gameOverPanel.setVisibility(ViewGroup.GONE);
        });
        LinearLayout.LayoutParams restartParams = new LinearLayout.LayoutParams(dp(230), dp(58));
        restartParams.topMargin = dp(26); restart.setLayoutParams(restartParams); over.addView(restart);
        gameOverPanel = over;
        gameOverPanel.setVisibility(ViewGroup.GONE);
        return over;
    }

    private void act(String message) {
        Toast.makeText(this, message != null ? message : "Действие выполнено", Toast.LENGTH_SHORT).show();
        refreshHud();
    }

    private void startHudLoop() {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                refreshHud();
                if (gs.gameOver) gameOverPanel.setVisibility(ViewGroup.VISIBLE);
                handler.postDelayed(this, 200);
            }
        }, 100);
    }

    private void refreshHud() {
        lblHash.setText("ХЕШИ  " + GameState.fmt(gs.hashes));
        lblScrap.setText("ЛОМ  " + GameState.fmt(gs.scrap));
        lblCrystal.setText("КРИСТ.  " + gs.crystals);
        lblWave.setText("ВОЛНА  " + gs.wave + (gs.waveActive ? (gs.wave > 0 && gs.wave % 10 == 0 ? "  // БОСС" : "  // АТАКА") : "")
                + "  •  " + gs.weatherName().toUpperCase());
        lblBase.setText("БАЗА  " + (int) Math.ceil(gs.baseHp) + "/" + (int) gs.baseMaxHp
                + "  •  Т" + gs.turretCount());
        lblBase.setTextColor(gs.baseHp <= gs.baseMaxHp * .3 ? Color.rgb(255, 74, 78) : CYAN);
        String weaponState = gs.manualReloading() ? "ПЕРЕЗАРЯДКА" : gs.manualOverheated() ? "ПЕРЕГРЕВ"
                : "БОЕЗАПАС " + gs.manualAmmo() + "/" + gs.manualMagazineSize();
        lblWeapon.setText("РУЧНОЙ ОГОНЬ  //  " + gs.manualWeaponName().toUpperCase()
                + " [" + gs.weaponRarityName(gs.manualWeaponType()).toUpperCase() + "] УР." + gs.manualWeaponLevel()
                + "  •  " + weaponState + "  •  " + gs.manualAmmoTypeName().toUpperCase()
                + (gs.manualCombo() > 1 ? "  •  КОМБО ×" + gs.manualCombo() : ""));
        lblResearch.setText("НАУКА  " + gs.researchStatus());
        lblCraft.setText("ЦЕХ  " + gs.craftStatus()
                + (gs.pendingTowerCount() > 0 ? "  •  РЕЗЕРВ " + gs.pendingTowerCount() : ""));
        if (abilityButton != null) abilityButton.setText(gs.weaponAbilityCooldownSeconds() > 0
                ? "СПЕЦ " + gs.weaponAbilityCooldownSeconds() + "с" : "СПЕЦ ГОТОВ");
        if (ultimateButton != null) ultimateButton.setText("УЛЬТА " + gs.ultimatePercent() + "%");
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams params = matchWrap(); params.topMargin = dp(4); row.setLayoutParams(params);
        return row;
    }

    private TextView tv(int size, int color, int gravity) {
        TextView text = new TextView(this);
        text.setTextSize(size); text.setTextColor(color); text.setGravity(gravity);
        text.setSingleLine(true); text.setIncludeFontPadding(false);
        return text;
    }

    private Button button(String label, boolean accent, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label); button.setTextSize(11); button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD); button.setAllCaps(false);
        button.setPadding(dp(2), 0, dp(2), 0); button.setMinWidth(0); button.setMinimumWidth(0);
        button.setMinHeight(0); button.setMinimumHeight(0);
        button.setClickable(true); button.setFocusable(true);
        button.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            listener.onClick(v);
        });
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                accent ? new int[]{Color.rgb(235, 132, 42), Color.rgb(181, 76, 25)}
                        : new int[]{Color.rgb(27, 79, 82), Color.rgb(15, 48, 53)});
        bg.setCornerRadius(dp(7)); bg.setStroke(dp(1), accent ? Color.rgb(255, 179, 77) : Color.rgb(44, 137, 137));
        button.setBackground(bg);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(53), 1f);
        params.setMargins(dp(3), 0, dp(3), 0); button.setLayoutParams(params);
        return button;
    }

    private GradientDrawable panelBackground(int alpha, boolean topCorners) {
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(alpha, 7, 24, 28), Color.argb(alpha, 4, 15, 19)});
        bg.setStroke(dp(1), Color.argb(170, 35, 101, 103));
        return bg;
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    @Override protected void onPause() { super.onPause(); gs.save(); }
    @Override protected void onStop() { super.onStop(); gs.save(); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
}
