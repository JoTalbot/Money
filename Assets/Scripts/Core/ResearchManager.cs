using System;
using UnityEngine;

namespace DeadRig.Core
{
    /// <summary>
    /// Исследования в стиле X-COM/UFO: проекты с временем выполнения и слотами (лаборатории).
    /// Идут в реальном времени, в том числе оффлайн (finishUtc фиксирован).
    /// Открывают рецепты крафта и дают постоянные бонусы.
    /// </summary>
    public class ResearchManager
    {
        private readonly SaveData _s;
        private readonly EconomyManager _econ;

        public ResearchManager(SaveData save, EconomyManager econ)
        {
            _s = save;
            _econ = econ;
        }

        /// <summary>Число параллельных слотов = число лабораторий.</summary>
        public int Slots => _s.researchLabLevel;

        public bool IsCompleted(string id) => _s.completedResearch.Contains(id);

        public bool IsInProgress(string id)
        {
            foreach (var e in _s.researchQueue)
                if (e.projectId == id) return true;
            return false;
        }

        /// <summary>Секунд до завершения текущего проекта (или -1 если нет в очереди).</summary>
        public double SecondsLeft(string id)
        {
            foreach (var e in _s.researchQueue)
                if (e.projectId == id)
                    return Math.Max(0, e.finishUtc - DateTimeOffset.UtcNow.ToUnixTimeSeconds());
            return -1;
        }

        public bool CanResearch(string id)
        {
            var def = ResearchCatalog.Get(id);
            if (def == null) return false;
            if (IsCompleted(id) || IsInProgress(id)) return false;
            if (_s.researchQueue.Count >= Slots) return false;
            if (!string.IsNullOrEmpty(def.Requires) && !IsCompleted(def.Requires)) return false;
            return _s.soft >= def.CostSoft && _s.scrap >= def.CostScrap;
        }

        public bool StartResearch(string id)
        {
            if (!CanResearch(id)) return false;
            var def = ResearchCatalog.Get(id);
            if (!_econ.TrySpendSoft(def.CostSoft)) return false;
            if (!_econ.TrySpendScrap(def.CostScrap)) return false;

            _s.researchQueue.Add(new ResearchEntry
            {
                projectId = id,
                finishUtc = DateTimeOffset.UtcNow.ToUnixTimeSeconds() + (long)def.DurationSeconds
            });
            return true;
        }

        /// <summary>Проверить и завершить готовые проекты. Вызывать каждый кадр и после загрузки.</summary>
        public void Tick()
        {
            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            for (int i = _s.researchQueue.Count - 1; i >= 0; i--)
            {
                var e = _s.researchQueue[i];
                if (now >= e.finishUtc)
                {
                    _s.researchQueue.RemoveAt(i);
                    Complete(e.projectId);
                }
            }
        }

        private void Complete(string id)
        {
            if (!_s.completedResearch.Contains(id))
                _s.completedResearch.Add(id);

            var def = ResearchCatalog.Get(id);
            foreach (var fx in def.Effects)
            {
                switch (fx.type)
                {
                    case EffectType.MiningMult: _s.researchMiningMult += fx.value; break;
                    case EffectType.TurretDmgMult: _s.researchTurretMult += fx.value; break;
                    case EffectType.ScrapGainMult: _s.researchScrapMult += fx.value; break;
                    case EffectType.UnlockRecipe:
                        if (!_s.unlockedRecipes.Contains(fx.stringValue))
                            _s.unlockedRecipes.Add(fx.stringValue);
                        break;
                    case EffectType.UnlockResearchSlot:
                        _s.researchLabLevel += (int)fx.value;
                        break;
                }
            }

            Debug.Log($"[Research] Завершено исследование: {def.Name}");
        }
    }
}
