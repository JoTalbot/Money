using UnityEngine;

namespace DeadRig.Game
{
    /// <summary>Светящийся 2D-импульс, летящий в экранных координатах к спрайту цели.</summary>
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

            Vector3 destination = _target.transform.position + Vector3.up * 0.72f;
            transform.position = Vector3.MoveTowards(transform.position, destination, _speed * Time.deltaTime);
            float pulse = 0.9f + Mathf.Sin(Time.time * 18f) * 0.12f;
            transform.localScale = Vector3.one * pulse;

            if (Vector3.Distance(transform.position, destination) < 0.12f)
            {
                _target.TakeDamage(_damage);
                Destroy(gameObject);
            }
        }
    }
}
