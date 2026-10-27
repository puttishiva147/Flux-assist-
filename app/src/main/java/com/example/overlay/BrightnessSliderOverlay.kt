package com.example.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.example.R
import com.example.data.repository.SettingsRepository
import com.example.haptics.HapticManager
import com.example.haptics.HapticType
import com.example.ui.motion.MotionSystem
import kotlin.math.abs

class BrightnessSliderOverlay(
    context: Context,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    private val cardContainer: LinearLayout
    private val hapticManager = HapticManager.getInstance(context)
    private val settings = SettingsRepository.getInstance(context).getSettings()
    private var isClosing = false
    private var lastHapticProgress = -1

    private val colorAccentBlue = Color.parseColor("#3B82F6")

    init {
        setBackgroundColor(Color.parseColor("#66000000"))
        isFocusable = true
        isFocusableInTouchMode = true
        setOnClickListener {
            dismissWithAnimation()
        }

        cardContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(20)
            setPadding(pad, pad, pad, pad)
            isClickable = true
            isFocusable = true

            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24).toFloat()
                setColor(Color.parseColor("#F414171E"))
                setStroke(dpToPx(1), Color.parseColor("#2A3342"))
            }
            background = bgDrawable
            elevation = dpToPx(20).toFloat()
        }

        buildContent()

        val cardParams = LayoutParams(
            dpToPx(290),
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        addView(cardContainer, cardParams)

        cardContainer.scaleX = 0.88f
        cardContainer.scaleY = 0.88f
        cardContainer.alpha = 0f

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

    private fun buildContent() {
        val headerRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val bottomPad = dpToPx(14)
            setPadding(0, 0, 0, bottomPad)
        }

        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_action_brightness)
            setColorFilter(colorAccentBlue)
            val s = dpToPx(22)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                marginEnd = dpToPx(10)
            }
        }

        val title = TextView(context).apply {
            text = "Screen Brightness"
            setTextColor(Color.parseColor("#F8FAFC"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.parseColor("#94A3B8"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            setOnClickListener { dismissWithAnimation() }
        }

        headerRow.addView(icon)
        headerRow.addView(title)
        headerRow.addView(closeBtn)
        cardContainer.addView(headerRow)

        val canWrite = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.System.canWrite(context)
        } else {
            true
        }

        val currentBrightness = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Exception) {
            128
        }
        lastHapticProgress = currentBrightness

        val percentText = TextView(context).apply {
            val pct = ((currentBrightness / 255f) * 100).toInt()
            text = "$pct%"
            setTextColor(colorAccentBlue)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.END
            setPadding(0, 0, dpToPx(4), dpToPx(4))
        }
        cardContainer.addView(percentText)

        val seekBar = SeekBar(context).apply {
            max = 255
            progress = currentBrightness
            progressTintList = ColorStateList.valueOf(colorAccentBlue)
            thumbTintList = ColorStateList.valueOf(colorAccentBlue)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    val pct = ((progress / 255f) * 100).toInt()
                    percentText.text = "$pct%"

                    if (fromUser) {
                        if (lastHapticProgress == -1 || abs(progress - lastHapticProgress) >= 22) {
                            lastHapticProgress = progress
                            hapticManager.performHaptic(HapticType.TINY, settings)
                        }

                        if (canWrite) {
                            try {
                                Settings.System.putInt(
                                    context.contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                                )
                                Settings.System.putInt(
                                    context.contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS,
                                    progress
                                )
                            } catch (ignored: Exception) {
                            }
                        }
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        cardContainer.addView(seekBar)

        if (!canWrite && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val warningText = TextView(context).apply {
                text = "⚠️ Grant 'Modify System Settings' permission to change brightness directly."
                setTextColor(Color.parseColor("#94A3B8"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                val topPad = dpToPx(12)
                setPadding(0, topPad, 0, dpToPx(6))
            }

            val grantBtn = TextView(context).apply {
                text = "Grant Permission"
                setTextColor(colorAccentBlue)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dpToPx(4), 0, dpToPx(4))
                setOnClickListener {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(fallback)
                    }
                    dismissWithAnimation()
                }
            }

            cardContainer.addView(warningText)
            cardContainer.addView(grantBtn)
        }
    }

    private fun animateOpen() {
        hapticManager.performHaptic(HapticType.LIGHT, settings)

        val isReduced = MotionSystem.isReducedMotion(context)
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (isReduced) 120L else 220L
            interpolator = MotionSystem.ENTER_INTERPOLATOR
            addUpdateListener {
                val fraction = it.animatedValue as Float
                cardContainer.scaleX = 0.88f + 0.12f * fraction
                cardContainer.scaleY = 0.88f + 0.12f * fraction
                cardContainer.alpha = fraction
            }
        }
        anim.start()
    }

    private fun dismissWithAnimation() {
        if (isClosing) return
        isClosing = true

        hapticManager.performHaptic(HapticType.TINY, settings)

        val isReduced = MotionSystem.isReducedMotion(context)
        val anim = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = if (isReduced) 80L else 160L
            interpolator = MotionSystem.EXIT_INTERPOLATOR
            addUpdateListener {
                val fraction = it.animatedValue as Float
                cardContainer.scaleX = 0.88f + 0.12f * fraction
                cardContainer.scaleY = 0.88f + 0.12f * fraction
                cardContainer.alpha = fraction
                alpha = fraction
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onDismiss()
                }
            })
        }
        anim.start()
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
