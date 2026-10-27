package com.example.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.example.R
import com.example.data.models.BallSettings
import com.example.utils.AppHelper
import com.example.utils.AppItem

class AppPickerOverlay(
    context: Context,
    private val settings: BallSettings,
    private val onAppSelected: (String) -> Unit,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    private val cardContainer: LinearLayout
    private var isClosing = false

    private val colorBgCard = Color.parseColor("#F414171E")
    private val colorBorderCard = Color.parseColor("#2A3342")
    private val colorTileBg = Color.parseColor("#1C212B")
    private val colorTileBorder = Color.parseColor("#2A3342")
    private val colorAccentBlue = Color.parseColor("#3B82F6")
    private val colorTextPrimary = Color.parseColor("#F8FAFC")
    private val colorTextSecondary = Color.parseColor("#94A3B8")

    init {
        setBackgroundColor(Color.parseColor("#66000000"))
        isFocusable = true
        isFocusableInTouchMode = true
        setOnClickListener {
            dismissWithAnimation()
        }

        cardContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(18)
            setPadding(pad, pad, pad, pad)
            isClickable = true
            isFocusable = true

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24).toFloat()
                setColor(colorBgCard)
                setStroke(dpToPx(1), colorBorderCard)
            }
            background = bg
            elevation = dpToPx(24).toFloat()
        }

        buildHeader()
        buildAppList()

        val displayMetrics = resources.displayMetrics
        val cardWidth = (displayMetrics.widthPixels * 0.88f).toInt().coerceAtMost(dpToPx(340))
        val cardHeight = (displayMetrics.heightPixels * 0.65f).toInt()

        val cardParams = LayoutParams(cardWidth, cardHeight).apply {
            gravity = Gravity.CENTER
        }
        addView(cardContainer, cardParams)

        cardContainer.scaleX = 0.88f
        cardContainer.scaleY = 0.88f
        cardContainer.alpha = 0f

        post { animateOpen() }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            dismissWithAnimation()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun buildHeader() {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(12))
        }

        val dot = View(context).apply {
            val size = dpToPx(8)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dpToPx(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorAccentBlue)
            }
        }

        val title = TextView(context).apply {
            text = "Pick Quick Launch App"
            setTextColor(colorTextPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(colorTextSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            setOnClickListener { dismissWithAnimation() }
        }

        row.addView(dot)
        row.addView(title)
        row.addView(closeBtn)
        cardContainer.addView(row)
    }

    private fun buildAppList() {
        val scrollView = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val listLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        val apps = AppHelper.getInstalledLauncherApps(context)

        for (app in apps) {
            val itemView = createAppRow(app)
            listLayout.addView(itemView)
        }

        scrollView.addView(listLayout)
        cardContainer.addView(scrollView)
    }

    private fun createAppRow(app: AppItem): View {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = dpToPx(10)
            val padV = dpToPx(8)
            setPadding(padH, padV, padH, padV)
            val marginV = dpToPx(2)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, marginV, 0, marginV)
            }
            background = createRipple(Color.TRANSPARENT, Color.parseColor("#263B82F6"), dpToPx(12).toFloat())
            isClickable = true
            isFocusable = true
            setOnClickListener {
                onAppSelected(app.packageName)
            }
        }

        val iconContainer = FrameLayout(context).apply {
            val s = dpToPx(38)
            layoutParams = LinearLayout.LayoutParams(s, s)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(10).toFloat()
                setColor(colorTileBg)
                setStroke(dpToPx(1), colorTileBorder)
            }

            val iconView = ImageView(context).apply {
                val iconSize = dpToPx(26)
                layoutParams = LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                }
                if (app.icon != null) {
                    setImageDrawable(app.icon)
                } else {
                    setImageResource(R.drawable.ic_action_settings)
                }
            }
            addView(iconView)
        }

        val label = TextView(context).apply {
            text = app.appName
            setTextColor(colorTextPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dpToPx(12)
            }
        }

        val addText = TextView(context).apply {
            text = "+"
            setTextColor(colorAccentBlue)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(8), 0, dpToPx(8), 0)
        }

        row.addView(iconContainer)
        row.addView(label)
        row.addView(addText)

        return row
    }

    private fun animateOpen() {
        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200L
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

        val anim = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 150L
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

    private fun createRipple(contentColor: Int, rippleColor: Int, radius: Float): RippleDrawable {
        val content = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(contentColor)
        }
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(Color.WHITE)
        }
        return RippleDrawable(
            android.content.res.ColorStateList.valueOf(rippleColor),
            content,
            mask
        )
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
