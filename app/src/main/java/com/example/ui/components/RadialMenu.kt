package com.example.ui.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.FloatingShape
import com.example.haptics.HapticManager
import com.example.haptics.HapticType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Data model for an item displayed in the circular RadialMenu.
 */
data class RadialMenuItem(
    val id: String,
    val label: String,
    val iconRes: Int? = null,
    val iconVector: ImageVector? = null,
    val contentDescription: String = label,
    val backgroundColor: Color = Color(0xFF1C212B),
    val borderColor: Color = Color(0xFF2A3342),
    val iconTint: Color = Color(0xFFF8FAFC),
    val onLongClick: (() -> Unit)? = null,
    val onClick: () -> Unit
)

/**
 * A composable 'RadialMenu' component that positions icons in a circular pattern
 * around a central floating action button or floating bar, using trigonometry (cos/sin of angles)
 * to calculate precise orbital positions for each menu item.
 *
 * @param items The list of [RadialMenuItem] to distribute along the circle.
 * @param modifier Modifier for the outer container.
 * @param isOpen Whether the radial menu is expanded.
 * @param onToggle Invoked when the central Floating Action Button or Floating Bar is pressed.
 * @param radius Orbital distance in Dp from the central trigger to the item centers.
 * @param startAngleDegrees Initial angle in degrees (e.g. -90f places first item at 12 o'clock).
 * @param sweepAngleDegrees Total angular distribution range (360f for full circle, 180f for semi-circle).
 * @param centerShape Form factor of central anchor ([FloatingShape.PILL_BAR] or [FloatingShape.SPHERE]).
 * @param centralFabColor Background color of the central anchor.
 * @param centralFabIconColor Tint of the central icon / plus.
 * @param showLabels Whether to display label tooltips above selected/hovered items.
 */
@Composable
fun RadialMenu(
    items: List<RadialMenuItem>,
    modifier: Modifier = Modifier,
    isOpen: Boolean = false,
    onToggle: () -> Unit = {},
    radius: Dp = 100.dp,
    startAngleDegrees: Float = -90f,
    sweepAngleDegrees: Float = 360f,
    centerShape: FloatingShape = FloatingShape.PILL_BAR,
    accentColor: Color = Color(0xFF3B82F6),
    centralFabColor: Color = accentColor,
    centralFabIconColor: Color = Color.White,
    showLabels: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hapticManager = remember { HapticManager.getInstance(context) }

    // Master container animation for Glassmorphism backdrop and central trigger
    val containerProgress = remember { Animatable(if (isOpen) 1f else 0f) }

    // Individual Compose Animatable instances per radial item for premium spring physics & bouncy expansion
    val itemAnimatables = remember(items.size) {
        List(items.size) { Animatable(if (isOpen) 1f else 0f) }
    }

    LaunchedEffect(isOpen, items.size) {
        if (isOpen) {
            launch {
                containerProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            items.indices.forEach { index ->
                launch {
                    delay(index * 26L)
                    itemAnimatables.getOrNull(index)?.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
        } else {
            launch {
                containerProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            items.indices.reversed().forEach { index ->
                launch {
                    delay((items.size - 1 - index) * 16L)
                    itemAnimatables.getOrNull(index)?.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
        }
    }

    // Rotation of central FAB icon (0 -> 135 deg to morph '+' into '✕')
    val fabRotation by animateFloatAsState(
        targetValue = if (isOpen) 135f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fab_rotation"
    )

    var activeHoverIndex by remember { mutableIntStateOf(-1) }

    Box(
        modifier = modifier.testTag("radial_menu_container"),
        contentAlignment = Alignment.Center
    ) {
        val count = items.size
        val radiusPx = with(density) { radius.toPx() }

        // Modern Minimalist Glassmorphism Container with RenderEffect Blur
        if (containerProgress.value > 0.005f) {
            val blurRadiusPx = with(density) { (24.dp * containerProgress.value).toPx() }
            val containerSize = (radius + 42.dp) * 2

            Box(
                modifier = Modifier
                    .size(containerSize)
                    .graphicsLayer {
                        clip = true
                        shape = CircleShape
                        alpha = containerProgress.value.coerceIn(0f, 1f)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadiusPx > 0.5f) {
                            renderEffect = RenderEffect
                                .createBlurEffect(blurRadiusPx, blurRadiusPx, Shader.TileMode.CLAMP)
                                .asComposeRenderEffect()
                        }
                    }
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0x661C212B),
                                Color(0x4014171E),
                                Color(0x180E1116),
                                Color(0x000E1116)
                            )
                        ),
                        shape = CircleShape
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f * containerProgress.value),
                                accentColor.copy(alpha = 0.28f * containerProgress.value),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Position each item using Trigonometry:
        // Angle = startAngle + i * (sweepAngle / count)
        // X = radius * cos(angle)
        // Y = radius * sin(angle)
        items.forEachIndexed { index, item ->
            val angleStep = if (count > 0) {
                if (sweepAngleDegrees >= 360f) sweepAngleDegrees / count
                else sweepAngleDegrees / (if (count > 1) (count - 1) else 1)
            } else 0f

            val angleDeg = startAngleDegrees + (index * angleStep)
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val animProg = itemAnimatables.getOrNull(index)?.value ?: containerProgress.value
            val isVisible = animProg > 0.005f

            if (isVisible) {
                val currentRadiusPx = radiusPx * animProg
                val xPx = (currentRadiusPx * cos(angleRad)).roundToInt()
                val yPx = (currentRadiusPx * sin(angleRad)).roundToInt()

                // Spring physics bounce overshoot on scale as it blooms into orbit
                val itemScale = (animProg * 1.05f).coerceIn(0f, 1.14f)
                val itemAlpha = animProg.coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .offset { IntOffset(xPx, yPx) }
                        .scale(itemScale)
                        .alpha(itemAlpha)
                        .size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RadialMenuSatelliteItem(
                        item = item,
                        testTag = "radial_item_${item.id}",
                        onClick = {
                            hapticManager.performHaptic(HapticType.LIGHT)
                            item.onClick()
                        }
                    )
                }
            }
        }

        // Central Trigger: Floating Bar (Pill Capsule) or Circular Orb (FAB)
        if (centerShape == FloatingShape.PILL_BAR) {
            Surface(
                onClick = {
                    hapticManager.performHaptic(if (isOpen) HapticType.TINY else HapticType.LIGHT)
                    onToggle()
                },
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF14171E),
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
                modifier = Modifier
                    .size(width = 84.dp, height = 44.dp)
                    .testTag("radial_menu_bar_fab")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Left and right accent indicator dots matching the floating bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color(0xFF94A3B8).copy(alpha = 0.7f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(Color(0xFF94A3B8).copy(alpha = 0.7f), CircleShape)
                        )
                    }

                    // Morphing plus / close icon inside the central pill bar
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (isOpen) "Close Menu" else "Open Radial Menu",
                        tint = accentColor,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(fabRotation)
                    )
                }
            }
        } else {
            FloatingActionButton(
                onClick = {
                    hapticManager.performHaptic(if (isOpen) HapticType.TINY else HapticType.LIGHT)
                    onToggle()
                },
                shape = CircleShape,
                containerColor = centralFabColor,
                contentColor = centralFabIconColor,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 10.dp
                ),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("radial_menu_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (isOpen) "Close Menu" else "Open Radial Menu",
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(fabRotation)
                )
            }
        }

        // Active Item Label Indicator (shown under or above menu when open)
        if (isOpen && showLabels) {
            Text(
                text = if (activeHoverIndex in items.indices) items[activeHoverIndex].label else "Tap an action",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = Color(0xFF94A3B8),
                modifier = Modifier
                    .offset(y = radius + 34.dp)
                    .background(Color(0xFF14171E), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF2A3342), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

/**
 * Individual satellite icon component orbiting the central FAB.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RadialMenuSatelliteItem(
    item: RadialMenuItem,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = item.backgroundColor,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, item.borderColor),
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = item.onLongClick
            )
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (item.iconRes != null) {
                Icon(
                    painter = painterResource(id = item.iconRes),
                    contentDescription = item.contentDescription,
                    tint = item.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            } else if (item.iconVector != null) {
                Icon(
                    imageVector = item.iconVector,
                    contentDescription = item.contentDescription,
                    tint = item.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
