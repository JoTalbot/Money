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

    /** Извилистые маршруты из четырёх порталов к ядру базы. */
    public static final double[][][] PATHS = {
            {{-7.2, 0}, {-5.7, -1.7}, {-3.6, -1.5}, {-2.7, 1.0}, {-1.2, 1.5}, {0, 0}},
            {{0, -7.2}, {1.8, -5.6}, {1.6, -3.6}, {-1.0, -2.5}, {-1.5, -1.1}, {0, 0}},
            {{7.2, 0}, {5.7, 1.7}, {3.6, 1.5}, {2.7, -1.0}, {1.2, -1.5}, {0, 0}},
            {{0, 7.2}, {-1.8, 5.6}, {-1.6, 3.6}, {1.0, 2.5}, {1.5, 1.1}, {0, 0}}
    };

    /** Фиксированные площадки строительства вдоль маршрутов. */
    public static final double[][] TOWER_SLOTS = {
            {-5.0, 0.2}, {-3.6, -2.6}, {0.1, -5.0}, {2.7, -3.4},
            {5.0, -0.2}, {3.6, 2.6}, {-0.1, 5.0}, {-2.7, 3.4}
    };
    private final int[] placedTowerTypes = new int[TOWER_SLOTS.length]; // 0 пусто, 1 пулемёт, 2 лазер, 3 тесла
    private int basicTurrets = 0;
    private int laserTurrets = 0;
    private int teslaTurrets = 0;
    private double fireRateMult = 0;
    private double rangeBonus = 0;

    // --- исследования/крафт ---
    public final List<String> doneResearch = new ArrayList<>();
    public String activeResearchId = null;
    private double activeResearchLeft = 0;
    public String activeCraftId = null;
    private double activeCraftLeft = 0;

    public double activeResearchSeconds() { return Math.max(0, activeResearchLeft); }
    public double activeCraftSeconds() { return Math.max(0, activeCraftLeft); }
    public double turretDamageBonus() { return turretMult; }
    public double turretFireRateBonus() { return fireRateMult; }
    public double turretRangeBonus() { return rangeBonus; }

    public boolean gameOver = false;

    private static final double OFFLINE_CAP = 8 * 3600.0;
    private final SharedPreferences prefs;

    public GameState(SharedPreferences prefs) {
        this.prefs = prefs;
        load();
        if (placedTowerCount() == 0) placedTowerTypes[0] = 1;
        syncTurrets();
    }

    // ===== формулы =====
    public double miningRate() { return 1.0 * minerLevel * (1 + miningMult); }
    public double minerUpgradeCost() { return 10.0 * Math.pow(1.5, minerLevel - 1); }
    public double turretUpgradeCost() { return 15.0 * Math.pow(1.6, turretLevel - 1); }
    public int turretCount() { return placedTowerCount(); }
    public int pendingTowerCount() {
        return Math.max(0, 1 + basicTurrets + laserTurrets + teslaTurrets - placedTowerCount());
    }
    public int towerTypeAt(int slot) {
        return slot >= 0 && slot < placedTowerTypes.length ? placedTowerTypes[slot] : 0;
    }
    private int placedTowerCount() {
        int count = 0;
        for (int type : placedTowerTypes) if (type != 0) count++;
        return count;
    }

    /** Ставит лучшую доступную башню на выбранную фиксированную площадку. */
    public String tryPlaceTower(int slot) {
        if (slot < 0 || slot >= placedTowerTypes.length) return "Неверная площадка";
        if (placedTowerTypes[slot] != 0) return "Площадка уже занята";
        int placedBasic = 0, placedLaser = 0, placedTesla = 0;
        for (int type : placedTowerTypes) {
            if (type == 1) placedBasic++;
            else if (type == 2) placedLaser++;
            else if (type == 3) placedTesla++;
        }
        int type = 0;
        if (placedTesla < teslaTurrets) type = 3;
        else if (placedLaser < laserTurrets) type = 2;
        else if (placedBasic < 1 + basicTurrets) type = 1;
        if (type == 0) return "Сначала создайте башню в цехе";
        placedTowerTypes[slot] = type;
        syncTurrets();
        return type == 3 ? "Тесла-башня установлена" : type == 2 ? "Лазерная башня установлена" : "Башня установлена";
    }

    // ===== действия игрока (всегда возвращают явный результат для интерфейса) =====
    public String tryUpgradeMiner() {
        double c = minerUpgradeCost();
        if (hashes < c) return "Не хватает хешей (нужно " + fmt(c) + ")";
        hashes -= c; minerLevel++;
        return "Ферма улучшена до уровня " + minerLevel;
    }

    public String tryUpgradeTurret() {
        double c = turretUpgradeCost();
        if (hashes < c) return "Не хватает хешей (нужно " + fmt(c) + ")";
        hashes -= c; turretLevel++;
        return "Оборона улучшена до уровня " + turretLevel;
    }

    public String tryStartWave() {
        if (waveActive) return "Волна уже идёт";
        startWave();
        return "Волна " + wave + " запущена";
    }

    public String tryStartResearch() {
        for (Defs.ResearchDef def : Defs.RESEARCH)
            if (!doneResearch.contains(def.id) && (def.requiresId == null || doneResearch.contains(def.requiresId)))
                return tryStartResearch(def.id);
        return "Всё изучено";
    }

    /** Запускает выбранный игроком проект из экрана науки. */
    public String tryStartResearch(String id) {
        if (activeResearchId != null) return "Исследование уже идёт";
        Defs.ResearchDef def = Defs.findResearch(id);
        if (def == null) return "Проект не найден";
        if (doneResearch.contains(id)) return "Уже изучено";
        if (def.requiresId != null && !doneResearch.contains(def.requiresId))
            return "Сначала изучите: " + Defs.findResearch(def.requiresId).name;
        if (hashes < def.costHashes || scrap < def.costScrap)
            return "Нужно " + fmt(def.costHashes) + " хешей и " + fmt(def.costScrap) + " лома";
        hashes -= def.costHashes; scrap -= def.costScrap;
        activeResearchId = def.id; activeResearchLeft = def.durationSec;
        return "Начато: " + def.name;
    }

    public String tryStartCraft() {
        if (activeCraftId != null) return "Производство уже идёт";
        boolean anyUnlocked = false;
        // Пока есть свободные площадки, цех приоритетно строит лучшую открытую башню.
        if (pendingTowerCount() == 0 && placedTowerCount() < TOWER_SLOTS.length) {
            for (int i = Defs.RECIPES.size() - 1; i >= 0; i--) {
                Defs.RecipeDef r = Defs.RECIPES.get(i);
                if (!r.outItem.startsWith("turret_") || r.outItem.equals("turret_module") || !isUnlocked(r)) continue;
                anyUnlocked = true;
                if (hashes < r.costHashes || scrap < r.costScrap) continue;
                hashes -= r.costHashes; scrap -= r.costScrap;
                activeCraftId = r.id; activeCraftLeft = r.durationSec;
                return "Производство: " + r.name;
            }
        }
        // Если башня ждёт установки или все слоты заняты — делаем усиление/стену.
        for (int i = Defs.RECIPES.size() - 1; i >= 0; i--) {
            Defs.RecipeDef r = Defs.RECIPES.get(i);
            if (r.outItem.startsWith("turret_") && !r.outItem.equals("turret_module")) continue;
            if (!isUnlocked(r)) continue;
            anyUnlocked = true;
            if (hashes < r.costHashes || scrap < r.costScrap) continue;
            hashes -= r.costHashes; scrap -= r.costScrap;
            activeCraftId = r.id; activeCraftLeft = r.durationSec;
            return "Производство: " + r.name;
        }
        if (pendingTowerCount() > 0) return "Установите башню из резерва на площадку";
        if (anyUnlocked) return "Не хватает ресурсов";
        return "Нет рецептов";
    }

    /** Запускает выбранный игроком рецепт из экрана цеха. */
    public String tryStartCraft(String id) {
        if (activeCraftId != null) return "Производство уже идёт";
        Defs.RecipeDef recipe = Defs.findRecipe(id);
        if (recipe == null) return "Рецепт не найден";
        if (!isUnlocked(recipe)) {
            Defs.ResearchDef required = Defs.findResearch(recipe.requiresResearchId);
            return "Нужно исследование: " + (required != null ? required.name : recipe.requiresResearchId);
        }
        boolean tower = recipe.outItem.startsWith("turret_") && !recipe.outItem.equals("turret_module");
        if (tower && pendingTowerCount() > 0) return "Сначала установите башню из резерва";
        if (tower && placedTowerCount() >= TOWER_SLOTS.length) return "Все площадки заняты";
        if (hashes < recipe.costHashes || scrap < recipe.costScrap)
            return "Нужно " + fmt(recipe.costHashes) + " хешей и " + fmt(recipe.costScrap) + " лома";
        hashes -= recipe.costHashes; scrap -= recipe.costScrap;
        activeCraftId = recipe.id; activeCraftLeft = recipe.durationSec;
        return "Производство: " + recipe.name;
    }

    public boolean isUnlocked(Defs.RecipeDef r) {
        return r.requiresResearchId == null || doneResearch.contains(r.requiresResearchId);
    }

    // ===== главный тик =====
    public void update(double dt) {
        if (gameOver) return;
        hashes += miningRate() * dt;

        // Движение по маршрутам tower defense и урон базе в конечной точке.
        for (int i = zombies.size() - 1; i >= 0; i--) {
            Zombie z = zombies.get(i);
            double[][] route = PATHS[z.pathId];
            if (z.pathIndex >= route.length) {
                baseHp -= z.damage;
                zombies.remove(i);
                if (baseHp <= 0) { baseHp = 0; gameOver = true; }
                continue;
            }
            double dx = route[z.pathIndex][0] - z.x;
            double dy = route[z.pathIndex][1] - z.y;
            double d = Math.hypot(dx, dy);
            if (d < 0.18) {
                z.pathIndex++;
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

        // Башни у дороги: дальность, скорострельность и урон зависят от исследований и типа.
        syncTurrets();
        for (Turret t : turrets) {
            t.cooldown -= dt;
            if (t.cooldown <= 0) {
                Zombie target = nearestZombie(t.x, t.y, 4.0 + rangeBonus + (t.type == 2 ? .8 : 0));
                if (target != null) {
                    double dx = target.x - t.x, dy = target.y - t.y;
                    double d = Math.max(.001, Math.hypot(dx, dy));
                    t.aimX = dx / d; t.aimY = dy / d;
                    double typeDamage = t.type == 3 ? 2.8 : t.type == 2 ? 2.0 : 1.0;
                    double dmg = 10.0 * turretLevel * (1 + turretMult) * typeDamage;
                    projectiles.add(new Projectile(t.x, t.y, target, dmg, t.type == 3 ? 10.0 : 14.0, t.type));
                    t.cooldown = (t.type == 3 ? 1.15 : t.type == 2 ? .85 : .7) / (1 + fireRateMult);
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
        int pathId = (int) (Math.random() * PATHS.length);
        double sx = PATHS[pathId][0][0] + (Math.random() - .5) * .22;
        double sy = PATHS[pathId][0][1] + (Math.random() - .5) * .22;
        double hp = 10.0 * Math.pow(1.25, wave - 1);
        zombies.add(new Zombie(sx, sy, hp, 1.25, 8.0, pathId));
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
        List<Turret> synced = new ArrayList<>();
        for (int slot = 0; slot < placedTowerTypes.length; slot++) {
            int type = placedTowerTypes[slot];
            if (type == 0) continue;
            Turret turret = null;
            for (Turret old : turrets) if (old.slot == slot) { turret = old; break; }
            if (turret == null) turret = new Turret();
            turret.slot = slot;
            turret.type = type;
            turret.x = TOWER_SLOTS[slot][0];
            turret.y = TOWER_SLOTS[slot][1];
            synced.add(turret);
        }
        turrets.clear();
        turrets.addAll(synced);
    }

    private void completeResearch() {
        Defs.ResearchDef d = Defs.findResearch(activeResearchId);
        doneResearch.add(activeResearchId);
        if (d != null) {
            if (d.effectType == 0) miningMult += d.value;
            else if (d.effectType == 1) turretMult += d.value;
            else if (d.effectType == 2) scrapMult += d.value;
            else if (d.effectType == 3) fireRateMult += d.value;
            else if (d.effectType == 4) rangeBonus += d.value;
        }
        activeResearchId = null;
    }

    private void completeCraft() {
        Defs.RecipeDef r = Defs.findRecipe(activeCraftId);
        if (r != null) {
            if (r.outItem.equals("turret_basic")) basicTurrets++;
            else if (r.outItem.equals("turret_laser")) laserTurrets++;
            else if (r.outItem.equals("turret_tesla")) teslaTurrets++;
            else if (r.outItem.equals("turret_module")) turretLevel++;
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
            o.put("teslaTurrets", teslaTurrets);
            o.put("fireRateMult", fireRateMult);
            o.put("rangeBonus", rangeBonus);
            JSONArray slots = new JSONArray();
            for (int type : placedTowerTypes) slots.put(type);
            o.put("placedTowerTypes", slots);
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
            basicTurrets = o.optInt("basicTurrets", 0);
            laserTurrets = o.optInt("laserTurrets", 0);
            teslaTurrets = o.optInt("teslaTurrets", 0);
            fireRateMult = o.optDouble("fireRateMult", 0);
            rangeBonus = o.optDouble("rangeBonus", 0);
            JSONArray slots = o.optJSONArray("placedTowerTypes");
            if (slots != null) {
                for (int i = 0; i < placedTowerTypes.length && i < slots.length(); i++)
                    placedTowerTypes[i] = slots.optInt(i, 0);
            } else {
                // Миграция сейва v0.2: раскладываем уже созданные башни по первым площадкам.
                int index = 0;
                for (int i = 0; i < 1 + basicTurrets && index < placedTowerTypes.length; i++) placedTowerTypes[index++] = 1;
                for (int i = 0; i < laserTurrets && index < placedTowerTypes.length; i++) placedTowerTypes[index++] = 2;
            }
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
        basicTurrets = 0; laserTurrets = 0; teslaTurrets = 0;
        fireRateMult = 0; rangeBonus = 0;
        for (int i = 0; i < placedTowerTypes.length; i++) placedTowerTypes[i] = 0;
        placedTowerTypes[0] = 1;
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
        int pathId, pathIndex = 1;
        Zombie(double x, double y, double hp, double speed, double damage, int pathId) {
            this.x = x; this.y = y; this.hp = hp; this.speed = speed; this.damage = damage;
            this.pathId = pathId;
        }
    }

    public static class Projectile {
        double x, y, damage, speed;
        int type;
        Zombie target;
        Projectile(double x, double y, Zombie t, double dmg, double spd, int type) {
            this.x = x; this.y = y; target = t; damage = dmg; speed = spd; this.type = type;
        }
    }

    public static class Turret {
        double x, y, cooldown, aimX = 1, aimY = 0;
        int slot, type = 1;
    }
}
