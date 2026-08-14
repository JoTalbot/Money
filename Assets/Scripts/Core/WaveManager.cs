using System;
using UnityEngine;

namespace ZombieMiner.Core
{
    /// <summary>Управление волнами зомби: номер волны, число врагов, HP, событие очистки.</summary>
    public class WaveManager
    {
        public int WaveNumber { get; private set; }
        public bool IsWaveActive { get; private set; }
        public int EnemiesRemaining { get; private set; }

        /// <summary>Событие: волна отбита (передаётся номер волны).</summary>
        public event Action<int> OnWaveCleared;

        /// <summary>Запустить следующую волну.</summary>
        public void StartNextWave()
        {
            WaveNumber++;
            EnemiesRemaining = EnemyCountFor(WaveNumber);
            IsWaveActive = true;
        }

        public int EnemyCountFor(int wave) => 5 + wave * 3;

        /// <summary>Максимальное HP врага на волне (растёт экспоненциально).</summary>
        public float EnemyHpFor(int wave) => 10f * Mathf.Pow(1.25f, wave - 1);

        /// <summary>Вызвать при смерти врага; при 0 — волна очищена.</summary>
        public void NotifyEnemyKilled()
        {
            EnemiesRemaining--;
            if (EnemiesRemaining <= 0 && IsWaveActive)
            {
                IsWaveActive = false;
                OnWaveCleared?.Invoke(WaveNumber);
            }
        }

        public void Reset()
        {
            WaveNumber = 0;
            IsWaveActive = false;
            EnemiesRemaining = 0;
        }
    }
}
