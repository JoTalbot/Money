using System.Collections.Generic;
using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>Преобразование координат игрового поля в настоящую 2D-изометрию 2:1.</summary>
    public static class IsoProjection
    {
        public static Vector3 ToScreen(Vector2 world, float elevation = 0f)
        {
            return new Vector3((world.x - world.y) * 0.5f, (world.x + world.y) * 0.25f + elevation, 0f);
        }

        public static int SortingOrder(Vector2 world, int offset = 0)
        {
            // Чем ближе объект к нижнему краю ромба, тем поверхнее он рисуется.
            return 5000 - Mathf.RoundToInt((world.x + world.y) * 100f) + offset;
        }
    }

    /// <summary>Загружает исходные PNG и создаёт Sprite без ручной настройки импортера.</summary>
    public static class IsometricVisuals
    {
        private static readonly Dictionary<string, Sprite> Cache = new Dictionary<string, Sprite>();

        public static Sprite Sprite(string name, float pixelsPerUnit, Vector2 pivot)
        {
            string key = name + ":" + pixelsPerUnit + ":" + pivot;
            if (Cache.TryGetValue(key, out Sprite cached)) return cached;

            Texture2D texture = Resources.Load<Texture2D>("Isometric/" + name);
            if (texture == null)
            {
                Debug.LogError("[DeadRig] Не найден 2D-ассет: " + name);
                return null;
            }

            texture.filterMode = FilterMode.Bilinear;
            texture.wrapMode = TextureWrapMode.Clamp;
            var sprite = UnityEngine.Sprite.Create(
                texture,
                new Rect(0f, 0f, texture.width, texture.height),
                pivot,
                pixelsPerUnit,
                0,
                SpriteMeshType.FullRect);
            sprite.name = name;
            Cache[key] = sprite;
            return sprite;
        }

        public static SpriteRenderer AddSprite(
            GameObject owner,
            string asset,
            float pixelsPerUnit,
            Vector2 pivot,
            int sortingOrder,
            Color? tint = null)
        {
            var renderer = owner.AddComponent<SpriteRenderer>();
            renderer.sprite = Sprite(asset, pixelsPerUnit, pivot);
            renderer.sortingOrder = sortingOrder;
            renderer.color = tint ?? Color.white;
            return renderer;
        }
    }

    /// <summary>Подстраивает ортографическую камеру под портретный экран Android.</summary>
    public sealed class IsometricCameraFitter : MonoBehaviour
    {
        public float RequiredHalfWidth = 5.7f;
        public float MinimumHalfHeight = 6.6f;

        private Camera _camera;
        private int _lastWidth;
        private int _lastHeight;

        private void Awake()
        {
            _camera = GetComponent<Camera>();
            Apply();
        }

        private void Update()
        {
            if (_lastWidth != Screen.width || _lastHeight != Screen.height) Apply();
        }

        private void Apply()
        {
            if (_camera == null) return;
            float aspect = Mathf.Max(0.1f, (float)Screen.width / Mathf.Max(1, Screen.height));
            _camera.orthographicSize = Mathf.Max(MinimumHalfHeight, RequiredHalfWidth / aspect);
            _lastWidth = Screen.width;
            _lastHeight = Screen.height;
        }
    }
}
