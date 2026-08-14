package com.deadrig.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Высококачественный 2D-изометрический рендер без Unity и внешних библиотек.
 * Игровые координаты проецируются в ромб 2:1; графика рисуется векторно Canvas API.
 */
public class GameView extends View {

    private static final int BG_TOP = Color.rgb(7, 16, 20);
    private static final int BG_BOTTOM = Color.rgb(13, 28, 29);
    private static final int TILE_TOP = Color.rgb(34, 48, 49);
    private static final int TILE_LEFT = Color.rgb(21, 32, 34);
    private static final int TILE_RIGHT = Color.rgb(16, 27, 29);
    private static final int LINE = Color.rgb(54, 73, 74);
    private static final int CYAN = Color.rgb(56, 231, 226);
    private static final int ORANGE = Color.rgb(226, 125, 35);
    private static final int RED = Color.rgb(244, 68, 86);

    public interface SlotListener { void onOccupiedSlot(int slot); }

    private final GameState gs;
    private final SlotListener slotListener;
    private long lastTime;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    private final Path path = new Path();

    private float unit;
    private float cx;
    private float cy;
    private GameState.Zombie heldTarget;
    private long touchDownAt;
    private boolean aimingAtEnemy;
    private boolean aimingHeadshot;

    public GameView(Context ctx, GameState gs, SlotListener slotListener) {
        super(ctx);
        this.gs = gs;
        this.slotListener = slotListener;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        // Аппаратный Canvas оставляет главный поток свободным для кнопок и жестов.
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now = System.currentTimeMillis();
        if (lastTime == 0) lastTime = now;
        double dt = Math.min(0.05, (now - lastTime) / 1000.0);
        lastTime = now;
        if (dt > 0) gs.update(dt);
        if (aimingAtEnemy && heldTarget != null && now - touchDownAt >= 180) {
            if (!gs.zombies.contains(heldTarget)) heldTarget = null;
            else if (gs.manualShoot(heldTarget, false, aimingHeadshot, now - touchDownAt >= 650))
                performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        }

        int w = getWidth(), h = getHeight();
        unit = Math.min(w / 18f, h / 30f);
        cx = w * 0.5f;
        cy = h * 0.53f;

        drawBackground(c, w, h);
        drawArena(c);
        drawRoutesAndSlots(c);
        drawMapObjects(c);
        drawSelectedTowerRange(c);
        drawSpawnRifts(c);
        drawSortedActors(c);
        drawCombatDrone(c);
        drawProjectiles(c);
        drawHitEffects(c);
        drawWeatherOverlay(c, w, h);
        drawVignette(c, w, h);
        // 30 FPS достаточно для idle/tower-defense и не блокирует обработку нажатий HUD.
        postInvalidateDelayed(33);
    }

    private PointF iso(double x, double y) {
        return new PointF(cx + (float) (x - y) * unit * 0.5f,
                cy + (float) (x + y) * unit * 0.25f);
    }

    private void drawBackground(Canvas c, int w, int h) {
        paint.setShader(new LinearGradient(0, 0, 0, h, BG_TOP, BG_BOTTOM, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint);
        paint.setShader(null);

        // Дальние лучи аварийного освещения.
        paint.setColor(Color.argb(12, 48, 232, 224));
        triangle(c, w * .08f, h, w * .40f, 0, w * .50f, 0, paint);
        paint.setColor(Color.argb(9, 234, 129, 38));
        triangle(c, w * .92f, h, w * .61f, 0, w * .53f, 0, paint);
    }

    private void drawArena(Canvas c) {
        final int radius = 8;
        // Рисуем от дальних тайлов к ближним, чтобы боковые грани перекрывались правильно.
        for (int sum = -radius * 2; sum <= radius * 2; sum++) {
            for (int x = -radius; x <= radius; x++) {
                int y = sum - x;
                if (y < -radius || y > radius) continue;
                drawTile(c, x, y);
            }
        }

        // Оранжевый защитный периметр вокруг ядра.
        PointF a = iso(-2.2, -2.2), b = iso(2.2, -2.2), d = iso(-2.2, 2.2), e = iso(2.2, 2.2);
        stroke.setColor(Color.argb(165, 226, 125, 35));
        stroke.setStrokeWidth(Math.max(2f, unit * .055f));
        path.reset(); path.moveTo(a.x, a.y); path.lineTo(b.x, b.y); path.lineTo(e.x, e.y);
        path.lineTo(d.x, d.y); path.close(); c.drawPath(path, stroke);
    }

    private void drawTile(Canvas c, int x, int y) {
        PointF top = iso(x, y - .5), right = iso(x + .5, y), bottom = iso(x, y + .5), left = iso(x - .5, y);
        float depth = unit * .09f;

        paint.setColor(TILE_LEFT);
        quad(c, left.x, left.y, bottom.x, bottom.y, bottom.x, bottom.y + depth, left.x, left.y + depth, paint);
        paint.setColor(TILE_RIGHT);
        quad(c, bottom.x, bottom.y, right.x, right.y, right.x, right.y + depth, bottom.x, bottom.y + depth, paint);

        int variation = ((x * 17 + y * 31) & 3) * 3;
        paint.setColor(Color.rgb(34 + variation, 48 + variation, 49 + variation));
        quad(c, top.x, top.y, right.x, right.y, bottom.x, bottom.y, left.x, left.y, paint);
        stroke.setColor(LINE);
        stroke.setStrokeWidth(Math.max(1f, unit * .018f));
        c.drawPath(path, stroke);

        if (((x * 13 + y * 7) & 15) == 0) {
            paint.setColor(Color.argb(150, 226, 125, 35));
            c.drawCircle(left.x * .3f + right.x * .7f, left.y * .3f + right.y * .7f,
                    Math.max(1.5f, unit * .035f), paint);
        }
    }

    private void drawMapObjects(Canvas c) {
        int[] hazardColors = {Color.TRANSPARENT, Color.rgb(255, 91, 38), Color.rgb(171, 255, 70), Color.rgb(112, 220, 255), Color.rgb(205, 112, 255)};
        for (int route = 0; route < GameState.PATHS.length; route++) {
            double[] zone = gs.routePointForView(route, 3);
            PointF p = iso(zone[0], zone[1]);
            int color = hazardColors[gs.hazardType()];
            paint.setColor(Color.argb(35, Color.red(color), Color.green(color), Color.blue(color)));
            c.drawOval(p.x - unit, p.y - unit * .5f, p.x + unit, p.y + unit * .5f, paint);
            stroke.setColor(Color.argb(135, Color.red(color), Color.green(color), Color.blue(color)));
            stroke.setStrokeWidth(unit * .045f); c.drawOval(p.x - unit, p.y - unit * .5f, p.x + unit, p.y + unit * .5f, stroke);
            if (gs.barricadeHp() > 0) {
                prism(c, p.x, p.y - unit * .12f, unit * .42f, unit * .18f, unit * .38f,
                        Color.rgb(105, 88, 65), Color.rgb(68, 53, 39), Color.rgb(49, 38, 29));
            }
        }
        double[][] nodes = {{-5.2, 5.2}, {5.2, -5.2}};
        for (int i = 0; i < gs.capturedNodes(); i++) {
            PointF p = iso(nodes[i][0], nodes[i][1]);
            paint.setColor(Color.argb(80, 62, 240, 226)); c.drawCircle(p.x, p.y, unit * .55f, paint);
            paint.setColor(CYAN); diamond(c, p.x, p.y, unit * .28f, unit * .14f, paint);
        }
    }

    private void drawSelectedTowerRange(Canvas c) {
        int slot = gs.selectedTowerSlot();
        if (slot < 0 || gs.towerTypeAt(slot) == 0) return;
        PointF p = iso(GameState.TOWER_SLOTS[slot][0], GameState.TOWER_SLOTS[slot][1]);
        float range = (float) gs.towerRangeAt(slot) * unit;
        paint.setColor(Color.argb(24, 64, 231, 225));
        c.drawOval(p.x - range, p.y - range * .5f, p.x + range, p.y + range * .5f, paint);
        stroke.setColor(Color.argb(145, 64, 231, 225)); stroke.setStrokeWidth(unit * .035f);
        c.drawOval(p.x - range, p.y - range * .5f, p.x + range, p.y + range * .5f, stroke);
    }

    /** Объёмная дорога tower defense и фиксированные монтажные площадки. */
    private void drawRoutesAndSlots(Canvas c) {
        for (int routeIndex = 0; routeIndex < GameState.PATHS.length; routeIndex++) {
            path.reset();
            double[] firstLogical = gs.routePointForView(routeIndex, 0);
            PointF first = iso(firstLogical[0], firstLogical[1]);
            path.moveTo(first.x, first.y);
            for (int i = 1; i < GameState.PATHS[routeIndex].length; i++) {
                double[] logical = gs.routePointForView(routeIndex, i);
                PointF point = iso(logical[0], logical[1]);
                path.lineTo(point.x, point.y);
            }
            stroke.setPathEffect(null);
            stroke.setStrokeWidth(unit * .72f);
            stroke.setColor(Color.rgb(13, 23, 25));
            c.drawPath(path, stroke);
            stroke.setStrokeWidth(unit * .58f);
            stroke.setColor(Color.rgb(50, 61, 61));
            c.drawPath(path, stroke);
            stroke.setStrokeWidth(unit * .055f);
            stroke.setColor(Color.argb(170, 231, 139, 49));
            stroke.setPathEffect(new DashPathEffect(new float[]{unit * .28f, unit * .22f}, 0));
            c.drawPath(path, stroke);
            stroke.setPathEffect(null);
        }

        double[][] minePoints = {{-2.8, .9}, {.8, -2.8}, {2.7, -.8}};
        for (int i = 0; i < Math.min(3, gs.mineCharges()); i++) {
            PointF mine = iso(minePoints[i][0], minePoints[i][1]);
            paint.setColor(Color.rgb(91, 48, 35)); diamond(c, mine.x, mine.y, unit * .22f, unit * .10f, paint);
            paint.setColor(RED); c.drawCircle(mine.x, mine.y - unit * .05f, unit * .055f, paint);
        }

        for (int i = 0; i < GameState.TOWER_SLOTS.length; i++) {
            double[] slot = GameState.TOWER_SLOTS[i];
            PointF p = iso(slot[0], slot[1]);
            boolean occupied = gs.towerTypeAt(i) != 0;
            float r = unit * .42f;
            paint.setColor(occupied ? Color.rgb(32, 47, 49)
                    : gs.pendingTowerCount() > 0 ? Color.rgb(47, 105, 101) : Color.rgb(29, 44, 46));
            diamond(c, p.x, p.y + unit * .18f, r, r * .42f, paint);
            stroke.setStrokeWidth(unit * .055f);
            stroke.setColor(occupied ? Color.rgb(74, 99, 99)
                    : gs.pendingTowerCount() > 0 ? CYAN : Color.rgb(72, 91, 91));
            diamond(c, p.x, p.y + unit * .18f, r, r * .42f, stroke);
            if (!occupied) {
                stroke.setStrokeWidth(unit * .05f);
                c.drawLine(p.x - r * .24f, p.y + unit * .18f, p.x + r * .24f, p.y + unit * .18f, stroke);
                c.drawLine(p.x, p.y + unit * .18f - r * .24f, p.x, p.y + unit * .18f + r * .24f, stroke);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            heldTarget = touchedZombie(event.getX(), event.getY());
            aimingAtEnemy = heldTarget != null;
            aimingHeadshot = heldTarget != null && isHeadshot(heldTarget, event.getY());
            touchDownAt = System.currentTimeMillis();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE && aimingAtEnemy) {
            GameState.Zombie movedTarget = touchedZombie(event.getX(), event.getY());
            if (movedTarget != null) {
                heldTarget = movedTarget;
                aimingHeadshot = isHeadshot(movedTarget, event.getY());
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            aimingAtEnemy = false; heldTarget = null; return true;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) return true;

        if (aimingAtEnemy) {
            boolean shortTap = System.currentTimeMillis() - touchDownAt < 220;
            if (heldTarget != null && gs.manualShoot(heldTarget, shortTap, aimingHeadshot, false))
                performHapticFeedback(shortTap ? android.view.HapticFeedbackConstants.LONG_PRESS
                        : android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            aimingAtEnemy = false; heldTarget = null;
            return true;
        }

        int best = -1;
        float bestDistance = unit * .82f;
        for (int i = 0; i < GameState.TOWER_SLOTS.length; i++) {
            PointF p = iso(GameState.TOWER_SLOTS[i][0], GameState.TOWER_SLOTS[i][1]);
            float distance = (float) Math.hypot(event.getX() - p.x, event.getY() - p.y);
            if (distance < bestDistance) { bestDistance = distance; best = i; }
        }
        if (best >= 0) {
            performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (gs.towerTypeAt(best) != 0 && slotListener != null) slotListener.onOccupiedSlot(best);
            else {
                String message = gs.tryPlaceTower(best);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                invalidate();
            }
        }
        return true;
    }

    private GameState.Zombie touchedZombie(float x, float y) {
        GameState.Zombie best = null;
        float bestDistance = unit * 1.05f;
        for (GameState.Zombie zombie : gs.zombies) {
            PointF p = iso(zombie.x, zombie.y);
            float vertical = unit * enemyScale(zombie.type) * 1.25f;
            float distance = (float) Math.hypot(x - p.x, y - (p.y - vertical));
            if (distance < bestDistance) { bestDistance = distance; best = zombie; }
        }
        return best;
    }

    private boolean isHeadshot(GameState.Zombie zombie, float touchY) {
        PointF p = iso(zombie.x, zombie.y);
        float scale = enemyScale(zombie.type);
        return touchY < p.y - unit * scale * .72f;
    }

    private void drawSpawnRifts(Canvas c) {
        for (int i = 0; i < GameState.PATHS.length; i++) {
            double[] start = gs.routePointForView(i, 0);
            drawRift(c, start[0], start[1]);
        }
    }

    private void drawRift(Canvas c, double x, double y) {
        PointF p = iso(x, y);
        float r = unit * .48f;
        paint.setShader(new RadialGradient(p.x, p.y, r * 1.8f,
                new int[]{Color.argb(120, 255, 63, 88), Color.argb(20, 255, 63, 88), Color.TRANSPARENT},
                new float[]{0f, .5f, 1f}, Shader.TileMode.CLAMP));
        c.drawCircle(p.x, p.y, r * 1.8f, paint);
        paint.setShader(null);
        paint.setColor(Color.rgb(58, 20, 39));
        diamond(c, p.x, p.y, r, r * .42f, paint);
        stroke.setColor(RED); stroke.setStrokeWidth(unit * .08f);
        diamond(c, p.x, p.y, r * .72f, r * .30f, stroke);
        paint.setColor(Color.rgb(255, 99, 78));
        for (int i = -1; i <= 1; i++) {
            float fx = p.x + i * r * .45f;
            triangle(c, fx, p.y - r * .25f, fx + r * .10f, p.y - r * .75f,
                    fx + r * .20f, p.y - r * .24f, paint);
        }
    }

    private void drawSortedActors(Canvas c) {
        List<Actor> actors = new ArrayList<>();
        actors.add(new Actor(0, 0, 0, null));
        for (GameState.Turret t : gs.turrets) actors.add(new Actor(t.x, t.y, 1, t));
        for (GameState.Zombie z : gs.zombies) actors.add(new Actor(z.x, z.y, 2, z));
        Collections.sort(actors, new Comparator<Actor>() {
            @Override public int compare(Actor a, Actor b) {
                return Double.compare(a.x + a.y, b.x + b.y);
            }
        });
        for (Actor actor : actors) {
            if (actor.type == 0) drawBase(c);
            else if (actor.type == 1) drawTurret(c, (GameState.Turret) actor.data);
            else drawZombie(c, (GameState.Zombie) actor.data);
        }
    }

    private void drawBase(Canvas c) {
        PointF p = iso(0, 0);
        float s = unit;
        // Контактная тень.
        paint.setColor(Color.argb(115, 0, 0, 0));
        c.drawOval(p.x - s * 1.75f, p.y + s * .56f, p.x + s * 1.75f, p.y + s * 1.15f, paint);

        // Нижняя бронированная платформа.
        prism(c, p.x, p.y + s * .45f, s * 1.72f, s * .76f, s * .42f,
                Color.rgb(62, 80, 83), Color.rgb(28, 42, 45), Color.rgb(20, 33, 36));
        int wallTier = Math.min(4, Math.max(0, ((int) gs.baseMaxHp - 100) / 20));
        for (int i = 0; i < wallTier; i++) {
            float side = i % 2 == 0 ? -1 : 1;
            float row = i / 2;
            prism(c, p.x + side * s * (1.55f + row * .22f), p.y + s * (.20f + row * .18f),
                    s * .34f, s * .18f, s * .55f, Color.rgb(86, 99, 101), Color.rgb(41, 55, 58), Color.rgb(29, 43, 46));
        }
        // Центральный бункер.
        prism(c, p.x, p.y - s * .38f, s * 1.05f, s * .51f, s * 1.22f,
                Color.rgb(83, 104, 106), Color.rgb(43, 61, 64), Color.rgb(31, 48, 51));

        // Боковые оранжевые бронепанели.
        paint.setColor(ORANGE);
        quad(c, p.x - s * 1.05f, p.y - s * .12f, p.x - s * .77f, p.y + s * .03f,
                p.x - s * .77f, p.y + s * .78f, p.x - s * 1.05f, p.y + s * .63f, paint);
        paint.setColor(Color.rgb(177, 82, 24));
        quad(c, p.x + s * 1.05f, p.y - s * .12f, p.x + s * .77f, p.y + s * .03f,
                p.x + s * .77f, p.y + s * .78f, p.x + s * 1.05f, p.y + s * .63f, paint);

        // Энергетическая дверь.
        paint.setShader(new LinearGradient(p.x, p.y + s * .24f, p.x, p.y + s * .95f,
                Color.rgb(89, 255, 241), Color.rgb(13, 124, 140), Shader.TileMode.CLAMP));
        diamond(c, p.x, p.y + s * .52f, s * .48f, s * .34f, paint);
        paint.setShader(null);

        // Реактор на крыше со свечением.
        paint.setShader(new RadialGradient(p.x, p.y - s * 1.27f, s * .82f,
                Color.argb(150, 62, 244, 235), Color.TRANSPARENT, Shader.TileMode.CLAMP));
        c.drawCircle(p.x, p.y - s * 1.27f, s * .82f, paint); paint.setShader(null);
        paint.setColor(Color.rgb(15, 28, 31)); c.drawCircle(p.x, p.y - s * 1.25f, s * .48f, paint);
        stroke.setColor(ORANGE); stroke.setStrokeWidth(s * .14f); c.drawCircle(p.x, p.y - s * 1.25f, s * .36f, stroke);
        paint.setColor(CYAN); c.drawCircle(p.x, p.y - s * 1.25f, s * .18f, paint);

        // Индикатор здоровья над базой.
        float health = (float) Math.max(0, Math.min(1, gs.baseHp / gs.baseMaxHp));
        paint.setColor(Color.argb(190, 5, 12, 14)); c.drawRoundRect(p.x - s, p.y - s * 2.02f, p.x + s, p.y - s * 1.86f, s * .08f, s * .08f, paint);
        paint.setColor(health > .3f ? CYAN : RED); c.drawRoundRect(p.x - s, p.y - s * 2.02f,
                p.x - s + s * 2f * health, p.y - s * 1.86f, s * .08f, s * .08f, paint);
    }

    private void drawTurret(Canvas c, GameState.Turret t) {
        PointF p = iso(t.x, t.y);
        float s = unit * .48f;
        paint.setColor(Color.argb(90, 0, 0, 0)); c.drawOval(p.x - s, p.y + s * .72f, p.x + s, p.y + s * 1.08f, paint);
        prism(c, p.x, p.y + s * .42f, s, s * .48f, s * .50f,
                Color.rgb(77, 94, 97), Color.rgb(37, 53, 56), Color.rgb(27, 43, 46));
        int towerTop = t.type == 6 ? Color.rgb(48, 139, 103) : t.type == 5 ? Color.rgb(142, 68, 46)
                : t.type == 4 ? Color.rgb(74, 151, 172) : t.type == 3 ? Color.rgb(113, 73, 145)
                : t.type == 2 ? Color.rgb(71, 84, 132) : Color.rgb(91, 107, 108);
        prism(c, p.x, p.y - s * .10f, s * .72f, s * .35f, s * .68f,
                towerTop, Color.rgb(43, 56, 62), Color.rgb(30, 44, 49));

        float dx = (float) (t.aimX - t.aimY);
        float dy = (float) ((t.aimX + t.aimY) * .5);
        float len = (float) Math.hypot(dx, dy);
        if (len < .01f) len = 1;
        dx /= len; dy /= len;
        stroke.setStrokeWidth(s * .22f); stroke.setColor(Color.rgb(18, 28, 32));
        c.drawLine(p.x, p.y - s * .52f, p.x + dx * s * 1.35f, p.y - s * .52f + dy * s * 1.35f, stroke);
        int energyColor = t.type == 6 ? Color.rgb(104, 255, 165) : t.type == 5 ? Color.rgb(255, 103, 55)
                : t.type == 4 ? Color.rgb(118, 222, 255) : t.type == 3 ? Color.rgb(206, 105, 255) : t.type == 2 ? CYAN : ORANGE;
        stroke.setStrokeWidth(s * .08f); stroke.setColor(energyColor);
        c.drawLine(p.x + dx * s, p.y - s * .52f + dy * s, p.x + dx * s * 1.4f, p.y - s * .52f + dy * s * 1.4f, stroke);
        paint.setColor(energyColor); c.drawCircle(p.x, p.y - s * .75f, s * .16f, paint);
        if (t.type == 6) {
            stroke.setColor(Color.argb(125, 104, 255, 165)); stroke.setStrokeWidth(s * .07f);
            c.drawCircle(p.x, p.y, s * 1.45f, stroke);
        }
        if (t.level > 1) {
            paint.setColor(Color.rgb(8, 19, 22)); c.drawCircle(p.x + s * .72f, p.y - s * .70f, s * .27f, paint);
            paint.setColor(energyColor); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(s * .36f); c.drawText(String.valueOf(t.level), p.x + s * .72f, p.y - s * .58f, paint);
            paint.setTextAlign(Paint.Align.LEFT); paint.setTypeface(null);
        }
    }

    private float enemyScale(int type) {
        if (type == 4) return .72f;
        if (type == 2 || type == 7 || type == 8) return .48f;
        if (type == 1 || type == 12) return .32f;
        if (type == 6) return .35f;
        if (type == 10) return .43f;
        return .38f;
    }

    private int enemyBodyColor(int type) {
        int[] colors = {Color.rgb(67,94,76), Color.rgb(72,115,86), Color.rgb(68,78,83), Color.rgb(75,91,43),
                Color.rgb(103,48,52), Color.rgb(61,128,76), Color.rgb(73,105,116), Color.rgb(48,91,121),
                Color.rgb(91,52,111), Color.rgb(49,105,102), Color.rgb(124,52,38), Color.rgb(105,76,48), Color.rgb(130,108,42)};
        return colors[Math.max(0, Math.min(colors.length - 1, type))];
    }

    private int enemyArmorColor(int type) {
        if (type == 4) return Color.rgb(175,52,43);
        if (type == 3 || type == 5) return Color.rgb(150,174,43);
        if (type == 2) return Color.rgb(105,120,126);
        if (type == 7) return Color.rgb(70,156,203);
        if (type == 8) return Color.rgb(166,80,198);
        if (type == 10) return Color.rgb(235,80,42);
        return ORANGE;
    }

    private void drawZombie(Canvas c, GameState.Zombie z) {
        PointF p = iso(z.x, z.y);
        float scale = enemyScale(z.type);
        float s = unit * scale;
        int body = enemyBodyColor(z.type);
        int armor = enemyArmorColor(z.type);
        paint.setColor(Color.argb(100, 0, 0, 0)); c.drawOval(p.x - s, p.y + s * .55f, p.x + s, p.y + s * .9f, paint);
        stroke.setStrokeWidth(s * .34f); stroke.setColor(Color.rgb(29, 43, 48));
        c.drawLine(p.x - s * .18f, p.y + s * .18f, p.x - s * .32f, p.y + s * .72f, stroke);
        c.drawLine(p.x + s * .18f, p.y + s * .18f, p.x + s * .32f, p.y + s * .72f, stroke);
        paint.setColor(body); diamond(c, p.x, p.y - s * .18f, s * .66f, s * .58f, paint);
        paint.setColor(armor);
        triangle(c, p.x - s * .48f, p.y - s * .24f, p.x, p.y + s * .38f, p.x, p.y - s * .07f, paint);
        paint.setColor(z.type == 3 ? Color.rgb(85, 114, 35) : Color.rgb(137, 67, 28));
        triangle(c, p.x + s * .48f, p.y - s * .24f, p.x, p.y + s * .38f, p.x, p.y - s * .07f, paint);
        prism(c, p.x, p.y - s * 1.12f, s * .48f, s * .26f, s * .65f,
                z.type == 4 ? Color.rgb(148, 70, 68) : z.type == 3 ? Color.rgb(128, 153, 68) : Color.rgb(111, 144, 103),
                Color.rgb(74, 108, 82), Color.rgb(56, 88, 69));
        paint.setColor(z.type == 3 ? Color.rgb(191, 255, 56) : Color.rgb(210, 255, 105));
        c.drawCircle(p.x - s * .18f, p.y - s * .93f, s * .10f, paint);
        paint.setColor(RED); c.drawCircle(p.x + s * .20f, p.y - s * .93f, s * .11f, paint);
        if (z.type == 6) {
            paint.setColor(Color.rgb(91, 128, 139));
            triangle(c, p.x - s * .45f, p.y - s * .25f, p.x - s * 1.15f, p.y - s * .75f, p.x - s * .75f, p.y + s * .2f, paint);
            triangle(c, p.x + s * .45f, p.y - s * .25f, p.x + s * 1.15f, p.y - s * .75f, p.x + s * .75f, p.y + s * .2f, paint);
        }
        if (z.type == 7) {
            stroke.setColor(Color.argb(170, 80, 195, 255)); stroke.setStrokeWidth(s * .08f); c.drawCircle(p.x, p.y - s * .35f, s * 1.15f, stroke);
        }
        if (z.type == 8) {
            stroke.setColor(Color.rgb(193, 96, 255)); stroke.setStrokeWidth(s * .08f); c.drawCircle(p.x, p.y - s * 1.15f, s * .55f, stroke);
        }
        if (z.type == 10) { paint.setColor(Color.rgb(255, 70, 42)); c.drawCircle(p.x, p.y - s * .1f, s * .28f, paint); }
        if (z.eliteModifier > 0) {
            int eliteColor = z.eliteModifier == 1 ? Color.rgb(230, 64, 86) : z.eliteModifier == 2 ? Color.rgb(255, 205, 62)
                    : z.eliteModifier == 3 ? Color.rgb(125, 185, 210) : Color.rgb(213, 126, 255);
            stroke.setColor(Color.argb(190, Color.red(eliteColor), Color.green(eliteColor), Color.blue(eliteColor)));
            stroke.setStrokeWidth(s * .06f); c.drawCircle(p.x, p.y - s * .4f, s * 1.05f, stroke);
        }
        if (z.type == 2 || z.type == 4) {
            stroke.setColor(z.type == 4 ? RED : Color.rgb(157, 185, 190)); stroke.setStrokeWidth(s * .10f);
            c.drawLine(p.x - s * .58f, p.y - s * .45f, p.x + s * .58f, p.y - s * .45f, stroke);
        }
        if (z.type != 0 || z.hp < z.maxHp) {
            float health = (float) Math.max(0, z.hp / z.maxHp);
            paint.setColor(Color.rgb(9, 16, 18)); c.drawRect(p.x - s, p.y - s * 1.72f, p.x + s, p.y - s * 1.57f, paint);
            paint.setColor(z.type == 4 ? RED : Color.rgb(102, 224, 109));
            c.drawRect(p.x - s, p.y - s * 1.72f, p.x - s + 2 * s * health, p.y - s * 1.57f, paint);
        }
        if (z.slowTimer > 0) {
            stroke.setColor(Color.argb(190, 118, 222, 255)); stroke.setStrokeWidth(s * .08f);
            c.drawCircle(p.x, p.y - s * .35f, s * .92f, stroke);
        }
        if (z.burnTimer > 0) {
            paint.setColor(Color.argb(190, 255, 86, 28));
            triangle(c, p.x - s * .35f, p.y + s * .5f, p.x, p.y - s * .15f, p.x + s * .2f, p.y + s * .5f, paint);
        }
        if (z.acidTimer > 0) {
            stroke.setColor(Color.argb(200, 151, 255, 68)); stroke.setStrokeWidth(s * .07f);
            c.drawCircle(p.x, p.y - s * .25f, s * .75f, stroke);
        }
        if (z == heldTarget && aimingAtEnemy && System.currentTimeMillis() - touchDownAt >= 650) {
            float r = s * 1.15f;
            stroke.setColor(Color.rgb(255, 210, 72)); stroke.setStrokeWidth(s * .055f);
            c.drawCircle(p.x, p.y - s * .55f, r, stroke);
            c.drawLine(p.x - r * 1.25f, p.y - s * .55f, p.x - r * .55f, p.y - s * .55f, stroke);
            c.drawLine(p.x + r * .55f, p.y - s * .55f, p.x + r * 1.25f, p.y - s * .55f, stroke);
        }
    }

    private void drawCombatDrone(Canvas c) {
        if (!gs.combatDroneUnlocked()) return;
        PointF p = iso(-.7, .7);
        float bob = (float) Math.sin(System.currentTimeMillis() * .006) * unit * .08f;
        float r = unit * .24f;
        paint.setColor(Color.argb(80, 70, 255, 220)); c.drawCircle(p.x, p.y - unit * 1.2f + bob, r * 2.3f, paint);
        paint.setColor(Color.rgb(42, 72, 76)); diamond(c, p.x, p.y - unit * 1.2f + bob, r * 1.6f, r * .7f, paint);
        paint.setColor(Color.rgb(100, 255, 220)); c.drawCircle(p.x, p.y - unit * 1.2f + bob, r * .42f, paint);
    }

    private void drawProjectiles(Canvas c) {
        for (GameState.Projectile p : gs.projectiles) {
            PointF q = iso(p.x, p.y);
            float r = unit * (p.type == 3 || p.type == 13 ? .18f : .13f);
            int glow = projectileColor(p.type);
            paint.setShader(new RadialGradient(q.x, q.y - unit * .18f, r * 3f,
                    glow, Color.TRANSPARENT, Shader.TileMode.CLAMP));
            c.drawCircle(q.x, q.y - unit * .18f, r * 3f, paint); paint.setShader(null);
            paint.setColor(Color.WHITE); c.drawCircle(q.x, q.y - unit * .18f, r, paint);
            if (p.critical || p.headshot) {
                stroke.setColor(p.headshot ? Color.rgb(255, 72, 72) : Color.rgb(255, 220, 80));
                stroke.setStrokeWidth(unit * .045f);
                c.drawCircle(q.x, q.y - unit * .18f, r * 2.2f, stroke);
            }
        }
    }

    private void drawHitEffects(Canvas c) {
        for (GameState.HitEffect effect : gs.hitEffects) {
            PointF p = iso(effect.x, effect.y);
            float progress = (float) (effect.life / .28);
            float radius = unit * (.18f + (1f - progress) * .55f);
            int color = projectileColor(effect.type);
            stroke.setColor(Color.argb((int) (220 * progress), Color.red(color), Color.green(color), Color.blue(color)));
            stroke.setStrokeWidth(unit * .07f * progress);
            c.drawCircle(p.x, p.y - unit * .25f, radius, stroke);
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 2 + (1 - progress);
                c.drawLine(p.x, p.y - unit * .25f,
                        p.x + (float) Math.cos(angle) * radius * 1.4f,
                        p.y - unit * .25f + (float) Math.sin(angle) * radius * .8f, stroke);
            }
        }
    }

    private int projectileColor(int type) {
        if (type == 20) return Color.rgb(100, 255, 220);
        if (type == 19) return Color.rgb(150, 255, 70);
        if (type == 18) return Color.rgb(205, 112, 255);
        if (type == 17) return Color.rgb(116, 222, 255);
        if (type == 16) return Color.rgb(255, 92, 38);
        if (type == 15) return Color.rgb(255, 150, 42);
        if (type == 14) return Color.rgb(255, 75, 75);
        if (type == 13) return Color.WHITE;
        if (type == 12) return Color.rgb(160, 255, 104);
        if (type == 11) return Color.rgb(255, 187, 65);
        if (type == 10) return Color.rgb(255, 231, 118);
        if (type == 4) return Color.rgb(118, 222, 255);
        if (type == 3) return Color.rgb(211, 104, 255);
        return type == 2 ? CYAN : ORANGE;
    }

    private void drawWeatherOverlay(Canvas c, int w, int h) {
        int weather = gs.weatherType();
        if (weather == 0) return;
        int color = weather == 1 ? Color.argb(35, 190, 210, 205) : weather == 2 ? Color.argb(28, 145, 210, 65)
                : weather == 3 ? Color.argb(30, 105, 205, 255) : Color.argb(25, 185, 105, 255);
        paint.setColor(color); c.drawRect(0, 0, w, h, paint);
        stroke.setStrokeWidth(unit * .025f);
        stroke.setColor(weather == 2 ? Color.argb(110, 180, 255, 80) : weather == 3 ? Color.argb(100, 190, 240, 255) : Color.argb(90, 210, 150, 255));
        for (int i = 0; i < 18; i++) {
            float x = (i * 97 + System.currentTimeMillis() / 25) % Math.max(1, w);
            float y = (i * 151 + System.currentTimeMillis() / 18) % Math.max(1, h);
            c.drawLine(x, y, x + (weather == 4 ? unit * .3f : 0), y + unit * .35f, stroke);
        }
    }

    private void drawVignette(Canvas c, int w, int h) {
        float radius = Math.max(w, h) * .72f;
        paint.setShader(new RadialGradient(w * .5f, h * .52f, radius,
                new int[]{Color.TRANSPARENT, Color.argb(20, 0, 0, 0), Color.argb(155, 0, 0, 0)},
                new float[]{0f, .62f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, paint); paint.setShader(null);
    }

    private void prism(Canvas c, float x, float y, float halfW, float halfH, float height,
                       int top, int left, int right) {
        paint.setColor(left);
        quad(c, x - halfW, y, x, y + halfH, x, y + halfH + height, x - halfW, y + height, paint);
        paint.setColor(right);
        quad(c, x, y + halfH, x + halfW, y, x + halfW, y + height, x, y + halfH + height, paint);
        paint.setColor(top);
        quad(c, x, y - halfH, x + halfW, y, x, y + halfH, x - halfW, y, paint);
        stroke.setColor(Color.rgb(12, 21, 23)); stroke.setStrokeWidth(Math.max(1.5f, unit * .035f));
        c.drawPath(path, stroke);
    }

    private void diamond(Canvas c, float x, float y, float halfW, float halfH, Paint p) {
        quad(c, x, y - halfH, x + halfW, y, x, y + halfH, x - halfW, y, p);
    }

    private void quad(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3,
                      float x4, float y4, Paint p) {
        path.reset(); path.moveTo(x1, y1); path.lineTo(x2, y2); path.lineTo(x3, y3);
        path.lineTo(x4, y4); path.close(); c.drawPath(path, p);
    }

    private void triangle(Canvas c, float x1, float y1, float x2, float y2, float x3, float y3, Paint p) {
        path.reset(); path.moveTo(x1, y1); path.lineTo(x2, y2); path.lineTo(x3, y3); path.close(); c.drawPath(path, p);
    }

    private static class Actor {
        final double x, y;
        final int type;
        final Object data;
        Actor(double x, double y, int type, Object data) {
            this.x = x; this.y = y; this.type = type; this.data = data;
        }
    }
}
