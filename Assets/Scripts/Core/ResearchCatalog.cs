using System.Collections.Generic;

namespace DeadRig.Core
{
    /// <summary>Тип эффекта исследования.</summary>
    public enum EffectType
    {
        MiningMult,          // +value к бонусу майнинга
        TurretDmgMult,       // +value к бонусу урона
        ScrapGainMult,       // +value к бонусу лома
        UnlockRecipe,        // stringValue = id рецепта крафта
        UnlockResearchSlot   // +value слотов исследований
    }

    /// <summary>Эффект исследования (числовой или строковый).</summary>
    [System.Serializable]
    public class ResearchEffect
    {
        public EffectType type;
        public double value;
        public string stringValue;

        public ResearchEffect() { }

        public ResearchEffect(EffectType t, double v) { type = t; value = v; stringValue = ""; }

        public ResearchEffect(EffectType t, string s) { type = t; value = 0; stringValue = s; }
    }

    /// <summary>Описание проекта исследования (узел дерева технологий).</summary>
    public class ResearchDef
    {
        public string Id;
        public string Name;          // русское название
        public string Description;   // краткое описание
        public double CostSoft;      // стоимость в "хешах"
        public double CostScrap;     // стоимость в "ломе"
        public double DurationSeconds;
        public string Requires;      // id обязательного предыдущего исследования (или null)
        public List<ResearchEffect> Effects = new List<ResearchEffect>();
    }

    /// <summary>Каталог исследований — дерево технологий в стиле X-COM/UFO.</summary>
    public static class ResearchCatalog
    {
        public static readonly List<ResearchDef> All = new List<ResearchDef>
        {
            new ResearchDef
            {
                Id = "r_mining_1", Name = "Оптимизация ASIC",
                Description = "+10% к скорости майнинга.",
                CostSoft = 50, CostScrap = 20, DurationSeconds = 60, Requires = null,
                Effects = { new ResearchEffect(EffectType.MiningMult, 0.10) }
            },
            new ResearchDef
            {
                Id = "r_scrap_salvage", Name = "Разбор зомби-техники",
                Description = "+25% лома с убитых зомби.",
                CostSoft = 120, CostScrap = 40, DurationSeconds = 120, Requires = "r_mining_1",
                Effects = { new ResearchEffect(EffectType.ScrapGainMult, 0.25) }
            },
            new ResearchDef
            {
                Id = "r_laser_turret", Name = "Лазерная турель",
                Description = "Открывает крафт лазерной турели.",
                CostSoft = 200, CostScrap = 60, DurationSeconds = 180, Requires = "r_scrap_salvage",
                Effects = { new ResearchEffect(EffectType.UnlockRecipe, "turret_laser") }
            },
            new ResearchDef
            {
                Id = "r_turret_dmg", Name = "Бронебойные патроны",
                Description = "+15% к урону турелей.",
                CostSoft = 300, CostScrap = 80, DurationSeconds = 240, Requires = "r_laser_turret",
                Effects = { new ResearchEffect(EffectType.TurretDmgMult, 0.15) }
            },
            new ResearchDef
            {
                Id = "r_lab_2", Name = "Вторая лаборатория",
                Description = "+1 параллельное исследование.",
                CostSoft = 500, CostScrap = 100, DurationSeconds = 300, Requires = "r_turret_dmg",
                Effects = { new ResearchEffect(EffectType.UnlockResearchSlot, 1) }
            },
        };

        public static ResearchDef Get(string id)
        {
            foreach (var d in All)
                if (d.Id == id) return d;
            return null;
        }
    }
}
