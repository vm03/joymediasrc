package com.example.joymediasrc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AutoSource", "Получен системный триггер: $action")

        // Запускаем сервис для мониторинга питания
        // Сервис сам обработает событие выхода из режима сна
        Log.d("AutoSource", "Запуск AutoSourceService для мониторинга питания")
        val serviceIntent = Intent(context, AutoSourceService::class.java).apply {
            action = AutoSourceService.ACTION_START
        }
        try {
            context.startForegroundService(serviceIntent)
            Log.d("AutoSource", "AutoSourceService успешно запущен")
        } catch (e: Exception) {
            Log.e("AutoSource", "Ошибка запуска AutoSourceService: ${e.message}")
        }
    }
}
