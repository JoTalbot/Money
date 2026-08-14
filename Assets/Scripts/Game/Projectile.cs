using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>Снаряд турели: летит в цель, при попадании наносит урон.</summary>
    public class Projectile : MonoBehaviour
    {
        private Enemy _target;
        private float _damage;
        private float _speed;

        public void Init(Enemy target, float damage, float speed)
        {
            _target = target;
            _damage = damage;
            _speed = speed;
        }

        private void Update()
        {
            if (_target == null)
            {
                Destroy(gameObject);
                return;
            }

            transform.position = Vector3.MoveTowards(
                transform.position, _target.transform.position, _speed * Time.deltaTime);

            if (Vector3.Distance(transform.position, _target.transform.position) < 0.35f)
            {
                _target.TakeDamage(_damage);
                Destroy(gameObject);
            }
        }
    }
}
