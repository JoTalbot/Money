package com.deadrig.app;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** Главный экран: игровое поле + HUD (валюты, кнопки, экран поражения). */
public class MainActivity extends Activity {

    private GameState gs;
    private TextView lblHash, lblScrap, lblCrystal, lblWave, lblBase, lblResearch, lblCraft;
    private View gameOverPanel;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        gs = new GameState(getSharedPreferences("deadrig", MODE_PRIVATE));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        GameView gameView = new GameView(this, gs);
        root.addView(gameView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // --- верхняя панель ---
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        top.setPadding(dp(12), dp(8), dp(12), dp(8));
        top.setBackgroundColor(Color.argb(160, 0, 0, 0));
        lblHash = tv(17, Color.rgb(255, 220, 120));
        lblScrap = tv(13, Color.rgb(180, 220, 255));
        lblCrystal = tv(13, Color.rgb(220, 180, 255));
        lblWave = tv(16, Color.WHITE);
        lblBase = tv(14, Color.rgb(255, 150, 150));
        lblResearch = tv(12, Color.rgb(160, 255, 160));
        lblCraft = tv(12, Color.rgb(160, 255, 160));
        top.addView(lblHash); top.addView(lblScrap); top.addView(lblCrystal);
        top.addView(lblWave); top.addView(lblBase); top.addView(lblResearch); top.addView(lblCraft);
        root.addView(top, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

        // --- нижняя панель кнопок ---
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setPadding(dp(6), dp(8), dp(6), dp(12));
        bottom.setBackgroundColor(Color.argb(160, 0, 0, 0));
        bottom.addView(btn("Ферма", v -> act(gs.tryUpgradeMiner())));
        bottom.addView(btn("Турель", v -> act(gs.tryUpgradeTurret())));
        bottom.addView(btn("Наука", v -> act(gs.tryStartResearch())));
        bottom.addView(btn("Крафт", v -> act(gs.tryStartCraft())));
        bottom.addView(btn("Волна", v -> act(gs.tryStartWave())));
        root.addView(bottom, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        // --- экран поражения ---
        LinearLayout over = new LinearLayout(this);
        over.setOrientation(LinearLayout.VERTICAL);
        over.setGravity(Gravity.CENTER);
        over.setBackgroundColor(Color.argb(210, 0, 0, 0));
        TextView overText = tv(26, Color.rgb(255, 90, 90));
        overText.setText("БАЗА УНИЧТОЖЕНА");
        over.addView(overText);
        Button restart = new Button(this);
        restart.setText("Заново");
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(220), dp(90));
        rp.topMargin = dp(24);
        restart.setLayoutParams(rp);
        restart.setOnClickListener(v -> { gs.reset(); gameOverPanel.setVisibility(ViewGroup.GONE); });
        over.addView(restart);
        gameOverPanel = over;
        gameOverPanel.setVisibility(ViewGroup.GONE);
        root.addView(over, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        startHudLoop();
    }

    private void act(String msg) {
        if (msg != null) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void startHudLoop() {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                refreshHud();
                if (gs.gameOver) gameOverPanel.setVisibility(ViewGroup.VISIBLE);
                handler.postDelayed(this, 250);
            }
        }, 250);
    }

    private void refreshHud() {
        lblHash.setText("Хеши: " + GameState.fmt(gs.hashes));
        lblScrap.setText("Лом: " + GameState.fmt(gs.scrap));
        lblCrystal.setText("Кристаллы: " + gs.crystals);
        lblWave.setText("Волна: " + gs.wave + (gs.waveActive ? " (зомби идут)" : ""));
        lblBase.setText("База: " + (int) Math.ceil(gs.baseHp) + "/" + (int) gs.baseMaxHp + "  Турели: " + gs.turretCount());
        lblResearch.setText("Наука: " + gs.researchStatus());
        lblCraft.setText("Мастерская: " + gs.craftStatus());
    }

    private TextView tv(int size, int color) {
        TextView t = new TextView(this);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.START);
        return t;
    }

    private Button btn(String label, android.view.View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(13);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(p);
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gs.save();
    }

    @Override
    protected void onStop() {
        super.onStop();
        gs.save();
    }
}
