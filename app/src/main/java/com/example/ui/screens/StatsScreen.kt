package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BgDarkNavy
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.GoldStar
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GameViewModel

@Composable
fun StatsScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val userStats by viewModel.userStats.collectAsState()
    val allProgress by viewModel.allProgress.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val totalGames = userStats?.totalGamesPlayed ?: 0
    val totalWins = userStats?.totalWins ?: 0
    val totalDeaths = userStats?.totalDeaths ?: 0
    val totalMoves = userStats?.totalMoves ?: 0L
    val endlessBest = userStats?.endlessHighScore ?: 0
    val levelsSolved = allProgress.count { it.isCompleted }
    val winRate = if (totalGames > 0) ((totalWins.toFloat() / totalGames) * 100).toInt() else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDarkNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("stats_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // TOP BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_stats_back")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "CAREER STATISTICS",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STATS TILES GRID
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(title = "SOLVED LEVELS", value = "$levelsSolved / 50", accent = NeonTeal, modifier = Modifier.weight(1f))
                StatCard(title = "WIN RATE", value = "$winRate%", accent = GoldStar, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(title = "ENDLESS BEST", value = "Floor $endlessBest", accent = PortalCyan, modifier = Modifier.weight(1f))
                StatCard(title = "DEATHS / RETRIES", value = "$totalDeaths", accent = DangerCoral, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(title = "TOTAL MOVES", value = "$totalMoves", accent = TextSecondary, modifier = Modifier.weight(1f))
                StatCard(title = "TOTAL COINS", value = "${uiState.totalCoins}", accent = GoldStar, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ACHIEVEMENTS SECTION
            Text(
                text = "ACHIEVEMENTS",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            AchievementItem(
                title = "First Step",
                desc = "Complete Level 1",
                isUnlocked = levelsSolved >= 1
            )
            AchievementItem(
                title = "Ten Down",
                desc = "Complete the first 10 levels",
                isUnlocked = levelsSolved >= 10
            )
            AchievementItem(
                title = "Trapshift Master",
                desc = "Complete 25 handcrafted levels",
                isUnlocked = levelsSolved >= 25
            )
            AchievementItem(
                title = "Crucible Champion",
                desc = "Conquer all 50 handcrafted levels",
                isUnlocked = levelsSolved >= 50
            )
            AchievementItem(
                title = "Endless Survivor",
                desc = "Reach Floor 10 in Endless Mode",
                isUnlocked = endlessBest >= 10
            )
            AchievementItem(
                title = "Daily Devotee",
                desc = "Complete a Daily Challenge",
                isUnlocked = uiState.dailyCompletedToday
            )
            AchievementItem(
                title = "Style Icon",
                desc = "Unlock a custom ball skin",
                isUnlocked = uiState.unlockedSkins.size > 1
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceCard)
            .border(1.5.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun AchievementItem(
    title: String,
    desc: String,
    isUnlocked: Boolean
) {
    val bg = if (isUnlocked) SurfaceCard else SurfaceDeep.copy(alpha = 0.6f)
    val tint = if (isUnlocked) GoldStar else TextMuted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, if (isUnlocked) GoldStar.copy(alpha = 0.4f) else SurfaceBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isUnlocked) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                color = if (isUnlocked) TextPrimary else TextMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
