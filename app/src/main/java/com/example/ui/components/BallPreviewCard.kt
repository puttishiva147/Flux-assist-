package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.BallSettings
import com.example.data.models.FloatingShape

@Composable
fun BallPreviewCard(
    settings: BallSettings,
    modifier: Modifier = Modifier
) {
    val style = settings.ballStyle

    val infiniteTransition = rememberInfiniteTransition(label = "previewPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ball_preview_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF14171D)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3240)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SYSTEM OVERLAY PREVIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = Color(style.primaryColor)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val previewBoxWidth = 160.dp
            val previewBoxHeight = 110.dp

            Box(
                modifier = Modifier
                    .size(previewBoxWidth, previewBoxHeight)
                    .background(Color(0xFF1A1F29), RoundedCornerShape(20.dp))
                    .border(1.dp, Color(0xFF26303E), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                val isPill = settings.floatingShape == FloatingShape.PILL_BAR
                val canvasWidth = if (isPill) (settings.ballSizeDp * 1.35f).dp else settings.ballSizeDp.dp
                val canvasHeight = if (isPill) (settings.ballSizeDp * 0.65f).dp else settings.ballSizeDp.dp

                Canvas(
                    modifier = Modifier
                        .size(canvasWidth, canvasHeight)
                        .testTag("ball_preview_canvas")
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f
                    val cy = h / 2f

                    if (isPill) {
                        val cr = (h / 2f) * pulseScale
                        val pad = h * 0.08f

                        // Outer Glow
                        drawRoundRect(
                            color = Color(style.glowColor).copy(alpha = settings.ballOpacity * 0.6f),
                            size = Size(w * pulseScale, h * pulseScale),
                            topLeft = Offset((w - w * pulseScale) / 2f, (h - h * pulseScale) / 2f),
                            cornerRadius = CornerRadius(cr, cr)
                        )

                        // Graphite Glass Body
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF2E3748), Color(style.innerCoreColor)),
                                startY = pad,
                                endY = h - pad
                            ),
                            topLeft = Offset(pad, pad),
                            size = Size(w - 2 * pad, h - 2 * pad),
                            cornerRadius = CornerRadius(cr - pad, cr - pad),
                            alpha = settings.ballOpacity
                        )

                        // Border Ring
                        drawRoundRect(
                            color = Color(style.ringColor).copy(alpha = settings.ballOpacity),
                            topLeft = Offset(pad, pad),
                            size = Size(w - 2 * pad, h - 2 * pad),
                            cornerRadius = CornerRadius(cr - pad, cr - pad),
                            style = Stroke(width = pad * 1.1f)
                        )

                        // Center Grip Dash
                        val dashW = w * 0.32f
                        val dashH = h * 0.18f
                        drawRoundRect(
                            color = Color(style.primaryColor),
                            topLeft = Offset(cx - dashW / 2f, cy - dashH / 2f),
                            size = Size(dashW, dashH),
                            cornerRadius = CornerRadius(dashH / 2f, dashH / 2f)
                        )

                        // Accent dots
                        val dotR = h * 0.09f
                        drawCircle(Color(0xFFCBD5E1), dotR, Offset(cx - w * 0.28f, cy))
                        drawCircle(Color(0xFFCBD5E1), dotR, Offset(cx + w * 0.28f, cy))
                    } else {
                        val radius = (size.minDimension / 2f) * pulseScale

                        // Outer glow
                        drawCircle(
                            color = Color(style.glowColor).copy(alpha = settings.ballOpacity * 0.5f),
                            radius = radius * 1.08f,
                            center = Offset(cx, cy)
                        )

                        // Mid glass ring
                        drawCircle(
                            color = Color(style.ringColor).copy(alpha = settings.ballOpacity),
                            radius = radius * 0.82f,
                            center = Offset(cx, cy),
                            style = Stroke(width = radius * 0.08f)
                        )

                        // Inner Core with Radial Gradient
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF333E50), Color(style.innerCoreColor)),
                                center = Offset(cx - radius * 0.2f, cy - radius * 0.2f),
                                radius = radius * 0.65f
                            ),
                            radius = radius * 0.65f,
                            center = Offset(cx, cy),
                            alpha = settings.ballOpacity
                        )

                        // Center focus dot
                        drawCircle(
                            color = Color(style.primaryColor),
                            radius = radius * 0.16f,
                            center = Offset(cx, cy)
                        )

                        // Directional dots
                        val dotRadius = radius * 0.045f
                        val offset = radius * 0.82f
                        drawCircle(Color(0xFFCBD5E1), dotRadius, Offset(cx, cy - offset))
                        drawCircle(Color(0xFFCBD5E1), dotRadius, Offset(cx, cy + offset))
                        drawCircle(Color(0xFFCBD5E1), dotRadius, Offset(cx - offset, cy))
                        drawCircle(Color(0xFFCBD5E1), dotRadius, Offset(cx + offset, cy))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "${settings.floatingShape.displayName} • ${style.displayName} • ${(settings.ballOpacity * 100).toInt()}% Opacity",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFFF1F5F9)
            )
        }
    }
}
