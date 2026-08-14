using System;
using UnityEngine;

namespace DeadRig.Core
{
    /// <summary>
    /// Крафт/производство предметов (мануфактура в стиле X-COM/UFO):
    /// рецепты требуют ресурсы и время, результат попадает в инвентарь.
    /// </summary>
    public class CraftingManager
    {
        private readonly SaveData _s;
        private readonly EconomyManager _econ;

        public CraftingManager(SaveData save, EconomyManager econ)
        {
            _s = save;
            _econ = econ;

            // Рецепты без требований доступны сразу
            foreach (var r in CraftingCatalog.All)
                if (string.IsNullOrEmpty(r.RequiredResearch) && !_s.unlockedRecipes.Contains(r.Id))
                    _s.unlockedRecipes.Add(r.Id);
        }

        /// <summary>Число параллельных цехов.</summary>
        public int Slots => 1 + _s.craftSlotsExtra;

        public bool IsUnlocked(string recipeId) => _s.unlockedRecipes.Contains(recipeId);

        public int ItemCount(string itemId)
        {
            foreach (var st in _s.inventory)
                if (st.itemId == itemId) return st.count;
            return 0;
        }

        public bool CanCraft(string recipeId)
        {
            var r = CraftingCatalog.Get(recipeId);
            if (r == null || !IsUnlocked(recipeId)) return false;
            if (_s.craftQueue.Count >= Slots) return false;
            return _s.soft >= r.CostSoft && _s.scrap >= r.CostScrap;
        }

        public bool StartCraft(string recipeId)
        {
            if (!CanCraft(recipeId)) return false;
            var r = CraftingCatalog.Get(recipeId);
            if (!_econ.TrySpendSoft(r.CostSoft)) return false;
            if (!_econ.TrySpendScrap(r.CostScrap)) return false;

            _s.craftQueue.Add(new CraftEntry
            {
                recipeId = recipeId,
                finishUtc = DateTimeOffset.UtcNow.ToUnixTimeSeconds() + (long)r.DurationSeconds
            });
            return true;
        }

        /// <summary>Завершить готовые предметы. Вызывать каждый кадр и после загрузки.</summary>
        public void Tick()
        {
            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            for (int i = _s.craftQueue.Count - 1; i >= 0; i--)
            {
                var e = _s.craftQueue[i];
                if (now >= e.finishUtc)
                {
                    _s.craftQueue.RemoveAt(i);
                    var recipe = CraftingCatalog.Get(e.recipeId);
                    AddItem(recipe.OutputItemId, 1);
                    Debug.Log($"[Craft] Готово: {recipe.Name}");
                }
            }
        }

        public void AddItem(string itemId, int count)
        {
            foreach (var st in _s.inventory)
            {
                if (st.itemId == itemId)
                {
                    st.count += count;
                    return;
                }
            }
            _s.inventory.Add(new ItemStack { itemId = itemId, count = count });
        }
    }
}
