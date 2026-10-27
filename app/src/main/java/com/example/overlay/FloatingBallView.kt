package com.example.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.example.data.models.BallSettings
import com.example.data.models.FloatingShape
import com.example.haptics.HapticManager
import com.example.haptics.HapticType
import com.example.ui.motion.MotionSystem
import kotlin.math.hypot

class FloatingBallView(
    context: Context,
    private val windowManager: WindowManager,
    private val layoutParams: WindowManager.LayoutParams,
    private var settings: BallSettings,
    private val onBallTapped: () -> Unit,
    private val onBallLongPressed: () -> Unit,
    private val onPositionChanged: (x: Int, y: Int) -> Unit
) : View(context) {

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val hapticManager = HapticManager.getInstance(context)

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var touchDownTime = 0L
    private var isDragging = false
    private var hasMoved = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val longPressRunnable = Runnable {
        if (!isDragging && !hasMoved) {
            hapticManager.performHaptic(HapticType.LONG_PRESS, settings)
            scaleAnim(currentScale, 1.12f) {
                scaleAnim(1.12f, 1.0f)
            }
            onBallLongPressed()
        }
    }

    private val idleFadeRunnable = Runnable {
        if (settings.autoFadeOnIdle) {
            fadeTo(settings.idleFadeOpacity)
        }
    }

    private var currentAlpha = settings.ballOpacity
    private var currentScale = 1.0f
    private var currentScaleAnim: ValueAnimator? = null
    private var currentFadeAnim: ValueAnimator? = null
    private var currentSnapAnim: ValueAnimator? = null

    private val outerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val midRingPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val innerCorePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val touchDotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectF = RectF()

    private var cachedShaderW = -1f
    private var cachedShaderH = -1f
    private var cachedShaderShape: FloatingShape? = null
    private var cachedShaderStyle: com.example.data.models.BallStyle? = null

    init {
        isClickable = true
        isFocusable = false
        resetIdleTimer()
    }

    fun updateSettings(newSettings: BallSettings) {
        val needsInvalidate = settings.floatingShape != newSettings.floatingShape ||
                settings.ballStyle != newSettings.ballStyle ||
                settings.ballOpacity != newSettings.ballOpacity ||
                settings.ballSizeDp != newSettings.ballSizeDp

        this.settings = newSettings
        currentAlpha = newSettings.ballOpacity
        if (needsInvalidate) {
            resetIdleTimer()
            invalidate()
        }
    }

    fun resetIdleTimer() {
        mainHandler.removeCallbacks(idleFadeRunnable)
        currentFadeAnim?.cancel()
        if (currentAlpha != settings.ballOpacity) {
            currentAlpha = settings.ballOpacity
            alpha = currentAlpha
        }
        if (settings.autoFadeOnIdle) {
            mainHandler.postDelayed(idleFadeRunnable, 3000)
        }
    }

    private fun fadeTo(targetAlpha: Float) {
        currentFadeAnim?.cancel()
        val duration = if (MotionSystem.isReducedMotion(context)) 100L else 320L
        currentFadeAnim = ValueAnimator.ofFloat(currentAlpha, targetAlpha).apply {
            this.duration = duration
            addUpdateListener {
                currentAlpha = it.animatedValue as Float
                alpha = currentAlpha
            }
            start()
        }
    }

    private fun scaleAnim(from: Float, to: Float, onEnd: (() -> Unit)? = null) {
        currentScaleAnim?.cancel()
        val duration = if (MotionSystem.isReducedMotion(context)) 60L else 140L
        currentScaleAnim = ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            interpolator = OvershootInterpolator(1.2f)
            addUpdateListener {
                currentScale = it.animatedValue as Float
                invalidate()
            }
            if (onEnd != null) {
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        onEnd()
                    }
                })
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Instantly cancel any ongoing animations immediately so touch reacts without hitch
                currentSnapAnim?.cancel()
                currentSnapAnim = null
                currentScaleAnim?.cancel()
                currentScaleAnim = null
                currentFadeAnim?.cancel()
                currentFadeAnim = null

                // Instant visual reaction on the exact touch frame (<1ms)
                currentAlpha = settings.ballOpacity
                alpha = currentAlpha
                currentScale = 0.90f
                isPressed = true
                invalidate()

                resetIdleTimer()
                touchDownTime = android.os.SystemClock.uptimeMillis()
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                hasMoved = false

                // Non-blocking tactile feedback
                hapticManager.performHaptic(HapticType.TINY, settings)

                mainHandler.removeCallbacks(longPressRunnable)
                mainHandler.postDelayed(longPressRunnable, longPressTimeout)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()

                if (!hasMoved) {
                    val distSq = dx * dx + dy * dy
                    if (distSq > touchSlop * touchSlop) {
                        hasMoved = true
                        isDragging = true
                        mainHandler.removeCallbacks(longPressRunnable)
                        hapticManager.performHaptic(HapticType.TINY, settings, isDragEvent = true)
                    }
                }

                if (isDragging) {
                    val displayMetrics = resources.displayMetrics
                    val screenWidth = displayMetrics.widthPixels
                    val screenHeight = displayMetrics.heightPixels

                    // Free movement: move anywhere across entire screen
                    val newX = (initialX + dx).coerceIn(0, screenWidth - width)
                    val newY = (initialY + dy).coerceIn(0, screenHeight - height)

                    if (layoutParams.x != newX || layoutParams.y != newY) {
                        layoutParams.x = newX
                        layoutParams.y = newY
                        try {
                            windowManager.updateViewLayout(this, layoutParams)
                        } catch (ignored: Exception) {
                        }
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mainHandler.removeCallbacks(longPressRunnable)
                resetIdleTimer()

                isPressed = false
                currentScale = 1.0f
                invalidate()

                val touchDuration = android.os.SystemClock.uptimeMillis() - touchDownTime
                val dx = (event.rawX - initialTouchX).toInt()
                val dy = (event.rawY - initialTouchY).toInt()
                val distSq = dx * dx + dy * dy

                // High-precision tap detection: handles quick touches and subtle finger rolls
                val isTap = !isDragging && (!hasMoved || (touchDuration < 280 && distSq < touchSlop * touchSlop * 2.25f))

                if (isTap) {
                    // Tap: instant trigger with zero debounce or throttle delay (<1ms)
                    hapticManager.performHaptic(HapticType.LIGHT, settings)
                    onBallTapped()
                } else if (isDragging) {
                    hapticManager.performHaptic(HapticType.MEDIUM, settings, isDragEvent = true)
                    if (settings.edgeSnapping) {
                        snapToNearestEdge()
                    } else {
                        onPositionChanged(layoutParams.x, layoutParams.y)
                    }
                }
                isDragging = false
                hasMoved = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun snapToNearestEdge() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val currentX = layoutParams.x
        val targetX = if (currentX + width / 2 < screenWidth / 2) 0 else screenWidth - width

        currentSnapAnim?.cancel()
        val duration = if (MotionSystem.isReducedMotion(context)) 80L else 240L
        currentSnapAnim = ValueAnimator.ofInt(currentX, targetX).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                layoutParams.x = it.animatedValue as Int
                try {
                    windowManager.updateViewLayout(this@FloatingBallView, layoutParams)
                } catch (ignored: Exception) {
                }
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    currentSnapAnim = null
                    onPositionChanged(targetX, layoutParams.y)
                }
            })
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        canvas.save()
        canvas.scale(currentScale, currentScale, cx, cy)

        if (settings.floatingShape == FloatingShape.PILL_BAR) {
            drawPillBar(canvas, w, h, cx, cy)
        } else {
            drawSphereOrb(canvas, w, h, cx, cy)
        }

        canvas.restore()
    }

    private fun drawPillBar(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        val style = settings.ballStyle
        val cornerRadius = h / 2f
        val pad = h * 0.08f

        // 1. Outer Glow around Pill
        outerGlowPaint.color = style.glowColor.toInt()
        outerGlowPaint.style = Paint.Style.FILL
        rectF.set(0f, 0f, w, h)
        canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, outerGlowPaint)

        // 2. Translucent Graphite Glass Body (Shader cached to avoid allocations)
        rectF.set(pad, pad, w - pad, h - pad)
        if (cachedShaderW != w || cachedShaderH != h || cachedShaderShape != FloatingShape.PILL_BAR || cachedShaderStyle != style) {
            cachedShaderW = w
            cachedShaderH = h
            cachedShaderShape = FloatingShape.PILL_BAR
            cachedShaderStyle = style
            innerCorePaint.shader = LinearGradient(
                0f, pad, 0f, h - pad,
                intArrayOf(0xEE2A3342.toInt(), style.innerCoreColor.toInt()),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rectF, cornerRadius - pad, cornerRadius - pad, innerCorePaint)

        // 3. Crisp Glass Border
        midRingPaint.color = style.ringColor.toInt()
        midRingPaint.style = Paint.Style.STROKE
        midRingPaint.strokeWidth = pad * 1.1f
        canvas.drawRoundRect(rectF, cornerRadius - pad, cornerRadius - pad, midRingPaint)

        // 4. Center Grip Dash (Refined Blue Accent)
        val dashW = w * 0.32f
        val dashH = h * 0.18f
        centerDotPaint.color = style.primaryColor.toInt()
        centerDotPaint.style = Paint.Style.FILL
        rectF.set(cx - dashW / 2f, cy - dashH / 2f, cx + dashW / 2f, cy + dashH / 2f)
        canvas.drawRoundRect(rectF, dashH / 2f, dashH / 2f, centerDotPaint)

        // 5. Left and Right Accent Dots
        val dotR = h * 0.09f
        touchDotPaint.color = 0xEE94A3B8.toInt()
        touchDotPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx - w * 0.28f, cy, dotR, touchDotPaint)
        canvas.drawCircle(cx + w * 0.28f, cy, dotR, touchDotPaint)
    }

    private fun drawSphereOrb(canvas: Canvas, w: Float, h: Float, cx: Float, cy: Float) {
        val radius = Math.min(w, h) / 2f
        val style = settings.ballStyle

        // 1. Outer Soft Glow
        outerGlowPaint.color = style.glowColor.toInt()
        outerGlowPaint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, radius, outerGlowPaint)

        // 2. Mid Ring (Glass Rim)
        midRingPaint.color = style.ringColor.toInt()
        midRingPaint.style = Paint.Style.STROKE
        midRingPaint.strokeWidth = radius * 0.08f
        canvas.drawCircle(cx, cy, radius * 0.82f, midRingPaint)

        // 3. Inner Core with Radial Gradient (Shader cached to avoid allocations)
        val coreRadius = radius * 0.65f
        if (cachedShaderW != w || cachedShaderH != h || cachedShaderShape != FloatingShape.SPHERE || cachedShaderStyle != style) {
            cachedShaderW = w
            cachedShaderH = h
            cachedShaderShape = FloatingShape.SPHERE
            cachedShaderStyle = style
            innerCorePaint.shader = RadialGradient(
                cx - coreRadius * 0.25f,
                cy - coreRadius * 0.25f,
                coreRadius,
                intArrayOf(0xEE2A3342.toInt(), style.innerCoreColor.toInt()),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, coreRadius, innerCorePaint)

        // 4. Center Focus Dot (Refined Blue Accent)
        centerDotPaint.color = style.primaryColor.toInt()
        canvas.drawCircle(cx, cy, radius * 0.16f, centerDotPaint)

        // 5. Directional Accent Dots
        val dotRadius = radius * 0.045f
        val dotOffset = radius * 0.82f
        touchDotPaint.color = 0xEE94A3B8.toInt()
        canvas.drawCircle(cx, cy - dotOffset, dotRadius, touchDotPaint)
        canvas.drawCircle(cx, cy + dotOffset, dotRadius, touchDotPaint)
        canvas.drawCircle(cx - dotOffset, cy, dotRadius, touchDotPaint)
        canvas.drawCircle(cx + dotOffset, cy, dotRadius, touchDotPaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mainHandler.removeCallbacksAndMessages(null)
        currentScaleAnim?.cancel()
        currentFadeAnim?.cancel()
        currentSnapAnim?.cancel()
    }
}
