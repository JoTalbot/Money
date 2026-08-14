namespace ZombieMiner.Core
{
    /// <summary>Престиж: сброс прогресса ради постоянного множителя дохода.</summary>
    public class MetaProgression
    {
        private readonly SaveData _s;

        public MetaProgression(SaveData save) => _s = save;

        public int PrestigeLevel => _s.prestigeLevel;

        /// <summary>Множитель дохода: +50% за каждый уровень престижа.</summary>
        public double Multiplier => 1.0 + _s.prestigeLevel * 0.5;

        /// <summary>Порог накопления "хешей" для следующего престижа.</summary>
        public double PrestigeRequirement() => 1000.0 * System.Math.Pow(10, _s.prestigeLevel);

        public bool CanPrestige() => _s.soft >= PrestigeRequirement();

        /// <summary>
        /// Сбрасывает мягкую валюту, лом и уровни, повышает престиж. True если успешно.
        /// Исследования, рецепты, инвентарь и кристаллы НЕ сбрасываются (мета-прогресс).
        /// </summary>
        public bool TryPrestige()
        {
            if (!CanPrestige()) return false;
            _s.soft = 0;
            _s.scrap = 0;
            _s.minerLevel = 1;
            _s.turretLevel = 1;
            _s.prestigeLevel++;
            return true;
        }
    }
}
