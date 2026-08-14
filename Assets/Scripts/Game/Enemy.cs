using System.Collections.Generic;
using DeadRig.Core;
using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>2D-зомби: движется в логических координатах изометрического поля.</summary>
    public class Enemy : MonoBehaviour
    {
        public static readonly List<Enemy> All = new List<Enemy>();

        public float Health { get; private set; }
        public Vector2 LogicalPosition { get; private set; }

        private float _speed;
        private float _damage;
        private SpriteRenderer _renderer;
        private Vector3 _baseScale;
        private float _phase;

        public void Init(float health, float speed, float damage, Vector2 logicalPosition)
        {
            Health = health;
            _speed = speed;
            _damage = damage;
            LogicalPosition = logicalPosition;
            transform.position = IsoProjection.ToScreen(LogicalPosition);
            _renderer = GetComponent<SpriteRenderer>();
            _baseScale = transform.localScale;
            _phase = Random.value * Mathf.PI * 2f;
            UpdateSorting();
        }

        private void OnEnable() => All.Add(this);
        private void OnDisable() => All.Remove(this);

        private void Update()
        {
            if (BaseCore.Instance == null || BaseCore.Instance.IsDead) return;

            LogicalPosition = Vector2.MoveTowards(LogicalPosition, Vector2.zero, _speed * Time.deltaTime);
            transform.position = IsoProjection.ToScreen(LogicalPosition, Mathf.Abs(Mathf.Sin(Time.time * 7f + _phase)) * 0.035f);

            // Лёгкий шаг и отражение по горизонтальному направлению без поворота плоского спрайта.
            if (_renderer != null)
            {
                _renderer.flipX = LogicalPosition.x - LogicalPosition.y < 0f;
                float pulse = 1f + Mathf.Sin(Time.time * 7f + _phase) * 0.025f;
                transform.localScale = new Vector3(_baseScale.x, _baseScale.y * pulse, 1f);
            }
            UpdateSorting();

            if (LogicalPosition.sqrMagnitude < 1.25f * 1.25f)
            {
                BaseCore.Instance.TakeDamage(_damage);
                Die();
            }
        }

        private void UpdateSorting()
        {
            if (_renderer != null)
                _renderer.sortingOrder = IsoProjection.SortingOrder(LogicalPosition, 40);
        }

        public void TakeDamage(float amount)
        {
            Health -= amount;
            if (_renderer != null)
                _renderer.color = new Color(1f, 0.55f, 0.55f);
            CancelInvoke(nameof(ResetTint));
            Invoke(nameof(ResetTint), 0.08f);
            if (Health <= 0f) Die();
        }

        private void ResetTint()
        {
            if (_renderer != null) _renderer.color = Color.white;
        }

        private void Die()
        {
            GameManager.Instance?.Waves?.NotifyEnemyKilled();
            Destroy(gameObject);
        }
    }
}
