using DeadRig.Core;
using DeadRig.Game;
using UnityEngine;
using UnityEngine.SceneManagement;
using UnityEngine.UI;

namespace DeadRig.UI
{
    /// <summary>
    /// Программный HUD (собирается кодом, без готовых префабов):
    /// сверху — валюты и волна, снизу — кнопки действий, по центру — экран поражения.
    /// </summary>
    public class HUD : MonoBehaviour
    {
        public WaveController WaveController;

        private Text _hashLabel;
        private Text _scrapLabel;
        private Text _crystalLabel;
        private Text _waveLabel;
        private Text _baseLabel;
        private Text _researchLabel;
        private Text _craftLabel;
        private Text _gameOverLabel;
        private GameObject _gameOverPanel;
        private Button _waveButton;
        private Text _waveBtnText;

        private Font _font;

        private void Awake() => Build();

        private Font BuiltinFont()
        {
            if (_font != null) return _font;
            _font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
            if (_font == null) _font = Resources.GetBuiltinResource<Font>("Arial.ttf");
            return _font;
        }

        private void Build()
        {
            var canvasGO = new GameObject("Canvas");
            canvasGO.transform.SetParent(transform, false);
            var canvas = canvasGO.AddComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            var scaler = canvasGO.AddComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1080f, 1920f);
            canvasGO.AddComponent<GraphicRaycaster>();

            // === Верхняя панель: валюты и статус ===
            var top = CreatePanel(canvasGO.transform, "TopBar", new Vector2(0f, 1f), new Vector2(0f, -60f), new Vector2(1080f, 120f));

            _hashLabel = CreateText(top.transform, "Hash", "Хеши: 0", new Vector2(0f, 1f), new Vector2(20f, -30f), new Vector2(320f, 44f), 32, TextAnchor.MiddleLeft);
            _scrapLabel = CreateText(top.transform, "Scrap", "Лом: 0", new Vector2(0f, 1f), new Vector2(20f, -72f), new Vector2(320f, 40f), 28, TextAnchor.MiddleLeft);
            _crystalLabel = CreateText(top.transform, "Crystal", "Кристаллы: 0", new Vector2(0.5f, 1f), new Vector2(0f, -30f), new Vector2(360f, 44f), 32, TextAnchor.MiddleCenter);
            _waveLabel = CreateText(top.transform, "Wave", "Волна: 0", new Vector2(0.5f, 1f), new Vector2(0f, -72f), new Vector2(360f, 40f), 28, TextAnchor.MiddleCenter);
            _baseLabel = CreateText(top.transform, "Base", "База: 100/100", new Vector2(1f, 1f), new Vector2(-20f, -30f), new Vector2(320f, 44f), 32, TextAnchor.MiddleRight);
            _researchLabel = CreateText(top.transform, "Research", "Наука: —", new Vector2(1f, 1f), new Vector2(-20f, -72f), new Vector2(340f, 40f), 26, TextAnchor.MiddleRight);
            _craftLabel = CreateText(top.transform, "Craft", "Мастерская: —", new Vector2(1f, 1f), new Vector2(-20f, -108f), new Vector2(340f, 36f), 22, TextAnchor.MiddleRight);

            // === Нижняя панель: кнопки действий ===
            var bottom = CreatePanel(canvasGO.transform, "BottomBar", new Vector2(0f, 0f), new Vector2(0f, 40f), new Vector2(1080f, 160f));

            _waveButton = CreateButton(bottom.transform, "WaveBtn", "Волна →", new Vector2(0.5f, 0f), new Vector2(0f, 12f), new Vector2(300f, 120f), 34);
            _waveBtnText = _waveButton.GetComponentInChildren<Text>();
            _waveButton.onClick.AddListener(OnWaveClicked);

            var farmBtn = CreateButton(bottom.transform, "FarmBtn", "Ферма", new Vector2(0f, 0f), new Vector2(12f, 12f), new Vector2(190f, 120f), 30);
            farmBtn.onClick.AddListener(() => GameManager.Instance?.Economy?.TryUpgradeMiner());
            farmBtn.GetComponentInChildren<Text>().text = "Ферма";

            var turretBtn = CreateButton(bottom.transform, "TurretBtn", "Турель", new Vector2(0f, 0f), new Vector2(210f, 12f), new Vector2(190f, 120f), 30);
            turretBtn.onClick.AddListener(() => GameManager.Instance?.Economy?.TryUpgradeTurret());

            var researchBtn = CreateButton(bottom.transform, "ResearchBtn", "Исследовать", new Vector2(1f, 0f), new Vector2(-210f, 12f), new Vector2(190f, 120f), 26);
            researchBtn.onClick.AddListener(OnResearchClicked);

            var craftBtn = CreateButton(bottom.transform, "CraftBtn", "Крафт", new Vector2(1f, 0f), new Vector2(-12f, 12f), new Vector2(190f, 120f), 30);
            craftBtn.onClick.AddListener(OnCraftClicked);

            // === Экран поражения ===
            _gameOverPanel = CreatePanel(canvasGO.transform, "GameOver", new Vector2(0.5f, 0.5f), Vector2.zero, new Vector2(700f, 400f));
            _gameOverLabel = CreateText(_gameOverPanel.transform, "Label", "БАЗА УНИЧТОЖЕНА", new Vector2(0.5f, 0.5f), new Vector2(0f, 60f), new Vector2(600f, 80f), 44, TextAnchor.MiddleCenter);
            var restartBtn = CreateButton(_gameOverPanel.transform, "Restart", "Заново", new Vector2(0.5f, 0.5f), new Vector2(0f, -80f), new Vector2(300f, 100f), 34);
            restartBtn.onClick.AddListener(OnRestartClicked);
            _gameOverPanel.SetActive(false);
        }

        private void Update()
        {
            var gm = GameManager.Instance;
            if (gm == null) return;

            var econ = gm.Economy;
            _hashLabel.text = "Хеши: " + Format(econ.Soft);
            _scrapLabel.text = "Лом: " + Format(econ.Scrap);
            _crystalLabel.text = "Кристаллы: " + econ.Hard;
            _waveLabel.text = "Волна: " + gm.Waves.WaveNumber + (gm.Waves.IsWaveActive ? " (идут зомби)" : "");

            var core = BaseCore.Instance;
            if (core != null)
                _baseLabel.text = "База: " + Mathf.CeilToInt(core.Health) + "/" + Mathf.CeilToInt(core.MaxHealth);

            _researchLabel.text = "Наука: " + FirstAvailableResearchName();
            _craftLabel.text = "Мастерская: " + FirstAvailableCraftName();

            bool waveActive = gm.Waves.IsWaveActive;
            _waveButton.interactable = !waveActive;
            _waveBtnText.text = waveActive ? "Идёт волна…" : "Волна →";

            if (core != null && core.IsDead)
                _gameOverPanel.SetActive(true);
        }

        private string FirstAvailableResearchName()
        {
            var research = GameManager.Instance?.Research;
            if (research == null) return "—";
            foreach (var def in ResearchCatalog.All)
                if (research.CanResearch(def.Id)) return def.Name;
            return "всё изучено";
        }

        private string FirstAvailableCraftName()
        {
            var crafting = GameManager.Instance?.Crafting;
            if (crafting == null) return "—";
            foreach (var r in CraftingCatalog.All)
                if (crafting.CanCraft(r.Id)) return r.Name;
            return "нет рецептов";
        }

        private void OnWaveClicked() => WaveController?.StartWave();

        private void OnResearchClicked()
        {
            var research = GameManager.Instance?.Research;
            if (research == null) return;
            foreach (var def in ResearchCatalog.All)
                if (research.StartResearch(def.Id)) return;
        }

        private void OnCraftClicked()
        {
            var crafting = GameManager.Instance?.Crafting;
            if (crafting == null) return;
            foreach (var r in CraftingCatalog.All)
                if (crafting.StartCraft(r.Id)) return;
        }

        private void OnRestartClicked()
        {
            SceneManager.LoadScene(SceneManager.GetActiveScene().buildIndex);
        }

        private static string Format(double v)
        {
            if (v >= 1e9) return (v / 1e9).ToString("0.#") + "B";
            if (v >= 1e6) return (v / 1e6).ToString("0.#") + "M";
            if (v >= 1e3) return (v / 1e3).ToString("0.#") + "K";
            return v.ToString("0");
        }

        // === Хелперы построения UI ===

        private GameObject CreatePanel(Transform parent, string name, Vector2 anchor, Vector2 anchoredPos, Vector2 size)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var img = go.AddComponent<Image>();
            img.color = new Color(0f, 0f, 0f, 0.55f);
            var rt = (RectTransform)go.transform;
            rt.anchorMin = anchor; rt.anchorMax = anchor; rt.pivot = anchor;
            rt.anchoredPosition = anchoredPos; rt.sizeDelta = size;
            return go;
        }

        private Text CreateText(Transform parent, string name, string value, Vector2 anchor, Vector2 anchoredPos, Vector2 size, int fontSize, TextAnchor align)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var t = go.AddComponent<Text>();
            t.font = BuiltinFont();
            t.text = value;
            t.fontSize = fontSize;
            t.color = Color.white;
            t.alignment = align;
            t.horizontalOverflow = HorizontalWrapMode.Overflow;
            t.verticalOverflow = VerticalWrapMode.Overflow;
            var rt = (RectTransform)go.transform;
            rt.anchorMin = anchor; rt.anchorMax = anchor; rt.pivot = anchor;
            rt.anchoredPosition = anchoredPos; rt.sizeDelta = size;
            return t;
        }

        private Button CreateButton(Transform parent, string name, string label, Vector2 anchor, Vector2 anchoredPos, Vector2 size, int fontSize)
        {
            var go = new GameObject(name);
            go.transform.SetParent(parent, false);
            var img = go.AddComponent<Image>();
            img.color = new Color(0.18f, 0.32f, 0.18f, 0.95f);
            var btn = go.AddComponent<Button>();
            btn.targetGraphic = img;
            var rt = (RectTransform)go.transform;
            rt.anchorMin = anchor; rt.anchorMax = anchor; rt.pivot = anchor;
            rt.anchoredPosition = anchoredPos; rt.sizeDelta = size;
            CreateText(go.transform, "Label", label, new Vector2(0.5f, 0.5f), Vector2.zero, size, fontSize, TextAnchor.MiddleCenter);
            return btn;
        }
    }
}
