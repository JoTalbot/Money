using DeadRig.Core;
using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>
    /// Спавнит зомби по волнам: волна = N врагов с HP из WaveManager.
    /// После зачистки — пауза, затем следующая волна.
    /// </summary>
    public class WaveController : MonoBehaviour
    {
        public float SpawnInterval = 1.0f;
        public float NextWaveDelay = 5f;
        public Transform[] SpawnPoints;

        private int _pendingSpawns;
        private float _spawnTimer;
        private float _nextWaveTimer = 2f; // автостарт первой волны через ~2 сек
        private bool _spawning;

        private void Update()
        {
            var gm = GameManager.Instance;
            if (gm == null || BaseCore.Instance == null || BaseCore.Instance.IsDead) return;

            var waves = gm.Waves;

            // Спавн врагов текущей волны
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

            // Следующая волна после паузы
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
            _spawnTimer = 0.5f;
            _nextWaveTimer = NextWaveDelay;
        }

        private void SpawnEnemy(int waveNumber)
        {
            if (SpawnPoints == null || SpawnPoints.Length == 0) return;
            var waves = GameManager.Instance.Waves;

            int idx = Random.Range(0, SpawnPoints.Length);

            var go = GameObject.CreatePrimitive(PrimitiveType.Capsule);
            go.name = "Zombie";
            Vector3 pos = SpawnPoints[idx].position + Random.insideUnitSphere * 0.4f;
            go.transform.position = new Vector3(pos.x, 0.5f, pos.z);
            go.transform.localScale = new Vector3(0.6f, 0.6f, 0.6f);
            go.GetComponent<Renderer>().material.color = new Color(0.3f, 0.75f, 0.25f);

            var enemy = go.AddComponent<Enemy>();
            enemy.Init(waves.EnemyHpFor(waveNumber), speed: 2.2f, damage: 8f, BaseCore.Instance.transform);
        }
    }
}
