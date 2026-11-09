package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CoinBadge
import com.example.ui.theme.BgDarkNavy
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.GoldStar
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.ReversePurple
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GameViewModel

@Composable
fun HomeScreen(
    viewModel: GameViewModel,
    onPlayLevels: () -> Unit,
    onPlayEndless: () -> Unit,
    onPlayDaily: () -> Unit,
    onPlayRage: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val allProgress by viewModel.allProgress.collectAsState()
    val completedCount = allProgress.count { it.isCompleted }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDarkNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("home_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP HEADER: Coins & Quick Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoinBadge(coins = uiState.totalCoins)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Stats Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .border(1.5.dp, SurfaceBorder, CircleShape)
                            .clickable(onClick = onOpenStats)
                            .testTag("btn_home_stats"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Statistics",
                            tint = NeonTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .border(1.5.dp, SurfaceBorder, CircleShape)
                            .clickable(onClick = onOpenSettings)
                            .testTag("btn_home_settings"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // TITLE HERO
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Animated Glowing Orb
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(NeonTeal, Color(0xFF005144), Color.Transparent)
                            )
                        )
                        .border(2.5.dp, NeonTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF00382E),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "TRAPSHIFT",
                    color = TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Text(
                    text = "ONE MORE TRY. ONE MORE TRAP.",
                    color = NeonTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 4 GAME MODE CARDS

            // 1. LEVEL MODE
            ModeCard(
                title = "LEVEL MODE",
                subtitle = "50 Handcrafted Puzzles",
                badgeText = "$completedCount / 50 Solved",
                icon = Icons.Default.PlayArrow,
                accentColor = NeonTeal,
                testTag = "card_mode_levels",
                onClick = onPlayLevels
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. ENDLESS MODE
            ModeCard(
                title = "ENDLESS MODE",
                subtitle = "Procedural Shifting Gauntlet",
                badgeText = "Best: Floor ${uiState.endlessBest}",
                icon = Icons.Default.Timeline,
                accentColor = PortalCyan,
                testTag = "card_mode_endless",
                onClick = onPlayEndless
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. DAILY CHALLENGE
            ModeCard(
                title = "DAILY CHALLENGE",
                subtitle = uiState.dailyDateString,
                badgeText = if (uiState.dailyCompletedToday) "COMPLETED TODAY" else "NEW PUZZLE",
                icon = if (uiState.dailyCompletedToday) Icons.Default.CheckCircle else Icons.Default.CalendarToday,
                accentColor = if (uiState.dailyCompletedToday) GoldStar else Color(0xFF06D6A0),
                testTag = "card_mode_daily",
                onClick = onPlayDaily
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. RAGE MODE
            ModeCard(
                title = "RAGE MODE",
                subtitle = "Limited Moves & Extreme Traps",
                badgeText = "HIGH DIFFICULTY",
                icon = Icons.Default.Dangerous,
                accentColor = DangerCoral,
                testTag = "card_mode_rage",
                onClick = onPlayRage
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(1.dp, accentColor, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = badgeText,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
