package com.taptime.game.gameplay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import com.taptime.game.GameManager
import com.taptime.game.TargetType
import com.taptime.game.model.BackgroundEffectType
import com.taptime.game.model.GameTheme
import com.taptime.game.model.SkinShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

object TargetPaths {
    fun pathFor(shape: SkinShape, radius: Float): Path {
        val r = radius.coerceAtLeast(1f)
        return when (shape) {
            SkinShape.CIRCLE -> Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        -r,
                        -r,
                        r,
                        r,
                    ),
                )
            }
            SkinShape.STAR -> starPath(r, 5, 0.45f)
            SkinShape.PIXEL -> Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        -r,
                        -r,
                        r,
                        r,
                        r * 0.12f,
                        r * 0.12f,
                    ),
                )
            }
            SkinShape.HEXAGON -> polygonPath(6, r)
            SkinShape.DIAMOND -> Path().apply {
                moveTo(0f, r)
                lineTo(r * 0.72f, 0f)
                lineTo(0f, -r)
                lineTo(-r * 0.72f, 0f)
                close()
            }
        }
    }

    private fun starPath(radius: Float, points: Int, innerRatio: Float): Path {
        val path = Path()
        val angleStep = (PI / points).toFloat()
        var angle = (-PI / 2).toFloat()
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) radius else radius * innerRatio
            val x = cos(angle) * r
            val y = sin(angle) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            angle += angleStep
        }
        path.close()
        return path
    }

    private fun polygonPath(sides: Int, radius: Float): Path {
        val path = Path()
        val angleStep = (2 * PI / sides).toFloat()
        var angle = (-PI / 2).toFloat()
        for (i in 0 until sides) {
            val x = cos(angle) * radius
            val y = sin(angle) * radius
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            angle += angleStep
        }
        path.close()
        return path
    }
}

class GameEngine {
    val targets = mutableListOf<GameTarget>()
    val popups = mutableListOf<ScorePopup>()
    val flashes = mutableListOf<BackgroundFlash>()

    var gridOffset = 0f
    var flashTimer = 0.0
    var nextFlashInterval = 8.0
    var elapsedTime = 0.0
    var spawnTimer = 0.0
    var matrixColumns = mutableListOf<MatrixColumn>()
    var sunsetPulse = 0.0

    private val topPadding = 160f
    private val bottomPadding = 70f
    private val sidePadding = 55f

    fun reset(size: Size, effectType: BackgroundEffectType) {
        targets.clear()
        popups.clear()
        flashes.clear()
        gridOffset = 0f
        flashTimer = 0.0
        nextFlashInterval = 8.0
        elapsedTime = 0.0
        spawnTimer = 0.0
        sunsetPulse = 0.0
        initMatrixColumns(size, effectType)
    }

    fun clearTargets() {
        targets.clear()
        spawnTimer = 0.0
    }

    private fun initMatrixColumns(size: Size, effectType: BackgroundEffectType) {
        matrixColumns.clear()
        if (effectType != BackgroundEffectType.MATRIX_RAIN) return
        val colCount = (size.width / 28f).toInt().coerceAtLeast(1)
        repeat(colCount) { i ->
            matrixColumns += MatrixColumn(
                x = i * 28f + 14f,
                y = Random.nextFloat() * size.height,
                speed = Random.nextFloat() * 80f + 60f,
                chars = randomMatrixChars(),
                alpha = Random.nextFloat() * 0.4f + 0.3f,
            )
        }
    }

    fun update(
        dt: Double,
        size: Size,
        gameManager: GameManager,
        theme: GameTheme,
        currentTimeSeconds: Double,
    ) {
        val state = gameManager.state.value
        if (state.needsClear) {
            clearTargets()
            gameManager.acknowledgeNeedsClear()
            return
        }
        if (state.gameState != com.taptime.game.GameState.PLAYING) return

        elapsedTime += dt
        gameManager.updateFever(dt)
        updateBackground(dt, size, state.score)
        updateMatrixRain(dt, size)

        val scoreFactor = state.score / 120.0
        val timeFactor = elapsedTime / 30.0
        val difficulty = 1.0 + scoreFactor + timeFactor
        val spawnInterval = maxOf(0.32, 1.05 - difficulty * 0.075)
        val cycleDuration = maxOf(0.95, 2.3 - difficulty * 0.11)

        spawnTimer += dt
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0.0
            spawnTarget(size, cycleDuration, currentTimeSeconds, state.score, state.lives, state.isFever)
        }

        val expired = targets.filter { target ->
            !target.hasBeenTapped &&
                !target.hasExpired &&
                currentTimeSeconds - target.spawnTimeSeconds >= target.cycleDuration
        }
        expired.forEach { handleExpired(it, gameManager, currentTimeSeconds) }

        targets.removeAll { it.hasBeenTapped || it.hasExpired }
        popups.removeAll { currentTimeSeconds - it.spawnTimeSeconds > it.durationSeconds }
        flashes.removeAll { currentTimeSeconds - it.spawnTimeSeconds > it.durationSeconds }

        if (state.isFever) {
            targets.filter { it.type != TargetType.DANGER && !it.feverAppearance }
                .forEach { it.feverAppearance = true }
        } else {
            targets.filter { it.feverAppearance }.forEach { it.feverAppearance = false }
        }
    }

    fun handleTap(
        tap: Offset,
        currentTimeSeconds: Double,
        gameManager: GameManager,
        theme: GameTheme,
    ): TargetType? {
        val target = targets.lastOrNull { t ->
            !t.hasBeenTapped && !t.hasExpired && t.isHit(tap, currentTimeSeconds - t.spawnTimeSeconds)
        } ?: return null

        target.hasBeenTapped = true
        when (target.type) {
            TargetType.STANDARD -> {
                val result = gameManager.handleCorrectTap(10)
                val pointsColor = if (gameManager.state.value.isFever) {
                    0xFFFFD91AFF
                } else {
                    theme.colors.standardGlow.value.toLong()
                }
                popups += ScorePopup(
                    position = target.position,
                    pointsText = "+${result.points} pts",
                    coinsText = "+${result.coins} Coins",
                    pointsColorArgb = pointsColor,
                    spawnTimeSeconds = currentTimeSeconds,
                )
            }
            TargetType.DANGER -> {
                gameManager.handleWrongTap()
                popups += ScorePopup(
                    position = target.position,
                    pointsText = "-1",
                    pointsColorArgb = 0xFFFF0000FF,
                    spawnTimeSeconds = currentTimeSeconds,
                    durationSeconds = 0.6,
                )
                checkGameOver(gameManager)
            }
            TargetType.EXTRA_LIFE -> {
                val result = gameManager.handleCorrectTap(5)
                gameManager.gainLife()
                popups += ScorePopup(
                    position = target.position,
                    pointsText = "+${result.points} pts",
                    coinsText = "+${result.coins} Coins",
                    bonusText = "+1 LIFE",
                    pointsColorArgb = theme.colors.extraLifeGlow.value.toLong(),
                    spawnTimeSeconds = currentTimeSeconds,
                )
            }
        }
        return target.type
    }

    private fun handleExpired(target: GameTarget, gameManager: GameManager, currentTimeSeconds: Double) {
        target.hasExpired = true
        when (target.type) {
            TargetType.STANDARD -> {
                gameManager.handleMissedStandard()
                checkGameOver(gameManager)
            }
            TargetType.DANGER, TargetType.EXTRA_LIFE -> Unit
        }
    }

    private fun checkGameOver(gameManager: GameManager) {
        if (gameManager.state.value.lives <= 0) {
            targets.forEach { it.hasExpired = true }
            targets.clear()
            gameManager.endGame()
        }
    }

    private fun spawnTarget(
        size: Size,
        cycleDuration: Double,
        currentTimeSeconds: Double,
        score: Int,
        lives: Int,
        isFever: Boolean,
    ) {
        val roll = Random.nextDouble()
        val extraLifeChance = if (lives < GameManager.MAX_LIVES) 0.06 else 0.03
        val dangerChance = if (isFever) 0.0 else min(0.35, 0.22 + score / 800.0)
        val type = when {
            roll < extraLifeChance -> TargetType.EXTRA_LIFE
            roll < extraLifeChance + dangerChance -> TargetType.DANGER
            else -> TargetType.STANDARD
        }

        val actualCycle = if (type == TargetType.EXTRA_LIFE) cycleDuration * 0.6 else cycleDuration
        val maxRadius = if (type == TargetType.EXTRA_LIFE) 36f else 44f

        var position = Offset.Zero
        var found = false
        repeat(12) {
            val x = Random.nextFloat() * (size.width - 2 * (sidePadding + maxRadius)) + sidePadding + maxRadius
            val y = Random.nextFloat() * (size.height - topPadding - bottomPadding - 2 * maxRadius) +
                bottomPadding + maxRadius
            val testPos = Offset(x, y)
            val minDist = maxRadius * 2 + 16
            val tooClose = targets.any { t ->
                val dx = t.position.x - testPos.x
                val dy = t.position.y - testPos.y
                sqrt(dx * dx + dy * dy) < minDist
            }
            if (!tooClose) {
                position = testPos
                found = true
                return@repeat
            }
        }
        if (!found) return

        targets += GameTarget(
            type = type,
            position = position,
            maxRadius = maxRadius,
            spawnTimeSeconds = currentTimeSeconds,
            cycleDuration = actualCycle,
            feverAppearance = isFever && type != TargetType.DANGER,
        )
    }

    private fun updateBackground(dt: Double, size: Size, score: Int) {
        val tier = when {
            score >= 2000 -> 2.0
            score >= 500 -> 1.0 + (score - 500) / 1500.0
            else -> score / 500.0
        }
        val scrollSpeed = 8f + tier.toFloat() * 18f
        gridOffset += scrollSpeed * dt.toFloat()
        val cellSize = 44f
        if (gridOffset >= cellSize) gridOffset -= cellSize

        nextFlashInterval = maxOf(0.8, 8.0 - tier * 3.5)
        flashTimer += dt
        if (flashTimer >= nextFlashInterval) {
            flashTimer = 0.0
            spawnBackgroundFlash(size, tier, elapsedTime)
            if (tier >= 2.0 && Random.nextBoolean()) {
                spawnBackgroundFlash(size, tier, elapsedTime)
            }
        }
    }

    private fun spawnBackgroundFlash(size: Size, tier: Double, currentTimeSeconds: Double) {
        val intensity = min(1.0, 0.15 + tier * 0.3)
        val isPink = Random.nextBoolean()
        val color = if (isPink) 0x66FF33B2 else 0x669933FF
        val alphaAdjusted = ((intensity * 0.25 * 255).toInt() shl 24) or (color and 0x00FFFFFF)
        flashes += BackgroundFlash(
            center = Offset(
                Random.nextFloat() * (size.width - 120f) + 60f,
                Random.nextFloat() * (size.height - 120f) + 60f,
            ),
            radius = Random.nextFloat() * 100f + 100f,
            colorArgb = alphaAdjusted.toLong(),
            spawnTimeSeconds = currentTimeSeconds,
            durationSeconds = 0.6 + tier * 0.3,
        )
    }

    private fun updateMatrixRain(dt: Double, size: Size) {
        sunsetPulse += dt
        matrixColumns.forEach { col ->
            col.y += col.speed * dt.toFloat()
            if (col.y > size.height + 60f) {
                col.y = -60f
                col.chars = randomMatrixChars()
            }
        }
    }

    private fun randomMatrixChars(): String =
        (0 until 8).joinToString("") { Random.nextInt(0x30A0, 0x30FF).toChar().toString() }
}
