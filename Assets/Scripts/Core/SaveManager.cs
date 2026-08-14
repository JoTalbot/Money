using System;
using System.Collections.Generic;
using System.IO;
using UnityEngine;

namespace DeadRig.Core
{
    /// <summary>Запись в очереди исследований.</summary>
    [Serializable]
    public class ResearchEntry
    {
        public string projectId;
        public long finishUtc; // Unix-секунды, когда завершится
    }

    /// <summary>Запись в очереди крафта.</summary>
    [Serializable]
    public class CraftEntry
    {
        public string recipeId;
        public long finishUtc;
    }

    /// <summary>Стек предметов в инвентаре.</summary>
    [Serializable]
    public class ItemStack
    {
        public string itemId;
        public int count;
    }

    /// <summary>Данные сохранения. Только [Serializable] публичные поля (для JsonUtility).</summary>
    [Serializable]
    public class SaveData
    {
        public int version = 2;

        // Валюты и ресурсы
        public double soft = 0;      // "хеши" — мягкая валюта (майнинг)
        public long hard = 0;        // "кристаллы" — премиум валюта
        public double scrap = 0;     // "лом" — ресурс для крафта (падает с зомби)
        public double totalEarned = 0;

        // Прогресс базы
        public int minerLevel = 1;   // уровень фермы (майнинг)
        public int turretLevel = 1;  // уровень обороны
        public int prestigeLevel = 0;

        // Исследования
        public int researchLabLevel = 1;                    // число параллельных слотов
        public double researchMiningMult = 0;               // суммарный бонус к майнингу
        public double researchTurretMult = 0;               // суммарный бонус к урону
        public double researchScrapMult = 0;                // суммарный бонус к лому
        public List<string> completedResearch = new List<string>();
        public List<string> unlockedRecipes = new List<string>();
        public List<ResearchEntry> researchQueue = new List<ResearchEntry>();

        // Крафт
        public int craftSlotsExtra = 0;                     // дополнительные цеха
        public List<CraftEntry> craftQueue = new List<CraftEntry>();
        public List<ItemStack> inventory = new List<ItemStack>();

        // Время последнего выхода (Unix-секунды) для оффлайн-дохода
        public long lastSeenUtc = 0;
    }

    /// <summary>
    /// Сохранение/загрузка в JSON-файл. Версия хранится внутри, чтобы при обновлениях
    /// мигрировать старые сейвы. Файл лежит в Application.persistentDataPath.
    /// </summary>
    public class SaveManager
    {
        public SaveData Data { get; private set; }

        private string FilePath => Path.Combine(Application.persistentDataPath, "save.json");

        public void Load()
        {
            Data = new SaveData();
            try
            {
                if (File.Exists(FilePath))
                {
                    var json = File.ReadAllText(FilePath);
                    Data = JsonUtility.FromJson<SaveData>(json) ?? new SaveData();
                    Sanitize(Data);
                }
            }
            catch (Exception e)
            {
                Debug.LogWarning($"[Save] Ошибка чтения сейва: {e.Message}. Начинаем с нуля.");
            }
        }

        /// <summary>Чинит null-поля у старых сейвов (после добавления новых полей/версий).</summary>
        private void Sanitize(SaveData d)
        {
            d.completedResearch ??= new List<string>();
            d.unlockedRecipes ??= new List<string>();
            d.researchQueue ??= new List<ResearchEntry>();
            d.craftQueue ??= new List<CraftEntry>();
            d.inventory ??= new List<ItemStack>();
            if (d.researchLabLevel < 1) d.researchLabLevel = 1;
            if (d.minerLevel < 1) d.minerLevel = 1;
            if (d.turretLevel < 1) d.turretLevel = 1;
        }

        public void SaveGame()
        {
            if (Data == null) Data = new SaveData();
            Data.lastSeenUtc = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
            var json = JsonUtility.ToJson(Data, prettyPrint: true);
            try
            {
                File.WriteAllText(FilePath, json);
            }
            catch (Exception e)
            {
                Debug.LogError($"[Save] Ошибка записи: {e.Message}");
            }
        }
    }
}
