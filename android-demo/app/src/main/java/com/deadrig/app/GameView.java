package com.deadrig.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Отрисовка игрового мира (вид сверху) + игровой цикл на базе View. */
public class GameView extends View {

    private final GameState gs;
    private long lastTime;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public GameView(Context ctx, GameState gs) {
        super(ctx);
        this.gs = gs;
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);

        long now = System.currentTimeMillis();
        double dt = Math.min(0.05, (now - lastTime) / 1000.0);
        lastTime = now;
        if (dt > 0) gs.update(dt);

        int w = getWidth(), h = getHeight();
        float scale = Math.min(w, h) / 32f;
        float cx = w / 2f, cy = h * 0.52f;

        // фон
        c.drawColor(Color.rgb(13, 17, 13));

        // спавн-точки по углам
        paint.setColor(Color.rgb(120, 30, 30));
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sy = -1; sy <= 1; sy += 2) {
                float x = cx + sx * 14 * scale, y = cy - sy * 14 * scale;
                c.drawRect(x - scale * 0.5f, y - scale * 0.5f, x + scale * 0.5f, y + scale * 0.5f, paint);
            }

        // база
        paint.setColor(Color.rgb(240, 140, 30));
        c.drawRoundRect(cx - scale * 1.4f, cy - scale * 1.4f, cx + scale * 1.4f, cy + scale * 1.4f, scale * 0.3f, scale * 0.3f, paint);

        // турели
        for (GameState.Turret t : gs.turrets) {
            float tx = cx + (float) t.x * scale, ty = cy - (float) t.y * scale;
            paint.setColor(t.laser ? Color.rgb(90, 90, 160) : Color.rgb(70, 70, 80));
            c.drawCircle(tx, ty, scale * 0.45f, paint);
            float ang = (float) Math.atan2(-t.aimY, t.aimX);
            float ex = tx + (float) Math.cos(ang) * scale * 0.9f;
            float ey = ty + (float) Math.sin(ang) * scale * 0.9f;
            paint.setColor(Color.rgb(40, 40, 50));
            c.drawLine(tx, ty, ex, ey, paint);
        }

        // зомби
        paint.setColor(Color.rgb(80, 190, 70));
        for (GameState.Zombie z : gs.zombies) {
            float zx = cx + (float) z.x * scale, zy = cy - (float) z.y * scale;
            c.drawCircle(zx, zy, scale * 0.42f, paint);
        }

        // снаряды
        paint.setColor(Color.rgb(120, 240, 255));
        for (GameState.Projectile p : gs.projectiles) {
            float px = cx + (float) p.x * scale, py = cy - (float) p.y * scale;
            c.drawCircle(px, py, scale * 0.18f, paint);
        }

        postInvalidateOnAnimation();
    }
}
