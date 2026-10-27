package com.example.ui.motion

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

object MotionSystem {

    val ENTER_INTERPOLATOR: TimeInterpolator = OvershootInterpolator(1.2f)
    val EXIT_INTERPOLATOR: TimeInterpolator = DecelerateInterpolator(1.5f)
    val PRESS_INTERPOLATOR: TimeInterpolator = DecelerateInterpolator()

    const val DURATION_PRESS_MS = 140L
    const val DURATION_RELEASE_MS = 200L
    const val DURATION_ENTER_MS = 260L
    const val DURATION_EXIT_MS = 180L

    fun isReducedMotion(context: Context): Boolean {
        return try {
            val scale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            scale <= 0.05f
        } catch (e: Exception) {
            false
        }
    }

    fun animatePressDown(view: View, targetScale: Float = 0.92f) {
        view.animate().cancel()
        val duration = if (isReducedMotion(view.context)) 40L else DURATION_PRESS_MS
        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(duration)
            .setInterpolator(PRESS_INTERPOLATOR)
            .start()
    }

    fun animateRelease(view: View, onEnd: (() -> Unit)? = null) {
        view.animate().cancel()
        val duration = if (isReducedMotion(view.context)) 60L else DURATION_RELEASE_MS
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(duration)
            .setInterpolator(ENTER_INTERPOLATOR)
            .withEndAction { onEnd?.invoke() }
            .start()
    }

    fun animateShake(view: View) {
        val anim = ValueAnimator.ofFloat(0f, -15f, 15f, -10f, 10f, -5f, 5f, 0f).apply {
            duration = 320L
            addUpdateListener {
                view.translationX = it.animatedValue as Float
            }
        }
        anim.start()
    }
}
