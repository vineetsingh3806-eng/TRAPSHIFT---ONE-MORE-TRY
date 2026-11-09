package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.levels.LevelRepository
import com.example.core.model.LevelDefinition
import com.example.ui.components.CoinBadge
import com.example.ui.theme.BgDarkNavy
import com.example.ui.theme.GoldStar
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GameViewModel

@Composable
fun LevelSelectScreen(
    viewModel: GameViewModel,
    onSelectLevel: (Int) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val allLevels = remember { LevelRepository.getAllLevels() }
    val progressList by viewModel.allProgress.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var selectedChapter by remember { mutableIntStateOf(1) }

    val chapters = listOf(
        "Ch 1: First Steps (1–5)",
        "Ch 2: Danger Ahead (6–10)",
        "Ch 3: Tick Tock (11–20)",
        "Ch 4: Mind Benders (21–30)",
        "Ch 5: Chaos Shift (31–40)",
        "Ch 6: Crucible (41–50)"
    )

    val currentChapterLevels = remember(selectedChapter) {
        allLevels.filter { it.chapter == selectedChapter }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDarkNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("level_select_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_level_select_back")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Text(
                    text = "SELECT LEVEL",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                CoinBadge(coins = uiState.totalCoins)
            }

            // CHAPTER TABS
            ScrollableTabRow(
                selectedTabIndex = selectedChapter - 1,
                containerColor = SurfaceDeep,
                contentColor = NeonTeal,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedChapter - 1]),
                        color = NeonTeal
                    )
                }
            ) {
                chapters.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedChapter == index + 1,
                        onClick = { selectedChapter = index + 1 },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedChapter == index + 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedChapter == index + 1) NeonTeal else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // LEVEL GRID
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(currentChapterLevels) { level ->
                    val progress = progressList.find { it.levelId == level.id }
                    // Level 1 is always unlocked by default
                    val isUnlocked = level.id == 1 || (progress?.isUnlocked == true)
                    val stars = progress?.stars ?: 0

                    LevelTile(
                        level = level,
                        isUnlocked = isUnlocked,
                        stars = stars,
                        onClick = {
                            if (isUnlocked) onSelectLevel(level.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LevelTile(
    level: LevelDefinition,
    isUnlocked: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    val bg = if (isUnlocked) SurfaceCard else SurfaceDeep.copy(alpha = 0.5f)
    val borderCol = if (isUnlocked) SurfaceBorder else SurfaceBorder.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, borderCol, RoundedCornerShape(16.dp))
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("level_tile_${level.id}"),
        contentAlignment = Alignment.Center
    ) {
        if (isUnlocked) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${level.id}",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Star dots
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 1..3) {
                        val earned = i <= stars
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (earned) GoldStar else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        } else {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
