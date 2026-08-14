using System;
using UnityEngine;

namespace ZombieMiner.Core
{
    /// <summary>
    /// Единая точка экономики. Источники и стоки валюты/ресурсов проходят ТОЛЬКО через этот класс.
    /// Это важно для "токен-ready" слоя (Путь C): в будущем локальный счётчик можно подменить
    /// на WalletAdapter (реальный кошелёк/смарт-контракт) без правки остального кода.
    /// </summary>
    public class EconomyManager
    {
        private readonly SaveData _s;

        // Базовая скорость майнинга в "хешах"/сек на 1 уровне
        private const double BaseMiningRate = 1.0;
        // Оффлайн-доход = 50% от онлайн-скорости
        private const double OfflineRateFactor = 0.5;
        // Максимум засчитываемого оффлайна — 8 часов
        private const int OfflineCapSeconds = 8 * 3600;

        public EconomyManager(SaveData save) => _s = save;

        public double Soft => _s.soft;
        public long Hard => _s.hard;
        public double Scrap => _s.scrap;
        public int MinerLevel => _s.minerLevel;
        public int TurretLevel => _s.turretLevel;

        /// <summary>Текущая скорость майнинга: уровень фермы × престиж × бонусы исследований.</summary>
        public double MiningRatePerSec =>
            BaseMiningRate * _s.minerLevel * PrestigeMultiplier() * (1.0 + _s.researchMiningMult);

        public double PrestigeMultiplier() => 1.0 + _s.prestigeLevel * 0.5;

        /// <summary>Множитель урона турелей (от исследований).</summary>
        public double TurretDamageMultiplier() => 1.0 + _s.researchTurretMult;

        /// <summary>Тик экономики — вызывается каждый кадр из GameManager.</summary>
        public void Tick(double deltaTime)
        {
            AddSoft(MiningRatePerSec * deltaTime);
        }

        public void AddSoft(double amount)
        {
            _s.soft += amount;
            _s.totalEarned += amount;
        }

        public void AddHard(long amount) => _s.hard += amount;

        public void AddScrap(double amount) => _s.scrap += amount;

        public bool TrySpendSoft(double cost)
        {
            if (_s.soft < cost) return false;
            _s.soft -= cost;
            return true;
        }

        public bool TrySpendHard(long cost)
        {
            if (_s.hard < cost) return false;
            _s.hard -= cost;
            return true;
        }

        public bool TrySpendScrap(double cost)
        {
            if (_s.scrap < cost) return false;
            _s.scrap -= cost;
            return true;
        }

        /// <summary>Стоимость апгрейда фермы растёт экспоненциально (×1.5 за уровень).</summary>
        public double MinerUpgradeCost() => 10.0 * Math.Pow(1.5, _s.minerLevel - 1);

        public bool TryUpgradeMiner()
        {
            double cost = MinerUpgradeCost();
            if (!TrySpendSoft(cost)) return false;
            _s.minerLevel++;
            return true;
        }

        /// <summary>Стоимость апгрейда обороны (×1.6 за уровень).</summary>
        public double TurretUpgradeCost() => 15.0 * Math.Pow(1.6, _s.turretLevel - 1);

        public bool TryUpgradeTurret()
        {
            double cost = TurretUpgradeCost();
            if (!TrySpendSoft(cost)) return false;
            _s.turretLevel++;
            return true;
        }

        /// <summary>Оффлайн-доход за время отсутствия. Вызывается один раз после загрузки.</summary>
        public void GrantOfflineEarnings()
        {
            long now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            long elapsed = now - _s.lastSeenUtc;
            if (elapsed <= 0) return;

            double capped = Math.Min((double)elapsed, OfflineCapSeconds);
            double gain = MiningRatePerSec * capped * OfflineRateFactor;
            if (gain > 0) AddSoft(gain);
        }

        /// <summary>Награда "хешами" за отбитую волну зомби.</summary>
        public double WaveReward(int waveNumber) => 20.0 * waveNumber * PrestigeMultiplier();

        /// <summary>Награда "ломом" за волну (с бонусом исследований).</summary>
        public double ScrapReward(int waveNumber) => 10.0 * waveNumber * (1.0 + _s.researchScrapMult);
    }
}
