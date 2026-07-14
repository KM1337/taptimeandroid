package com.taptime.game.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taptime.game.ui.theme.NeonPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CyberpunkBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "bgPulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0.07f, 0.07f, 0.10f),
                        Color(0.03f, 0.03f, 0.06f),
                        Color(0.05f, 0.05f, 0.08f),
                    ),
                ),
            )
            .drawBehind {
                val cellSize = 42f
                val baseAlpha = 0.025f + pulse * 0.035f
                val center = Offset(size.width / 2, size.height / 2)
                val maxDist = sqrt(
                    (size.width / 2) * (size.width / 2) + (size.height / 2) * (size.height / 2),
                )

                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        NeonPalette.Cyan.copy(alpha = baseAlpha),
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = 0.5f,
                    )
                    y += cellSize
                }
                var x = 0f
                while (x <= size.width) {
                    drawLine(
                        NeonPalette.Cyan.copy(alpha = baseAlpha),
                        Offset(x, 0f),
                        Offset(x, size.height),
                        strokeWidth = 0.5f,
                    )
                    x += cellSize
                }

                val circleAlpha = 0.04f + pulse * 0.08f
                var gy = 0f
                while (gy <= size.height) {
                    var gx = 0f
                    while (gx <= size.width) {
                        val dist = sqrt((gx - center.x) * (gx - center.x) + (gy - center.y) * (gy - center.y))
                        val distFactor = 1f - minOf(1f, dist / maxDist)
                        drawCircle(
                            color = NeonPalette.Cyan.copy(alpha = circleAlpha * distFactor),
                            radius = 10f,
                            center = Offset(gx, gy),
                            style = Stroke(width = 1f),
                        )
                        gx += cellSize * 3
                    }
                    gy += cellSize * 3
                }

                val hexAlpha = 0.03f + pulse * 0.04f
                val hexPositions = listOf(
                    Triple(size.width * 0.2f, size.height * 0.3f, 60f),
                    Triple(size.width * 0.8f, size.height * 0.7f, 80f),
                    Triple(size.width * 0.15f, size.height * 0.75f, 45f),
                    Triple(size.width * 0.85f, size.height * 0.2f, 55f),
                )
                hexPositions.forEach { (hx, hy, hr) ->
                    drawPath(
                        hexPath(hx, hy, hr),
                        NeonPalette.Cyan.copy(alpha = hexAlpha),
                        style = Stroke(width = 1f),
                    )
                }
            },
    )
}

private fun hexPath(cx: Float, cy: Float, radius: Float): Path {
    val path = Path()
    for (i in 0 until 6) {
        val angle = i * PI.toFloat() / 3f
        val px = cx + cos(angle) * radius
        val py = cy + sin(angle) * radius
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    return path
}

@Composable
fun PulsingGlassButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: Int = 22,
    onClick: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "btnPulse")
    val glow by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "glow",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(2.dp, color, RoundedCornerShape(18.dp))
            .shadow((4 + glow * 16).dp, RoundedCornerShape(18.dp), ambientColor = color, spotColor = color)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(vertical = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun NeonButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    fontSize: Int = 18,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (filled) {
                    Modifier
                        .background(if (enabled) color else color.copy(alpha = 0.4f))
                        .shadow(14.dp, shape, spotColor = color)
                } else {
                    Modifier.border(2.dp, if (enabled) color else color.copy(alpha = 0.4f), shape)
                },
            )
            .pointerInput(enabled) {
                if (enabled) detectTapGestures(onTap = { onClick() })
            }
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (filled) Color.Black else color,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun StatChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(50))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

fun Modifier.neonGlow(color: Color): Modifier = this.shadow(16.dp, spotColor = color, ambientColor = color)
