using UnityEngine;

namespace ZombieMiner.Core
{
    /// <summary>
    /// Точка входа игры. Живёт в единственном GameObject'е сцены (создаётся Bootstrap'ом).
    /// Связывает все системы: экономику, волны, сохранение, престиж.
    /// </summary>
    public class GameManager : MonoBehaviour
    {
        public static GameManager Instance { get; private set; }

        public EconomyManager Economy { get; private set; }
        public WaveManager Waves { get; private set; }
        public SaveManager Save { get; private set; }
        public MetaProgression Meta { get; private set; }

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }

            Instance = this;
            DontDestroyOnLoad(gameObject);
            Init();
        }

        private void Init()
        {
            Save = new SaveManager();
            Save.Load();

            Economy = new EconomyManager(Save.Data);
            Waves = new WaveManager();
            Meta = new MetaProgression(Save.Data);

            // Оффлайн-доход за время отсутствия игрока
            Economy.GrantOfflineEarnings();
        }

        private void Update()
        {
            Economy?.Tick(Time.deltaTime);
        }

        private void OnApplicationPause(bool paused)
        {
            if (paused) Save?.SaveGame();
        }

        private void OnApplicationQuit()
        {
            Save?.SaveGame();
        }
    }
}
