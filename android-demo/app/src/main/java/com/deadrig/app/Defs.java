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

        RecipeDef c;
        c = new RecipeDef(); c.id = "turret_basic"; c.name = "Базовая турель"; c.outItem = "turret_basic";
        c.costHashes = 30; c.costScrap = 10; c.durationSec = 20; RECIPES.add(c);

        c = new RecipeDef(); c.id = "turret_laser"; c.name = "Лазерная турель"; c.outItem = "turret_laser";
        c.requiresResearchId = "r_laser"; c.costHashes = 120; c.costScrap = 40; c.durationSec = 90; RECIPES.add(c);

        c = new RecipeDef(); c.id = "wall"; c.name = "Стена"; c.outItem = "wall";
        c.costHashes = 20; c.costScrap = 15; c.durationSec = 15; RECIPES.add(c);
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
