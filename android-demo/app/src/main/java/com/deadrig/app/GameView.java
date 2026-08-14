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
            else if (gs.manualShoot(heldTarget, false))
                performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
        }

        int w = getWidth(), h = getHeight();
        unit = Math.min(w / 18f, h / 30f);
        cx = w * 0.5f;
        cy = h * 0.53f;

        drawBackground(c, w, h);
        drawArena(c);
        drawRoutesAndSlots(c);
        drawSpawnRifts(c);
        drawSortedActors(c);
        drawProjectiles(c);
        drawHitEffects(c);
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

    /** Объёмная дорога tower defense и фиксированные монтажные площадки. */
    private void drawRoutesAndSlots(Canvas c) {
        for (double[][] route : GameState.PATHS) {
            path.reset();
            PointF first = iso(route[0][0], route[0][1]);
            path.moveTo(first.x, first.y);
            for (int i = 1; i < route.length; i++) {
                PointF point = iso(route[i][0], route[i][1]);
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
            touchDownAt = System.currentTimeMillis();
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_MOVE && aimingAtEnemy) {
            GameState.Zombie movedTarget = touchedZombie(event.getX(), event.getY());
            if (movedTarget != null) heldTarget = movedTarget;
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            aimingAtEnemy = false; heldTarget = null; return true;
        }
        if (event.getAction() != MotionEvent.ACTION_UP) return true;

        if (aimingAtEnemy) {
            boolean shortTap = System.currentTimeMillis() - touchDownAt < 220;
            if (heldTarget != null && gs.manualShoot(heldTarget, shortTap))
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
            float vertical = zombie.type == 4 ? unit * .9f : unit * .45f;
            float distance = (float) Math.hypot(x - p.x, y - (p.y - vertical));
            if (distance < bestDistance) { bestDistance = distance; best = zombie; }
        }
        return best;
    }

    private void drawSpawnRifts(Canvas c) {
        drawRift(c, -7.2, 0);
        drawRift(c, 0, -7.2);
        drawRift(c, 7.2, 0);
        drawRift(c, 0, 7.2);
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
        int towerTop = t.type == 4 ? Color.rgb(74, 151, 172) : t.type == 3 ? Color.rgb(113, 73, 145)
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
        int energyColor = t.type == 4 ? Color.rgb(118, 222, 255) : t.type == 3 ? Color.rgb(206, 105, 255) : t.type == 2 ? CYAN : ORANGE;
        stroke.setStrokeWidth(s * .08f); stroke.setColor(energyColor);
        c.drawLine(p.x + dx * s, p.y - s * .52f + dy * s, p.x + dx * s * 1.4f, p.y - s * .52f + dy * s * 1.4f, stroke);
        paint.setColor(energyColor); c.drawCircle(p.x, p.y - s * .75f, s * .16f, paint);
        if (t.level > 1) {
            paint.setColor(Color.rgb(8, 19, 22)); c.drawCircle(p.x + s * .72f, p.y - s * .70f, s * .27f, paint);
            paint.setColor(energyColor); paint.setTextAlign(Paint.Align.CENTER); paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            paint.setTextSize(s * .36f); c.drawText(String.valueOf(t.level), p.x + s * .72f, p.y - s * .58f, paint);
            paint.setTextAlign(Paint.Align.LEFT); paint.setTypeface(null);
        }
    }

    private void drawZombie(Canvas c, GameState.Zombie z) {
        PointF p = iso(z.x, z.y);
        float scale = z.type == 4 ? .72f : z.type == 2 ? .48f : z.type == 1 ? .32f : .38f;
        float s = unit * scale;
        int body = z.type == 4 ? Color.rgb(103, 48, 52) : z.type == 3 ? Color.rgb(75, 91, 43)
                : z.type == 2 ? Color.rgb(68, 78, 83) : z.type == 1 ? Color.rgb(72, 115, 86) : Color.rgb(67, 94, 76);
        int armor = z.type == 4 ? Color.rgb(175, 52, 43) : z.type == 3 ? Color.rgb(150, 174, 43)
                : z.type == 2 ? Color.rgb(105, 120, 126) : ORANGE;
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
    }

    private void drawProjectiles(Canvas c) {
        for (GameState.Projectile p : gs.projectiles) {
            PointF q = iso(p.x, p.y);
            float r = unit * (p.type == 3 || p.type == 13 ? .18f : .13f);
            int glow = p.type == 13 ? Color.rgb(220, 246, 255) : p.type == 12 ? Color.rgb(160, 255, 104)
                    : p.type == 11 ? Color.rgb(255, 187, 65) : p.type == 10 ? Color.rgb(255, 231, 118)
                    : p.type == 4 ? Color.rgb(118, 222, 255) : p.type == 3 ? Color.rgb(212, 104, 255) : p.type == 2 ? CYAN : ORANGE;
            paint.setShader(new RadialGradient(q.x, q.y - unit * .18f, r * 3f,
                    glow, Color.TRANSPARENT, Shader.TileMode.CLAMP));
            c.drawCircle(q.x, q.y - unit * .18f, r * 3f, paint); paint.setShader(null);
            paint.setColor(Color.WHITE); c.drawCircle(q.x, q.y - unit * .18f, r, paint);
        }
    }

    private void drawHitEffects(Canvas c) {
        for (GameState.HitEffect effect : gs.hitEffects) {
            PointF p = iso(effect.x, effect.y);
            float progress = (float) (effect.life / .28);
            float radius = unit * (.18f + (1f - progress) * .55f);
            int color = effect.type == 13 ? Color.WHITE : effect.type == 12 ? Color.rgb(160, 255, 104)
                    : effect.type >= 10 ? Color.rgb(255, 205, 83)
                    : effect.type == 4 ? Color.rgb(118, 222, 255) : effect.type == 3 ? Color.rgb(211, 104, 255) : effect.type == 2 ? CYAN : ORANGE;
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
