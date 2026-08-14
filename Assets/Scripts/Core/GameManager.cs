using UnityEngine;

namespace DeadRig.Core
{
    /// <summary>
    /// Точка входа игры. Живёт в единственном GameObject'е сцены (создаётся Bootstrap'ом).
    /// Связывает все системы: экономику, волны, исследование, крафт, сохранение, престиж.
    /// </summary>
    public class GameManager : MonoBehaviour
    {
        public static GameManager Instance { get; private set; }

        public EconomyManager Economy { get; private set; }
        public WaveManager Waves { get; private set; }
        public SaveManager Save { get; private set; }
        public MetaProgression Meta { get; private set; }
        public ResearchManager Research { get; private set; }
        public CraftingManager Crafting { get; private set; }

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
            Research = new ResearchManager(Save.Data, Economy);
            Crafting = new CraftingManager(Save.Data, Economy);

            // Оффлайн: доход, исследования и крафт идут в реальном времени
            Economy.GrantOfflineEarnings();
            Research.Tick();
            Crafting.Tick();

            Waves.OnWaveCleared += OnWaveCleared;
        }

        private void Update()
        {
            Economy?.Tick(Time.deltaTime);
            Research?.Tick();
            Crafting?.Tick();
        }

        private void OnWaveCleared(int waveNumber)
        {
            // Награда за отбитую волну: "хеши" + "лом"
            Economy.AddSoft(Economy.WaveReward(waveNumber));
            Economy.AddScrap(Economy.ScrapReward(waveNumber));
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
