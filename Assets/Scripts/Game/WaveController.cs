using DeadRig.Core;
using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>Спавнит 2D-зомби из изометрических разломов и управляет волнами.</summary>
    public class WaveController : MonoBehaviour
    {
        public float SpawnInterval = 1.0f;
        public float NextWaveDelay = 5f;
        public Transform[] SpawnPoints;

        private int _pendingSpawns;
        private float _spawnTimer;
        private float _nextWaveTimer = 2f;
        private bool _spawning;

        private void Update()
        {
            var gm = GameManager.Instance;
            if (gm == null || BaseCore.Instance == null || BaseCore.Instance.IsDead) return;
            var waves = gm.Waves;

            if (_spawning)
            {
                _spawnTimer -= Time.deltaTime;
                if (_spawnTimer <= 0f && _pendingSpawns > 0)
                {
                    SpawnEnemy(waves.WaveNumber);
                    _pendingSpawns--;
                    _spawnTimer = SpawnInterval;
                }
                if (_pendingSpawns <= 0) _spawning = false;
            }

            if (!waves.IsWaveActive && !_spawning && _pendingSpawns <= 0)
            {
                _nextWaveTimer -= Time.deltaTime;
                if (_nextWaveTimer <= 0f) StartWave();
            }
        }

        public void StartWave()
        {
            var waves = GameManager.Instance?.Waves;
            if (waves == null || waves.IsWaveActive) return;
            if (BaseCore.Instance != null && BaseCore.Instance.IsDead) return;

            waves.StartNextWave();
            _pendingSpawns = waves.EnemyCountFor(waves.WaveNumber);
            _spawning = true;
            _spawnTimer = 0.4f;
            _nextWaveTimer = NextWaveDelay;
        }

        private void SpawnEnemy(int waveNumber)
        {
            if (SpawnPoints == null || SpawnPoints.Length == 0) return;
            var waves = GameManager.Instance.Waves;
            Transform spawn = SpawnPoints[Random.Range(0, SpawnPoints.Length)];
            var point = spawn.GetComponent<IsometricSpawnPoint>();
            Vector2 logical = point != null ? point.LogicalPosition : Vector2.zero;
            logical += Random.insideUnitCircle * 0.32f;

            var go = new GameObject("Zombie");
            IsometricVisuals.AddSprite(go, "zombie", 170f, new Vector2(0.5f, 0.06f), 0);
            var enemy = go.AddComponent<Enemy>();
            enemy.Init(waves.EnemyHpFor(waveNumber), speed: 1.35f, damage: 8f, logicalPosition: logical);
        }
    }
}
