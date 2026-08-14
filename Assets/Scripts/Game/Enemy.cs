using System.Collections.Generic;
using DeadRig.Core;
using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>
    /// Зомби: идёт к ядру базы, атакует вплотную, погибает от турелей.
    /// Прототип — примитив (капсула), цвет задаёт создатель.
    /// </summary>
    public class Enemy : MonoBehaviour
    {
        /// <summary>Все живые враги на поле (для поиска цели турелью).</summary>
        public static readonly List<Enemy> All = new List<Enemy>();

        public float Health { get; private set; }
        private float _speed;
        private float _damage;
        private Transform _target;

        public void Init(float health, float speed, float damage, Transform target)
        {
            Health = health;
            _speed = speed;
            _damage = damage;
            _target = target;
        }

        private void OnEnable() => All.Add(this);
        private void OnDisable() => All.Remove(this);

        private void Update()
        {
            if (_target == null || BaseCore.Instance == null) return;

            transform.position = Vector3.MoveTowards(
                transform.position, _target.position, _speed * Time.deltaTime);

            Vector3 dir = _target.position - transform.position;
            dir.y = 0f;
            if (dir.sqrMagnitude > 0.001f)
                transform.rotation = Quaternion.LookRotation(dir);

            // Дошёл до базы — бьёт по ядру и исчезает
            if (Vector3.Distance(transform.position, _target.position) < 1.2f)
            {
                BaseCore.Instance.TakeDamage(_damage);
                Die();
            }
        }

        public void TakeDamage(float amount)
        {
            Health -= amount;
            if (Health <= 0f) Die();
        }

        private void Die()
        {
            // И убитый, и прорвавшийся считаются "отработанными" — волна кончается, когда поле чисто
            GameManager.Instance?.Waves?.NotifyEnemyKilled();
            Destroy(gameObject);
        }
    }
}
