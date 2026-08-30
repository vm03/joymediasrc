package com.example.joymediasrc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AutoSource", "Получен системный триггер: $action")

        // Проверяем, что это интент загрузки
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action?.contains("POWERON") == true) {

            // Запускаем с небольшой задержкой (3-5 секунд),
            // чтобы аудиокарта магнитолы и сам плеер успели проснуться
            Handler(Looper.getMainLooper()).postDelayed({
                sendTargetIntent(context)

                // Шаг 2: Через 1.5 секунды после переключения звука возвращаем дефолтный лаунчер
                Handler(Looper.getMainLooper()).postDelayed({
                    returnToDefaultLauncher(context)
                }, 500)

            }, 300)
        }
    }

    private fun sendTargetIntent(context: Context) {
        val prefs = context.getSharedPreferences("AutoSourcePrefs", Context.MODE_PRIVATE)
        // Получаем сохраненный интент (по умолчанию BTAUDIO)
        val targetAction = prefs.getString("selected_action", "com.bw.intent.action.BTAUDIO") ?: return

        try {
            val startIntent = Intent(targetAction).apply {
                // Указываем конкретный класс родного плеера вашей магнитолы
                setClassName("com.bw.mediaplayer", "com.bw.mediaplayer.activity.MainActivity")
                // Важные флаги: запуск из фонового потока + вывод на передний план (как в нашей рабочей adb команде)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(startIntent)
            Log.d("AutoSource", "Успешно отправлен интент: $targetAction")
        } catch (e: Exception) {
            Log.e("AutoSource", "Ошибка отправки интента: ${e.message}")
        }
    }
    private fun returnToDefaultLauncher(context: Context) {
        try {
            // Создаем чистый интент домашнего экрана
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launcherIntent)
            Log.d("AutoSource", "Успешно вернулись на дефолтный лаунчер")
        } catch (e: Exception) {
            Log.e("AutoSource", "Не удалось запустить дефолтный лаунчер: ${e.message}")
        }
    }
}
