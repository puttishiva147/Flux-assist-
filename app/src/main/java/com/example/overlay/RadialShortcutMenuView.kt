package com.example.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.R
import com.example.actions.ActionExecutor
import com.example.data.models.ActionType
import com.example.data.models.BallSettings
import com.example.data.models.FloatingShape
import com.example.haptics.HapticManager
import com.example.haptics.HapticType
import com.example.ui.motion.MotionSystem
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class RadialShortcutMenuView(
    context: Context,
    private val windowManager: WindowManager,
    private val settings: BallSettings,
    private val ballX: Int,
    private val ballY: Int,
    private val ballSize: Int,
    private val onActionClicked: (ActionType) -> Unit,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    private val hapticManager = HapticManager.getInstance(context)
    private var isClosing = false
    val isClosingMenu: Boolean get() = isClosing
    private var currentDismissAnim: ValueAnimator? = null

    // Sophisticated Graphite & Refined Blue Design Tokens
    private val colorTileBg = Color.parseColor("#1C212B")
    private val colorTileBorder = Color.parseColor("#2A3342")
    private val colorAccentBlue = settings.radialAccentColor.toInt()
    private val colorTextPrimary = Color.parseColor("#F8FAFC")
    private val colorTextSecondary = Color.parseColor("#94A3B8")

    private val centerFabContainer: FrameLayout
    private val centerPlusText: TextView
    private val satelliteViews = mutableListOf<View>()
    private val satelliteOffsets = mutableListOf<Pair<Float, Float>>()

    private val orbitRadiusPx: Float
    private val orbitCenterX: Int
    private val orbitCenterY: Int
    private val isPillBar: Boolean

    init {
        isPillBar = settings.radialCenterShape == FloatingShape.PILL_BAR

        // Scrim background with Glassmorphism blur
        setBackgroundColor(Color.parseColor("#4410141C"))
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            try {
                setRenderEffect(android.graphics.RenderEffect.createBlurEffect(24f, 24f, android.graphics.Shader.TileMode.CLAMP))
            } catch (ignored: Exception) {
            }
        }
        isFocusable = true
        isFocusableInTouchMode = true
        setOnClickListener {
            dismissWithAnimation()
        }

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        orbitRadiusPx = dpToPx(106).toFloat()
        val marginBuffer = (orbitRadiusPx + dpToPx(38)).toInt()

        // Clamp the orbit center so the full circular orbit is guaranteed visible on screen
        orbitCenterX = (ballX + ballSize / 2).coerceIn(marginBuffer, (screenWidth - marginBuffer).coerceAtLeast(marginBuffer))
        orbitCenterY = (ballY + ballSize / 2).coerceIn(marginBuffer, (screenHeight - marginBuffer).coerceAtLeast(marginBuffer))

        // Minimalist Glassmorphism circular disc framing the radial orbit
        val glassDiameter = ((orbitRadiusPx + dpToPx(38)) * 2).toInt()
        val glassOrb = View(context).apply {
            val p = LayoutParams(glassDiameter, glassDiameter).apply {
                leftMargin = orbitCenterX - glassDiameter / 2
                topMargin = orbitCenterY - glassDiameter / 2
            }
            layoutParams = p
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#3319202B"))
                setStroke(dpToPx(1f), Color.parseColor("#25FFFFFF"))
            }
            elevation = dpToPx(1).toFloat()
        }
        addView(glassOrb)

        val actions = settings.actions
        val count = actions.size

        // Calculate Trigonometric Orbit Offsets:
        // angle = -90° (top / 12 o'clock) + i * (360° / N)
        // dx = radius * cos(angle)
        // dy = radius * sin(angle)
        for (i in 0 until count) {
            val angleDeg = -90.0 + (i * (360.0 / count))
            val angleRad = Math.toRadians(angleDeg)
            val dx = (orbitRadiusPx * cos(angleRad)).toFloat()
            val dy = (orbitRadiusPx * sin(angleRad)).toFloat()
            satelliteOffsets.add(Pair(dx, dy))

            val satelliteView = createSatelliteItem(actions[i])
            addView(satelliteView)
            satelliteViews.add(satelliteView)
        }

        // Central Trigger: Floating Bar or Circular Orb with '+'
        val fabW = if (isPillBar) dpToPx(84) else dpToPx(54)
        val fabH = if (isPillBar) dpToPx(42) else dpToPx(54)

        centerFabContainer = FrameLayout(context).apply {
            val params = LayoutParams(fabW, fabH).apply {
                leftMargin = orbitCenterX - fabW / 2
                topMargin = orbitCenterY - fabH / 2
            }
            layoutParams = params

            background = GradientDrawable().apply {
                if (isPillBar) {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = fabH / 2f
                } else {
                    shape = GradientDrawable.OVAL
                }
                setColor(Color.parseColor("#14171E"))
                setStroke(dpToPx(1.5f), colorAccentBlue)
            }
            elevation = dpToPx(16).toFloat()
            isClickable = true
            isFocusable = true
        }

        if (isPillBar) {
            // Left and right accent indicator dots matching the floating bar
            val dotSize = dpToPx(4)
            val leftDot = View(context).apply {
                layoutParams = LayoutParams(dotSize, dotSize).apply {
                    gravity = Gravity.CENTER_VERTICAL or Gravity.START
                    marginStart = dpToPx(10)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#94A3B8"))
                }
            }
            val rightDot = View(context).apply {
                layoutParams = LayoutParams(dotSize, dotSize).apply {
                    gravity = Gravity.CENTER_VERTICAL or Gravity.END
                    marginEnd = dpToPx(10)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#94A3B8"))
                }
            }
            centerFabContainer.addView(leftDot)
            centerFabContainer.addView(rightDot)
        }

        centerPlusText = TextView(context).apply {
            text = "+"
            setTextColor(colorAccentBlue)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isPillBar) 24f else 28f)
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }

        centerFabContainer.addView(centerPlusText)
        addView(centerFabContainer)

        centerFabContainer.setOnClickListener {
            dismissWithAnimation()
        }

        // Initial entrance state (collapsed at center)
        for (view in satelliteViews) {
            view.scaleX = 0f
            view.scaleY = 0f
            view.alpha = 0f
            view.translationX = 0f
            view.translationY = 0f
        }

        post {
            animateOpen()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            dismissWithAnimation()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun createSatelliteItem(action: ActionType): View {
        val isFlashlightActive = action == ActionType.FLASHLIGHT && ActionExecutor.isFlashlightOn
        val isFrequentApp = action == ActionType.FREQUENT_APP
        val frequentApp = if (isFrequentApp) com.example.utils.AppUsageTracker.getMostFrequentlyUsedApp(context) else null
        val itemSize = dpToPx(50)

        val container = FrameLayout(context).apply {
            val params = LayoutParams(itemSize, itemSize).apply {
                leftMargin = orbitCenterX - itemSize / 2
                topMargin = orbitCenterY - itemSize / 2
            }
            layoutParams = params

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (isFlashlightActive) {
                    setColor(Color.parseColor("#2A3B82F6"))
                    setStroke(dpToPx(1.5f), colorAccentBlue)
                } else if (isFrequentApp) {
                    setColor(Color.parseColor("#1C1E2E"))
                    setStroke(dpToPx(1.5f), colorAccentBlue)
                } else {
                    setColor(colorTileBg)
                    setStroke(dpToPx(1f), colorTileBorder)
                }
            }
            elevation = dpToPx(10).toFloat()
            isClickable = true
            isFocusable = true
        }

        val customIconId = settings.customIcons[action.id]
        val iconRes = com.example.data.models.ShortcutIconRegistry.getIconRes(customIconId, action.iconRes)

        val iconView = ImageView(context).apply {
            val iconSize = dpToPx(if (isFrequentApp && frequentApp?.icon != null) 26 else 22)
            layoutParams = LayoutParams(iconSize, iconSize).apply {
                gravity = Gravity.CENTER
            }
            if (isFrequentApp && frequentApp?.icon != null) {
                setImageDrawable(frequentApp.icon)
                clearColorFilter()
            } else {
                setImageResource(iconRes)
                if (isFlashlightActive || isFrequentApp || customIconId != null) {
                    setColorFilter(colorAccentBlue)
                } else {
                    setColorFilter(colorTextPrimary)
                }
            }
        }
        container.addView(iconView)

        // Dynamic badge indicator for Frequently Used slot or customized icon
        if (isFrequentApp || customIconId != null) {
            val badge = View(context).apply {
                val badgeSize = dpToPx(if (isFrequentApp) 7 else 5)
                layoutParams = LayoutParams(badgeSize, badgeSize).apply {
                    gravity = Gravity.TOP or Gravity.END
                    topMargin = dpToPx(5)
                    marginEnd = dpToPx(5)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(colorAccentBlue)
                }
            }
            container.addView(badge)
        }

        setupMicroInteraction(
            touchTarget = container,
            onLongAction = {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("extra_customize_action_icon", action.id)
                }
                try {
                    context.startActivity(intent)
                } catch (ignored: Exception) {
                }
                dismissWithAnimation()
            }
        ) {
            if (isFrequentApp && frequentApp != null) {
                com.example.utils.AppUsageTracker.recordAppLaunch(context, frequentApp.packageName)
                com.example.utils.AppHelper.launchApp(context, frequentApp.packageName)
                dismissWithAnimation()
            } else {
                onActionClicked(action)
            }
        }

        return container
    }

    private fun setupMicroInteraction(
        touchTarget: View,
        onLongAction: (() -> Unit)? = null,
        onAction: () -> Unit
    ) {
        val longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var isLongPressFired = false
        val longPressRunnable = Runnable {
            isLongPressFired = true
            hapticManager.performHaptic(HapticType.MEDIUM, settings, isActionEvent = true)
            MotionSystem.animateRelease(touchTarget)
            onLongAction?.invoke()
        }

        touchTarget.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPressFired = false
                    MotionSystem.animatePressDown(touchTarget, 0.88f)
                    hapticManager.performHaptic(HapticType.TINY, settings, isActionEvent = true)
                    if (onLongAction != null) {
                        longPressHandler.postDelayed(longPressRunnable, 480L)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    if (!isLongPressFired) {
                        MotionSystem.animateRelease(touchTarget) {
                            if (!isClosing) {
                                onAction()
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    MotionSystem.animateRelease(touchTarget)
                    true
                }
                else -> false
            }
        }
    }

    private fun animateOpen() {
        hapticManager.performHaptic(HapticType.LIGHT, settings)

        val isReduced = MotionSystem.isReducedMotion(context)
        val duration = if (isReduced) 120L else 280L

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = OvershootInterpolator(1.3f)
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                // Rotate central icon from '+' to '✕' (0 -> 135 deg)
                centerPlusText.rotation = 135f * fraction
                if (!isPillBar) {
                    centerFabContainer.rotation = 135f * fraction
                } else {
                    centerFabContainer.scaleX = 1f + 0.05f * fraction
                    centerFabContainer.scaleY = 1f + 0.05f * fraction
                }

                // Trigonometric expansion of satellites outward along orbital vectors
                for (i in satelliteViews.indices) {
                    val view = satelliteViews[i]
                    val (targetDx, targetDy) = satelliteOffsets[i]
                    view.translationX = targetDx * fraction
                    view.translationY = targetDy * fraction
                    view.scaleX = (fraction * 1.05f).coerceIn(0f, 1f)
                    view.scaleY = (fraction * 1.05f).coerceIn(0f, 1f)
                    view.alpha = fraction.coerceIn(0f, 1f)
                }
            }
        }
        anim.start()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isClosing && ev.action == MotionEvent.ACTION_DOWN) {
            // Rapid tap during dismissal: finalize dismiss instantly so touch is never swallowed
            dismissWithAnimation(instant = true)
            return false
        }
        return super.dispatchTouchEvent(ev)
    }

    fun dismissWithAnimation(instant: Boolean = false) {
        if (isClosing) {
            currentDismissAnim?.cancel()
            currentDismissAnim = null
            onDismiss()
            return
        }
        isClosing = true

        if (instant) {
            onDismiss()
            return
        }

        hapticManager.performHaptic(HapticType.TINY, settings)

        val isReduced = MotionSystem.isReducedMotion(context)
        val duration = if (isReduced) 60L else 140L

        currentDismissAnim?.cancel()
        val anim = ValueAnimator.ofFloat(1f, 0f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedValue as Float
                centerPlusText.rotation = 135f * fraction
                if (!isPillBar) {
                    centerFabContainer.rotation = 135f * fraction
                } else {
                    centerFabContainer.scaleX = 1f + 0.05f * fraction
                    centerFabContainer.scaleY = 1f + 0.05f * fraction
                }

                for (i in satelliteViews.indices) {
                    val view = satelliteViews[i]
                    val (targetDx, targetDy) = satelliteOffsets[i]
                    view.translationX = targetDx * fraction
                    view.translationY = targetDy * fraction
                    view.scaleX = fraction
                    view.scaleY = fraction
                    view.alpha = fraction
                }
                alpha = fraction
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    currentDismissAnim = null
                    onDismiss()
                }
            })
        }
        currentDismissAnim = anim
        anim.start()
    }

    private fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
        ).toInt()
    }

    private fun dpToPx(dp: Int): Int = dpToPx(dp.toFloat())
}
