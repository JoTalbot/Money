using System;
using System.IO;
using UnityEngine;

namespace ZombieMiner.Core
{
    /// <summary>Данные сохранения. Только [Serializable] публичные поля (для JsonUtility).</summary>
    [Serializable]
    public class SaveData
    {
        public int version = 1;

        // Валюты
        public double soft = 0;      // "хеши" — мягкая валюта (майнинг)
        public long hard = 0;        // "кристаллы" — премиум валюта
        public double totalEarned = 0;

        // Прогресс
        public int minerLevel = 1;   // уровень фермы (майнинг)
        public int turretLevel = 1;  // уровень обороны
        public int prestigeLevel = 0;

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
                }
            }
            catch (Exception e)
            {
                Debug.LogWarning($"[Save] Ошибка чтения сейва: {e.Message}. Начинаем с нуля.");
            }
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
