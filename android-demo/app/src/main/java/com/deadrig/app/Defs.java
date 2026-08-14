package com.deadrig.app;

import java.util.ArrayList;
import java.util.List;

/** Статические данные игры: дерево исследований и рецепты крафта. */
public class Defs {

    public static class ResearchDef {
        public String id, name, desc, requiresId;
        public double costHashes, costScrap, durationSec;
        public int effectType; // 0 = +майнинг, 1 = +урон, 2 = +лом, -1 = без числового эффекта (открывает крафт)
        public double value;
    }

    public static class RecipeDef {
        public String id, name, outItem, requiresResearchId;
        public double costHashes, costScrap, durationSec;
    }

    public static final List<ResearchDef> RESEARCH = new ArrayList<>();
    public static final List<RecipeDef> RECIPES = new ArrayList<>();

    static {
        ResearchDef r;
        r = new ResearchDef(); r.id = "r_mining"; r.name = "Оптимизация ASIC"; r.desc = "+10% к майнингу";
        r.costHashes = 50; r.costScrap = 20; r.durationSec = 60; r.effectType = 0; r.value = 0.10; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_scrap"; r.name = "Разбор зомби-техники"; r.desc = "+25% лома";
        r.requiresId = "r_mining"; r.costHashes = 120; r.costScrap = 40; r.durationSec = 120; r.effectType = 2; r.value = 0.25; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_laser"; r.name = "Лазерная турель"; r.desc = "Открывает крафт лазерной турели";
        r.requiresId = "r_scrap"; r.costHashes = 200; r.costScrap = 60; r.durationSec = 180; r.effectType = -1; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_dmg"; r.name = "Бронебойные патроны"; r.desc = "+15% к урону";
        r.requiresId = "r_laser"; r.costHashes = 300; r.costScrap = 80; r.durationSec = 240; r.effectType = 1; r.value = 0.15; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_rate"; r.name = "Сервоприводы наведения"; r.desc = "+20% к скорострельности";
        r.requiresId = "r_dmg"; r.costHashes = 420; r.costScrap = 110; r.durationSec = 300; r.effectType = 3; r.value = 0.20; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_range"; r.name = "Квантовый радар"; r.desc = "+1.2 к дальности башен";
        r.requiresId = "r_rate"; r.costHashes = 560; r.costScrap = 150; r.durationSec = 360; r.effectType = 4; r.value = 1.2; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_tesla"; r.name = "Катушка Теслы"; r.desc = "Открывает тяжёлую тесла-башню";
        r.requiresId = "r_range"; r.costHashes = 800; r.costScrap = 220; r.durationSec = 480; r.effectType = -1; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_cryo"; r.name = "Криогенный контур"; r.desc = "Открывает замедляющую крио-башню";
        r.requiresId = "r_range"; r.costHashes = 650; r.costScrap = 190; r.durationSec = 420; r.effectType = -1; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_module"; r.name = "Модульная оборона"; r.desc = "Открывает крафт комплекта усиления";
        r.requiresId = "r_dmg"; r.costHashes = 380; r.costScrap = 100; r.durationSec = 260; r.effectType = -1; RESEARCH.add(r);
        r = new ResearchDef(); r.id = "r_rocket"; r.name = "Ракетная платформа"; r.desc = "Открывает башню с большим радиусом взрыва";
        r.requiresId = "r_dmg"; r.costHashes = 620; r.costScrap = 190; r.durationSec = 380; r.effectType = -1; RESEARCH.add(r);
        r = new ResearchDef(); r.id = "r_support"; r.name = "Поле поддержки"; r.desc = "Открывает усилитель соседних башен";
        r.requiresId = "r_range"; r.costHashes = 700; r.costScrap = 210; r.durationSec = 420; r.effectType = -1; RESEARCH.add(r);
        r = new ResearchDef(); r.id = "r_traps"; r.name = "Инженерные ловушки"; r.desc = "Открывает дорожные мины";
        r.requiresId = "r_scrap"; r.costHashes = 260; r.costScrap = 90; r.durationSec = 190; r.effectType = -1; RESEARCH.add(r);

        // Отдельная ветка ручного оружия оператора.
        r = new ResearchDef(); r.id = "r_weapon_auto"; r.name = "Автомат оператора"; r.desc = "Открывает автоматический огонь";
        r.costHashes = 90; r.costScrap = 25; r.durationSec = 75; r.effectType = -1; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_weapon_shotgun"; r.name = "Штурмовой дробовик"; r.desc = "Открывает урон по группе";
        r.requiresId = "r_weapon_auto"; r.costHashes = 220; r.costScrap = 70; r.durationSec = 170; r.effectType = -1; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_weapon_rail"; r.name = "Переносной рельсотрон"; r.desc = "Открывает бронебойный сверхвыстрел";
        r.requiresId = "r_weapon_shotgun"; r.costHashes = 520; r.costScrap = 160; r.durationSec = 360; r.effectType = -1; RESEARCH.add(r);

        r = new ResearchDef(); r.id = "r_weapon_sniper"; r.name = "Снайперская платформа"; r.desc = "Открывает винтовку с ×3.15 уроном в голову";
        r.requiresId = "r_weapon_rail"; r.costHashes = 650; r.costScrap = 190; r.durationSec = 420; r.effectType = -1; RESEARCH.add(r);
        r = new ResearchDef(); r.id = "r_weapon_grenade"; r.name = "Микрогранаты"; r.desc = "Открывает взрывной урон по площади";
        r.requiresId = "r_weapon_shotgun"; r.costHashes = 430; r.costScrap = 150; r.durationSec = 300; r.effectType = -1; RESEARCH.add(r);
        r = new ResearchDef(); r.id = "r_weapon_flame"; r.name = "Термитная смесь"; r.desc = "Открывает огнемёт и горение";
        r.requiresId = "r_weapon_grenade"; r.costHashes = 560; r.costScrap = 180; r.durationSec = 360; r.effectType = -1; RESEARCH.add(r);
        r = new ResearchDef(); r.id = "r_weapon_cryo"; r.name = "Ручная крионика"; r.desc = "Открывает крио-пушку";
        r.requiresId = "r_weapon_flame"; r.costHashes = 690; r.costScrap = 220; r.durationSec = 420; r.effectType = -1; RESEARCH.add(r);
        r = new ResearchDef(); r.id = "r_weapon_tesla"; r.name = "Портативная тесла"; r.desc = "Открывает цепную тесла-винтовку";
        r.requiresId = "r_weapon_cryo"; r.costHashes = 820; r.costScrap = 270; r.durationSec = 480; r.effectType = -1; RESEARCH.add(r);
        r = new ResearchDef(); r.id = "r_weapon_acid"; r.name = "Коррозионная биохимия"; r.desc = "Открывает кислотомёт";
        r.requiresId = "r_weapon_tesla"; r.costHashes = 940; r.costScrap = 310; r.durationSec = 540; r.effectType = -1; RESEARCH.add(r);
        r = new ResearchDef(); r.id = "r_combat_drone"; r.name = "Боевой дрон"; r.desc = "Открывает автономного помощника";
        r.requiresId = "r_weapon_auto"; r.costHashes = 350; r.costScrap = 120; r.durationSec = 250; r.effectType = -1; RESEARCH.add(r);
        r = new ResearchDef(); r.id = "r_special_ammo"; r.name = "Специальные боеприпасы"; r.desc = "Бронебойные, зажигательные и электрические патроны";
        r.requiresId = "r_weapon_auto"; r.costHashes = 300; r.costScrap = 100; r.durationSec = 220; r.effectType = -1; RESEARCH.add(r);

        RecipeDef c;
        c = new RecipeDef(); c.id = "turret_basic"; c.name = "Базовая турель"; c.outItem = "turret_basic";
        c.costHashes = 30; c.costScrap = 10; c.durationSec = 20; RECIPES.add(c);

        c = new RecipeDef(); c.id = "turret_laser"; c.name = "Лазерная турель"; c.outItem = "turret_laser";
        c.requiresResearchId = "r_laser"; c.costHashes = 120; c.costScrap = 40; c.durationSec = 90; RECIPES.add(c);

        c = new RecipeDef(); c.id = "wall"; c.name = "Стена"; c.outItem = "wall";
        c.costHashes = 20; c.costScrap = 15; c.durationSec = 15; RECIPES.add(c);

        c = new RecipeDef(); c.id = "turret_tesla"; c.name = "Тесла-башня"; c.outItem = "turret_tesla";
        c.requiresResearchId = "r_tesla"; c.costHashes = 300; c.costScrap = 130; c.durationSec = 150; RECIPES.add(c);

        c = new RecipeDef(); c.id = "turret_cryo"; c.name = "Крио-башня"; c.outItem = "turret_cryo";
        c.requiresResearchId = "r_cryo"; c.costHashes = 260; c.costScrap = 120; c.durationSec = 135; RECIPES.add(c);

        c = new RecipeDef(); c.id = "turret_module"; c.name = "Комплект усиления"; c.outItem = "turret_module";
        c.requiresResearchId = "r_module"; c.costHashes = 180; c.costScrap = 75; c.durationSec = 100; RECIPES.add(c);
        c = new RecipeDef(); c.id = "turret_rocket"; c.name = "Ракетная башня"; c.outItem = "turret_rocket";
        c.requiresResearchId = "r_rocket"; c.costHashes = 340; c.costScrap = 150; c.durationSec = 170; RECIPES.add(c);
        c = new RecipeDef(); c.id = "turret_support"; c.name = "Башня поддержки"; c.outItem = "turret_support";
        c.requiresResearchId = "r_support"; c.costHashes = 300; c.costScrap = 145; c.durationSec = 160; RECIPES.add(c);
        c = new RecipeDef(); c.id = "road_mines"; c.name = "Комплект дорожных мин"; c.outItem = "road_mines";
        c.requiresResearchId = "r_traps"; c.costHashes = 90; c.costScrap = 55; c.durationSec = 60; RECIPES.add(c);

        c = new RecipeDef(); c.id = "weapon_auto"; c.name = "Автомат оператора"; c.outItem = "weapon_auto";
        c.requiresResearchId = "r_weapon_auto"; c.costHashes = 80; c.costScrap = 30; c.durationSec = 45; RECIPES.add(c);

        c = new RecipeDef(); c.id = "weapon_shotgun"; c.name = "Штурмовой дробовик"; c.outItem = "weapon_shotgun";
        c.requiresResearchId = "r_weapon_shotgun"; c.costHashes = 190; c.costScrap = 75; c.durationSec = 100; RECIPES.add(c);

        c = new RecipeDef(); c.id = "weapon_rail"; c.name = "Переносной рельсотрон"; c.outItem = "weapon_rail";
        c.requiresResearchId = "r_weapon_rail"; c.costHashes = 480; c.costScrap = 180; c.durationSec = 220; RECIPES.add(c);
        c = new RecipeDef(); c.id = "weapon_sniper"; c.name = "Снайперская винтовка"; c.outItem = "weapon_sniper";
        c.requiresResearchId = "r_weapon_sniper"; c.costHashes = 620; c.costScrap = 220; c.durationSec = 260; RECIPES.add(c);
        c = new RecipeDef(); c.id = "weapon_grenade"; c.name = "Гранатомёт"; c.outItem = "weapon_grenade";
        c.requiresResearchId = "r_weapon_grenade"; c.costHashes = 420; c.costScrap = 170; c.durationSec = 190; RECIPES.add(c);
        c = new RecipeDef(); c.id = "weapon_flame"; c.name = "Огнемёт"; c.outItem = "weapon_flame";
        c.requiresResearchId = "r_weapon_flame"; c.costHashes = 540; c.costScrap = 210; c.durationSec = 230; RECIPES.add(c);
        c = new RecipeDef(); c.id = "weapon_cryo"; c.name = "Крио-пушка"; c.outItem = "weapon_cryo";
        c.requiresResearchId = "r_weapon_cryo"; c.costHashes = 660; c.costScrap = 250; c.durationSec = 270; RECIPES.add(c);
        c = new RecipeDef(); c.id = "weapon_tesla"; c.name = "Тесла-винтовка"; c.outItem = "weapon_tesla";
        c.requiresResearchId = "r_weapon_tesla"; c.costHashes = 780; c.costScrap = 290; c.durationSec = 310; RECIPES.add(c);
        c = new RecipeDef(); c.id = "weapon_acid"; c.name = "Кислотомёт"; c.outItem = "weapon_acid";
        c.requiresResearchId = "r_weapon_acid"; c.costHashes = 900; c.costScrap = 340; c.durationSec = 360; RECIPES.add(c);
        c = new RecipeDef(); c.id = "combat_drone"; c.name = "Боевой дрон"; c.outItem = "combat_drone";
        c.requiresResearchId = "r_combat_drone"; c.costHashes = 360; c.costScrap = 150; c.durationSec = 180; RECIPES.add(c);
        c = new RecipeDef(); c.id = "special_ammo"; c.name = "Комплект специальных патронов"; c.outItem = "special_ammo";
        c.requiresResearchId = "r_special_ammo"; c.costHashes = 240; c.costScrap = 110; c.durationSec = 130; RECIPES.add(c);
    }

    public static ResearchDef findResearch(String id) {
        for (ResearchDef d : RESEARCH) if (d.id.equals(id)) return d;
        return null;
    }

    public static RecipeDef findRecipe(String id) {
        for (RecipeDef d : RECIPES) if (d.id.equals(id)) return d;
        return null;
    }
}
