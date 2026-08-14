using System.Collections.Generic;

namespace DeadRig.Core
{
    /// <summary>Рецепт крафта (производства предмета).</summary>
    public class RecipeDef
    {
        public string Id;
        public string Name;          // русское название
        public string OutputItemId;  // id предмета на выходе
        public double CostSoft;      // "хеши"
        public double CostScrap;     // "лом"
        public double DurationSeconds;
        public string RequiredResearch; // id исследования, открывающего рецепт (null = доступно сразу)
    }

    /// <summary>Каталог рецептов — мануфактура в стиле X-COM/UFO.</summary>
    public static class CraftingCatalog
    {
        public static readonly List<RecipeDef> All = new List<RecipeDef>
        {
            new RecipeDef
            {
                Id = "turret_basic", Name = "Базовая турель", OutputItemId = "turret_basic",
                CostSoft = 30, CostScrap = 10, DurationSeconds = 20, RequiredResearch = null
            },
            new RecipeDef
            {
                Id = "turret_laser", Name = "Лазерная турель", OutputItemId = "turret_laser",
                CostSoft = 120, CostScrap = 40, DurationSeconds = 90, RequiredResearch = "r_laser_turret"
            },
            new RecipeDef
            {
                Id = "wall", Name = "Стена", OutputItemId = "wall",
                CostSoft = 20, CostScrap = 15, DurationSeconds = 15, RequiredResearch = null
            },
            new RecipeDef
            {
                Id = "booster_overclock", Name = "Бустер: разгон", OutputItemId = "booster_overclock",
                CostSoft = 60, CostScrap = 5, DurationSeconds = 30, RequiredResearch = null
            },
        };

        public static RecipeDef Get(string id)
        {
            foreach (var r in All)
                if (r.Id == id) return r;
            return null;
        }
    }
}
