using DeadRig.Core;
using DeadRig.UI;
using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>
    /// Собирает 2D-изометрическую арену: тайловое поле, бункер, турель,
    /// разломы спавна, декоративный свет и HUD. Не использует 3D-примитивы.
    /// </summary>
    public class PrototypeBuilder : MonoBehaviour
    {
        private static readonly Color Background = new Color(0.035f, 0.055f, 0.062f, 1f);

        private void Awake()
        {
            EnsureGameManager();
            BuildCamera();
            BuildGround();
            BuildBase();
            BuildTurret();
            Transform[] spawnPoints = BuildSpawnPoints();
            WaveController wc = BuildWaveController(spawnPoints);
            BuildHud(wc);
        }

        private void EnsureGameManager()
        {
            if (GameManager.Instance == null)
                new GameObject("GameManager").AddComponent<GameManager>();
        }

        private void BuildCamera()
        {
            Camera cam = Camera.main;
            if (cam == null)
            {
                var cameraObject = new GameObject("Main Camera");
                cameraObject.tag = "MainCamera";
                cam = cameraObject.AddComponent<Camera>();
            }

            cam.orthographic = true;
            cam.clearFlags = CameraClearFlags.SolidColor;
            cam.backgroundColor = Background;
            cam.transform.position = new Vector3(0f, 0.6f, -10f);
            cam.transform.rotation = Quaternion.identity;
            cam.nearClipPlane = 0.1f;
            cam.farClipPlane = 30f;
            if (cam.GetComponent<IsometricCameraFitter>() == null)
                cam.gameObject.AddComponent<IsometricCameraFitter>();
        }

        private void BuildGround()
        {
            var root = new GameObject("IsometricGround");
            const int radius = 7;
            Sprite tile = IsometricVisuals.Sprite("ground_tile", 256f, new Vector2(0.5f, 0.5f));

            for (int x = -radius; x <= radius; x++)
            {
                for (int y = -radius; y <= radius; y++)
                {
                    var go = new GameObject("Tile_" + x + "_" + y);
                    go.transform.SetParent(root.transform, false);
                    var logical = new Vector2(x, y);
                    go.transform.position = IsoProjection.ToScreen(logical);
                    var renderer = go.AddComponent<SpriteRenderer>();
                    renderer.sprite = tile;
                    renderer.sortingOrder = IsoProjection.SortingOrder(logical, -1000);
                    float noise = Mathf.PerlinNoise((x + 20) * 0.31f, (y + 20) * 0.31f);
                    renderer.color = Color.Lerp(new Color(0.78f, 0.84f, 0.84f), Color.white, noise);
                }
            }
        }

        private void BuildBase()
        {
            var core = new GameObject("BaseCore");
            Vector2 logical = Vector2.zero;
            core.transform.position = IsoProjection.ToScreen(logical);
            var renderer = IsometricVisuals.AddSprite(
                core, "base_core", 145f, new Vector2(0.5f, 0.08f), IsoProjection.SortingOrder(logical, 20));
            renderer.transform.localScale = Vector3.one * 0.94f;
            core.AddComponent<BaseCore>();
        }

        private void BuildTurret()
        {
            var turret = new GameObject("Turret");
            Vector2 logical = new Vector2(3.2f, -2.7f);
            turret.transform.position = IsoProjection.ToScreen(logical);
            IsometricVisuals.AddSprite(
                turret, "turret", 150f, new Vector2(0.5f, 0.09f), IsoProjection.SortingOrder(logical, 30));

            var logic = turret.AddComponent<Turret>();
            logic.LogicalPosition = logical;
            logic.Range = 9f;
            logic.FireInterval = 0.7f;
        }

        private Transform[] BuildSpawnPoints()
        {
            Vector2[] positions =
            {
                new Vector2(-7.5f, 0f),
                new Vector2(0f, -7.5f),
                new Vector2(7.5f, 0f),
                new Vector2(0f, 7.5f),
            };

            var points = new Transform[positions.Length];
            for (int i = 0; i < positions.Length; i++)
            {
                var marker = new GameObject("SpawnRift_" + i);
                marker.transform.position = IsoProjection.ToScreen(positions[i]);
                IsometricVisuals.AddSprite(
                    marker, "spawn_rift", 150f, new Vector2(0.5f, 0.25f),
                    IsoProjection.SortingOrder(positions[i], 5));
                var point = marker.AddComponent<IsometricSpawnPoint>();
                point.LogicalPosition = positions[i];
                points[i] = marker.transform;
            }
            return points;
        }

        private WaveController BuildWaveController(Transform[] spawnPoints)
        {
            var go = new GameObject("WaveController");
            var wc = go.AddComponent<WaveController>();
            wc.SpawnPoints = spawnPoints;
            return wc;
        }

        private void BuildHud(WaveController wc)
        {
            var hud = new GameObject("HUD").AddComponent<HUD>();
            hud.WaveController = wc;
        }
    }

    public sealed class IsometricSpawnPoint : MonoBehaviour
    {
        public Vector2 LogicalPosition;
    }
}
