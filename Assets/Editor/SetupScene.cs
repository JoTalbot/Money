#if UNITY_EDITOR
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;

namespace DeadRig.EditorTools
{
    /// <summary>
    /// Создаёт стартовую сцену с Bootstrap-объектом и добавляет её в Build Settings.
    /// Меню: DeadRig → Создать сцену.
    /// </summary>
    public static class SetupScene
    {
        [MenuItem("DeadRig/Создать сцену")]
        public static void CreateScene()
        {
            var scene = EditorSceneManager.NewScene(NewSceneSetup.DefaultGameObjects, NewSceneMode.Single);

            var go = new GameObject("Bootstrap");
            go.AddComponent<DeadRig.Core.Bootstrap>();

            if (!AssetDatabase.IsValidFolder("Assets/Scenes"))
                AssetDatabase.CreateFolder("Assets", "Scenes");

            const string path = "Assets/Scenes/Main.unity";
            EditorSceneManager.SaveScene(scene, path);

            EditorBuildSettings.scenes = new[] { new EditorBuildSettingsScene(path, true) };

            Debug.Log("[DeadRig] Сцена создана: " + path);
        }
    }
}
#endif
