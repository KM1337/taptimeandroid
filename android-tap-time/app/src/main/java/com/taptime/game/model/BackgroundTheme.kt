package com.taptime.game.model

import androidx.compose.ui.graphics.Color

enum class BackgroundEffectType {
    DEEP_SPACE_GRID,
    MATRIX_RAIN,
    CYBER_SUNSET,
}

data class BackgroundTheme(
    val id: String,
    val name: String,
    val price: Int,
    val description: String,
    val effectType: BackgroundEffectType,
    val primaryColor: Color,
    val secondaryColor: Color,
)

private fun c(r: Float, g: Float, b: Float) = Color(r, g, b)

object BackgroundCatalog {
    val allBackgrounds: List<BackgroundTheme> = listOf(
        BackgroundTheme(
            id = "deep_space",
            name = "Deep Space Grid",
            price = 0,
            description = "Faint blue neon grid",
            effectType = BackgroundEffectType.DEEP_SPACE_GRID,
            primaryColor = c(0f, 0.35f, 0.55f),
            secondaryColor = c(0.03f, 0.03f, 0.09f),
        ),
        BackgroundTheme(
            id = "matrix_green",
            name = "Matrix Green",
            price = 20_000,
            description = "Falling digital code effect",
            effectType = BackgroundEffectType.MATRIX_RAIN,
            primaryColor = c(0f, 1f, 0.3f),
            secondaryColor = c(0.01f, 0.05f, 0.02f),
        ),
        BackgroundTheme(
            id = "cyber_sunset",
            name = "Cyber Sunset",
            price = 45_000,
            description = "Synthwave grid with glowing horizon",
            effectType = BackgroundEffectType.CYBER_SUNSET,
            primaryColor = c(1f, 0.4f, 0.6f),
            secondaryColor = c(0.15f, 0.05f, 0.2f),
        ),
    )

    fun background(id: String): BackgroundTheme =
        allBackgrounds.firstOrNull { it.id == id } ?: allBackgrounds.first()
}
