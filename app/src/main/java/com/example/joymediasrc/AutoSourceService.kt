package com.example.joymediasrc

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat

class AutoSourceService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isScreenOn = false

    companion object {
        const val ACTION_START = "com.example.joymediasrc.ACTION_START"
        const val ACTION_STOP = "com.example.joymediasrc.ACTION_STOP"
        private const val CHANNEL_ID = "AutoSourceChannel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d("AutoSourceService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AutoSourceService", "onStartCommand called with action: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopMonitoring()
        }

        return START_STICKY
    }

    private fun startMonitoring() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        // Получаем WakeLock для удержания устройства в рабочем состоянии
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AutoSourceService::WakeLockTag"
        ).apply {
            acquire(10 * 60 * 1000L) // 10 минут
        }

        // Проверяем текущее состояние экрана
        isScreenOn = powerManager.isScreenOn
        Log.d("AutoSourceService", "Initial screen state: $isScreenOn")

        // Регистрируем слушатель изменений состояния экрана (только для Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val screenCallback = object : PowerManager.ScreenCallback() {
                override fun onScreenStateChanged(isOn: Boolean) {
                    Log.d("AutoSourceService", "Screen state changed to: $isOn")
                    isScreenOn = isOn

                    if (isOn) {
                        // Экран включен - выполняем переключение аудио
                        Log.d("AutoSourceService", "Screen turned ON, switching audio source")
                        Handler(Looper.getMainLooper()).postDelayed({
                            sendTargetIntent(applicationContext)
                            Handler(Looper.getMainLooper()).postDelayed({
                                returnToDefaultLauncher(applicationContext)
                            }, 500)
                        }, 300)
                    }
                }
            }
            powerManager.registerScreenCallback(screenCallback, mainHandler)
        }

        // Запускаем периодическую проверку (для старых версий Android)
        startPeriodicCheck(powerManager)

        // Показываем уведомление о работе сервиса
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun startPeriodicCheck(powerManager: PowerManager) {
        // Проверяем состояние экрана каждые 5 секунд
        val checkRunnable = object : Runnable {
            override fun run() {
                if (isScreenOn && !powerManager.isScreenOn) {
                    // Экран только что выключился
                    isScreenOn = false
                } else if (!isScreenOn && powerManager.isScreenOn) {
                    // Экран только что включился
                    Log.d("AutoSourceService", "Screen turned ON (periodic check)")
                    Handler(Looper.getMainLooper()).postDelayed({
                        sendTargetIntent(applicationContext)
                        Handler(Looper.getMainLooper()).postDelayed({
                            returnToDefaultLauncher(applicationContext)
                        }, 500)
                    }, 300)
                }
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(checkRunnable)
    }

    private fun stopMonitoring() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d("AutoSourceService", "WakeLock released")
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun sendTargetIntent(context: Context) {
        val prefs = context.getSharedPreferences("AutoSourcePrefs", Context.MODE_PRIVATE)
        val targetAction = prefs.getString("selected_action", "com.bw.intent.action.BTAUDIO") ?: return

        try {
            val startIntent = Intent(targetAction).apply {
                setClassName("com.bw.mediaplayer", "com.bw.mediaplayer.activity.MainActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(startIntent)
            Log.d("AutoSourceService", "Successfully sent intent: $targetAction")
        } catch (e: Exception) {
            Log.e("AutoSourceService", "Error sending intent: ${e.message}", e)
        }
    }

    private fun returnToDefaultLauncher(context: Context) {
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(launcherIntent)
            Log.d("AutoSourceService", "Returned to default launcher")
        } catch (e: Exception) {
            Log.e("AutoSourceService", "Error returning to launcher: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto Source Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service for switching audio source on power events"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Source Service")
            .setContentText("Monitoring power state for audio source switching")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AutoSourceService", "Service destroyed")
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
    }
}
