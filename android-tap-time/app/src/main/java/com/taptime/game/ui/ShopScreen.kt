package com.taptime.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taptime.game.GameManager
import com.taptime.game.GameManagerState
import com.taptime.game.model.BackgroundCatalog
import com.taptime.game.model.BackgroundTheme
import com.taptime.game.model.GameTheme
import com.taptime.game.model.ThemeCatalog
import com.taptime.game.ui.components.CyberpunkBackground
import com.taptime.game.ui.components.NeonButton
import com.taptime.game.ui.components.StatChip
import com.taptime.game.ui.components.neonGlow
import com.taptime.game.ui.theme.NeonPalette

@Composable
fun ShopScreen(
    gameManager: GameManager,
    onBack: () -> Unit,
) {
    val state by gameManager.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        CyberpunkBackground()

        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonPalette.Cyan,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "SHOP",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.neonGlow(NeonPalette.Purple),
                )
                Spacer(modifier = Modifier.weight(1f))
                StatChip(text = "◎ ${state.totalCoins}", color = NeonPalette.Gold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TabChip(
                    label = "Skins",
                    selected = selectedTab == 0,
                    color = NeonPalette.Cyan,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 0 },
                )
                TabChip(
                    label = "Backgrounds",
                    selected = selectedTab == 1,
                    color = NeonPalette.Green,
                    modifier = Modifier.weight(1f),
                    onClick = { selectedTab = 1 },
                )
            }

            NeonButton(
                label = if (state.isWatchingAd) "Loading..." else "Watch Ad — Get 50 Coins",
                color = NeonPalette.Green,
                filled = false,
                fontSize = 16,
                enabled = !state.isWatchingAd,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = { gameManager.watchAdForCoins() },
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (selectedTab == 0) {
                    items(ThemeCatalog.allThemes, key = { it.id }) { theme ->
                        ThemeRow(theme, state, gameManager)
                    }
                } else {
                    items(BackgroundCatalog.allBackgrounds, key = { it.id }) { bg ->
                        BackgroundRow(bg, state, gameManager)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) color.copy(alpha = 0.08f) else Color.Transparent)
            .border(
                width = 1.5.dp,
                color = if (selected) color.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) color else Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ThemeRow(theme: GameTheme, state: GameManagerState, gameManager: GameManager) {
    ShopItemRow(
        title = theme.name,
        description = theme.description,
        accent = theme.colors.accent,
        isSelected = state.selectedThemeId == theme.id,
        isUnlocked = theme.id in state.unlockedThemeIds,
        price = theme.price,
        canAfford = state.totalCoins >= theme.price,
        onPurchase = { gameManager.purchaseTheme(theme) },
        onEquip = { gameManager.selectTheme(theme) },
    )
}

@Composable
private fun BackgroundRow(bg: BackgroundTheme, state: GameManagerState, gameManager: GameManager) {
    ShopItemRow(
        title = bg.name,
        description = bg.description,
        accent = bg.primaryColor,
        isSelected = state.selectedBackgroundId == bg.id,
        isUnlocked = bg.id in state.unlockedBackgroundIds,
        price = bg.price,
        canAfford = state.totalCoins >= bg.price,
        onPurchase = { gameManager.purchaseBackground(bg) },
        onEquip = { gameManager.selectBackground(bg) },
    )
}

@Composable
private fun ShopItemRow(
    title: String,
    description: String,
    accent: Color,
    isSelected: Boolean,
    isUnlocked: Boolean,
    price: Int,
    canAfford: Boolean,
    onPurchase: () -> Unit,
    onEquip: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSelected) accent.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
            )
            .border(
                1.5.dp,
                if (isSelected) accent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(18.dp),
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.2f))
                .neonGlow(accent),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(description, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp)
        }

        when {
            isUnlocked && isSelected -> {
                Text(
                    "EQUIPPED",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(accent.copy(alpha = 0.15f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            isUnlocked -> {
                Text(
                    "EQUIP",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable(onClick = onEquip)
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                )
            }
            else -> {
                Text(
                    text = "◎ $price",
                    color = if (canAfford) NeonPalette.Gold else Color.White.copy(alpha = 0.35f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (canAfford) Color.Transparent else Color.Gray.copy(alpha = 0.15f))
                        .border(
                            1.5.dp,
                            if (canAfford) NeonPalette.Gold else Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(50),
                        )
                        .then(if (canAfford) Modifier.clickable(onClick = onPurchase) else Modifier)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}
