using System;
using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>
    /// Ядро базы (буровой риг): если зомби добрались — база получает урон.
    /// При нуле HP — поражение (событие OnGameOver).
    /// </summary>
    public class BaseCore : MonoBehaviour
    {
        public static BaseCore Instance { get; private set; }

        public event Action OnGameOver;

        public float MaxHealth = 100f;
        public float Health { get; private set; }
        public bool IsDead => Health <= 0f;

        private void Awake()
        {
            Instance = this;
            Health = MaxHealth;
        }

        public void TakeDamage(float amount)
        {
            if (IsDead) return;
            Health = Mathf.Max(0f, Health - amount);
            if (IsDead) OnGameOver?.Invoke();
        }
    }
}
