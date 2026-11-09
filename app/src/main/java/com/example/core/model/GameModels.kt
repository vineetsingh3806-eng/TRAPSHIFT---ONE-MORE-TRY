package com.example.core.model

data class Position(val x: Int, val y: Int) {
    fun step(direction: Direction): Position = when (direction) {
        Direction.UP -> Position(x, y - 1)
        Direction.DOWN -> Position(x, y + 1)
        Direction.LEFT -> Position(x - 1, y)
        Direction.RIGHT -> Position(x + 1, y)
    }

    fun manhattanDistanceTo(other: Position): Int =
        kotlin.math.abs(x - other.x) + kotlin.math.abs(y - other.y)
}

enum class Direction {
    UP, DOWN, LEFT, RIGHT;

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

enum class TileType {
    FLOOR,             // Standard safe walkable tile
    WALL,              // Solid blocking wall
    GOAL,              // True exit portal (triggers victory)
    FAKE_GOAL,         // Trap exit: looks like goal, laughs/teleports back
    HAZARD,            // Static spikes / laser (triggers instant failure)
    FAKE_SAFE,         // Looks safe, but collapses into void on step
    COUNTDOWN,         // Ticks down on each player turn; turns into hazard at 0
    DISAPPEARING,      // Walkable once, turns unstable and crumbles after exit
    SWITCH,            // Step to toggle linked bridges / gates
    BRIDGE_OPEN,       // Passable bridge
    BRIDGE_CLOSED,     // Impassable void/gap until switch pressed
    PORTAL_A,          // Enters Portal A -> Exits Portal B
    PORTAL_B,          // Enters Portal B -> Exits Portal A
    REVERSE_CONTROLS,  // Disorients controls (Inverts directional input) for 3 turns
    INVISIBLE_PATH,    // Appears as void, but is secretly safe (reveals on touch/clue)
    MEMORY_TILE,       // Sequence puzzle tile (lights up when stepped)
    MOVING_HAZARD,     // Patrols back and forth on each player turn
    SHIFTING_WALL_OPEN,// Wall currently passable
    SHIFTING_WALL_SHUT // Wall currently impassable
}

enum class GameStatus {
    IDLE,
    PLAYING,
    WON,
    FAILED
}

enum class GameMode {
    LEVELS,
    ENDLESS,
    DAILY,
    RAGE
}

enum class ControlScheme {
    BOTH,
    SWIPE_ONLY,
    DPAD_ONLY
}

enum class BallSkin(val id: String, val displayName: String, val costCoins: Int) {
    CLASSIC("classic", "Neon Orb", 0),
    CYBER_PULSE("cyber_pulse", "Cyber Pulse", 50),
    RETRO_PIXEL("retro_pixel", "8-Bit Hero", 100),
    INFERNO("inferno", "Inferno Core", 150),
    COSMIC_VOID("cosmic_void", "Cosmic Void", 250);

    companion object {
        fun fromId(id: String): BallSkin = entries.find { it.id == id } ?: CLASSIC
    }
}

enum class BoardTheme(val id: String, val displayName: String) {
    MIDNIGHT("midnight", "Midnight Cyber"),
    NEON_ARCADE("neon_arcade", "Neon Arcade"),
    DEEP_DUNGEON("deep_dungeon", "Deep Dungeon"),
    VOLCANIC_RAGE("volcanic_rage", "Volcanic Rage");

    companion object {
        fun fromId(id: String): BoardTheme = entries.find { it.id == id } ?: MIDNIGHT
    }
}

data class LevelDefinition(
    val id: Int,
    val chapter: Int,
    val title: String,
    val subtitle: String,
    val width: Int,
    val height: Int,
    val startPos: Position,
    val goalPos: Position,
    val rawGrid: List<String>,
    val targetMoves3Star: Int,
    val targetMoves2Star: Int,
    val maxMovesAllowed: Int? = null,
    val mechanicsHint: String? = null,
    val memorySequence: List<Position> = emptyList()
)
