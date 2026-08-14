package com.deadrig.app;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

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

    // --- ручное оружие оператора ---
    private int manualWeaponType = 0; // 0 пистолет, 1 автомат, 2 дробовик, 3 рельсотрон
    private int manualWeaponLevel = 1;
    private double manualCooldown = 0;
    private int manualAmmo = 12;
    private double manualReloadTimer = 0;
    private double manualHeat = 0;
    private boolean manualOverheated = false;
    private int manualCombo = 0;
    private double manualComboTimer = 0;
    private int ownedWeaponMask = 1; // пистолет всегда открыт
    private int damageModuleLevel = 0;
    private int coolingModuleLevel = 0;
    private int magazineModuleLevel = 0;
    private double weaponAbilityCooldown = 0;
    private double overdriveTimer = 0;
    private double ultimateCharge = 0;
    private int manualAmmoType = 0; // 0 обычные, 1 бронебойные, 2 зажигательные, 3 электрические
    private boolean specialAmmoUnlocked = false;
    private boolean combatDroneUnlocked = false;
    private double droneCooldown = 0;
    private final int[] weaponRarity = new int[WeaponCatalog.COUNT];
    private final double[] weaponDamageRoll = new double[WeaponCatalog.COUNT];
    private final double[] weaponSpeedRoll = new double[WeaponCatalog.COUNT];
    private final double[] weaponCritRoll = new double[WeaponCatalog.COUNT];

    // --- мета-прогресс и ежедневные задания ---
    private int prestigeLevel = 0;
    private String dailyKey = "";
    private int dailyKills = 0;
    private int dailyWaves = 0;
    private int dailyShots = 0;
    private boolean dailyClaimed = false;

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
    private boolean bossSpawnedThisWave = false;

    // --- сущности ---
    public final List<Zombie> zombies = new ArrayList<>();
    public final List<Projectile> projectiles = new ArrayList<>();
    public final List<HitEffect> hitEffects = new ArrayList<>();
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
    private final int[] towerLevels = new int[TOWER_SLOTS.length];
    private int movingTowerSlot = -1;
    private int basicTurrets = 0;
    private int laserTurrets = 0;
    private int teslaTurrets = 0;
    private int cryoTurrets = 0;
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
        ensureWeaponRolls();
        checkDailyReset();
        if (placedTowerCount() == 0) placedTowerTypes[0] = 1;
        for (int i = 0; i < towerLevels.length; i++)
            if (placedTowerTypes[i] != 0 && towerLevels[i] == 0) towerLevels[i] = 1;
        syncTurrets();
    }

    private void ensureWeaponRolls() {
        for (int i = 0; i < WeaponCatalog.COUNT; i++) {
            if (weaponDamageRoll[i] <= 0) weaponDamageRoll[i] = .92 + Math.random() * .17;
            if (weaponSpeedRoll[i] <= 0) weaponSpeedRoll[i] = .93 + Math.random() * .15;
            if (weaponCritRoll[i] <= 0) weaponCritRoll[i] = Math.random() * .06;
        }
    }

    private void rollCraftedWeapon(int type) {
        type = WeaponCatalog.clamp(type);
        double roll = Math.random();
        weaponRarity[type] = roll > .985 ? 3 : roll > .90 ? 2 : roll > .65 ? 1 : 0;
        weaponDamageRoll[type] = .90 + Math.random() * .23;
        weaponSpeedRoll[type] = .91 + Math.random() * .19;
        weaponCritRoll[type] = Math.random() * .09;
    }

    // ===== формулы =====
    public double miningRate() { return 1.0 * minerLevel * (1 + miningMult) * (1 + prestigeLevel * .5); }
    public int prestigeLevel() { return prestigeLevel; }
    public double prestigeRequirement() { return 5000.0 * Math.pow(10, prestigeLevel); }
    public String dailyStatus() {
        checkDailyReset();
        return "Зомби " + dailyKills + "/50  •  волны " + dailyWaves + "/5  •  выстрелы " + dailyShots + "/100";
    }
    public boolean dailyReady() { return dailyKills >= 50 && dailyWaves >= 5 && dailyShots >= 100 && !dailyClaimed; }
    public String claimDailyReward() {
        checkDailyReset();
        if (dailyClaimed) return "Награда уже получена";
        if (!dailyReady()) return "Задания ещё не выполнены";
        dailyClaimed = true; crystals += 5; scrap += 75;
        return "Ежедневная награда: +5 кристаллов и +75 лома";
    }
    public String tryPrestige() {
        if (activeResearchId != null || activeCraftId != null) return "Сначала завершите исследование и производство";
        if (hashes < prestigeRequirement()) return "Нужно " + fmt(prestigeRequirement()) + " хешей";
        prestigeLevel++;
        hashes = 0; scrap = 0; minerLevel = 1; turretLevel = 1; manualWeaponLevel = 1;
        manualAmmo = manualMagazineSize(); manualHeat = 0; manualReloadTimer = 0; manualCombo = 0; ultimateCharge = 0;
        wave = 0; waveActive = false; zombies.clear(); projectiles.clear(); hitEffects.clear();
        basicTurrets = laserTurrets = teslaTurrets = cryoTurrets = 0;
        for (int i = 0; i < placedTowerTypes.length; i++) { placedTowerTypes[i] = 0; towerLevels[i] = 0; }
        placedTowerTypes[0] = 1; towerLevels[0] = 1; syncTurrets();
        return "Новый узел запущен. Бонус дохода: +" + (prestigeLevel * 50) + "%";
    }
    public double minerUpgradeCost() { return 10.0 * Math.pow(1.5, minerLevel - 1); }
    public double turretUpgradeCost() { return 15.0 * Math.pow(1.6, turretLevel - 1); }
    public int manualWeaponType() { return manualWeaponType; }
    public int manualWeaponLevel() { return manualWeaponLevel; }
    public int manualMagazineSize() {
        int base = WeaponCatalog.MAGAZINE[WeaponCatalog.clamp(manualWeaponType)];
        return Math.max(1, (int) Math.round(base * (1 + magazineModuleLevel * .15)));
    }
    public int manualAmmo() { return manualAmmo; }
    public double manualHeat() { return manualHeat; }
    public boolean manualOverheated() { return manualOverheated; }
    public boolean manualReloading() { return manualReloadTimer > 0; }
    public int manualCombo() { return manualCombo; }
    public int damageModuleLevel() { return damageModuleLevel; }
    public int coolingModuleLevel() { return coolingModuleLevel; }
    public int magazineModuleLevel() { return magazineModuleLevel; }
    public boolean ownsWeapon(int type) { return (ownedWeaponMask & (1 << type)) != 0; }
    public int weaponAbilityCooldownSeconds() { return (int) Math.ceil(weaponAbilityCooldown); }
    public int ultimatePercent() { return (int) Math.min(100, Math.round(ultimateCharge)); }
    public int manualAmmoType() { return manualAmmoType; }
    public String manualAmmoTypeName() {
        String[] names = {"обычные", "ББ", "огонь", "электро"};
        return names[Math.max(0, Math.min(3, manualAmmoType))];
    }
    public boolean specialAmmoUnlocked() { return specialAmmoUnlocked; }
    public boolean combatDroneUnlocked() { return combatDroneUnlocked; }
    public int weaponRarity(int type) { return weaponRarity[WeaponCatalog.clamp(type)]; }
    public String weaponRarityName(int type) { return WeaponCatalog.RARITY[weaponRarity(type)]; }
    public String manualWeaponName() { return WeaponCatalog.NAMES[WeaponCatalog.clamp(manualWeaponType)]; }
    public double manualWeaponDamage() {
        int type = WeaponCatalog.clamp(manualWeaponType);
        return WeaponCatalog.DAMAGE[type] * WeaponCatalog.RARITY_MULT[weaponRarity[type]] * weaponDamageRoll[type]
                * (1 + (manualWeaponLevel - 1) * .35) * (1 + damageModuleLevel * .12);
    }
    public double manualUpgradeCost() { return 20.0 * Math.pow(1.7, manualWeaponLevel - 1); }
    public int turretCount() { return placedTowerCount(); }
    public int pendingTowerCount() {
        return Math.max(0, 1 + basicTurrets + laserTurrets + teslaTurrets + cryoTurrets - placedTowerCount());
    }
    public int towerTypeAt(int slot) {
        return slot >= 0 && slot < placedTowerTypes.length ? placedTowerTypes[slot] : 0;
    }
    public int towerLevelAt(int slot) {
        return slot >= 0 && slot < towerLevels.length ? Math.max(1, towerLevels[slot]) : 1;
    }
    public double towerUpgradeCost(int slot) { return 25.0 * Math.pow(towerLevelAt(slot), 1.65); }
    public boolean isMovingTower() { return movingTowerSlot >= 0; }
    private int placedTowerCount() {
        int count = 0;
        for (int type : placedTowerTypes) if (type != 0) count++;
        return count;
    }

    /** Ставит лучшую доступную башню на выбранную фиксированную площадку. */
    public String tryPlaceTower(int slot) {
        if (slot < 0 || slot >= placedTowerTypes.length) return "Неверная площадка";
        if (placedTowerTypes[slot] != 0) return "Площадка уже занята";
        if (movingTowerSlot >= 0) {
            placedTowerTypes[slot] = placedTowerTypes[movingTowerSlot];
            towerLevels[slot] = towerLevels[movingTowerSlot];
            placedTowerTypes[movingTowerSlot] = 0;
            towerLevels[movingTowerSlot] = 0;
            movingTowerSlot = -1;
            syncTurrets();
            return "Башня перемещена";
        }
        int placedBasic = 0, placedLaser = 0, placedTesla = 0, placedCryo = 0;
        for (int type : placedTowerTypes) {
            if (type == 1) placedBasic++;
            else if (type == 2) placedLaser++;
            else if (type == 3) placedTesla++;
            else if (type == 4) placedCryo++;
        }
        int type = 0;
        if (placedCryo < cryoTurrets) type = 4;
        else if (placedTesla < teslaTurrets) type = 3;
        else if (placedLaser < laserTurrets) type = 2;
        else if (placedBasic < 1 + basicTurrets) type = 1;
        if (type == 0) return "Сначала создайте башню в цехе";
        placedTowerTypes[slot] = type;
        towerLevels[slot] = 1;
        syncTurrets();
        return type == 4 ? "Крио-башня установлена" : type == 3 ? "Тесла-башня установлена"
                : type == 2 ? "Лазерная башня установлена" : "Башня установлена";
    }

    public String tryUpgradeTower(int slot) {
        if (towerTypeAt(slot) == 0) return "На площадке нет башни";
        double cost = towerUpgradeCost(slot);
        if (hashes < cost) return "Нужно " + fmt(cost) + " хешей";
        hashes -= cost;
        towerLevels[slot] = towerLevelAt(slot) + 1;
        syncTurrets();
        return "Башня улучшена до уровня " + towerLevels[slot];
    }

    public String equipManualWeapon(int type) {
        if (type < 0 || type >= WeaponCatalog.COUNT || !ownsWeapon(type)) return "Оружие ещё не создано";
        manualWeaponType = type; manualAmmo = manualMagazineSize(); manualHeat = 0; manualReloadTimer = 0;
        return manualWeaponName() + " экипирован";
    }

    public String selectAmmoType(int type) {
        if (type < 0 || type > 3) return "Неизвестный тип боеприпасов";
        if (type > 0 && !specialAmmoUnlocked) return "Сначала исследуйте специальные боеприпасы";
        manualAmmoType = type;
        String[] names = {"Обычные", "Бронебойные", "Зажигательные", "Электрические"};
        return "Боеприпасы: " + names[type];
    }

    public String upgradeWeaponModule(int module) {
        int level = module == 0 ? damageModuleLevel : module == 1 ? coolingModuleLevel : magazineModuleLevel;
        double cost = 30 * Math.pow(1.8, level);
        if (scrap < cost) return "Нужно " + fmt(cost) + " лома";
        scrap -= cost;
        if (module == 0) damageModuleLevel++;
        else if (module == 1) coolingModuleLevel++;
        else { magazineModuleLevel++; manualAmmo = Math.min(manualAmmo, manualMagazineSize()); }
        return "Модуль улучшен до уровня " + (level + 1);
    }

    public String activateWeaponAbility() {
        if (weaponAbilityCooldown > 0) return "Способность будет готова через " + weaponAbilityCooldownSeconds() + " сек.";
        if (zombies.isEmpty()) return "Нет целей";
        if (manualWeaponType == 1) {
            overdriveTimer = 5.0;
        } else {
            int hits = manualWeaponType == 3 ? zombies.size() : manualWeaponType == 2 ? Math.min(5, zombies.size()) : Math.min(3, zombies.size());
            for (int i = 0; i < hits; i++) {
                Zombie target = zombies.get(i);
                double mult = manualWeaponType == 3 ? 2.2 : manualWeaponType == 2 ? 1.2 : 1.5;
                target.hp -= manualWeaponDamage() * mult;
                target.lastHitManual = true;
                hitEffects.add(new HitEffect(target.x, target.y, 10 + manualWeaponType));
            }
        }
        weaponAbilityCooldown = 18.0;
        return "Активирована способность: " + manualWeaponName();
    }

    public String activateUltimate() {
        if (ultimateCharge < 100) return "Ультимейт заряжен на " + ultimatePercent() + "%";
        for (Zombie zombie : zombies) {
            zombie.hp -= Math.max(120, manualWeaponDamage() * 6);
            zombie.lastHitManual = true;
            hitEffects.add(new HitEffect(zombie.x, zombie.y, 13));
        }
        ultimateCharge = 0;
        return "Орбитальный импульс активирован";
    }

    public String tryUpgradeManualWeapon() {
        double cost = manualUpgradeCost();
        if (hashes < cost) return "Нужно " + fmt(cost) + " хешей";
        hashes -= cost;
        manualWeaponLevel++;
        return manualWeaponName() + " улучшен до уровня " + manualWeaponLevel;
    }

    /** Ручной выстрел с критами, попаданием в голову, магазином и перегревом. */
    public boolean manualShoot(Zombie target, boolean charged, boolean headshot, boolean preciseAim) {
        if (target == null || !zombies.contains(target) || manualCooldown > 0 || gameOver
                || manualReloadTimer > 0 || manualOverheated) return false;
        if (manualAmmo <= 0) { startManualReload(); return false; }

        int weapon = WeaponCatalog.clamp(manualWeaponType);
        boolean critical = Math.random() < Math.min(.65, .10 + manualWeaponLevel * .012
                + weaponCritRoll[weapon] + (preciseAim ? .18 : 0));
        double headMultiplier = weapon == 4 ? 3.15 : 2.2;
        double damage = manualWeaponDamage() * (charged ? 1.65 : 1.0)
                * (headshot ? headMultiplier : 1.0) * (preciseAim ? 1.28 : 1.0) * (critical ? 1.75 : 1.0);
        double interval = WeaponCatalog.INTERVAL[weapon] / weaponSpeedRoll[weapon];
        if (overdriveTimer > 0) interval *= .48;
        manualCooldown = interval * (charged ? 1.12 : 1.0);
        manualAmmo--;
        double heatGain = WeaponCatalog.HEAT[weapon];
        if (overdriveTimer > 0) heatGain *= .45;
        manualHeat = Math.min(1.2, manualHeat + heatGain);
        if (manualHeat >= 1.0) manualOverheated = true;
        if (manualAmmo <= 0) startManualReload();
        dailyShots++;
        double projectileSpeed = weapon == 3 || weapon == 4 ? 30 : weapon == 5 ? 10 : weapon == 6 ? 12 : 18;
        Projectile shot = new Projectile(0, 0, target, damage, projectileSpeed, 10 + weapon);
        shot.critical = critical;
        shot.headshot = headshot;
        shot.ammoType = manualAmmoType;
        projectiles.add(shot);
        return true;
    }

    public String startManualReload() {
        if (manualAmmo >= manualMagazineSize()) return "Магазин уже полный";
        if (manualReloadTimer > 0) return "Перезарядка уже идёт";
        manualReloadTimer = WeaponCatalog.RELOAD[WeaponCatalog.clamp(manualWeaponType)];
        return "Перезарядка";
    }

    public String beginMoveTower(int slot) {
        if (towerTypeAt(slot) == 0) return "На площадке нет башни";
        movingTowerSlot = slot;
        return "Выберите свободную площадку";
    }

    public String cancelMoveTower() {
        movingTowerSlot = -1;
        return "Перемещение отменено";
    }

    public String trySellTower(int slot) {
        int type = towerTypeAt(slot);
        if (type == 0) return "На площадке нет башни";
        if (turretCount() <= 1) return "Последнюю башню продавать нельзя";
        if (type == 1) {
            if (basicTurrets <= 0) return "Стартовую башню продавать нельзя";
            basicTurrets--;
        } else if (type == 2) laserTurrets--;
        else if (type == 3) teslaTurrets--;
        else if (type == 4) cryoTurrets--;
        double refund = (type == 4 ? 50 : type == 3 ? 65 : type == 2 ? 25 : 8) + towerLevelAt(slot) * 4;
        scrap += refund;
        placedTowerTypes[slot] = 0;
        towerLevels[slot] = 0;
        if (movingTowerSlot == slot) movingTowerSlot = -1;
        syncTurrets();
        return "Башня продана: +" + fmt(refund) + " лома";
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
        int weaponTier = weaponTypeForItem(recipe.outItem);
        if (weaponTier > 0 && ownsWeapon(weaponTier)) return "Это оружие уже создано";
        if ("combat_drone".equals(recipe.outItem) && combatDroneUnlocked) return "Боевой дрон уже создан";
        if ("special_ammo".equals(recipe.outItem) && specialAmmoUnlocked) return "Специальные боеприпасы уже открыты";
        if (tower && pendingTowerCount() > 0) return "Сначала установите башню из резерва";
        if (tower && placedTowerCount() >= TOWER_SLOTS.length) return "Все площадки заняты";
        if (hashes < recipe.costHashes || scrap < recipe.costScrap)
            return "Нужно " + fmt(recipe.costHashes) + " хешей и " + fmt(recipe.costScrap) + " лома";
        hashes -= recipe.costHashes; scrap -= recipe.costScrap;
        activeCraftId = recipe.id; activeCraftLeft = recipe.durationSec;
        return "Производство: " + recipe.name;
    }

    private int weaponTypeForItem(String item) {
        if ("weapon_auto".equals(item)) return 1;
        if ("weapon_shotgun".equals(item)) return 2;
        if ("weapon_rail".equals(item)) return 3;
        if ("weapon_sniper".equals(item)) return 4;
        if ("weapon_grenade".equals(item)) return 5;
        if ("weapon_flame".equals(item)) return 6;
        if ("weapon_cryo".equals(item)) return 7;
        if ("weapon_tesla".equals(item)) return 8;
        if ("weapon_acid".equals(item)) return 9;
        return 0;
    }

    public boolean isUnlocked(Defs.RecipeDef r) {
        return r.requiresResearchId == null || doneResearch.contains(r.requiresResearchId);
    }

    // ===== главный тик =====
    public void update(double dt) {
        if (gameOver) return;
        manualCooldown = Math.max(0, manualCooldown - dt);
        weaponAbilityCooldown = Math.max(0, weaponAbilityCooldown - dt);
        overdriveTimer = Math.max(0, overdriveTimer - dt);
        droneCooldown = Math.max(0, droneCooldown - dt);
        manualHeat = Math.max(0, manualHeat - dt * (.28 + coolingModuleLevel * .08));
        if (manualOverheated && manualHeat <= .45) manualOverheated = false;
        if (manualReloadTimer > 0) {
            manualReloadTimer -= dt;
            if (manualReloadTimer <= 0) { manualReloadTimer = 0; manualAmmo = manualMagazineSize(); }
        }
        if (manualComboTimer > 0) {
            manualComboTimer -= dt;
            if (manualComboTimer <= 0) manualCombo = 0;
        }
        hashes += miningRate() * dt;

        // Движение по маршрутам tower defense и урон базе в конечной точке.
        for (int i = zombies.size() - 1; i >= 0; i--) {
            Zombie z = zombies.get(i);
            if (z.burnTimer > 0) { z.burnTimer -= dt; z.hp -= z.burnDps * dt; }
            z.acidTimer = Math.max(0, z.acidTimer - dt);
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
            z.slowTimer = Math.max(0, z.slowTimer - dt);
            double speedMultiplier = z.slowTimer > 0 ? .48 : 1.0;
            z.x += dx / d * z.speed * speedMultiplier * dt;
            z.y += dy / d * z.speed * speedMultiplier * dt;
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
                dailyWaves++;
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
                Zombie target = nearestZombie(t.x, t.y, 4.0 + rangeBonus + (t.type == 2 ? .8 : t.type == 4 ? .5 : 0));
                if (target != null) {
                    double dx = target.x - t.x, dy = target.y - t.y;
                    double d = Math.max(.001, Math.hypot(dx, dy));
                    t.aimX = dx / d; t.aimY = dy / d;
                    double typeDamage = t.type == 4 ? .72 : t.type == 3 ? 2.8 : t.type == 2 ? 2.0 : 1.0;
                    double dmg = 10.0 * turretLevel * t.level * (1 + turretMult) * typeDamage;
                    projectiles.add(new Projectile(t.x, t.y, target, dmg, t.type == 3 ? 10.0 : t.type == 4 ? 12.0 : 14.0, t.type));
                    t.cooldown = (t.type == 3 ? 1.15 : t.type == 4 ? .82 : t.type == 2 ? .85 : .7) / (1 + fireRateMult);
                } else {
                    t.cooldown = 0.1;
                }
            }
        }

        if (combatDroneUnlocked && droneCooldown <= 0 && !zombies.isEmpty()) {
            Zombie target = nearestZombie(0, 0, 8.0);
            if (target != null) {
                projectiles.add(new Projectile(-.7, .7, target, 8 * (1 + prestigeLevel * .12), 16, 20));
                droneCooldown = .55;
            }
        }

        // снаряды
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            if (!zombies.contains(p.target)) { projectiles.remove(i); continue; }
            double dx = p.target.x - p.x, dy = p.target.y - p.y;
            double d = Math.hypot(dx, dy);
            if (d < 0.35) {
                double resistance = 1.0;
                if (p.target.type == 2 && p.type == 1) resistance = .55; // броня держит пули
                if (p.target.type == 1 && p.type == 2) resistance = 1.20; // лазер эффективен против бегунов
                if (p.target.type == 3 && p.type == 3) resistance = .80; // токсичная плоть гасит разряд
                if (p.target.type == 4 && p.type == 3) resistance = 1.15;
                if (p.target.type == 2 && (p.type == 10 || p.type == 11)) resistance = .65;
                if (p.target.type == 2 && p.type == 13) resistance = 1.55; // рельсотрон пробивает броню
                if (p.target.acidTimer > 0) resistance *= 1.28;
                if (p.ammoType == 1 && p.target.type == 2) resistance *= 1.55;
                p.target.hp -= p.damage * resistance;
                if (p.type >= 10 && p.type < 20) p.target.lastHitManual = true;
                if (p.type == 16 || p.ammoType == 2) {
                    p.target.burnTimer = Math.max(p.target.burnTimer, 3.5);
                    p.target.burnDps = Math.max(p.target.burnDps, p.damage * .32);
                }
                if (p.type == 17) p.target.slowTimer = Math.max(p.target.slowTimer, 4.0);
                if (p.type == 19) p.target.acidTimer = Math.max(p.target.acidTimer, 5.0);
                if (p.type == 4) p.target.slowTimer = Math.max(p.target.slowTimer, 3.2);
                if (p.type == 3) { // цепная молния переходит ещё на две близкие цели
                    int chains = 0;
                    for (Zombie other : zombies) {
                        if (other == p.target || chains >= 2) continue;
                        if (Math.hypot(other.x - p.target.x, other.y - p.target.y) < 1.8) {
                            other.hp -= p.damage * .42;
                            hitEffects.add(new HitEffect(other.x, other.y, 3));
                            chains++;
                        }
                    }
                }
                if (p.type == 12 || p.type == 15) { // дробовик или гранатомёт
                    double radius = p.type == 15 ? 2.15 : 1.25;
                    double splash = p.type == 15 ? .72 : .42;
                    for (Zombie other : zombies)
                        if (other != p.target && Math.hypot(other.x - p.target.x, other.y - p.target.y) < radius)
                            other.hp -= p.damage * splash;
                }
                if (p.type == 18 || p.ammoType == 3) { // тесла-винтовка или электрические патроны
                    int chains = 0;
                    for (Zombie other : zombies) {
                        if (other == p.target || chains >= (p.type == 18 ? 3 : 1)) continue;
                        if (Math.hypot(other.x - p.target.x, other.y - p.target.y) < 1.7) {
                            other.hp -= p.damage * (p.type == 18 ? .55 : .28);
                            hitEffects.add(new HitEffect(other.x, other.y, 18)); chains++;
                        }
                    }
                }
                hitEffects.add(new HitEffect(p.x, p.y, p.type));
                projectiles.remove(i);
                continue;
            }
            p.x += dx / d * p.speed * dt;
            p.y += dy / d * p.speed * dt;
        }
        for (int i = zombies.size() - 1; i >= 0; i--) {
            Zombie dead = zombies.get(i);
            if (dead.hp <= 0) {
                ultimateCharge = Math.min(100, ultimateCharge + (dead.lastHitManual ? 5 : 2));
                if (dead.lastHitManual) {
                    manualCombo = Math.min(99, manualCombo + 1);
                    manualComboTimer = 3.0;
                    hashes += Math.min(20, manualCombo * .35);
                }
                zombies.remove(i); dailyKills++;
            }
        }

        for (int i = hitEffects.size() - 1; i >= 0; i--) {
            HitEffect effect = hitEffects.get(i);
            effect.life -= dt;
            if (effect.life <= 0) hitEffects.remove(i);
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
        bossSpawnedThisWave = false;
        waveActive = true;
    }

    private void spawnZombie() {
        int pathId = (int) (Math.random() * PATHS.length);
        double sx = PATHS[pathId][0][0] + (Math.random() - .5) * .22;
        double sy = PATHS[pathId][0][1] + (Math.random() - .5) * .22;
        int type = 0;
        if (wave % 10 == 0 && !bossSpawnedThisWave) { type = 4; bossSpawnedThisWave = true; }
        else {
            double roll = Math.random();
            if (wave >= 7 && roll < .12) type = 3;
            else if (wave >= 4 && roll < .30) type = 2;
            else if (wave >= 2 && roll < .52) type = 1;
        }
        double baseHp = 10.0 * Math.pow(1.25, wave - 1);
        double hpMult = type == 4 ? 12.0 : type == 2 ? 2.5 : type == 3 ? 1.35 : type == 1 ? .68 : 1.0;
        double speed = type == 4 ? .72 : type == 2 ? .88 : type == 3 ? 1.08 : type == 1 ? 1.82 : 1.25;
        double damage = type == 4 ? 35 : type == 3 ? 14 : type == 2 ? 12 : type == 1 ? 6 : 8;
        zombies.add(new Zombie(sx, sy, baseHp * hpMult, speed, damage, pathId, type));
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
            turret.level = towerLevelAt(slot);
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
            int craftedWeapon = weaponTypeForItem(r.outItem);
            if (craftedWeapon > 0) {
                ownedWeaponMask |= 1 << craftedWeapon;
                manualWeaponType = craftedWeapon;
                rollCraftedWeapon(craftedWeapon);
                manualAmmo = manualMagazineSize();
            } else if (r.outItem.equals("combat_drone")) combatDroneUnlocked = true;
            else if (r.outItem.equals("special_ammo")) specialAmmoUnlocked = true;
            else if (r.outItem.equals("turret_basic")) basicTurrets++;
            else if (r.outItem.equals("turret_laser")) laserTurrets++;
            else if (r.outItem.equals("turret_tesla")) teslaTurrets++;
            else if (r.outItem.equals("turret_cryo")) cryoTurrets++;
            else if (r.outItem.equals("turret_module")) turretLevel++;
            else if (r.outItem.equals("wall")) {
                baseMaxHp += 20;
                baseHp = Math.min(baseMaxHp, baseHp + 20);
            }
        }
        activeCraftId = null;
    }

    private void checkDailyReset() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String today = format.format(new Date());
        if (!today.equals(dailyKey)) {
            dailyKey = today; dailyKills = 0; dailyWaves = 0; dailyShots = 0; dailyClaimed = false;
        }
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
            o.put("manualWeaponType", manualWeaponType);
            o.put("manualWeaponLevel", manualWeaponLevel);
            o.put("manualAmmo", manualAmmo);
            o.put("manualHeat", manualHeat);
            o.put("ownedWeaponMask", ownedWeaponMask);
            o.put("damageModuleLevel", damageModuleLevel);
            o.put("coolingModuleLevel", coolingModuleLevel);
            o.put("magazineModuleLevel", magazineModuleLevel);
            o.put("ultimateCharge", ultimateCharge);
            o.put("manualAmmoType", manualAmmoType);
            o.put("specialAmmoUnlocked", specialAmmoUnlocked);
            o.put("combatDroneUnlocked", combatDroneUnlocked);
            JSONArray rarity = new JSONArray(), damageRolls = new JSONArray(), speedRolls = new JSONArray(), critRolls = new JSONArray();
            for (int i = 0; i < WeaponCatalog.COUNT; i++) {
                rarity.put(weaponRarity[i]); damageRolls.put(weaponDamageRoll[i]);
                speedRolls.put(weaponSpeedRoll[i]); critRolls.put(weaponCritRoll[i]);
            }
            o.put("weaponRarity", rarity); o.put("weaponDamageRoll", damageRolls);
            o.put("weaponSpeedRoll", speedRolls); o.put("weaponCritRoll", critRolls);
            o.put("prestigeLevel", prestigeLevel);
            o.put("dailyKey", dailyKey);
            o.put("dailyKills", dailyKills);
            o.put("dailyWaves", dailyWaves);
            o.put("dailyShots", dailyShots);
            o.put("dailyClaimed", dailyClaimed);
            o.put("miningMult", miningMult);
            o.put("turretMult", turretMult);
            o.put("scrapMult", scrapMult);
            o.put("baseHp", baseHp);
            o.put("baseMaxHp", baseMaxHp);
            o.put("wave", wave);
            o.put("basicTurrets", basicTurrets);
            o.put("laserTurrets", laserTurrets);
            o.put("teslaTurrets", teslaTurrets);
            o.put("cryoTurrets", cryoTurrets);
            o.put("fireRateMult", fireRateMult);
            o.put("rangeBonus", rangeBonus);
            JSONArray slots = new JSONArray();
            JSONArray levels = new JSONArray();
            for (int type : placedTowerTypes) slots.put(type);
            for (int level : towerLevels) levels.put(level);
            o.put("placedTowerTypes", slots);
            o.put("towerLevels", levels);
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
            manualWeaponType = o.optInt("manualWeaponType", 0);
            manualWeaponLevel = Math.max(1, o.optInt("manualWeaponLevel", 1));
            manualAmmo = o.optInt("manualAmmo", manualMagazineSize());
            manualHeat = o.optDouble("manualHeat", 0);
            ownedWeaponMask = o.optInt("ownedWeaponMask", (1 << (manualWeaponType + 1)) - 1);
            damageModuleLevel = o.optInt("damageModuleLevel", 0);
            coolingModuleLevel = o.optInt("coolingModuleLevel", 0);
            magazineModuleLevel = o.optInt("magazineModuleLevel", 0);
            ultimateCharge = o.optDouble("ultimateCharge", 0);
            manualAmmoType = o.optInt("manualAmmoType", 0);
            specialAmmoUnlocked = o.optBoolean("specialAmmoUnlocked", false);
            combatDroneUnlocked = o.optBoolean("combatDroneUnlocked", false);
            JSONArray rarity = o.optJSONArray("weaponRarity");
            JSONArray damageRolls = o.optJSONArray("weaponDamageRoll");
            JSONArray speedRolls = o.optJSONArray("weaponSpeedRoll");
            JSONArray critRolls = o.optJSONArray("weaponCritRoll");
            for (int i = 0; i < WeaponCatalog.COUNT; i++) {
                if (rarity != null) weaponRarity[i] = rarity.optInt(i, 0);
                if (damageRolls != null) weaponDamageRoll[i] = damageRolls.optDouble(i, 0);
                if (speedRolls != null) weaponSpeedRoll[i] = speedRolls.optDouble(i, 0);
                if (critRolls != null) weaponCritRoll[i] = critRolls.optDouble(i, 0);
            }
            prestigeLevel = o.optInt("prestigeLevel", 0);
            dailyKey = o.optString("dailyKey", "");
            dailyKills = o.optInt("dailyKills", 0);
            dailyWaves = o.optInt("dailyWaves", 0);
            dailyShots = o.optInt("dailyShots", 0);
            dailyClaimed = o.optBoolean("dailyClaimed", false);
            miningMult = o.optDouble("miningMult", 0);
            turretMult = o.optDouble("turretMult", 0);
            scrapMult = o.optDouble("scrapMult", 0);
            baseHp = o.getDouble("baseHp");
            baseMaxHp = o.getDouble("baseMaxHp");
            wave = o.getInt("wave");
            basicTurrets = o.optInt("basicTurrets", 0);
            laserTurrets = o.optInt("laserTurrets", 0);
            teslaTurrets = o.optInt("teslaTurrets", 0);
            cryoTurrets = o.optInt("cryoTurrets", 0);
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
            JSONArray levels = o.optJSONArray("towerLevels");
            if (levels != null)
                for (int i = 0; i < towerLevels.length && i < levels.length(); i++) towerLevels[i] = levels.optInt(i, 1);
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
        manualWeaponType = 0; manualWeaponLevel = 1; manualCooldown = 0; manualAmmo = 12;
        manualReloadTimer = 0; manualHeat = 0; manualOverheated = false; manualCombo = 0; manualComboTimer = 0;
        ownedWeaponMask = 1; damageModuleLevel = coolingModuleLevel = magazineModuleLevel = 0;
        weaponAbilityCooldown = overdriveTimer = ultimateCharge = 0; prestigeLevel = 0;
        manualAmmoType = 0; specialAmmoUnlocked = false; combatDroneUnlocked = false; droneCooldown = 0;
        for (int i = 0; i < WeaponCatalog.COUNT; i++) {
            weaponRarity[i] = 0; weaponDamageRoll[i] = weaponSpeedRoll[i] = weaponCritRoll[i] = 0;
        }
        ensureWeaponRolls();
        dailyKey = ""; dailyKills = dailyWaves = dailyShots = 0; dailyClaimed = false;
        miningMult = 0; turretMult = 0; scrapMult = 0;
        baseHp = 100; baseMaxHp = 100;
        wave = 0; waveActive = false; zombiesToSpawn = 0; spawnTimer = 0; nextWaveTimer = 2.0;
        zombies.clear(); projectiles.clear(); hitEffects.clear(); turrets.clear();
        basicTurrets = 0; laserTurrets = 0; teslaTurrets = 0; cryoTurrets = 0;
        fireRateMult = 0; rangeBonus = 0;
        for (int i = 0; i < placedTowerTypes.length; i++) { placedTowerTypes[i] = 0; towerLevels[i] = 0; }
        placedTowerTypes[0] = 1; towerLevels[0] = 1; movingTowerSlot = -1;
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
        double x, y, hp, maxHp, speed, damage, slowTimer, burnTimer, burnDps, acidTimer;
        int pathId, pathIndex = 1, type;
        boolean lastHitManual;
        Zombie(double x, double y, double hp, double speed, double damage, int pathId, int type) {
            this.x = x; this.y = y; this.hp = hp; this.maxHp = hp; this.speed = speed; this.damage = damage;
            this.pathId = pathId; this.type = type;
        }
    }

    public static class HitEffect {
        double x, y, life = .28;
        int type;
        HitEffect(double x, double y, int type) { this.x = x; this.y = y; this.type = type; }
    }

    public static class Projectile {
        double x, y, damage, speed;
        int type, ammoType;
        boolean critical, headshot;
        Zombie target;
        Projectile(double x, double y, Zombie t, double dmg, double spd, int type) {
            this.x = x; this.y = y; target = t; damage = dmg; speed = spd; this.type = type;
        }
    }

    public static class Turret {
        double x, y, cooldown, aimX = 1, aimY = 0;
        int slot, type = 1, level = 1;
    }
}
