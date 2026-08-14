using DeadRig.Core;
using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>
    /// Турель: находит ближайшего зомби в радиусе, поворачивается и стреляет.
    /// Урон зависит от бонуса исследований (EconomyManager.TurretDamageMultiplier).
    /// </summary>
    public class Turret : MonoBehaviour
    {
        public float Range = 9f;
        public float FireInterval = 0.7f;
        public float BaseDamage = 10f;

        private float _cooldown;

        private void Update()
        {
            _cooldown -= Time.deltaTime;

            Enemy target = FindTarget();
            if (target == null) return;

            Vector3 dir = target.transform.position - transform.position;
            dir.y = 0f;
            if (dir.sqrMagnitude > 0.001f)
                transform.rotation = Quaternion.LookRotation(dir);

            if (_cooldown <= 0f)
            {
                Fire(target);
                _cooldown = FireInterval;
            }
        }

        private Enemy FindTarget()
        {
            Enemy best = null;
            float bestDist = Range;
            foreach (var e in Enemy.All)
            {
                float d = Vector3.Distance(transform.position, e.transform.position);
                if (d <= bestDist) { bestDist = d; best = e; }
            }
            return best;
        }

        private void Fire(Enemy target)
        {
            float mult = GameManager.Instance != null
                ? (float)GameManager.Instance.Economy.TurretDamageMultiplier()
                : 1f;

            var go = GameObject.CreatePrimitive(PrimitiveType.Sphere);
            go.transform.position = transform.position + transform.forward * 1f + Vector3.up * 0.6f;
            go.transform.localScale = Vector3.one * 0.25f;
            go.GetComponent<Renderer>().material.color = Color.cyan;
            Destroy(go.GetComponent<Collider>()); // снаряду коллайдер не нужен — летит по коду

            var p = go.AddComponent<Projectile>();
            p.Init(target, BaseDamage * mult, 14f);
        }
    }
}
