using UnityEngine;

namespace DeadRig.Core
{
    /// <summary>
    /// Создаёт GameManager, если его ещё нет в сцене.
    /// Вешайте на любой GameObject (или используйте меню DeadRig → Создать сцену).
    /// </summary>
    public class Bootstrap : MonoBehaviour
    {
        private void Awake()
        {
            if (FindObjectOfType<GameManager>() == null)
            {
                var go = new GameObject("GameManager");
                go.AddComponent<GameManager>();
            }
        }
    }
}
