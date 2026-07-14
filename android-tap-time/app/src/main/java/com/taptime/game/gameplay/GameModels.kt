package com.taptime.game.gameplay

import androidx.compose.ui.geometry.Offset
import com.taptime.game.TargetType
import java.util.UUID

data class GameTarget(
    val id: String = UUID.randomUUID().toString(),
    val type: TargetType,
    val position: Offset,
    val maxRadius: Float,
    val spawnTimeSeconds: Double,
    val cycleDuration: Double,
    var hasBeenTapped: Boolean = false,
    var hasExpired: Boolean = false,
    var feverAppearance: Boolean = false,
) {
    fun scaleAt(elapsedSeconds: Double): Float {
        if (hasBeenTapped || hasExpired) return 0f
        val growDur = cycleDuration * 0.28
        val holdDur = cycleDuration * 0.17
        val shrinkDur = cycleDuration * 0.55
        return when {
            elapsedSeconds >= cycleDuration -> 0f
            elapsedSeconds < growDur -> {
                val t = (elapsedSeconds / growDur).toFloat().coerceIn(0f, 1f)
                0.01f + easeOut(t) * 0.99f
            }
            elapsedSeconds < growDur + holdDur -> 1f
            else -> {
                val t = ((elapsedSeconds - growDur - holdDur) / shrinkDur)
                    .toFloat()
                    .coerceIn(0f, 1f)
                1f - easeIn(t)
            }
        }
    }

    fun currentRadius(elapsedSeconds: Double): Float = maxRadius * scaleAt(elapsedSeconds)

    fun isHit(tap: Offset, elapsedSeconds: Double): Boolean {
        val scale = scaleAt(elapsedSeconds)
        if (scale <= 0.1f) return false
        val dx = tap.x - position.x
        val dy = tap.y - position.y
        return kotlin.math.sqrt(dx * dx + dy * dy) <= currentRadius(elapsedSeconds)
    }

    private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t)
    private fun easeIn(t: Float): Float = t * t
}

data class ScorePopup(
    val id: String = UUID.randomUUID().toString(),
    val position: Offset,
    val pointsText: String,
    val coinsText: String? = null,
    val bonusText: String? = null,
    val pointsColorArgb: Long,
    val coinsColorArgb: Long = 0xFFFFD11AFF,
    val spawnTimeSeconds: Double,
    val durationSeconds: Double = 0.75,
)

data class BackgroundFlash(
    val id: String = UUID.randomUUID().toString(),
    val center: Offset,
    val radius: Float,
    val colorArgb: Long,
    val spawnTimeSeconds: Double,
    val durationSeconds: Double,
)

data class MatrixColumn(
    val x: Float,
    var y: Float,
    val speed: Float,
    val chars: String,
    val alpha: Float,
)
