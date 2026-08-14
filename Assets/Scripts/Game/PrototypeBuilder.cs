using DeadRig.Core;
using DeadRig.UI;
using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>
    /// Собирает прототипный уровень из примитивов (low-poly заглушки):
    /// земля, ядро базы, турель, точки спавна зомби, контроллер волн и HUD.
    /// Работает без готовых ассетов и сцены.
    /// </summary>
    public class PrototypeBuilder : MonoBehaviour
    {
        private void Awake()
        {
            EnsureGameManager();

            BuildGround();
            BuildCamera();
            BuildBase();
            BuildTurret();
            Transform[] spawnPoints = BuildSpawnPoints();
            WaveController wc = BuildWaveController(spawnPoints);
            BuildHud(wc);
        }

        private void EnsureGameManager()
        {
            if (GameManager.Instance == null)
            {
                var go = new GameObject("GameManager");
                go.AddComponent<GameManager>();
            }
        }

        private void BuildGround()
        {
            var ground = GameObject.CreatePrimitive(PrimitiveType.Plane);
            ground.name = "Ground";
            ground.transform.localScale = new Vector3(5f, 1f, 5f); // 50x50
            ground.GetComponent<Renderer>().material.color = new Color(0.12f, 0.14f, 0.12f);
        }

        private void BuildCamera()
        {
            var cam = Camera.main;
            if (cam == null) return;
            cam.transform.position = new Vector3(0f, 14f, -12f);
            cam.transform.rotation = Quaternion.Euler(52f, 0f, 0f);
        }

        private void BuildBase()
        {
            var core = GameObject.CreatePrimitive(PrimitiveType.Cube);
            core.name = "BaseCore";
            core.transform.position = new Vector3(0f, 0.6f, 0f);
            core.transform.localScale = new Vector3(1.6f, 1.2f, 1.6f);
            core.GetComponent<Renderer>().material.color = new Color(0.95f, 0.55f, 0.1f);
            core.AddComponent<BaseCore>();
        }

        private void BuildTurret()
        {
            var turret = new GameObject("Turret");
            turret.transform.position = new Vector3(0f, 0.5f, 4.5f);

            var body = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            body.name = "Body";
            body.transform.SetParent(turret.transform, false);
            body.transform.localScale = new Vector3(0.8f, 0.4f, 0.8f);
            body.GetComponent<Renderer>().material.color = new Color(0.35f, 0.35f, 0.4f);

            var barrel = GameObject.CreatePrimitive(PrimitiveType.Cube);
            barrel.name = "Barrel";
            barrel.transform.SetParent(turret.transform, false);
            barrel.transform.localPosition = new Vector3(0f, 0.5f, 0.5f);
            barrel.transform.localScale = new Vector3(0.25f, 0.25f, 1f);
            barrel.GetComponent<Renderer>().material.color = new Color(0.25f, 0.25f, 0.3f);

            var t = turret.AddComponent<Turret>();
            t.Range = 9f;
            t.FireInterval = 0.7f;
        }

        private Transform[] BuildSpawnPoints()
        {
            Vector3[] corners =
            {
                new Vector3(-14f, 0f, -14f),
                new Vector3( 14f, 0f, -14f),
                new Vector3(-14f, 0f,  14f),
                new Vector3( 14f, 0f,  14f),
            };

            var points = new Transform[corners.Length];
            for (int i = 0; i < corners.Length; i++)
            {
                var marker = GameObject.CreatePrimitive(PrimitiveType.Cube);
                marker.name = "SpawnPoint_" + i;
                marker.transform.position = corners[i];
                marker.transform.localScale = new Vector3(0.4f, 0.4f, 0.4f);
                marker.GetComponent<Renderer>().material.color = new Color(0.6f, 0.1f, 0.1f);
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
            var go = new GameObject("HUD");
            var hud = go.AddComponent<HUD>();
            hud.WaveController = wc;
        }
    }
}
