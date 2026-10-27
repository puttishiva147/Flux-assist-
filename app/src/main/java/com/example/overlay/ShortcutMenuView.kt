package com.example.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.R
import com.example.actions.ActionExecutor
import com.example.data.models.ActionType
import com.example.data.models.BallSettings
import com.example.haptics.HapticManager
import com.example.haptics.HapticType
import com.example.service.AssistXAccessibilityService
import com.example.ui.motion.MotionSystem
import com.example.utils.AppHelper

class ShortcutMenuView(
    context: Context,
    private val windowManager: WindowManager,
    private val settings: BallSettings,
    private val ballX: Int,
    private val ballY: Int,
    private val ballSize: Int,
    private val onActionClicked: (ActionType) -> Unit,
    private val onAppClicked: (String) -> Unit,
    private val onAddAppClicked: () -> Unit,
    private val onDismiss: () -> Unit
) : FrameLayout(context) {

    private val cardContainer: LinearLayout
    private val hapticManager = HapticManager.getInstance(context)
    private var isClosing = false
    val isClosingMenu: Boolean get() = isClosing
    private var currentDismissAnim: ValueAnimator? = null
    private val animatedItems = mutableListOf<View>()

    // Sophisticated Graphite & Refined Blue Design Tokens
    private val colorBgCard = Color.parseColor("#F414171E")
    private val colorBorderCard = Color.parseColor("#384252")
    private val colorTileBg = Color.parseColor("#1C212B")
    private val colorTileBorder = Color.parseColor("#2A3342")
    private val colorAccentBlue = Color.parseColor("#3B82F6")
    private val colorTextPrimary = Color.parseColor("#F8FAFC")
    private val colorTextSecondary = Color.parseColor("#94A3B8")
    private val colorTextMuted = Color.parseColor("#64748B")

    init {
        // Fullscreen frosted backdrop scrim
        setBackgroundColor(Color.parseColor("#66000000"))
        isFocusable = true
        isFocusableInTouchMode = true
        setOnClickListener {
            dismissWithAnimation()
        }

        // Main Graphite System Card Container
        cardContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dpToPx(16)
            setPadding(pad, pad, pad, pad)
            isClickable = true
            isFocusable = true

            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(24).toFloat()
                setColor(colorBgCard)
                setStroke(dpToPx(1), colorBorderCard)
            }
            background = bgDrawable
            elevation = dpToPx(24).toFloat()
        }

        buildHeader()
        if (settings.quickApps.isNotEmpty()) {
            buildQuickAppsSection()
            buildHairlineDivider()
        }
        buildActionGrid()
        buildSystemStatusFooter()

        val displayMetrics = resources.displayMetrics
        val cardWidth = (displayMetrics.widthPixels * 0.90f).toInt().coerceAtMost(dpToPx(340))

        val cardParams = LayoutParams(
            cardWidth,
            LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        addView(cardContainer, cardParams)

        // Initial entrance state
        cardContainer.scaleX = 0.88f
        cardContainer.scaleY = 0.88f
        cardContainer.alpha = 0f

        for (item in animatedItems) {
            item.translationY = dpToPx(10).toFloat()
            item.alpha = 0f
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

    private fun buildHeader() {
        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(12))
        }

        // Refined Blue Status Dot
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

        // Title
        val titleView = TextView(context).apply {
            text = "ASSISTX"
            setTextColor(colorTextPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.08f
        }

        // System Nav Badge
        val badge = TextView(context).apply {
            text = "SYSTEM NAV"
            setTextColor(colorAccentBlue)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(6).toFloat()
                setColor(Color.parseColor("#183B82F6"))
                setStroke(dpToPx(1), Color.parseColor("#443B82F6"))
            }
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = dpToPx(8)
            }
        }

        val spacer = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }

        // Settings Button (Graphite circle)
        val settingsBtn = FrameLayout(context).apply {
            val btnSize = dpToPx(34)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            background = createRipple(Color.parseColor("#1C212B"), Color.parseColor("#33FFFFFF"), dpToPx(17).toFloat(), colorTileBorder)
            isClickable = true
            isFocusable = true

            val icon = ImageView(context).apply {
                val s = dpToPx(17)
                layoutParams = LayoutParams(s, s).apply { gravity = Gravity.CENTER }
                setImageResource(R.drawable.ic_action_settings)
                setColorFilter(colorTextSecondary)
            }
            addView(icon)

            setupMicroInteraction(this) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
                dismissWithAnimation()
            }
        }

        // Close Button (Graphite circle)
        val closeBtn = FrameLayout(context).apply {
            val btnSize = dpToPx(34)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginStart = dpToPx(8)
            }
            background = createRipple(Color.parseColor("#1C212B"), Color.parseColor("#33FFFFFF"), dpToPx(17).toFloat(), colorTileBorder)
            isClickable = true
            isFocusable = true

            val closeText = TextView(context).apply {
                text = "✕"
                setTextColor(colorTextSecondary)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }
            addView(closeText)

            setupMicroInteraction(this) {
                dismissWithAnimation()
            }
        }

        headerLayout.addView(dot)
        headerLayout.addView(titleView)
        headerLayout.addView(badge)
        headerLayout.addView(spacer)
        headerLayout.addView(settingsBtn)
        headerLayout.addView(closeBtn)

        cardContainer.addView(headerLayout)
    }

    private fun buildQuickAppsSection() {
        val sectionHeader = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(6))
        }

        val title = TextView(context).apply {
            text = "QUICK LAUNCH"
            setTextColor(colorTextMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.14f
        }
        sectionHeader.addView(title)
        cardContainer.addView(sectionHeader)

        val scrollView = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val appRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val quickApps = settings.quickApps
        for (pkg in quickApps) {
            val appItem = AppHelper.getAppItem(context, pkg)
            if (appItem != null) {
                val itemView = createQuickAppItem(appItem.appName, appItem.icon, pkg)
                appRow.addView(itemView)
                animatedItems.add(itemView)
            }
        }

        val addBtn = createAddAppButton()
        appRow.addView(addBtn)
        animatedItems.add(addBtn)

        scrollView.addView(appRow)
        cardContainer.addView(scrollView)
    }

    private fun createQuickAppItem(name: String, icon: android.graphics.drawable.Drawable?, pkg: String): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val p = dpToPx(4)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(dpToPx(58), ViewGroup.LayoutParams.WRAP_CONTENT)
            background = createRipple(Color.TRANSPARENT, Color.parseColor("#20FFFFFF"), dpToPx(12).toFloat())
            isClickable = true
            isFocusable = true
        }

        val iconContainer = FrameLayout(context).apply {
            val s = dpToPx(38)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(colorTileBg)
                setStroke(dpToPx(1), colorTileBorder)
            }

            val img = ImageView(context).apply {
                val iconSize = dpToPx(24)
                layoutParams = LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                }
                if (icon != null) {
                    setImageDrawable(icon)
                } else {
                    setImageResource(R.drawable.ic_action_settings)
                }
            }
            addView(img)
        }

        val label = TextView(context).apply {
            text = name
            setTextColor(colorTextSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(3)
            }
        }

        layout.addView(iconContainer)
        layout.addView(label)

        setupMicroInteraction(layout, iconContainer) {
            onAppClicked(pkg)
            dismissWithAnimation()
        }

        return layout
    }

    private fun createAddAppButton(): View {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val p = dpToPx(4)
            setPadding(p, p, p, p)
            layoutParams = LinearLayout.LayoutParams(dpToPx(58), ViewGroup.LayoutParams.WRAP_CONTENT)
            background = createRipple(Color.TRANSPARENT, Color.parseColor("#20FFFFFF"), dpToPx(12).toFloat())
            isClickable = true
            isFocusable = true
        }

        val iconContainer = FrameLayout(context).apply {
            val s = dpToPx(38)
            layoutParams = LinearLayout.LayoutParams(s, s).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(Color.parseColor("#123B82F6"))
                setStroke(dpToPx(1), colorAccentBlue)
            }

            val txt = TextView(context).apply {
                text = "+"
                setTextColor(colorAccentBlue)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            }
            addView(txt)
        }

        val label = TextView(context).apply {
            text = "Add App"
            setTextColor(colorTextSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(3)
            }
        }

        layout.addView(iconContainer)
        layout.addView(label)

        setupMicroInteraction(layout, iconContainer) {
            onAddAppClicked()
            dismissWithAnimation()
        }

        return layout
    }

    private fun buildHairlineDivider() {
        val divider = View(context).apply {
            val h = dpToPx(1)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, h).apply {
                topMargin = dpToPx(10)
                bottomMargin = dpToPx(10)
            }
            val grad = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(0x00FFFFFF, 0x1EFFFFFF, 0x00FFFFFF)
            )
            background = grad
        }
        cardContainer.addView(divider)
    }

    private fun buildActionGrid() {
        val sectionTitle = TextView(context).apply {
            text = "CONTROLS & GESTURES"
            setTextColor(colorTextMuted)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            letterSpacing = 0.14f
            setPadding(dpToPx(4), dpToPx(2), 0, dpToPx(6))
        }
        cardContainer.addView(sectionTitle)

        val actions = settings.actions
        val columnCount = 4

        val gridLayout = GridLayout(context).apply {
            this.columnCount = columnCount
            alignmentMode = GridLayout.ALIGN_BOUNDS
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        for (action in actions) {
            val itemView = createActionItem(action)
            gridLayout.addView(itemView)
            animatedItems.add(itemView)
        }

        cardContainer.addView(gridLayout)
    }

    private fun createActionItem(action: ActionType): View {
        val isFlashlightActive = action == ActionType.FLASHLIGHT && ActionExecutor.isFlashlightOn
        val isFrequentApp = action == ActionType.FREQUENT_APP
        val frequentApp = if (isFrequentApp) com.example.utils.AppUsageTracker.getMostFrequentlyUsedApp(context) else null

        val itemLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val padH = dpToPx(2)
            val padV = dpToPx(6)
            setPadding(padH, padV, padH, padV)

            val rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
            val colSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            layoutParams = GridLayout.LayoutParams(rowSpec, colSpec).apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                setGravity(Gravity.FILL_HORIZONTAL)
                setMargins(dpToPx(1), dpToPx(2), dpToPx(1), dpToPx(2))
            }
            background = createRipple(Color.TRANSPARENT, Color.parseColor("#263B82F6"), dpToPx(16).toFloat())
            isClickable = true
            isFocusable = true
        }

        // Sophisticated Graphite Squircle Icon Container
        val iconContainer = FrameLayout(context).apply {
            val containerSize = dpToPx(44)
            layoutParams = LinearLayout.LayoutParams(containerSize, containerSize).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }

            val iconBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(14).toFloat()
                if (isFlashlightActive) {
                    setColor(Color.parseColor("#2A3B82F6"))
                    setStroke(dpToPx(1), colorAccentBlue)
                } else if (isFrequentApp) {
                    setColor(Color.parseColor("#1C1E2E"))
                    setStroke(dpToPx(1), colorAccentBlue)
                } else {
                    setColor(colorTileBg)
                    setStroke(dpToPx(1), colorTileBorder)
                }
            }
            background = iconBg

            val customIconId = settings.customIcons[action.id]
            val iconRes = com.example.data.models.ShortcutIconRegistry.getIconRes(customIconId, action.iconRes)

            val iconView = ImageView(context).apply {
                val iconSize = dpToPx(if (isFrequentApp && frequentApp?.icon != null) 24 else 22)
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
            addView(iconView)
        }

        // Action Label
        val labelView = TextView(context).apply {
            text = if (isFrequentApp && frequentApp != null) frequentApp.appName else context.getString(action.titleRes)
            setTextColor(if (isFlashlightActive || isFrequentApp) colorAccentBlue else colorTextSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
            }
        }

        itemLayout.addView(iconContainer)
        itemLayout.addView(labelView)

        setupMicroInteraction(
            touchTarget = itemLayout,
            animatedView = iconContainer,
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

        return itemLayout
    }

    private fun buildSystemStatusFooter() {
        val isAccessibilityActive = AssistXAccessibilityService.isRunning()

        val footerContainer = FrameLayout(context).apply {
            val topMargin = dpToPx(10)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                this.topMargin = topMargin
            }
        }

        if (isAccessibilityActive) {
            // Subtle, confident status line
            val statusRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dpToPx(4), 0, 0)
            }

            val greenDot = View(context).apply {
                val s = dpToPx(6)
                layoutParams = LinearLayout.LayoutParams(s, s).apply {
                    marginEnd = dpToPx(6)
                }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#10B981"))
                }
            }

            val statusText = TextView(context).apply {
                text = "System Navigation Active • Real Back, Home & Recents"
                setTextColor(colorTextMuted)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f)
            }

            statusRow.addView(greenDot)
            statusRow.addView(statusText)
            footerContainer.addView(statusRow)
        } else {
            // High-visibility action banner to enable accessibility
            val banner = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val padH = dpToPx(10)
                val padV = dpToPx(6)
                setPadding(padH, padV, padH, padV)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(10).toFloat()
                    setColor(Color.parseColor("#1E2416"))
                    setStroke(dpToPx(1), colorAccentBlue)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                    } catch (ignored: Exception) {
                    }
                    dismissWithAnimation()
                }
            }

            val warnText = TextView(context).apply {
                text = "⚠️ Enable Accessibility for Back & Recents"
                setTextColor(Color.parseColor("#FDE047"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            val actionPill = TextView(context).apply {
                text = "Enable →"
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(dpToPx(8), dpToPx(3), dpToPx(8), dpToPx(3))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dpToPx(6).toFloat()
                    setColor(colorAccentBlue)
                }
            }

            banner.addView(warnText)
            banner.addView(actionPill)
            footerContainer.addView(banner)
        }

        cardContainer.addView(footerContainer)
    }

    private fun setupMicroInteraction(
        touchTarget: View,
        animatedView: View = touchTarget,
        onLongAction: (() -> Unit)? = null,
        onAction: () -> Unit
    ) {
        val longPressHandler = android.os.Handler(android.os.Looper.getMainLooper())
        var isLongPressFired = false
        val longPressRunnable = Runnable {
            isLongPressFired = true
            hapticManager.performHaptic(HapticType.MEDIUM, settings, isActionEvent = true)
            MotionSystem.animateRelease(animatedView)
            onLongAction?.invoke()
        }

        touchTarget.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPressFired = false
                    MotionSystem.animatePressDown(animatedView, 0.90f)
                    hapticManager.performHaptic(HapticType.TINY, settings, isActionEvent = true)
                    if (onLongAction != null) {
                        longPressHandler.postDelayed(longPressRunnable, 480L)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    if (!isLongPressFired) {
                        MotionSystem.animateRelease(animatedView) {
                            if (!isClosing) {
                                onAction()
                            }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    longPressHandler.removeCallbacks(longPressRunnable)
                    MotionSystem.animateRelease(animatedView)
                    true
                }
                else -> false
            }
        }
    }

    private fun animateOpen() {
        hapticManager.performHaptic(HapticType.LIGHT, settings)

        val isReduced = MotionSystem.isReducedMotion(context)
        val cardAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (isReduced) 120L else 240L
            interpolator = MotionSystem.ENTER_INTERPOLATOR
            addUpdateListener {
                val fraction = it.animatedValue as Float
                cardContainer.scaleX = 0.88f + 0.12f * fraction
                cardContainer.scaleY = 0.88f + 0.12f * fraction
                cardContainer.alpha = fraction
            }
        }
        cardAnim.start()

        val delayStep = if (isReduced) 10L else 18L
        for (i in animatedItems.indices) {
            val item = animatedItems[i]
            val delay = i * delayStep
            item.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(if (isReduced) 100L else 180L)
                .setStartDelay(delay)
                .setInterpolator(MotionSystem.ENTER_INTERPOLATOR)
                .start()
        }
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
                    currentDismissAnim = null
                    onDismiss()
                }
            })
        }
        currentDismissAnim = anim
        anim.start()
    }

    private fun createRipple(contentColor: Int, rippleColor: Int, radius: Float, strokeColor: Int? = null): RippleDrawable {
        val content = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(contentColor)
            if (strokeColor != null) {
                setStroke(dpToPx(1), strokeColor)
            }
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
