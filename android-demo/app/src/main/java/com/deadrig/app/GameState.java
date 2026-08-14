package com.deadrig.app;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Вся игровая логика DeadRig (Android-демо): экономика, волны зомби, турели,
 * исследования и крафт. Портировано 1:1 с Unity-ядра (DeadRig.Core).
 */
public class GameState {

    // --- экономика ---
    public double hashes = 0;
    public double scrap = 0;
    public long crystals = 0;
    public int minerLevel = 1;
    public int turretLevel = 1;

    // --- бонусы исследований ---
    public double miningMult = 0;
    public double turretMult = 0;
    public double scrapMult = 0;

    // --- база ---
    public double baseHp = 100;
    public double baseMaxHp = 100;

    // --- волны ---
    public int wave = 0;
    public boolean waveActive = false;
    private int zombiesToSpawn = 0;
    private double spawnTimer = 0;
    private double nextWaveTimer = 2.0;

    // --- сущности ---
    public final List<Zombie> zombies = new ArrayList<>();
    public final List<Projectile> projectiles = new ArrayList<>();
    public final List<Turret> turrets = new ArrayList<>();

    private int basicTurrets = 0;
    private int laserTurrets = 0;

    // --- исследования/крафт ---
    public final List<String> doneResearch = new ArrayList<>();
    public String activeResearchId = null;
    private double activeResearchLeft = 0;
    public String activeCraftId = null;
    private double activeCraftLeft = 0;

    public boolean gameOver = false;

    private static final double OFFLINE_CAP = 8 * 3600.0;
    private final SharedPreferences prefs;

    public GameState(SharedPreferences prefs) {
        this.prefs = prefs;
        load();
    }

    // ===== формулы =====
    public double miningRate() { return 1.0 * minerLevel * (1 + miningMult); }
    public double minerUpgradeCost() { return 10.0 * Math.pow(1.5, minerLevel - 1); }
    public double turretUpgradeCost() { return 15.0 * Math.pow(1.6, turretLevel - 1); }
    public int turretCount() { return 1 + basicTurrets + laserTurrets; }

    // ===== действия игрока (возвращают null при успехе, иначе текст для тоста) =====
    public String tryUpgradeMiner() {
        double c = minerUpgradeCost();
        if (hashes < c) return "Не хватает хешей (нужно " + fmt(c) + ")";
        hashes -= c; minerLevel++;
        return null;
    }

    public String tryUpgradeTurret() {
        double c = turretUpgradeCost();
        if (hashes < c) return "Не хватает хешей (нужно " + fmt(c) + ")";
        hashes -= c; turretLevel++;
        return null;
    }

    public String tryStartWave() {
        if (waveActive) return "Волна уже идёт";
        startWave();
        return null;
    }

    public String tryStartResearch() {
        if (activeResearchId != null) return "Исследование уже идёт";
        boolean any = false;
        for (Defs.ResearchDef d : Defs.RESEARCH) {
            if (doneResearch.contains(d.id)) continue;
            any = true;
            if (d.requiresId != null && !doneResearch.contains(d.requiresId)) continue;
            if (hashes < d.costHashes || scrap < d.costScrap) continue;
            hashes -= d.costHashes; scrap -= d.costScrap;
            activeResearchId = d.id; activeResearchLeft = d.durationSec;
            return "Начато: " + d.name;
        }
        if (any) return "Не хватает ресурсов";
        return "Всё изучено";
    }

    public String tryStartCraft() {
        if (activeCraftId != null) return "Производство уже идёт";
        boolean anyUnlocked = false;
        for (Defs.RecipeDef r : Defs.RECIPES) {
            if (!isUnlocked(r)) continue;
            anyUnlocked = true;
            if (hashes < r.costHashes || scrap < r.costScrap) continue;
            hashes -= r.costHashes; scrap -= r.costScrap;
            activeCraftId = r.id; activeCraftLeft = r.durationSec;
            return "Производство: " + r.name;
        }
        if (anyUnlocked) return "Не хватает ресурсов";
        return "Нет рецептов";
    }

    public boolean isUnlocked(Defs.RecipeDef r) {
        return r.requiresResearchId == null || doneResearch.contains(r.requiresResearchId);
    }

    // ===== главный тик =====
    public void update(double dt) {
        if (gameOver) return;
        hashes += miningRate() * dt;

        // движение зомби и урон базе
        for (int i = zombies.size() - 1; i >= 0; i--) {
            Zombie z = zombies.get(i);
            double dx = -z.x, dy = -z.y;
            double d = Math.hypot(dx, dy);
            if (d < 0.7) {
                baseHp -= z.damage;
                zombies.remove(i);
                if (baseHp <= 0) { baseHp = 0; gameOver = true; }
                continue;
            }
            z.x += dx / d * z.speed * dt;
            z.y += dy / d * z.speed * dt;
        }

        // спавн и очистка волн
        if (waveActive) {
            spawnTimer -= dt;
            if (spawnTimer <= 0 && zombiesToSpawn > 0) {
                spawnZombie();
                zombiesToSpawn--;
                spawnTimer = 1.0;
            }
            if (zombiesToSpawn <= 0 && zombies.isEmpty()) {
                waveActive = false;
                hashes += 20.0 * wave;
                scrap += 10.0 * wave * (1 + scrapMult);
                nextWaveTimer = 5.0;
            }
        } else {
            nextWaveTimer -= dt;
            if (nextWaveTimer <= 0) startWave();
        }

        // турели
        syncTurrets();
        for (Turret t : turrets) {
            t.cooldown -= dt;
            if (t.cooldown <= 0) {
                Zombie target = nearestZombie(t.x, t.y, 9.0);
                if (target != null) {
                    double dx = target.x - t.x, dy = target.y - t.y;
                    double d = Math.hypot(dx, dy);
                    t.aimX = dx / d; t.aimY = dy / d;
                    double dmg = 10.0 * turretLevel * (1 + turretMult) * (t.laser ? 2 : 1);
                    projectiles.add(new Projectile(t.x, t.y, target, dmg, 14.0));
                    t.cooldown = 0.7;
                } else {
                    t.cooldown = 0.1;
                }
            }
        }

        // снаряды
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            if (!zombies.contains(p.target)) { projectiles.remove(i); continue; }
            double dx = p.target.x - p.x, dy = p.target.y - p.y;
            double d = Math.hypot(dx, dy);
            if (d < 0.35) {
                p.target.hp -= p.damage;
                projectiles.remove(i);
                if (p.target.hp <= 0) zombies.remove(p.target);
                continue;
            }
            p.x += dx / d * p.speed * dt;
            p.y += dy / d * p.speed * dt;
        }

        // исследование
        if (activeResearchId != null) {
            activeResearchLeft -= dt;
            if (activeResearchLeft <= 0) completeResearch();
        }

        // крафт
        if (activeCraftId != null) {
            activeCraftLeft -= dt;
            if (activeCraftLeft <= 0) completeCraft();
        }
    }

    private void startWave() {
        wave++;
        zombiesToSpawn = 5 + wave * 3;
        spawnTimer = 0.5;
        waveActive = true;
    }

    private void spawnZombie() {
        double sx = (Math.random() < 0.5 ? -14 : 14);
        double sy = (Math.random() < 0.5 ? -14 : 14);
        double hp = 10.0 * Math.pow(1.25, wave - 1);
        zombies.add(new Zombie(sx, sy, hp, 2.2, 8.0));
    }

    private Zombie nearestZombie(double x, double y, double range) {
        Zombie best = null; double bd = range;
        for (Zombie z : zombies) {
            double d = Math.hypot(z.x - x, z.y - y);
            if (d <= bd) { bd = d; best = z; }
        }
        return best;
    }

    private void syncTurrets() {
        int want = turretCount();
        while (turrets.size() < want) turrets.add(new Turret());
        while (turrets.size() > want) turrets.remove(turrets.size() - 1);
        double r = 2.4;
        for (int i = 0; i < turrets.size(); i++) {
            Turret t = turrets.get(i);
            double a = i * 2 * Math.PI / turrets.size() + 0.5;
            t.x = Math.cos(a) * r;
            t.y = Math.sin(a) * r;
            t.laser = i > basicTurrets;
        }
    }

    private void completeResearch() {
        Defs.ResearchDef d = Defs.findResearch(activeResearchId);
        doneResearch.add(activeResearchId);
        if (d != null) {
            if (d.effectType == 0) miningMult += d.value;
            else if (d.effectType == 1) turretMult += d.value;
            else if (d.effectType == 2) scrapMult += d.value;
        }
        activeResearchId = null;
    }

    private void completeCraft() {
        Defs.RecipeDef r = Defs.findRecipe(activeCraftId);
        if (r != null) {
            if (r.outItem.equals("turret_basic")) basicTurrets++;
            else if (r.outItem.equals("turret_laser")) laserTurrets++;
            else if (r.outItem.equals("wall")) {
                baseMaxHp += 20;
                baseHp = Math.min(baseMaxHp, baseHp + 20);
            }
        }
        activeCraftId = null;
    }

    // ===== сохранение/загрузка =====
    public void save() {
        try {
            JSONObject o = new JSONObject();
            o.put("hashes", hashes);
            o.put("scrap", scrap);
            o.put("crystals", crystals);
            o.put("minerLevel", minerLevel);
            o.put("turretLevel", turretLevel);
            o.put("miningMult", miningMult);
            o.put("turretMult", turretMult);
            o.put("scrapMult", scrapMult);
            o.put("baseHp", baseHp);
            o.put("baseMaxHp", baseMaxHp);
            o.put("wave", wave);
            o.put("basicTurrets", basicTurrets);
            o.put("laserTurrets", laserTurrets);
            o.put("lastSeen", System.currentTimeMillis());
            JSONArray arr = new JSONArray();
            for (String s : doneResearch) arr.put(s);
            o.put("doneResearch", arr);
            prefs.edit().putString("save", o.toString()).apply();
        } catch (Exception ignored) { }
    }

    private void load() {
        String s = prefs.getString("save", null);
        if (s == null) return;
        try {
            JSONObject o = new JSONObject(s);
            hashes = o.getDouble("hashes");
            scrap = o.getDouble("scrap");
            crystals = o.getLong("crystals");
            minerLevel = o.getInt("minerLevel");
            turretLevel = o.getInt("turretLevel");
            miningMult = o.optDouble("miningMult", 0);
            turretMult = o.optDouble("turretMult", 0);
            scrapMult = o.optDouble("scrapMult", 0);
            baseHp = o.getDouble("baseHp");
            baseMaxHp = o.getDouble("baseMaxHp");
            wave = o.getInt("wave");
            basicTurrets = o.getInt("basicTurrets");
            laserTurrets = o.getInt("laserTurrets");
            long ls = o.getLong("lastSeen");
            JSONArray arr = o.optJSONArray("doneResearch");
            doneResearch.clear();
            if (arr != null) for (int i = 0; i < arr.length(); i++) doneResearch.add(arr.getString(i));
            long now = System.currentTimeMillis();
            double elapsed = (now - ls) / 1000.0;
            if (elapsed > 0) {
                double gain = miningRate() * Math.min(elapsed, OFFLINE_CAP) * 0.5;
                if (gain > 0) hashes += gain;
            }
        } catch (Exception ignored) { }
    }

    public void reset() {
        hashes = 0; scrap = 0; crystals = 0; minerLevel = 1; turretLevel = 1;
        miningMult = 0; turretMult = 0; scrapMult = 0;
        baseHp = 100; baseMaxHp = 100;
        wave = 0; waveActive = false; zombiesToSpawn = 0; spawnTimer = 0; nextWaveTimer = 2.0;
        zombies.clear(); projectiles.clear(); turrets.clear();
        basicTurrets = 0; laserTurrets = 0;
        doneResearch.clear(); activeResearchId = null; activeCraftId = null;
        activeResearchLeft = 0; activeCraftLeft = 0;
        gameOver = false;
        prefs.edit().clear().apply();
    }

    // ===== статус для HUD =====
    public String researchStatus() {
        if (activeResearchId != null) {
            Defs.ResearchDef d = Defs.findResearch(activeResearchId);
            return d.name + " (" + (int) Math.ceil(activeResearchLeft) + "с)";
        }
        for (Defs.ResearchDef d : Defs.RESEARCH)
            if (!doneResearch.contains(d.id)) return d.name + " (доступно)";
        return "всё изучено";
    }

    public String craftStatus() {
        if (activeCraftId != null) {
            Defs.RecipeDef r = Defs.findRecipe(activeCraftId);
            return r.name + " (" + (int) Math.ceil(activeCraftLeft) + "с)";
        }
        for (Defs.RecipeDef r : Defs.RECIPES)
            if (isUnlocked(r)) return r.name + " (доступно)";
        return "нет рецептов";
    }

    public static String fmt(double v) {
        if (v >= 1e9) return String.format(Locale.US, "%.1fB", v / 1e9);
        if (v >= 1e6) return String.format(Locale.US, "%.1fM", v / 1e6);
        if (v >= 1e3) return String.format(Locale.US, "%.1fK", v / 1e3);
        return String.valueOf((long) v);
    }

    // ===== сущности =====
    public static class Zombie {
        double x, y, hp, speed, damage;
        Zombie(double x, double y, double hp, double speed, double damage) {
            this.x = x; this.y = y; this.hp = hp; this.speed = speed; this.damage = damage;
        }
    }

    public static class Projectile {
        double x, y, damage, speed;
        Zombie target;
        Projectile(double x, double y, Zombie t, double dmg, double spd) {
            this.x = x; this.y = y; target = t; damage = dmg; speed = spd;
        }
    }

    public static class Turret {
        double x, y, cooldown, aimX = 1, aimY = 0;
        boolean laser;
    }
}
