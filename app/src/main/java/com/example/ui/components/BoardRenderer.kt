package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.engine.GameEngine
import com.example.core.model.BallSkin
import com.example.core.model.BoardTheme
import com.example.core.model.Direction
import com.example.core.model.Position
import com.example.core.model.TileType
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.EmeraldSwitch
import com.example.ui.theme.GoldStar
import com.example.ui.theme.GridFloorDark
import com.example.ui.theme.GridFloorLight
import com.example.ui.theme.NeonTeal
import com.example.ui.theme.PortalCyan
import com.example.ui.theme.PortalMagenta
import com.example.ui.theme.ReversePurple
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.WallColor
import com.example.ui.theme.WallHighlight
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BoardRenderer(
    engine: GameEngine,
    skin: BallSkin,
    theme: BoardTheme,
    onSwipe: (Direction) -> Unit,
    modifier: Modifier = Modifier
) {
    val ballX = remember { Animatable(engine.ballPosition.x.toFloat()) }
    val ballY = remember { Animatable(engine.ballPosition.y.toFloat()) }

    LaunchedEffect(engine.ballPosition) {
        ballX.animateTo(
            targetValue = engine.ballPosition.x.toFloat(),
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f)
        )
    }

    LaunchedEffect(engine.ballPosition) {
        ballY.animateTo(
            targetValue = engine.ballPosition.y.toFloat(),
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 800f)
        )
    }

    // Portal & Hazard rotation pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val warningPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warning"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(engine.width.toFloat() / engine.height.toFloat())
            .padding(8.dp)
            .testTag("game_board_canvas")
            .pointerInput(engine) {
                var dragAccumX = 0f
                var dragAccumY = 0f
                val threshold = 40f

                detectDragGestures(
                    onDragStart = {
                        dragAccumX = 0f
                        dragAccumY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumX += dragAmount.x
                        dragAccumY += dragAmount.y
                    },
                    onDragEnd = {
                        if (abs(dragAccumX) > abs(dragAccumY)) {
                            if (abs(dragAccumX) > threshold) {
                                if (dragAccumX > 0) onSwipe(Direction.RIGHT) else onSwipe(Direction.LEFT)
                            }
                        } else {
                            if (abs(dragAccumY) > threshold) {
                                if (dragAccumY > 0) onSwipe(Direction.DOWN) else onSwipe(Direction.UP)
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cellWidth = size.width / engine.width
            val cellHeight = size.height / engine.height
            val tileSize = minOf(cellWidth, cellHeight)

            // 1. Draw Board Background Grid
            for (y in 0 until engine.height) {
                for (x in 0 until engine.width) {
                    val tile = engine.getTileAt(x, y)
                    val topLeft = Offset(x * cellWidth, y * cellHeight)
                    val cellSize = Size(cellWidth, cellHeight)

                    // Draw floor cell
                    val isAlt = (x + y) % 2 == 0
                    val baseFloorColor = if (isAlt) GridFloorLight else GridFloorDark
                    drawRect(color = baseFloorColor, topLeft = topLeft, size = cellSize)

                    // Subtle cell border
                    drawRect(
                        color = SurfaceBorder.copy(alpha = 0.25f),
                        topLeft = topLeft,
                        size = cellSize,
                        style = Stroke(width = 1f)
                    )

                    // Draw Tile Specifics
                    drawTile(
                        tile = tile,
                        pos = Position(x, y),
                        topLeft = topLeft,
                        cellSize = cellSize,
                        pulseAngle = pulseAngle,
                        warningPulse = warningPulse,
                        engine = engine
                    )
                }
            }

            // 2. Draw Moving Hazards
            for (hazard in engine.currentTraps.movingHazards) {
                val hLeft = Offset(hazard.currentPos.x * cellWidth, hazard.currentPos.y * cellHeight)
                drawMovingSentry(hLeft, Size(cellWidth, cellHeight), hazard.direction, pulseAngle)
            }

            // 3. Draw Player Ball with smooth animated coordinates
            val currentBallX = ballX.value
            val currentBallY = ballY.value
            val ballCenter = Offset(
                x = (currentBallX + 0.5f) * cellWidth,
                y = (currentBallY + 0.5f) * cellHeight
            )
            val ballRadius = tileSize * 0.38f

            drawPlayerBall(
                center = ballCenter,
                radius = ballRadius,
                skin = skin,
                confusionTurns = engine.confusionTurns,
                pulseAngle = pulseAngle
            )
        }
    }
}

private fun DrawScope.drawTile(
    tile: TileType,
    pos: Position,
    topLeft: Offset,
    cellSize: Size,
    pulseAngle: Float,
    warningPulse: Float,
    engine: GameEngine
) {
    val center = Offset(topLeft.x + cellSize.width / 2f, topLeft.y + cellSize.height / 2f)
    val minDim = minOf(cellSize.width, cellSize.height)

    when (tile) {
        TileType.WALL -> {
            drawRoundRect(
                color = WallColor,
                topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
                size = Size(cellSize.width - 4f, cellSize.height - 4f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Bevel highlight
            drawRoundRect(
                color = WallHighlight,
                topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
                size = Size(cellSize.width - 4f, 6f),
                cornerRadius = CornerRadius(6f, 6f)
            )
        }

        TileType.GOAL -> {
            // Radiant Golden Vortex
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoldStar, GoldStar.copy(alpha = 0.2f), Color.Transparent),
                    center = center,
                    radius = minDim * 0.45f
                ),
                radius = minDim * 0.45f,
                center = center
            )
            // Spinning portal ring
            drawArc(
                color = GoldStar,
                startAngle = pulseAngle,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(center.x - minDim * 0.35f, center.y - minDim * 0.35f),
                size = Size(minDim * 0.7f, minDim * 0.7f),
                style = Stroke(width = 4f)
            )
            drawCircle(color = Color.White, radius = minDim * 0.12f, center = center)
        }

        TileType.FAKE_GOAL -> {
            // Looks like goal with faint purple hint
            drawCircle(
                color = GoldStar.copy(alpha = 0.8f),
                radius = minDim * 0.4f,
                center = center
            )
            drawArc(
                color = ReversePurple,
                startAngle = -pulseAngle,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = Offset(center.x - minDim * 0.35f, center.y - minDim * 0.35f),
                size = Size(minDim * 0.7f, minDim * 0.7f),
                style = Stroke(width = 3f)
            )
        }

        TileType.HAZARD -> {
            // Spikes (4 red triangular points)
            val spikeColor = DangerCoral
            val spikeRadius = minDim * 0.32f
            for (i in 0 until 4) {
                val angle = (i * 90f + 45f) * (PI.toFloat() / 180f)
                val tip = Offset(center.x + cos(angle) * spikeRadius, center.y + sin(angle) * spikeRadius)
                val base1 = Offset(
                    center.x + cos(angle + 0.3f) * (spikeRadius * 0.3f),
                    center.y + sin(angle + 0.3f) * (spikeRadius * 0.3f)
                )
                val base2 = Offset(
                    center.x + cos(angle - 0.3f) * (spikeRadius * 0.3f),
                    center.y + sin(angle - 0.3f) * (spikeRadius * 0.3f)
                )
                val path = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(base1.x, base1.y)
                    lineTo(base2.x, base2.y)
                    close()
                }
                drawPath(path, color = spikeColor)
            }
            drawCircle(color = DangerCoral.copy(alpha = 0.3f * warningPulse), radius = minDim * 0.42f, center = center)
        }

        TileType.FAKE_SAFE -> {
            // Subtle crack line indicating danger
            drawLine(
                color = DangerCoral.copy(alpha = 0.35f),
                start = Offset(topLeft.x + 6f, topLeft.y + cellSize.height * 0.3f),
                end = Offset(topLeft.x + cellSize.width * 0.7f, topLeft.y + cellSize.height * 0.8f),
                strokeWidth = 2f
            )
        }

        TileType.COUNTDOWN -> {
            val countdown = engine.currentTraps.countdownTiles.find { it.pos == pos }
            val turnsLeft = countdown?.remainingTurns ?: 5
            val badgeColor = if (turnsLeft <= 2) DangerCoral else GoldStar
            drawRoundRect(
                color = badgeColor.copy(alpha = 0.3f),
                topLeft = Offset(center.x - minDim * 0.3f, center.y - minDim * 0.3f),
                size = Size(minDim * 0.6f, minDim * 0.6f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 3f)
            )
            // Inner dots for countdown ticks
            for (dot in 0 until turnsLeft.coerceAtMost(5)) {
                val dotX = center.x - (minDim * 0.2f) + (dot * (minDim * 0.1f))
                drawCircle(color = badgeColor, radius = 4f, center = Offset(dotX, center.y))
            }
        }

        TileType.DISAPPEARING -> {
            val dt = engine.currentTraps.disappearingTiles.find { it.pos == pos }
            if (dt?.isCrumbled == true) {
                drawRect(color = Color.Black, topLeft = topLeft, size = cellSize)
            } else if (dt?.isStepped == true) {
                drawRect(color = DangerCoral.copy(alpha = 0.4f), topLeft = topLeft, size = cellSize)
            } else {
                // Stone crack pattern
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(center.x - minDim * 0.25f, center.y - minDim * 0.25f),
                    end = Offset(center.x + minDim * 0.25f, center.y + minDim * 0.25f),
                    strokeWidth = 2f
                )
            }
        }

        TileType.SWITCH -> {
            val link = engine.currentTraps.switchBridgeLinks.find { it.switchPos == pos }
            val isAct = link?.isActivated == true
            val col = if (isAct) EmeraldSwitch else Color(0xFF6B7280)
            drawCircle(color = col, radius = minDim * 0.3f, center = center)
            drawCircle(color = Color.White, radius = minDim * 0.12f, center = center)
        }

        TileType.BRIDGE_OPEN -> {
            drawRoundRect(
                color = EmeraldSwitch.copy(alpha = 0.5f),
                topLeft = Offset(topLeft.x + 3f, topLeft.y + 3f),
                size = Size(cellSize.width - 6f, cellSize.height - 6f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }

        TileType.BRIDGE_CLOSED -> {
            // Gap with dashed border
            drawRect(color = Color.Black.copy(alpha = 0.8f), topLeft = topLeft, size = cellSize)
            drawRect(
                color = DangerCoral.copy(alpha = 0.5f),
                topLeft = topLeft,
                size = cellSize,
                style = Stroke(width = 2f)
            )
        }

        TileType.PORTAL_A -> {
            drawCircle(
                brush = Brush.radialGradient(listOf(PortalCyan, Color.Transparent), center = center, radius = minDim * 0.45f),
                radius = minDim * 0.45f,
                center = center
            )
            drawArc(
                color = PortalCyan,
                startAngle = pulseAngle,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - minDim * 0.35f, center.y - minDim * 0.35f),
                size = Size(minDim * 0.7f, minDim * 0.7f),
                style = Stroke(width = 3f)
            )
        }

        TileType.PORTAL_B -> {
            drawCircle(
                brush = Brush.radialGradient(listOf(PortalMagenta, Color.Transparent), center = center, radius = minDim * 0.45f),
                radius = minDim * 0.45f,
                center = center
            )
            drawArc(
                color = PortalMagenta,
                startAngle = -pulseAngle,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - minDim * 0.35f, center.y - minDim * 0.35f),
                size = Size(minDim * 0.7f, minDim * 0.7f),
                style = Stroke(width = 3f)
            )
        }

        TileType.REVERSE_CONTROLS -> {
            drawCircle(
                color = ReversePurple.copy(alpha = 0.4f),
                radius = minDim * 0.38f,
                center = center
            )
            drawArc(
                color = ReversePurple,
                startAngle = pulseAngle * 2,
                sweepAngle = 200f,
                useCenter = false,
                topLeft = Offset(center.x - minDim * 0.28f, center.y - minDim * 0.28f),
                size = Size(minDim * 0.56f, minDim * 0.56f),
                style = Stroke(width = 3f)
            )
        }

        TileType.SHIFTING_WALL_SHUT -> {
            drawRoundRect(
                color = WallColor,
                topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
                size = Size(cellSize.width - 4f, cellSize.height - 4f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawCircle(color = DangerCoral, radius = 5f, center = center)
        }

        TileType.SHIFTING_WALL_OPEN -> {
            drawRoundRect(
                color = WallColor.copy(alpha = 0.2f),
                topLeft = Offset(topLeft.x + 4f, topLeft.y + 4f),
                size = Size(cellSize.width - 8f, cellSize.height - 8f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2f)
            )
            drawCircle(color = EmeraldSwitch, radius = 5f, center = center)
        }

        TileType.INVISIBLE_PATH -> {
            if (pos in engine.revealedInvisiblePaths) {
                // Revealed path: Glowing crystalline ethereal bridge
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(NeonTeal.copy(alpha = 0.6f), PortalCyan.copy(alpha = 0.45f))
                    ),
                    topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
                    size = Size(cellSize.width - 4f, cellSize.height - 4f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 3f, center = center)
            } else {
                // Unrevealed: Looks like deep dark abyss with a faint subtle stardust glint
                drawRect(color = Color(0xFF030712), topLeft = topLeft, size = cellSize)
                drawCircle(
                    color = PortalCyan.copy(alpha = 0.2f * warningPulse),
                    radius = 2f,
                    center = center
                )
            }
        }

        TileType.MEMORY_TILE -> {
            val seqIndex = engine.level.memorySequence.indexOf(pos)
            val isActivated = seqIndex in 0 until engine.memoryProgress
            val padColor = if (isActivated) EmeraldSwitch else Color(0xFF38BDF8)
            val alpha = if (isActivated) 0.85f else 0.4f

            drawRoundRect(
                color = padColor.copy(alpha = alpha),
                topLeft = Offset(center.x - minDim * 0.32f, center.y - minDim * 0.32f),
                size = Size(minDim * 0.64f, minDim * 0.64f),
                cornerRadius = CornerRadius(8f, 8f),
                style = if (isActivated) Fill else Stroke(width = 3f)
            )

            // Step dots indicator
            val numDots = if (seqIndex >= 0) seqIndex + 1 else 1
            for (d in 0 until numDots.coerceAtMost(4)) {
                val dX = center.x - ((numDots - 1) * 6f) + (d * 12f)
                drawCircle(
                    color = if (isActivated) Color.White else padColor,
                    radius = 3.5f,
                    center = Offset(dX, center.y)
                )
            }
        }

        else -> {
            // Standard floor
        }
    }
}

private fun DrawScope.drawMovingSentry(
    topLeft: Offset,
    cellSize: Size,
    direction: Direction,
    pulseAngle: Float
) {
    val center = Offset(topLeft.x + cellSize.width / 2f, topLeft.y + cellSize.height / 2f)
    val radius = minOf(cellSize.width, cellSize.height) * 0.35f

    // Mechanical spike ball body
    drawCircle(color = DangerCoral, radius = radius, center = center)
    drawCircle(color = Color(0xFF6B1D25), radius = radius * 0.6f, center = center)
    drawCircle(color = Color.White, radius = radius * 0.25f, center = center)

    // Pulsing danger halo
    drawCircle(
        color = DangerCoral.copy(alpha = 0.3f),
        radius = radius * 1.3f,
        center = center,
        style = Stroke(width = 2f)
    )
}

private fun DrawScope.drawPlayerBall(
    center: Offset,
    radius: Float,
    skin: BallSkin,
    confusionTurns: Int,
    pulseAngle: Float
) {
    // Drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.45f),
        radius = radius * 0.95f,
        center = Offset(center.x + 3f, center.y + 5f)
    )

    when (skin) {
        BallSkin.CLASSIC -> {
            // Glossy Electric Neon Teal Orb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF70FCE4), NeonTeal, Color(0xFF007565)),
                    center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
                    radius = radius * 1.2f
                ),
                radius = radius,
                center = center
            )
            // Specular shiny highlight
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = radius * 0.28f,
                center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
            )
        }

        BallSkin.CYBER_PULSE -> {
            // Neon Pulsar with outer glow
            drawCircle(
                color = NeonTeal.copy(alpha = 0.35f),
                radius = radius * 1.4f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = radius * 0.85f,
                center = center
            )
            drawCircle(
                color = NeonTeal,
                radius = radius * 0.6f,
                center = center
            )
        }

        BallSkin.RETRO_PIXEL -> {
            // 8-bit chunky hero
            drawRect(
                color = GoldStar,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            drawRect(
                color = Color.Black,
                topLeft = Offset(center.x - radius * 0.4f, center.y - radius * 0.4f),
                size = Size(radius * 0.35f, radius * 0.35f)
            )
        }

        BallSkin.INFERNO -> {
            // Burning fire core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFF275), Color(0xFFFF8C42), DangerCoral),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
        }

        BallSkin.COSMIC_VOID -> {
            // Cosmic starfield sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ReversePurple, Color(0xFF10002B)),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawCircle(color = Color.White, radius = 2f, center = Offset(center.x - 4f, center.y - 3f))
            drawCircle(color = Color.White, radius = 2.5f, center = Offset(center.x + 5f, center.y + 4f))
        }
    }

    // Confusion indicator (reverse controls)
    if (confusionTurns > 0) {
        drawArc(
            color = ReversePurple,
            startAngle = pulseAngle * 3,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(center.x - radius * 1.4f, center.y - radius * 1.4f),
            size = Size(radius * 2.8f, radius * 2.8f),
            style = Stroke(width = 3f)
        )
    }
}
