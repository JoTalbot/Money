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

        r = new ResearchDef(); r.id = "r_module"; r.name = "Модульная оборона"; r.desc = "Открывает крафт комплекта усиления";
        r.requiresId = "r_dmg"; r.costHashes = 380; r.costScrap = 100; r.durationSec = 260; r.effectType = -1; RESEARCH.add(r);

        RecipeDef c;
        c = new RecipeDef(); c.id = "turret_basic"; c.name = "Базовая турель"; c.outItem = "turret_basic";
        c.costHashes = 30; c.costScrap = 10; c.durationSec = 20; RECIPES.add(c);

        c = new RecipeDef(); c.id = "turret_laser"; c.name = "Лазерная турель"; c.outItem = "turret_laser";
        c.requiresResearchId = "r_laser"; c.costHashes = 120; c.costScrap = 40; c.durationSec = 90; RECIPES.add(c);

        c = new RecipeDef(); c.id = "wall"; c.name = "Стена"; c.outItem = "wall";
        c.costHashes = 20; c.costScrap = 15; c.durationSec = 15; RECIPES.add(c);

        c = new RecipeDef(); c.id = "turret_tesla"; c.name = "Тесла-башня"; c.outItem = "turret_tesla";
        c.requiresResearchId = "r_tesla"; c.costHashes = 300; c.costScrap = 130; c.durationSec = 150; RECIPES.add(c);

        c = new RecipeDef(); c.id = "turret_module"; c.name = "Комплект усиления"; c.outItem = "turret_module";
        c.requiresResearchId = "r_module"; c.costHashes = 180; c.costScrap = 75; c.durationSec = 100; RECIPES.add(c);
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
