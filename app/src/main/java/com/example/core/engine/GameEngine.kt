package com.example.core.engine

import com.example.core.model.Direction
import com.example.core.model.GameStatus
import com.example.core.model.LevelDefinition
import com.example.core.model.Position
import com.example.core.model.TileType
import com.example.core.traps.CountdownTile
import com.example.core.traps.DisappearingTile
import com.example.core.traps.FakeSafeTile
import com.example.core.traps.MovingHazard
import com.example.core.traps.ShiftingWallGroup
import com.example.core.traps.SwitchBridgeLink
import com.example.core.traps.TrapConfiguration

data class MoveResult(
    val ballMoved: Boolean,
    val newPosition: Position,
    val gameStatus: GameStatus,
    val failureReason: String? = null,
    val eventType: GameEvent = GameEvent.NONE,
    val movesCount: Int = 0,
    val starsEarned: Int = 0
)

enum class GameEvent {
    NONE,
    MOVE,
    WALL_BUMP,
    SWITCH_TOGGLE,
    PORTAL_WARP,
    FAKE_GOAL_BAMBOOZLE,
    REVERSE_CONTROLS_TRIGGER,
    DISAPPEARING_COLLAPSE,
    MEMORY_STEP,
    MEMORY_FAIL,
    VICTORY,
    DEATH
}

class GameEngine(val level: LevelDefinition) {

    val width: Int = level.width
    val height: Int = level.height
    var ballPosition: Position = level.startPos
        private set
    var moveCount: Int = 0
        private set
    var confusionTurns: Int = 0
        private set
    var memoryProgress: Int = 0
        private set
    var isMemoryCompleted: Boolean = false
        private set
    var gameStatus: GameStatus = GameStatus.IDLE
        private set
    var failureReason: String? = null
        private set

    // Working grid
    private val grid: Array<Array<TileType>> = Array(height) { y ->
        Array(width) { x ->
            parseCharToTileType(level.rawGrid.getOrNull(y)?.getOrNull(x) ?: '.')
        }
    }

    private val initialTraps: TrapConfiguration = extractTrapConfiguration()
    var currentTraps: TrapConfiguration = initialTraps.deepCopy()
        private set

    private var previousBallPosition: Position? = null
    val revealedInvisiblePaths: MutableSet<Position> = mutableSetOf()

    init {
        reset()
    }

    fun reset() {
        ballPosition = level.startPos
        previousBallPosition = null
        moveCount = 0
        confusionTurns = 0
        memoryProgress = 0
        isMemoryCompleted = false
        gameStatus = GameStatus.PLAYING
        failureReason = null
        revealedInvisiblePaths.clear()
        currentTraps = initialTraps.deepCopy()

        // Reconstruct base grid
        for (y in 0 until height) {
            for (x in 0 until width) {
                grid[y][x] = parseCharToTileType(level.rawGrid.getOrNull(y)?.getOrNull(x) ?: '.')
            }
        }
    }

    fun getTileAt(x: Int, y: Int): TileType {
        if (x !in 0 until width || y !in 0 until height) return TileType.WALL
        val base = grid[y][x]

        // Check if shifting wall group is currently shut or open
        for (group in currentTraps.shiftingWalls) {
            if (Position(x, y) in group.positions) {
                return if (group.isClosed) TileType.SHIFTING_WALL_SHUT else TileType.SHIFTING_WALL_OPEN
            }
        }

        // Check switch/bridge state
        for (link in currentTraps.switchBridgeLinks) {
            if (Position(x, y) in link.bridgePositions) {
                return if (link.isActivated) TileType.BRIDGE_OPEN else TileType.BRIDGE_CLOSED
            }
        }

        // Check disappearing tile
        for (dt in currentTraps.disappearingTiles) {
            if (dt.pos == Position(x, y) && dt.isCrumbled) {
                return TileType.HAZARD
            }
        }

        // Check countdown tile state
        for (ct in currentTraps.countdownTiles) {
            if (ct.pos == Position(x, y) && ct.remainingTurns <= 0) {
                return TileType.HAZARD
            }
        }

        return base
    }

    fun move(inputDirection: Direction): MoveResult {
        if (gameStatus != GameStatus.PLAYING) {
            return MoveResult(
                ballMoved = false,
                newPosition = ballPosition,
                gameStatus = gameStatus,
                failureReason = failureReason,
                eventType = GameEvent.NONE,
                movesCount = moveCount
            )
        }

        // Handle reverse controls if active
        val actualDirection = if (confusionTurns > 0) {
            confusionTurns--
            inputDirection.opposite()
        } else {
            inputDirection
        }

        val target = ballPosition.step(actualDirection)

        // 1. Check boundary
        if (target.x !in 0 until width || target.y !in 0 until height) {
            return MoveResult(
                ballMoved = false,
                newPosition = ballPosition,
                gameStatus = gameStatus,
                eventType = GameEvent.WALL_BUMP,
                movesCount = moveCount
            )
        }

        val targetTile = getTileAt(target.x, target.y)

        // 2. Check solid impassable obstacles
        if (targetTile == TileType.WALL ||
            targetTile == TileType.BRIDGE_CLOSED ||
            targetTile == TileType.SHIFTING_WALL_SHUT
        ) {
            return MoveResult(
                ballMoved = false,
                newPosition = ballPosition,
                gameStatus = gameStatus,
                eventType = GameEvent.WALL_BUMP,
                movesCount = moveCount
            )
        }

        // Step accepted!
        moveCount++
        previousBallPosition = ballPosition
        ballPosition = target
        var event = GameEvent.MOVE

        // Check invisible path discovery
        if (target in currentTraps.invisiblePaths) {
            revealedInvisiblePaths.add(target)
        }

        // Check disappearing tile from previous step
        previousBallPosition?.let { prevPos ->
            for (dt in currentTraps.disappearingTiles) {
                if (dt.pos == prevPos && dt.isStepped && !dt.isCrumbled) {
                    dt.isCrumbled = true
                }
            }
        }

        // Mark current disappearing tile as stepped
        for (dt in currentTraps.disappearingTiles) {
            if (dt.pos == target) {
                dt.isStepped = true
            }
        }

        // 3. Process Tile Interactions
        when (targetTile) {
            TileType.GOAL -> {
                gameStatus = GameStatus.WON
                event = GameEvent.VICTORY
                val stars = calculateStars(moveCount, level)
                return MoveResult(
                    ballMoved = true,
                    newPosition = ballPosition,
                    gameStatus = gameStatus,
                    eventType = event,
                    movesCount = moveCount,
                    starsEarned = stars
                )
            }
            TileType.FAKE_GOAL -> {
                // Trap! Sends ball back to start and mocks player
                ballPosition = level.startPos
                event = GameEvent.FAKE_GOAL_BAMBOOZLE
            }
            TileType.HAZARD -> {
                gameStatus = GameStatus.FAILED
                failureReason = "Impaled on sharp spikes!"
                return MoveResult(
                    ballMoved = true,
                    newPosition = ballPosition,
                    gameStatus = gameStatus,
                    failureReason = failureReason,
                    eventType = GameEvent.DEATH,
                    movesCount = moveCount
                )
            }
            TileType.FAKE_SAFE -> {
                gameStatus = GameStatus.FAILED
                failureReason = "The tile was an illusion! Watch for faint cracks!"
                return MoveResult(
                    ballMoved = true,
                    newPosition = ballPosition,
                    gameStatus = gameStatus,
                    failureReason = failureReason,
                    eventType = GameEvent.DEATH,
                    movesCount = moveCount
                )
            }
            TileType.SWITCH -> {
                for (link in currentTraps.switchBridgeLinks) {
                    if (link.switchPos == target) {
                        link.isActivated = !link.isActivated
                        event = GameEvent.SWITCH_TOGGLE
                    }
                }
            }
            TileType.PORTAL_A -> {
                val partner = currentTraps.portalsB.firstOrNull()
                if (partner != null) {
                    ballPosition = partner
                    event = GameEvent.PORTAL_WARP
                }
            }
            TileType.PORTAL_B -> {
                val partner = currentTraps.portalsA.firstOrNull()
                if (partner != null) {
                    ballPosition = partner
                    event = GameEvent.PORTAL_WARP
                }
            }
            TileType.REVERSE_CONTROLS -> {
                confusionTurns = 3
                event = GameEvent.REVERSE_CONTROLS_TRIGGER
            }
            TileType.MEMORY_TILE -> {
                if (level.memorySequence.isNotEmpty()) {
                    val expectedPos = level.memorySequence.getOrNull(memoryProgress)
                    if (target == expectedPos) {
                        memoryProgress++
                        event = GameEvent.MEMORY_STEP
                        if (memoryProgress >= level.memorySequence.size) {
                            isMemoryCompleted = true
                        }
                    } else {
                        memoryProgress = 0
                        event = GameEvent.MEMORY_FAIL
                    }
                } else {
                    event = GameEvent.MEMORY_STEP
                }
            }
            else -> {
                // Regular floor or bridge
            }
        }

        // 4. Update Countdown Tiles
        for (ct in currentTraps.countdownTiles) {
            if (ct.remainingTurns > 0) {
                ct.remainingTurns--
                if (ct.remainingTurns <= 0 && ct.pos == ballPosition) {
                    gameStatus = GameStatus.FAILED
                    failureReason = "Countdown timer expired under your feet!"
                    return MoveResult(
                        ballMoved = true,
                        newPosition = ballPosition,
                        gameStatus = gameStatus,
                        failureReason = failureReason,
                        eventType = GameEvent.DEATH,
                        movesCount = moveCount
                    )
                }
            }
        }

        // 5. Update Shifting Walls
        for (group in currentTraps.shiftingWalls) {
            if (moveCount % group.cadenceTurns == 0) {
                group.isClosed = !group.isClosed
            }
        }

        // 6. Update Moving Hazards
        for (hazard in currentTraps.movingHazards) {
            val nextHazardPos = hazard.currentPos.step(hazard.direction)
            if (nextHazardPos.x < hazard.minPos.x || nextHazardPos.x > hazard.maxPos.x ||
                nextHazardPos.y < hazard.minPos.y || nextHazardPos.y > hazard.maxPos.y ||
                getTileAt(nextHazardPos.x, nextHazardPos.y) == TileType.WALL
            ) {
                hazard.direction = hazard.direction.opposite()
                hazard.currentPos = hazard.currentPos.step(hazard.direction)
            } else {
                hazard.currentPos = nextHazardPos
            }

            // Hazard collides with player
            if (hazard.currentPos == ballPosition) {
                gameStatus = GameStatus.FAILED
                failureReason = "Crushed by a moving sentry obstacle!"
                return MoveResult(
                    ballMoved = true,
                    newPosition = ballPosition,
                    gameStatus = gameStatus,
                    failureReason = failureReason,
                    eventType = GameEvent.DEATH,
                    movesCount = moveCount
                )
            }
        }

        // 7. Check Rage Mode Move Limit
        level.maxMovesAllowed?.let { maxMoves ->
            if (moveCount >= maxMoves && gameStatus != GameStatus.WON) {
                gameStatus = GameStatus.FAILED
                failureReason = "Out of moves! Rage limit exceeded ($maxMoves moves)!"
                return MoveResult(
                    ballMoved = true,
                    newPosition = ballPosition,
                    gameStatus = gameStatus,
                    failureReason = failureReason,
                    eventType = GameEvent.DEATH,
                    movesCount = moveCount
                )
            }
        }

        return MoveResult(
            ballMoved = true,
            newPosition = ballPosition,
            gameStatus = gameStatus,
            eventType = event,
            movesCount = moveCount
        )
    }

    private fun calculateStars(moves: Int, level: LevelDefinition): Int {
        return when {
            moves <= level.targetMoves3Star -> 3
            moves <= level.targetMoves2Star -> 2
            else -> 1
        }
    }

    private fun parseCharToTileType(c: Char): TileType = when (c) {
        '#' -> TileType.WALL
        'G' -> TileType.GOAL
        'F' -> TileType.FAKE_GOAL
        'X' -> TileType.HAZARD
        'S' -> TileType.SWITCH
        'B' -> TileType.BRIDGE_CLOSED
        'O' -> TileType.BRIDGE_OPEN
        '1' -> TileType.PORTAL_A
        '2' -> TileType.PORTAL_B
        'R' -> TileType.REVERSE_CONTROLS
        'K' -> TileType.FAKE_SAFE
        'C' -> TileType.COUNTDOWN
        'D' -> TileType.DISAPPEARING
        'I' -> TileType.INVISIBLE_PATH
        'E' -> TileType.MEMORY_TILE
        'M' -> TileType.MOVING_HAZARD
        'W' -> TileType.SHIFTING_WALL_SHUT
        else -> TileType.FLOOR
    }

    private fun extractTrapConfiguration(): TrapConfiguration {
        val config = TrapConfiguration()
        val switches = mutableListOf<Position>()
        val bridges = mutableListOf<Position>()

        for (y in 0 until height) {
            val row = level.rawGrid.getOrNull(y) ?: ""
            for (x in 0 until width) {
                val c = row.getOrNull(x) ?: '.'
                val pos = Position(x, y)
                when (c) {
                    '1' -> config.portalsA.add(pos)
                    '2' -> config.portalsB.add(pos)
                    'F' -> config.fakeGoals.add(pos)
                    'K' -> config.fakeSafeTiles.add(FakeSafeTile(pos))
                    'C' -> config.countdownTiles.add(CountdownTile(pos, 5, 5))
                    'D' -> config.disappearingTiles.add(DisappearingTile(pos))
                    'S' -> switches.add(pos)
                    'B', 'O' -> bridges.add(pos)
                    'R' -> config.reverseControlTiles.add(pos)
                    'I' -> config.invisiblePaths.add(pos)
                    'E' -> config.memoryTiles.add(pos)
                    'M' -> {
                        // Default horizontal patrol within row boundaries
                        config.movingHazards.add(
                            MovingHazard(
                                currentPos = pos,
                                direction = Direction.RIGHT,
                                minPos = Position(kotlin.math.max(0, x - 2), y),
                                maxPos = Position(kotlin.math.min(width - 1, x + 2), y)
                            )
                        )
                    }
                    'W' -> {
                        config.shiftingWalls.add(
                            ShiftingWallGroup(
                                positions = listOf(pos),
                                cadenceTurns = 2,
                                isClosed = true
                            )
                        )
                    }
                }
            }
        }

        // Link switches to nearby bridges
        if (switches.isNotEmpty() && bridges.isNotEmpty()) {
            config.switchBridgeLinks.add(
                SwitchBridgeLink(
                    switchPos = switches.first(),
                    isActivated = false,
                    bridgePositions = bridges
                )
            )
        }

        return config
    }
}
