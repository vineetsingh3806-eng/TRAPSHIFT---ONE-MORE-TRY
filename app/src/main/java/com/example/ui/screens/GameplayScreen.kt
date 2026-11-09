package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.core.model.ControlScheme
import com.example.core.model.GameMode
import com.example.core.model.GameStatus
import com.example.ui.components.BoardRenderer
import com.example.ui.components.CoinBadge
import com.example.ui.components.DPadControls
import com.example.ui.components.FailureDialog
import com.example.ui.components.PauseDialog
import com.example.ui.components.VictoryDialog
import com.example.ui.theme.BgDarkNavy
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.GoldStar
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.ReversePurple
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.GameViewModel

@Composable
fun GameplayScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val engine = viewModel.engine

    BackHandler {
        if (uiState.isPaused) {
            viewModel.setPaused(false)
        } else {
            onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDarkNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("gameplay_screen")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("btn_back")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val modeTitle = when (uiState.currentMode) {
                        GameMode.LEVELS -> "LEVEL ${uiState.currentLevel.id}"
                        GameMode.ENDLESS -> "FLOOR ${uiState.endlessRound}"
                        GameMode.DAILY -> "DAILY CHALLENGE"
                        GameMode.RAGE -> "RAGE ${uiState.currentLevel.id - 500}"
                    }
                    Text(
                        text = modeTitle,
                        color = NeonTeal,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = uiState.currentLevel.title,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinBadge(coins = uiState.totalCoins)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.setPaused(true) },
                        modifier = Modifier.testTag("btn_pause")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = TextPrimary
                        )
                    }
                }
            }

            // STATS BAR (Moves, 3-Star Target, Rage move cap)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Move counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MOVES: ",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${engine.moveCount}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (uiState.currentLevel.maxMovesAllowed != null) {
                        Text(
                            text = " / ${uiState.currentLevel.maxMovesAllowed}",
                            color = DangerCoral,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3-Star target
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = GoldStar,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "3-Star: ≤ ${uiState.currentLevel.targetMoves3Star}",
                        color = GoldStar,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Quick Action Buttons (Hint & Instant Reset)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Hint button
                    IconButton(
                        onClick = { viewModel.requestHint() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .testTag("btn_hint")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = "Hint",
                            tint = GoldStar,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Instant Reset (<100ms)
                    IconButton(
                        onClick = { viewModel.restartCurrentLevel() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceCard)
                            .testTag("btn_restart")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Restart",
                            tint = NeonTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // REVERSE CONTROLS ALERT BANNER
            AnimatedVisibility(
                visible = engine.confusionTurns > 0,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ReversePurple.copy(alpha = 0.25f))
                        .border(1.5.dp, ReversePurple, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CONTROLS REVERSED! (${engine.confusionTurns} moves left)",
                        color = Color(0xFFD8BBFF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // HINT DIRECTION BANNER
            AnimatedVisibility(
                visible = uiState.activeHintDirection != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GoldStar.copy(alpha = 0.2f))
                        .border(1.5.dp, GoldStar, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "HINT: Move ${uiState.activeHintDirection?.name}",
                        color = GoldStar,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // THE GAME BOARD
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                BoardRenderer(
                    engine = engine,
                    skin = uiState.selectedSkin,
                    theme = uiState.selectedTheme,
                    onSwipe = { dir -> viewModel.handleMove(dir) }
                )
            }

            // D-PAD CONTROLS (If enabled in settings)
            if (uiState.controlScheme != ControlScheme.SWIPE_ONLY) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp, top = 4.dp)
                ) {
                    DPadControls(
                        onDirection = { dir -> viewModel.handleMove(dir) }
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // DIALOGS
        if (engine.gameStatus == GameStatus.WON) {
            val starsEarned = when {
                engine.moveCount <= uiState.currentLevel.targetMoves3Star -> 3
                engine.moveCount <= uiState.currentLevel.targetMoves2Star -> 2
                else -> 1
            }
            VictoryDialog(
                stars = starsEarned,
                moves = engine.moveCount,
                targetMoves3Star = uiState.currentLevel.targetMoves3Star,
                coinsEarned = uiState.lastEarnedCoins,
                onNextLevel = { viewModel.nextLevel() },
                onReplay = { viewModel.restartCurrentLevel() },
                onMenu = onNavigateBack
            )
        }

        if (engine.gameStatus == GameStatus.FAILED) {
            FailureDialog(
                reason = engine.failureReason ?: "You tripped into an unknown trap!",
                onRetry = { viewModel.restartCurrentLevel() },
                onMenu = onNavigateBack
            )
        }

        if (uiState.isPaused) {
            PauseDialog(
                onResume = { viewModel.setPaused(false) },
                onRestart = { viewModel.restartCurrentLevel() },
                onMenu = onNavigateBack
            )
        }
    }
}
