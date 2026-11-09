package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.model.Direction
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.GoldStar
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDeep
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DPadControls(
    onDirection: (Direction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.testTag("dpad_controls"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // UP
        DPadButton(
            icon = Icons.Default.ArrowUpward,
            contentDesc = "Move Up",
            testTag = "dpad_up",
            onClick = { onDirection(Direction.UP) }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT
            DPadButton(
                icon = Icons.Default.ArrowBack,
                contentDesc = "Move Left",
                testTag = "dpad_left",
                onClick = { onDirection(Direction.LEFT) }
            )

            // Center Spacer / Indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(SurfaceBorder.copy(alpha = 0.5f))
            )

            // RIGHT
            DPadButton(
                icon = Icons.Default.ArrowForward,
                contentDesc = "Move Right",
                testTag = "dpad_right",
                onClick = { onDirection(Direction.RIGHT) }
            )
        }

        // DOWN
        DPadButton(
            icon = Icons.Default.ArrowDownward,
            contentDesc = "Move Down",
            testTag = "dpad_down",
            onClick = { onDirection(Direction.DOWN) }
        )
    }
}

@Composable
fun DPadButton(
    icon: ImageVector,
    contentDesc: String,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(SurfaceCard, SurfaceDeep)
                )
            )
            .border(2.dp, SurfaceBorder, CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = NeonTeal,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun ArcadeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    testTag: String = "arcade_button"
) {
    val bgColor = if (isPrimary) NeonTeal else SurfaceCard
    val contentColor = if (isPrimary) Color(0xFF00382E) else TextPrimary
    val borderColor = if (isPrimary) Color(0xFF70FCE4) else SurfaceBorder

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun CoinBadge(coins: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.5.dp, GoldStar.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("coin_badge"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MonetizationOn,
            contentDescription = "Coins",
            tint = GoldStar,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$coins",
            color = GoldStar,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun StarRatingDisplay(stars: Int, maxStars: Int = 3, size: Int = 28) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..maxStars) {
            val isEarned = i <= stars
            val scale by animateFloatAsState(
                targetValue = if (isEarned) 1f else 0.8f,
                animationSpec = spring(),
                label = "star_scale"
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = if (isEarned) "Star Earned" else "Star Empty",
                tint = if (isEarned) GoldStar else TextMuted.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(size.dp)
                    .scale(scale)
            )
        }
    }
}

@Composable
fun VictoryDialog(
    stars: Int,
    moves: Int,
    targetMoves3Star: Int,
    coinsEarned: Int,
    onNextLevel: () -> Unit,
    onReplay: () -> Unit,
    onMenu: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDeep,
            border = BorderStroke(2.dp, GoldStar.copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("victory_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PORTAL REACHED!",
                    color = GoldStar,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
                StarRatingDisplay(stars = stars, size = 42)

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Completed in $moves moves",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "3-Star target: $targetMoves3Star moves",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceCard)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = GoldStar,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+$coinsEarned Coins",
                        color = GoldStar,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                ArcadeButton(
                    text = "NEXT LEVEL",
                    onClick = onNextLevel,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.PlayArrow,
                    testTag = "btn_next_level"
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArcadeButton(
                        text = "RETRY",
                        onClick = onReplay,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Refresh,
                        isPrimary = false,
                        testTag = "btn_victory_replay"
                    )
                    ArcadeButton(
                        text = "MAP",
                        onClick = onMenu,
                        modifier = Modifier.weight(1f),
                        isPrimary = false,
                        testTag = "btn_victory_menu"
                    )
                }
            }
        }
    }
}

@Composable
fun FailureDialog(
    reason: String,
    onRetry: () -> Unit,
    onMenu: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDeep,
            border = BorderStroke(2.dp, DangerCoral),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("failure_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ONE MORE TRY!",
                    color = DangerCoral,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = reason,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))
                ArcadeButton(
                    text = "TRY AGAIN",
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Refresh,
                    testTag = "btn_try_again"
                )

                Spacer(modifier = Modifier.height(10.dp))
                ArcadeButton(
                    text = "EXIT TO MAP",
                    onClick = onMenu,
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = false,
                    testTag = "btn_fail_menu"
                )
            }
        }
    }
}

@Composable
fun PauseDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SurfaceDeep,
            border = BorderStroke(2.dp, SurfaceBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pause_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PAUSED",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(20.dp))
                ArcadeButton(
                    text = "RESUME",
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.PlayArrow,
                    testTag = "btn_pause_resume"
                )

                Spacer(modifier = Modifier.height(10.dp))
                ArcadeButton(
                    text = "RESTART LEVEL",
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.Refresh,
                    isPrimary = false,
                    testTag = "btn_pause_restart"
                )

                Spacer(modifier = Modifier.height(10.dp))
                ArcadeButton(
                    text = "EXIT TO MAP",
                    onClick = onMenu,
                    modifier = Modifier.fillMaxWidth(),
                    isPrimary = false,
                    testTag = "btn_pause_menu"
                )
            }
        }
    }
}
