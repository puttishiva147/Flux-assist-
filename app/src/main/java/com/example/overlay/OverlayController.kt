package com.example.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import com.example.actions.ActionExecutor
import com.example.data.models.ActionType
import com.example.data.models.BallSettings
import com.example.data.models.FloatingShape
import com.example.data.repository.SettingsRepository
import com.example.utils.AppHelper

class OverlayController(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val actionExecutor = ActionExecutor(context)

    private var ballView: FloatingBallView? = null
    private var ballLayoutParams: WindowManager.LayoutParams? = null

    private var menuView: ShortcutMenuView? = null
    private var radialMenuView: RadialShortcutMenuView? = null
    private var brightnessOverlay: BrightnessSliderOverlay? = null
    private var appPickerOverlay: AppPickerOverlay? = null

    private var isBallAttached = false

    fun isAttached(): Boolean = isBallAttached

    private fun getDimensions(settings: BallSettings): Pair<Int, Int> {
        val basePx = dpToPx(settings.ballSizeDp)
        return when (settings.floatingShape) {
            FloatingShape.PILL_BAR -> Pair((basePx * 1.35f).toInt(), (basePx * 0.65f).toInt())
            FloatingShape.SPHERE -> Pair(basePx, basePx)
        }
    }

    fun showBall() {
        if (isBallAttached || !Settings.canDrawOverlays(context)) return

        AppHelper.preloadAppsAsync(context)

        val settings = settingsRepository.getSettings()
        val (ballW, ballH) = getDimensions(settings)

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Free positioning: can be placed anywhere on screen
        val initialX = if (settings.lastPosX in 0..(screenWidth - ballW)) {
            settings.lastPosX
        } else {
            screenWidth - ballW
        }

        val initialY = if (settings.lastPosY in 0..(screenHeight - ballH)) {
            settings.lastPosY
        } else {
            screenHeight / 2 - ballH / 2
        }

        val params = WindowManager.LayoutParams(
            ballW,
            ballH,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = initialX
            y = initialY
        }
        ballLayoutParams = params

        ballView = FloatingBallView(
            context = context,
            windowManager = windowManager,
            layoutParams = params,
            settings = settings,
            onBallTapped = { openMenu() },
            onBallLongPressed = { openMenu() },
            onPositionChanged = { newX, newY ->
                settingsRepository.savePosition(newX, newY)
            }
        )

        try {
            windowManager.addView(ballView, params)
            isBallAttached = true
        } catch (e: Exception) {
            isBallAttached = false
        }
    }

    fun hideBall() {
        closeMenu()
        closeBrightnessSlider()
        closeAppPicker()
        if (isBallAttached && ballView != null) {
            try {
                windowManager.removeView(ballView)
            } catch (ignored: Exception) {
            }
            ballView = null
            isBallAttached = false
        }
    }

    fun updateSettings(newSettings: BallSettings) {
        ballView?.updateSettings(newSettings)

        val (newW, newH) = getDimensions(newSettings)
        ballLayoutParams?.let { params ->
            if (params.width != newW || params.height != newH) {
                params.width = newW
                params.height = newH
                ballView?.let { view ->
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (ignored: Exception) {
                    }
                }
            }
        }
    }

    private fun openMenu() {
        // Toggle menu or cancel ongoing closing to support repeated rapid taps flawlessly
        if (radialMenuView != null) {
            if (radialMenuView?.isClosingMenu == true) {
                closeMenu()
            } else {
                radialMenuView?.dismissWithAnimation()
                return
            }
        }
        if (menuView != null) {
            if (menuView?.isClosingMenu == true) {
                closeMenu()
            } else {
                menuView?.dismissWithAnimation()
                return
            }
        }

        val settings = settingsRepository.getSettings()
        val currentBallX = ballLayoutParams?.x ?: 0
        val currentBallY = ballLayoutParams?.y ?: 0
        val ballSize = ballLayoutParams?.width ?: dpToPx(settings.ballSizeDp)

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            menuParams.flags = menuParams.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            menuParams.blurBehindRadius = dpToPx(28)
        }

        if (settings.menuStyle == com.example.data.models.MenuStyle.RADIAL_ORBIT) {
            radialMenuView = RadialShortcutMenuView(
                context = context,
                windowManager = windowManager,
                settings = settings,
                ballX = currentBallX,
                ballY = currentBallY,
                ballSize = ballSize,
                onActionClicked = { action ->
                    actionExecutor.execute(
                        action = action,
                        onShowBrightnessSlider = { showBrightnessSlider() },
                        onCloseMenu = { closeMenu() }
                    )
                },
                onDismiss = {
                    closeMenu()
                }
            )
            try {
                windowManager.addView(radialMenuView, menuParams)
            } catch (e: Exception) {
                radialMenuView = null
            }
        } else {
            menuView = ShortcutMenuView(
                context = context,
                windowManager = windowManager,
                settings = settings,
                ballX = currentBallX,
                ballY = currentBallY,
                ballSize = ballSize,
                onActionClicked = { action ->
                    actionExecutor.execute(
                        action = action,
                        onShowBrightnessSlider = { showBrightnessSlider() },
                        onCloseMenu = { closeMenu() }
                    )
                },
                onAppClicked = { pkg ->
                    AppHelper.launchApp(context, pkg)
                },
                onAddAppClicked = {
                    showAppPicker()
                },
                onDismiss = {
                    closeMenu()
                }
            )
            try {
                windowManager.addView(menuView, menuParams)
            } catch (e: Exception) {
                menuView = null
            }
        }
    }

    fun closeMenu() {
        menuView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (ignored: Exception) {
            }
            menuView = null
        }
        radialMenuView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (ignored: Exception) {
            }
            radialMenuView = null
        }
        ballView?.resetIdleTimer()
    }

    fun showAppPicker() {
        if (appPickerOverlay != null) return

        val settings = settingsRepository.getSettings()
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        appPickerOverlay = AppPickerOverlay(
            context = context,
            settings = settings,
            onAppSelected = { pkg ->
                settingsRepository.addQuickApp(pkg)
                closeAppPicker()
                // Reopen updated menu
                openMenu()
            },
            onDismiss = {
                closeAppPicker()
            }
        )

        try {
            windowManager.addView(appPickerOverlay, params)
        } catch (e: Exception) {
            appPickerOverlay = null
        }
    }

    fun closeAppPicker() {
        appPickerOverlay?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (ignored: Exception) {
            }
            appPickerOverlay = null
        }
    }

    fun showBrightnessSlider() {
        if (brightnessOverlay != null) return

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        brightnessOverlay = BrightnessSliderOverlay(context) {
            closeBrightnessSlider()
        }

        try {
            windowManager.addView(brightnessOverlay, params)
        } catch (e: Exception) {
            brightnessOverlay = null
        }
    }

    fun closeBrightnessSlider() {
        brightnessOverlay?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (ignored: Exception) {
            }
            brightnessOverlay = null
        }
    }

    fun destroy() {
        hideBall()
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}
