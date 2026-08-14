using DeadRig.Core;
using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>2D-турель: выбирает цель по логическим координатам и выпускает энергоимпульсы.</summary>
    public class Turret : MonoBehaviour
    {
        public float Range = 9f;
        public float FireInterval = 0.7f;
        public float BaseDamage = 10f;
        public Vector2 LogicalPosition;

        private float _cooldown;
        private SpriteRenderer _renderer;

        private void Awake() => _renderer = GetComponent<SpriteRenderer>();

        private void Update()
        {
            _cooldown -= Time.deltaTime;
            Enemy target = FindTarget();
            if (target == null) return;

            if (_renderer != null)
                _renderer.flipX = target.LogicalPosition.x - target.LogicalPosition.y < LogicalPosition.x - LogicalPosition.y;

            if (_cooldown <= 0f)
            {
                Fire(target);
                _cooldown = FireInterval;
            }
        }

        private Enemy FindTarget()
        {
            Enemy best = null;
            float bestDistance = Range;
            foreach (Enemy enemy in Enemy.All)
            {
                if (enemy == null) continue;
                float distance = Vector2.Distance(LogicalPosition, enemy.LogicalPosition);
                if (distance <= bestDistance)
                {
                    bestDistance = distance;
                    best = enemy;
                }
            }
            return best;
        }

        private void Fire(Enemy target)
        {
            float multiplier = GameManager.Instance != null
                ? (float)GameManager.Instance.Economy.TurretDamageMultiplier()
                : 1f;

            var go = new GameObject("EnergyProjectile");
            go.transform.position = IsoProjection.ToScreen(LogicalPosition, 1.15f);
            IsometricVisuals.AddSprite(go, "projectile", 210f, new Vector2(0.5f, 0.5f), 9000);
            var projectile = go.AddComponent<Projectile>();
            projectile.Init(target, BaseDamage * multiplier, 10f);
        }
    }
}
