package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.AssistXApp
import com.example.MainActivity
import com.example.R
import com.example.data.repository.SettingsRepository
import com.example.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingBallService : Service() {

    private lateinit var settingsRepository: SettingsRepository
    private var overlayController: OverlayController? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsRepository = SettingsRepository.getInstance(this)
        overlayController = OverlayController(this, settingsRepository)

        // Immediately promote to Foreground Service so the ball stays alive in background
        startInForeground()
        observeSettings()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Keep Flux Assist ball alive even when the app is backgrounded or cleared from recents
        val settings = settingsRepository.getSettings()
        if (settings.isServiceEnabled) {
            startInForeground()
            if (overlayController?.isAttached() == false) {
                overlayController?.showBall()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                settingsRepository.setServiceEnabled(false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_TOGGLE -> {
                if (overlayController?.isAttached() == true) {
                    overlayController?.hideBall()
                } else {
                    overlayController?.showBall()
                }
            }

            ACTION_START -> {
                startInForeground()
                overlayController?.showBall()
            }

            ACTION_UPDATE_SETTINGS -> {
                val settings = settingsRepository.getSettings()
                overlayController?.updateSettings(settings)
            }
        }

        return START_STICKY
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            startForeground(AssistXApp.NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(AssistXApp.NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val toggleIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FloatingBallService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, FloatingBallService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, AssistXApp.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_action_settings)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_action_recents, getString(R.string.notification_action_toggle), toggleIntent)
            .addAction(R.drawable.ic_action_lock, getString(R.string.notification_action_stop), stopIntent)
            .build()
    }

    private fun observeSettings() {
        var lastAppliedSettings: com.example.data.models.BallSettings? = null
        serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                if (settings.isServiceEnabled) {
                    if (overlayController?.isAttached() == false) {
                        overlayController?.showBall()
                        lastAppliedSettings = settings
                    } else {
                        // Skip redundant updates if only lastPosX / lastPosY changed during drag/snap
                        val prev = lastAppliedSettings
                        val isVisualChange = prev == null ||
                                prev.floatingShape != settings.floatingShape ||
                                prev.ballStyle != settings.ballStyle ||
                                prev.ballSizeDp != settings.ballSizeDp ||
                                prev.ballOpacity != settings.ballOpacity ||
                                prev.menuStyle != settings.menuStyle ||
                                prev.radialAccentColor != settings.radialAccentColor ||
                                prev.radialCenterShape != settings.radialCenterShape ||
                                prev.actions != settings.actions ||
                                prev.quickApps != settings.quickApps ||
                                prev.customIcons != settings.customIcons ||
                                prev.edgeSnapping != settings.edgeSnapping ||
                                prev.autoFadeOnIdle != settings.autoFadeOnIdle ||
                                prev.idleFadeOpacity != settings.idleFadeOpacity

                        if (isVisualChange) {
                            overlayController?.updateSettings(settings)
                        }
                        lastAppliedSettings = settings
                    }
                } else {
                    overlayController?.hideBall()
                    lastAppliedSettings = null
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        overlayController?.destroy()
        overlayController = null
        if (instance == this) {
            instance = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.assistx.ACTION_START"
        const val ACTION_STOP = "com.example.assistx.ACTION_STOP"
        const val ACTION_TOGGLE = "com.example.assistx.ACTION_TOGGLE"
        const val ACTION_UPDATE_SETTINGS = "com.example.assistx.ACTION_UPDATE_SETTINGS"

        @Volatile
        var instance: FloatingBallService? = null
            private set

        fun isRunning(): Boolean = instance != null

        fun start(context: Context) {
            val intent = Intent(context, FloatingBallService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingBallService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun updateSettings(context: Context) {
            if (isRunning()) {
                val intent = Intent(context, FloatingBallService::class.java).apply {
                    action = ACTION_UPDATE_SETTINGS
                }
                context.startService(intent)
            }
        }
    }
}
