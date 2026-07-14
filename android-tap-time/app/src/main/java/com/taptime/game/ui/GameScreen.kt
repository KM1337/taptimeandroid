package com.taptime.game.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taptime.game.GameManager
import com.taptime.game.TargetType
import com.taptime.game.gameplay.GameEngine
import com.taptime.game.gameplay.ScorePopup
import com.taptime.game.gameplay.TargetPaths
import com.taptime.game.model.BackgroundEffectType
import com.taptime.game.model.BackgroundTheme
import com.taptime.game.model.GameTheme
import com.taptime.game.ui.theme.NeonPalette
import kotlin.math.min

@Composable
fun GameScreen(gameManager: GameManager) {
    val state by gameManager.state.collectAsStateWithLifecycle()
    val theme = gameManager.currentTheme
    val background = gameManager.currentBackground

    val engine = remember { GameEngine() }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var gameTimeSeconds by remember { mutableFloatStateOf(0f) }
    var shakeOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(state.sessionId, background.id) {
        gameTimeSeconds = 0f
        if (canvasSize.width > 0) {
            engine.reset(canvasSize, background.effectType)
        }
    }

    LaunchedEffect(state.sessionId, canvasSize, background.id) {
        if (canvasSize.width <= 0f) return@LaunchedEffect
        engine.reset(canvasSize, background.effectType)
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos == 0L) {
                    lastNanos = frameNanos
                    return@withFrameNanos
                }
                val dt = min((frameNanos - lastNanos) / 1_000_000_000.0, 1.0 / 30.0)
                lastNanos = frameNanos
                gameTimeSeconds += dt.toFloat()
                engine.update(dt, canvasSize, gameManager, theme, gameTimeSeconds.toDouble())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    canvasSize = Size(it.width.toFloat(), it.height.toFloat())
                }
                .pointerInput(state.sessionId) {
                    detectTapGestures { offset ->
                        val hit = engine.handleTap(
                            offset,
                            gameTimeSeconds.toDouble(),
                            gameManager,
                            theme,
                        )
                        if (hit && gameManager.state.value.lives < state.lives &&
                            gameManager.state.value.lives <= state.lives
                        ) {
                            // danger tap shake
                            shakeOffset = Offset(8f, -6f)
                        }
                    }
                },
        ) {
            if (canvasSize.width <= 0f) return@Canvas

            translate(shakeOffset.x, shakeOffset.y) {
                drawGameBackground(background, theme, engine, state.score)
                drawBackgroundFlashes(engine, gameTimeSeconds)
                drawTargets(engine, theme, gameTimeSeconds)
                drawPopups(engine, gameTimeSeconds)
            }
        }

        if (state.isFever) {
            val transition = rememberInfiniteTransition(label = "fever")
            val alpha by transition.animateFloat(
                initialValue = 0.05f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(tween(120), RepeatMode.Reverse),
                label = "feverAlpha",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0.6f, 0.2f, 1f, alpha)),
            )
        }

        GameHud(
            state = state,
            theme = theme,
            onToggleMute = { gameManager.setMuted(!state.isMuted) },
        )
    }

    LaunchedEffect(shakeOffset) {
        if (shakeOffset != Offset.Zero) {
            kotlinx.coroutines.delay(80)
            shakeOffset = Offset.Zero
        }
    }
}

@Composable
private fun GameHud(
    state: com.taptime.game.GameManagerState,
    theme: GameTheme,
    onToggleMute: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                repeat(3) { index ->
                    Icon(
                        imageVector = if (index < state.lives) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = null,
                        tint = if (index < state.lives) NeonPalette.Red else Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(22.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "${state.score}",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            )

            IconButton(onClick = onToggleMute) {
                Icon(
                    imageVector = if (state.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = "Mute",
                    tint = if (state.isMuted) Color.White.copy(alpha = 0.3f) else NeonPalette.Cyan,
                )
            }
        }

        if (state.comboMultiplier > 1) {
            Text(
                text = "⚡ COMBO x${state.comboMultiplier}",
                color = NeonPalette.Gold,
                fontWeight = FontWeight.Heavy,
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp),
            )
        }

        if (state.isFever) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "🔥 FEVER!  ${"%.1f".format(state.feverTimeRemaining)}",
                    color = NeonPalette.Gold,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                LinearProgressIndicator(
                    progress = { (state.feverTimeRemaining / 8.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .padding(top = 4.dp),
                    color = NeonPalette.Gold,
                    trackColor = Color.White.copy(alpha = 0.1f),
                )
            }
        }
    }
}

private fun DrawScope.drawGameBackground(
    background: BackgroundTheme,
    theme: GameTheme,
    engine: GameEngine,
    score: Int,
) {
    drawRect(background.secondaryColor)

    when (background.effectType) {
        BackgroundEffectType.CYBER_SUNSET -> {
            val horizonY = size.height * 0.38f
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        background.primaryColor.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = horizonY,
                ),
            )
            drawLine(
                background.primaryColor.copy(alpha = 0.4f),
                Offset(0f, horizonY),
                Offset(size.width, horizonY),
                strokeWidth = 3f,
            )
            drawCircle(
                background.primaryColor.copy(alpha = 0.12f),
                radius = 50f,
                center = Offset(size.width / 2, horizonY + 60f),
            )
        }
        BackgroundEffectType.MATRIX_RAIN -> {
            engine.matrixColumns.forEach { col ->
                drawContext.canvas.nativeCanvas.drawText(
                    col.chars,
                    col.x,
                    col.y,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            (col.alpha * 255).toInt(),
                            0,
                            255,
                            77,
                        )
                        textSize = 28f
                        isAntiAlias = true
                    },
                )
            }
        }
        BackgroundEffectType.DEEP_SPACE_GRID -> Unit
    }

    val gridColor = background.primaryColor.copy(alpha = 0.18f)
    val cellSize = 44f
    val offset = engine.gridOffset

    translate(-offset, -offset * 0.6f) {
        var x = 0f
        while (x <= size.width + cellSize) {
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height + cellSize), strokeWidth = 0.8f)
            x += cellSize
        }
        var y = 0f
        while (y <= size.height + cellSize) {
            drawLine(gridColor, Offset(0f, y), Offset(size.width + cellSize, y), strokeWidth = 0.8f)
            y += cellSize
        }
        var ix = 0f
        var col = 0
        while (ix <= size.width + cellSize) {
            var iy = 0f
            var row = 0
            while (iy <= size.height + cellSize) {
                if ((col + row) % 3 == 0) {
                    drawCircle(
                        background.primaryColor.copy(alpha = 0.3f),
                        1.5f,
                        Offset(ix, iy),
                    )
                }
                iy += cellSize
                row++
            }
            ix += cellSize
            col++
        }
    }
}

private fun DrawScope.drawBackgroundFlashes(engine: GameEngine, currentTime: Float) {
    engine.flashes.forEach { flash ->
        val elapsed = currentTime - flash.spawnTimeSeconds
        val t = (elapsed / flash.durationSeconds).toFloat().coerceIn(0f, 1f)
        val alpha = if (t < 0.3f) t / 0.3f else 1f - (t - 0.3f) / 0.7f
        drawCircle(
            Color(flash.colorArgb).copy(alpha = alpha.coerceIn(0f, 1f)),
            flash.radius,
            flash.center,
        )
    }
}

private fun DrawScope.drawTargets(
    engine: GameEngine,
    theme: GameTheme,
    currentTime: Float,
) {
    engine.targets.forEach { target ->
        val elapsed = currentTime - target.spawnTimeSeconds
        val scale = target.scaleAt(elapsed.toDouble())
        if (scale <= 0.05f) return@forEach

        val (fill, glow) = targetColors(target.type, theme, target.feverAppearance)

        translate(target.position.x, target.position.y) {
            scale(scale, scale) {
                val path = TargetPaths.pathFor(theme.shape, target.maxRadius)
                drawPath(path, fill.copy(alpha = 0.25f))
                drawPath(path, glow, style = Stroke(width = 3f))
                if (target.type == TargetType.STANDARD) {
                    drawCircle(Color.White, 4f, Offset.Zero)
                }
                if (target.type == TargetType.EXTRA_LIFE) {
                    drawCircle(glow.copy(alpha = 0.3f), target.maxRadius * 0.35f)
                }
            }
        }
    }
}

private fun targetColors(type: TargetType, theme: GameTheme, fever: Boolean): Pair<Color, Color> {
    if (fever && type != TargetType.DANGER) {
        return Color(1f, 0.85f, 0.2f) to Color(1f, 0.75f, 0.1f)
    }
    return when (type) {
        TargetType.STANDARD -> theme.colors.standardFill to theme.colors.standardGlow
        TargetType.DANGER -> theme.colors.dangerFill to theme.colors.dangerGlow
        TargetType.EXTRA_LIFE -> theme.colors.extraLifeFill to theme.colors.extraLifeGlow
    }
}

private fun DrawScope.drawPopups(engine: GameEngine, currentTime: Float) {
    val measurer = androidx.compose.ui.text.TextMeasurer(
        androidx.compose.ui.text.font.FontFamily.Default,
        androidx.compose.ui.unit.Density(density),
        androidx.compose.ui.unit.LayoutDirection.Ltr,
    )
    engine.popups.forEach { popup ->
        drawPopup(popup, currentTime, measurer)
    }
}

private fun DrawScope.drawPopup(
    popup: ScorePopup,
    currentTime: Float,
    measurer: androidx.compose.ui.text.TextMeasurer,
) {
    val elapsed = currentTime - popup.spawnTimeSeconds
    val t = (elapsed / popup.durationSeconds).toFloat().coerceIn(0f, 1f)
    val yOffset = -65f * t
    val alpha = 1f - t

    var y = popup.position.y + yOffset
    drawTextLine(measurer, popup.pointsText, popup.position.x, y, Color(popup.pointsColorArgb).copy(alpha = alpha), 19f)
    popup.coinsText?.let {
        y += 22f
        drawTextLine(measurer, it, popup.position.x, y, Color(popup.coinsColorArgb).copy(alpha = alpha), 17f)
    }
    popup.bonusText?.let {
        y += 20f
        drawTextLine(measurer, it, popup.position.x, y, NeonPalette.FeverGold.copy(alpha = alpha), 15f)
    }
}

private fun DrawScope.drawTextLine(
    measurer: androidx.compose.ui.text.TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color,
    sizeSp: Float,
) {
    val style = TextStyle(
        color = color,
        fontSize = sizeSp.sp,
        fontWeight = FontWeight.Bold,
    )
    val result = measurer.measure(text, style)
    drawText(
        textLayoutResult = result,
        topLeft = Offset(x - result.size.width / 2f, y - result.size.height / 2f),
    )
}
