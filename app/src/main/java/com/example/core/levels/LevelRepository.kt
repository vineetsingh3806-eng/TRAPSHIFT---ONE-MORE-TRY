package com.example.core.levels

import com.example.core.model.LevelDefinition
import com.example.core.model.Position
import com.example.core.validation.ReachabilitySolver
import java.util.Calendar
import java.util.Random

object LevelRepository {

    fun getLevel(id: Int): LevelDefinition {
        return allHandcraftedLevels.find { it.id == id } ?: allHandcraftedLevels.first()
    }

    fun getAllLevels(): List<LevelDefinition> = allHandcraftedLevels

    fun getRageLevels(): List<LevelDefinition> = rageLevelList

    fun generateEndlessLevel(roundNumber: Int): LevelDefinition {
        val width = when {
            roundNumber < 5 -> 6
            roundNumber < 15 -> 7
            roundNumber < 25 -> 8
            else -> 9
        }
        val height = width
        val random = Random(System.currentTimeMillis() + roundNumber * 7919L)

        // Try candidate generations until a verified solvable one is produced
        for (attempt in 0..100) {
            val candidateGrid = Array(height) { CharArray(width) { '.' } }
            // Border walls
            for (x in 0 until width) {
                candidateGrid[0][x] = '#'
                candidateGrid[height - 1][x] = '#'
            }
            for (y in 0 until height) {
                candidateGrid[y][0] = '#'
                candidateGrid[y][width - 1] = '#'
            }

            val start = Position(1, 1)
            val goal = Position(width - 2, height - 2)
            candidateGrid[start.y][start.x] = '.'
            candidateGrid[goal.y][goal.x] = 'G'

            // Add random walls and hazards based on round
            val wallCount = width + (roundNumber / 3).coerceAtMost(width * 2)
            for (w in 0 until wallCount) {
                val rx = 1 + random.nextInt(width - 2)
                val ry = 1 + random.nextInt(height - 2)
                if (Position(rx, ry) != start && Position(rx, ry) != goal) {
                    candidateGrid[ry][rx] = '#'
                }
            }

            // Add traps
            if (roundNumber >= 3) {
                val spikeCount = (roundNumber / 4).coerceIn(1, 4)
                for (s in 0 until spikeCount) {
                    val rx = 1 + random.nextInt(width - 2)
                    val ry = 1 + random.nextInt(height - 2)
                    if (candidateGrid[ry][rx] == '.') {
                        candidateGrid[ry][rx] = if (roundNumber >= 10 && random.nextBoolean()) 'C' else 'X'
                    }
                }
            }

            if (roundNumber >= 8) {
                val rx = 1 + random.nextInt(width - 2)
                val ry = 1 + random.nextInt(height - 2)
                if (candidateGrid[ry][rx] == '.') {
                    candidateGrid[ry][rx] = 'D'
                }
            }

            val rawGrid = candidateGrid.map { String(it) }
            val candidateLevel = LevelDefinition(
                id = 1000 + roundNumber,
                chapter = 99,
                title = "Endless Floor $roundNumber",
                subtitle = "Survive the shifting gauntlet",
                width = width,
                height = height,
                startPos = start,
                goalPos = goal,
                rawGrid = rawGrid,
                targetMoves3Star = width + height,
                targetMoves2Star = width + height + 4
            )

            val solution = ReachabilitySolver.solve(candidateLevel)
            if (solution.isSolvable && solution.optimalMoves >= (width + height - 4)) {
                return candidateLevel.copy(
                    targetMoves3Star = solution.optimalMoves,
                    targetMoves2Star = solution.optimalMoves + 3
                )
            }
        }

        // Fallback guaranteed solvable layout
        return createFallbackSolvableLevel(roundNumber, width, height)
    }

    fun generateDailyChallenge(calendar: Calendar = Calendar.getInstance()): LevelDefinition {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dateSeed = (year * 10000L) + (month * 100L) + day + 424242L

        val random = Random(dateSeed)
        val width = 7
        val height = 7

        for (attempt in 0..100) {
            val candidateGrid = Array(height) { CharArray(width) { '.' } }
            for (x in 0 until width) {
                candidateGrid[0][x] = '#'
                candidateGrid[height - 1][x] = '#'
            }
            for (y in 0 until height) {
                candidateGrid[y][0] = '#'
                candidateGrid[y][width - 1] = '#'
            }

            val start = Position(1, 1)
            val goal = Position(width - 2, height - 2)
            candidateGrid[goal.y][goal.x] = 'G'

            // Place obstacles deterministically
            val wallCount = 6
            for (i in 0 until wallCount) {
                val rx = 1 + random.nextInt(width - 2)
                val ry = 1 + random.nextInt(height - 2)
                if (Position(rx, ry) != start && Position(rx, ry) != goal) {
                    candidateGrid[ry][rx] = '#'
                }
            }

            // Add countdown and disappearing tiles
            val rx1 = 1 + random.nextInt(width - 2)
            val ry1 = 1 + random.nextInt(height - 2)
            if (candidateGrid[ry1][rx1] == '.') candidateGrid[ry1][rx1] = 'C'

            val rx2 = 1 + random.nextInt(width - 2)
            val ry2 = 1 + random.nextInt(height - 2)
            if (candidateGrid[ry2][rx2] == '.') candidateGrid[ry2][rx2] = 'D'

            val raw = candidateGrid.map { String(it) }
            val dailyDef = LevelDefinition(
                id = 9000,
                chapter = 0,
                title = "Daily Challenge",
                subtitle = String.format("%04d-%02d-%02d", year, month, day),
                width = width,
                height = height,
                startPos = start,
                goalPos = goal,
                rawGrid = raw,
                targetMoves3Star = 12,
                targetMoves2Star = 16
            )

            val sol = ReachabilitySolver.solve(dailyDef)
            if (sol.isSolvable && sol.optimalMoves >= 8) {
                return dailyDef.copy(
                    targetMoves3Star = sol.optimalMoves,
                    targetMoves2Star = sol.optimalMoves + 3
                )
            }
        }

        return createFallbackSolvableLevel(999, 7, 7).copy(
            title = "Daily Challenge",
            subtitle = String.format("%04d-%02d-%02d", year, month, day)
        )
    }

    private fun createFallbackSolvableLevel(id: Int, w: Int, h: Int): LevelDefinition {
        val grid = listOf(
            "#######",
            "#...#G#",
            "#.###.#",
            "#.....#",
            "###.###",
            "#.....#",
            "#######"
        )
        return LevelDefinition(
            id = id,
            chapter = 1,
            title = "Trial Run",
            subtitle = "Pave your way",
            width = 7,
            height = 7,
            startPos = Position(1, 5),
            goalPos = Position(5, 1),
            rawGrid = grid,
            targetMoves3Star = 10,
            targetMoves2Star = 14
        )
    }

    // Handcrafted 50 Levels
    private val allHandcraftedLevels: List<LevelDefinition> by lazy {
        generate50HandcraftedLevels()
    }

    // 10 Rage Mode Levels with strict move caps
    private val rageLevelList: List<LevelDefinition> by lazy {
        listOf(
            LevelDefinition(
                id = 501, chapter = 7, title = "Rage 1: Tightrope", subtitle = "Move limit: 9 moves",
                width = 6, height = 6, startPos = Position(1, 4), goalPos = Position(4, 1),
                rawGrid = listOf(
                    "######",
                    "#.F.G#",
                    "#.#.##",
                    "#...X#",
                    "#...##",
                    "######"
                ),
                targetMoves3Star = 6, targetMoves2Star = 8, maxMovesAllowed = 9,
                mechanicsHint = "Watch out for the fake exit!"
            ),
            LevelDefinition(
                id = 502, chapter = 7, title = "Rage 2: The Squeeze", subtitle = "Move limit: 12 moves",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#.X.XG#",
                    "#.###.#",
                    "#..C..#",
                    "#.###.#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 10, targetMoves2Star = 11, maxMovesAllowed = 12,
                mechanicsHint = "Countdown ticks down fast!"
            ),
            LevelDefinition(
                id = 503, chapter = 7, title = "Rage 3: Inverted Logic", subtitle = "Move limit: 14 moves",
                width = 7, height = 7, startPos = Position(1, 1), goalPos = Position(5, 5),
                rawGrid = listOf(
                    "#######",
                    "#..R..#",
                    "#.###.#",
                    "#.#X#.#",
                    "#.###.#",
                    "#....G#",
                    "#######"
                ),
                targetMoves3Star = 10, targetMoves2Star = 12, maxMovesAllowed = 14,
                mechanicsHint = "Purple tile reverses controls for 3 turns."
            ),
            LevelDefinition(
                id = 504, chapter = 7, title = "Rage 4: Crumbling Edge", subtitle = "Move limit: 12 moves",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...XG#",
                    "#.DDD.#",
                    "#.....#",
                    "#.DDD.#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 10, maxMovesAllowed = 12,
                mechanicsHint = "Disappearing floor never comes back."
            ),
            LevelDefinition(
                id = 505, chapter = 7, title = "Rage 5: Portal Trap", subtitle = "Move limit: 11 moves",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 3),
                rawGrid = listOf(
                    "#######",
                    "#2..F.#",
                    "###.###",
                    "#....G#",
                    "###1###",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 10, maxMovesAllowed = 11,
                mechanicsHint = "Portals teleport you across gaps."
            ),
            LevelDefinition(
                id = 506, chapter = 7, title = "Rage 6: Dual Deception", subtitle = "Move limit: 16 moves",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#...#F.#",
                    "#.#.##.#",
                    "#.C..#.#",
                    "#.#.##.#",
                    "#R...#G#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 12, targetMoves2Star = 14, maxMovesAllowed = 16
            ),
            LevelDefinition(
                id = 507, chapter = 7, title = "Rage 7: Sentry Sprint", subtitle = "Move limit: 15 moves",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...XG#",
                    "#.M.#.#",
                    "#.....#",
                    "#.M.#.#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 10, targetMoves2Star = 12, maxMovesAllowed = 15
            ),
            LevelDefinition(
                id = 508, chapter = 7, title = "Rage 8: Shifting Doom", subtitle = "Move limit: 18 moves",
                width = 8, height = 8, startPos = Position(1, 1), goalPos = Position(6, 6),
                rawGrid = listOf(
                    "########",
                    "#..W..G#",
                    "#.####.#",
                    "#..W...#",
                    "#.####.#",
                    "#..W...#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 14, targetMoves2Star = 16, maxMovesAllowed = 18
            ),
            LevelDefinition(
                id = 509, chapter = 7, title = "Rage 9: Switch Gambit", subtitle = "Move limit: 20 moves",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#S...BG#",
                    "###.####",
                    "#......#",
                    "####.###",
                    "#......#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 15, targetMoves2Star = 17, maxMovesAllowed = 20
            ),
            LevelDefinition(
                id = 510, chapter = 7, title = "Rage 10: Grand Cataclysm", subtitle = "Move limit: 24 moves",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#S..#1.G#",
                    "###.#####",
                    "#..C..2.#",
                    "#.#####.#",
                    "#..R..M.#",
                    "#.#####.#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 18, targetMoves2Star = 21, maxMovesAllowed = 24
            )
        )
    }

    private fun generate50HandcraftedLevels(): List<LevelDefinition> {
        val list = mutableListOf<LevelDefinition>()

        // ==========================================
        // CHAPTER 1: FIRST STEPS (Levels 1–5)
        // Focus: Movement, Boundaries, Intuition
        // ==========================================
        list.add(
            LevelDefinition(
                id = 1, chapter = 1, title = "The Awakening", subtitle = "Swipe or tap D-pad to move",
                width = 5, height = 5, startPos = Position(1, 3), goalPos = Position(3, 1),
                rawGrid = listOf(
                    "#####",
                    "#..G#",
                    "#...#",
                    "#...#",
                    "#####"
                ),
                targetMoves3Star = 4, targetMoves2Star = 6,
                mechanicsHint = "Guide the ball directly to the glowing portal!"
            )
        )

        list.add(
            LevelDefinition(
                id = 2, chapter = 1, title = "Around the Block", subtitle = "Solid walls block your path",
                width = 6, height = 5, startPos = Position(1, 3), goalPos = Position(4, 1),
                rawGrid = listOf(
                    "######",
                    "#...G#",
                    "#.##.#",
                    "#....#",
                    "######"
                ),
                targetMoves3Star = 5, targetMoves2Star = 7,
                mechanicsHint = "Navigate around the center walls."
            )
        )

        list.add(
            LevelDefinition(
                id = 3, chapter = 1, title = "Zig-Zag Corridor", subtitle = "Corner navigation",
                width = 6, height = 6, startPos = Position(1, 4), goalPos = Position(4, 1),
                rawGrid = listOf(
                    "######",
                    "#...G#",
                    "#.#.##",
                    "#...##",
                    "#...##",
                    "######"
                ),
                targetMoves3Star = 6, targetMoves2Star = 8,
                mechanicsHint = "Take corners smoothly."
            )
        )

        list.add(
            LevelDefinition(
                id = 4, chapter = 1, title = "The Winding Way", subtitle = "Plan your steps carefully",
                width = 6, height = 6, startPos = Position(1, 1), goalPos = Position(1, 4),
                rawGrid = listOf(
                    "######",
                    "#....#",
                    "####.#",
                    "#....#",
                    "#G####",
                    "######"
                ),
                targetMoves3Star = 9, targetMoves2Star = 12,
                mechanicsHint = "Follow the winding path."
            )
        )

        list.add(
            LevelDefinition(
                id = 5, chapter = 1, title = "Chapter 1 Finale", subtitle = "Putting it all together",
                width = 6, height = 6, startPos = Position(1, 4), goalPos = Position(4, 1),
                rawGrid = listOf(
                    "######",
                    "#...G#",
                    "#.##.#",
                    "#..#.#",
                    "#....#",
                    "######"
                ),
                targetMoves3Star = 6, targetMoves2Star = 9,
                mechanicsHint = "Ready for the real traps?"
            )
        )

        // ==========================================
        // CHAPTER 2: DANGER AHEAD (Levels 6–10)
        // Focus: Spikes & Fake Safe Tiles
        // ==========================================
        list.add(
            LevelDefinition(
                id = 6, chapter = 2, title = "Spike Alley", subtitle = "Red spikes are lethal",
                width = 6, height = 6, startPos = Position(1, 4), goalPos = Position(4, 1),
                rawGrid = listOf(
                    "######",
                    "#...G#",
                    "#.X..#",
                    "#..X.#",
                    "#....#",
                    "######"
                ),
                targetMoves3Star = 6, targetMoves2Star = 8,
                mechanicsHint = "Avoid the red spikes at all costs!"
            )
        )

        list.add(
            LevelDefinition(
                id = 7, chapter = 2, title = "Watch Your Step", subtitle = "Some tiles are not what they seem",
                width = 6, height = 6, startPos = Position(1, 4), goalPos = Position(4, 1),
                rawGrid = listOf(
                    "######",
                    "#...G#",
                    "#....#",
                    "#.K..#",
                    "#....#",
                    "######"
                ),
                targetMoves3Star = 6, targetMoves2Star = 8,
                mechanicsHint = "Notice the subtle cracks on fake safe tiles."
            )
        )

        list.add(
            LevelDefinition(
                id = 8, chapter = 2, title = "Minefield Maze", subtitle = "Choose the safe corridor",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...#G#",
                    "#.X.#.#",
                    "#.....#",
                    "#.X#X.#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 11,
                mechanicsHint = "Look for the open side corridor."
            )
        )

        list.add(
            LevelDefinition(
                id = 9, chapter = 2, title = "The Deceiver", subtitle = "Don't rush forward",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...XG#",
                    "#K..#.#",
                    "#.....#",
                    "#...K.#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 11,
                mechanicsHint = "Don't step onto suspicious discolored tiles."
            )
        )

        list.add(
            LevelDefinition(
                id = 10, chapter = 2, title = "Crossroads of Peril", subtitle = "Chapter 2 Mastery",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#..#XG#",
                    "#.##..#",
                    "#..X..#",
                    "#.##..#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 10, targetMoves2Star = 13,
                mechanicsHint = "Plan your route before your first move."
            )
        )

        // ==========================================
        // CHAPTER 3: TICK TOCK (Levels 11–20)
        // Focus: Countdown tiles, Moving Hazards, Disappearing Floors
        // ==========================================
        list.add(
            LevelDefinition(
                id = 11, chapter = 3, title = "Countdown Gauntlet", subtitle = "Tiles tick down on every turn",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...#G#",
                    "#.C.#.#",
                    "#.....#",
                    "#.C.#.#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 11,
                mechanicsHint = "Pass over countdown tiles before they hit 0!"
            )
        )

        list.add(
            LevelDefinition(
                id = 12, chapter = 3, title = "The Sentry Patrol", subtitle = "Moving hazard patrols back and forth",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...#G#",
                    "#.M...#",
                    "#.....#",
                    "#.###.#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 11,
                mechanicsHint = "Time your move past the patrolling sentry."
            )
        )

        list.add(
            LevelDefinition(
                id = 13, chapter = 3, title = "Crumbling Floor", subtitle = "Floors collapse behind you",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...#G#",
                    "#.D.#.#",
                    "#...#.#",
                    "#.D.#.#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 11,
                mechanicsHint = "Disappearing tiles crumble once you step off."
            )
        )

        list.add(
            LevelDefinition(
                id = 14, chapter = 3, title = "Dual Sentries", subtitle = "Synchronized obstacles",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...XG#",
                    "#.M...#",
                    "#.....#",
                    "#.M...#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 11
            )
        )

        list.add(
            LevelDefinition(
                id = 15, chapter = 3, title = "Chasm Bridge", subtitle = "No turning back",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...#G#",
                    "#...#.#",
                    "#.DDD.#",
                    "#...#.#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 11
            )
        )

        // Levels 16 to 20
        list.add(
            LevelDefinition(
                id = 16, chapter = 3, title = "Rapid Transit", subtitle = "Don't hesitate",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...#G#",
                    "#C.C#.#",
                    "#.....#",
                    "###.###",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 10, targetMoves2Star = 13
            )
        )

        list.add(
            LevelDefinition(
                id = 17, chapter = 3, title = "Sentry Dance", subtitle = "Read the patrol arrows",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#..#XG#",
                    "#.M.#.#",
                    "#...#.#",
                    "#.M.#.#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 10, targetMoves2Star = 13
            )
        )

        list.add(
            LevelDefinition(
                id = 18, chapter = 3, title = "Thin Ice", subtitle = "Plan every step",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#....#G#",
                    "#.DD.#.#",
                    "#......#",
                    "#.DD.#.#",
                    "#......#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 11, targetMoves2Star = 14
            )
        )

        list.add(
            LevelDefinition(
                id = 19, chapter = 3, title = "Pressure Gauge", subtitle = "Countdowns and sentries",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#...##G#",
                    "#.M..#.#",
                    "#.C....#",
                    "#.M..#.#",
                    "#......#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 12, targetMoves2Star = 15
            )
        )

        list.add(
            LevelDefinition(
                id = 20, chapter = 3, title = "Chapter 3 Climax", subtitle = "Master of timing",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#...X.G#",
                    "#.DDD#.#",
                    "#.M....#",
                    "#.DDD#.#",
                    "#..C...#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 13, targetMoves2Star = 17
            )
        )

        // ==========================================
        // CHAPTER 4: MIND BENDERS (Levels 21–30)
        // Focus: Fake Exits, Portals, Switches, Bridges, Shifting Walls
        // ==========================================
        list.add(
            LevelDefinition(
                id = 21, chapter = 4, title = "The Bamboozle", subtitle = "Not all exits are real",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 5),
                rawGrid = listOf(
                    "#######",
                    "#..F..#",
                    "#.###.#",
                    "#.....#",
                    "#.###.#",
                    "#....G#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 10,
                mechanicsHint = "True goal has a rotating golden vortex."
            )
        )

        list.add(
            LevelDefinition(
                id = 22, chapter = 4, title = "Warp Gate", subtitle = "Teleport across space",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#2...G#",
                    "#######",
                    "#.....#",
                    "#1#####",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 6, targetMoves2Star = 9,
                mechanicsHint = "Step into Portal 1 to emerge at Portal 2."
            )
        )

        list.add(
            LevelDefinition(
                id = 23, chapter = 4, title = "The Bridge Switch", subtitle = "Toggle pathways",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...#G#",
                    "#####B#",
                    "#S....#",
                    "###.###",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 10, targetMoves2Star = 13,
                mechanicsHint = "Step on the switch 'S' to extend bridge 'B'."
            )
        )

        list.add(
            LevelDefinition(
                id = 24, chapter = 4, title = "Shifting Labyrinth", subtitle = "Walls move on cadence",
                width = 7, height = 7, startPos = Position(1, 5), goalPos = Position(5, 1),
                rawGrid = listOf(
                    "#######",
                    "#...#G#",
                    "#.W...#",
                    "#.....#",
                    "#.W...#",
                    "#.....#",
                    "#######"
                ),
                targetMoves3Star = 8, targetMoves2Star = 11,
                mechanicsHint = "Shifting walls toggle open/closed every 2 turns."
            )
        )

        list.add(
            LevelDefinition(
                id = 25, chapter = 4, title = "Mirror Warp", subtitle = "Two portals, one goal",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#2....G#",
                    "########",
                    "#..#...#",
                    "#..#.1.#",
                    "#####.##",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 10, targetMoves2Star = 14
            )
        )

        // Levels 26 to 30
        list.add(
            LevelDefinition(
                id = 26, chapter = 4, title = "Switch & Deceive", subtitle = "Think before you leap",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#...F.G#",
                    "#####B##",
                    "#S.....#",
                    "####.###",
                    "#......#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 12, targetMoves2Star = 16
            )
        )

        list.add(
            LevelDefinition(
                id = 27, chapter = 4, title = "Cadence Corridor", subtitle = "Time your passage",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#...##G#",
                    "#.W....#",
                    "#.#....#",
                    "#.W....#",
                    "#.#....#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 12, targetMoves2Star = 15
            )
        )

        list.add(
            LevelDefinition(
                id = 28, chapter = 4, title = "The Grand Portal", subtitle = "Cross-room teleportation",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#...#2G#",
                    "#.1.####",
                    "#......#",
                    "####.###",
                    "#......#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 10, targetMoves2Star = 13
            )
        )

        list.add(
            LevelDefinition(
                id = 29, chapter = 4, title = "Switch Roulette", subtitle = "Multiple gates",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#S...BG#",
                    "###.####",
                    "#..C...#",
                    "####.###",
                    "#......#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 14, targetMoves2Star = 18
            )
        )

        list.add(
            LevelDefinition(
                id = 30, chapter = 4, title = "Chapter 4 Climax", subtitle = "Master of mind tricks",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#2...FG#",
                    "###.####",
                    "#S..1..#",
                    "####.###",
                    "#......#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 14, targetMoves2Star = 18
            )
        )

        // ==========================================
        // CHAPTER 5: CHAOS SHIFT (Levels 31–40)
        // Focus: Reverse Controls, Invisible Paths, Memory Traps
        // ==========================================
        list.add(
            LevelDefinition(
                id = 31, chapter = 5, title = "Inversion Alley", subtitle = "Controls reverse on purple tiles",
                width = 8, height = 7, startPos = Position(1, 5), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#....#G#",
                    "#.##.#.#",
                    "#..R...#",
                    "#.####.#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 9, targetMoves2Star = 13,
                mechanicsHint = "Control reverses when you step on the purple icon!"
            )
        )

        list.add(
            LevelDefinition(
                id = 32, chapter = 5, title = "The Phantom Crossing", subtitle = "Trust the invisible path",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#....#G#",
                    "#..X.#.#",
                    "###II..#",
                    "#..#...#",
                    "#..X...#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 10, targetMoves2Star = 14,
                mechanicsHint = "Invisible tiles bridge the chasm. Look for faint stardust!"
            )
        )

        list.add(
            LevelDefinition(
                id = 33, chapter = 5, title = "Sequence of Truth", subtitle = "Step in precise order",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#....#G#",
                    "#...#E##",
                    "#...##.#",
                    "#.E..E.#",
                    "#...#..#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 12, targetMoves2Star = 16,
                mechanicsHint = "Step on the memory pads in sequence.",
                memorySequence = listOf(Position(2, 4), Position(5, 4), Position(5, 2))
            )
        )

        list.add(
            LevelDefinition(
                id = 34, chapter = 5, title = "Mirrored Danger", subtitle = "Spikes and inverted thought",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#....XG#",
                    "#.####.#",
                    "#..R...#",
                    "#.####.#",
                    "#..X...#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 11, targetMoves2Star = 15,
                mechanicsHint = "Steer inverted controls around sharp spikes."
            )
        )

        list.add(
            LevelDefinition(
                id = 35, chapter = 5, title = "Ghost Bridge Sentry", subtitle = "Moving patrol on phantom ground",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#....#G#",
                    "#.M....#",
                    "###II###",
                    "#......#",
                    "#.M....#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 11, targetMoves2Star = 15,
                mechanicsHint = "Wait for sentries to clear before stepping on invisible paths."
            )
        )

        list.add(
            LevelDefinition(
                id = 36, chapter = 5, title = "Echo Chambers", subtitle = "Cadence walls and memory echoes",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#.E..#G#",
                    "#.##.#.#",
                    "#..W...#",
                    "#.####.#",
                    "#.E..E.#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 14, targetMoves2Star = 18,
                mechanicsHint = "Activate all sequence pads while dodging shifting walls.",
                memorySequence = listOf(Position(2, 5), Position(5, 5), Position(2, 1))
            )
        )

        list.add(
            LevelDefinition(
                id = 37, chapter = 5, title = "Confusion Nexus", subtitle = "Portals into reverse controls",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#2...#G#",
                    "#####.##",
                    "#..R...#",
                    "#.####.#",
                    "#...1..#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 9, targetMoves2Star = 13,
                mechanicsHint = "Portals keep momentum even when controls flip!"
            )
        )

        list.add(
            LevelDefinition(
                id = 38, chapter = 5, title = "Disappearing Mirage", subtitle = "Ticking floors over the void",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#....#G#",
                    "#.DD...#",
                    "###II###",
                    "#.DD...#",
                    "#..C...#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 11, targetMoves2Star = 15,
                mechanicsHint = "Never hesitate on crumbling tiles."
            )
        )

        list.add(
            LevelDefinition(
                id = 39, chapter = 5, title = "The Triad Vault", subtitle = "Memory, switches, and bridge",
                width = 8, height = 8, startPos = Position(1, 6), goalPos = Position(6, 1),
                rawGrid = listOf(
                    "########",
                    "#S...BG#",
                    "####.###",
                    "#......#",
                    "#.E..E.#",
                    "#..##..#",
                    "#......#",
                    "########"
                ),
                targetMoves3Star = 14, targetMoves2Star = 18,
                mechanicsHint = "Complete the sequence pads, toggle the switch, cross the bridge.",
                memorySequence = listOf(Position(2, 4), Position(5, 4))
            )
        )

        list.add(
            LevelDefinition(
                id = 40, chapter = 5, title = "Chaos Convergence", subtitle = "Chapter 5 Grand Climax",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#S..#2.G#",
                    "###.###B#",
                    "#..R...##",
                    "#.#####.#",
                    "#..1.W..#",
                    "#.#####.#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 16, targetMoves2Star = 21,
                mechanicsHint = "Combine portals, switch bridges, and inverted steering."
            )
        )

        // ==========================================
        // CHAPTER 6: THE CRUCIBLE (Levels 41–50)
        // Focus: Master multi-room puzzles & gauntlets
        // ==========================================
        list.add(
            LevelDefinition(
                id = 41, chapter = 6, title = "Twin Gate Complex", subtitle = "Dual switch coordination",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#S...B.G#",
                    "###.#####",
                    "#.......#",
                    "#.#####.#",
                    "#...S.B.#",
                    "#.#####.#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 16, targetMoves2Star = 20,
                mechanicsHint = "Unlock sequential gates across the central divide."
            )
        )

        list.add(
            LevelDefinition(
                id = 42, chapter = 6, title = "Hall of Illusions", subtitle = "Spot the authentic vortex",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#2..#F.G#",
                    "###.#####",
                    "#..F....#",
                    "#.#####.#",
                    "#...1..F#",
                    "#.#####.#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 12, targetMoves2Star = 16,
                mechanicsHint = "True goal has golden spin rings. Fake exits send you back."
            )
        )

        list.add(
            LevelDefinition(
                id = 43, chapter = 6, title = "Crossfire Confusion", subtitle = "Patrols and inverted mind",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#....#XG#",
                    "#.M.....#",
                    "#..R....#",
                    "#.#####.#",
                    "#.M..C..#",
                    "#.#####.#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 15, targetMoves2Star = 19,
                mechanicsHint = "Time moves around moving hazards while managing inverted controls."
            )
        )

        list.add(
            LevelDefinition(
                id = 44, chapter = 6, title = "The Obsidian Archive", subtitle = "Four-step sequence mastery",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#.....#G#",
                    "#.E.#.E.#",
                    "#...#...#",
                    "#.#####.#",
                    "#.......#",
                    "#.E...E.#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 20, targetMoves2Star = 25,
                mechanicsHint = "Four memory pads in clock order to unlock the archive exit.",
                memorySequence = listOf(Position(2, 6), Position(6, 6), Position(6, 2), Position(2, 2))
            )
        )

        list.add(
            LevelDefinition(
                id = 45, chapter = 6, title = "The Void Walk", subtitle = "Multi-tier invisible bridges",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#.....#G#",
                    "#..X..#.#",
                    "###IIII##",
                    "#.......#",
                    "###IIII##",
                    "#..X....#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 14, targetMoves2Star = 18,
                mechanicsHint = "Trace the invisible paths connecting upper and lower tiers."
            )
        )

        list.add(
            LevelDefinition(
                id = 46, chapter = 6, title = "Ticking Labyrinth", subtitle = "Countdown against cadence walls",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#....#XG#",
                    "#.W.##..#",
                    "#...#C..#",
                    "#.####..#",
                    "#.W..C..#",
                    "#.####..#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 16, targetMoves2Star = 20,
                mechanicsHint = "Cadence walls cycle every 2 turns. Don't let countdowns trap you."
            )
        )

        list.add(
            LevelDefinition(
                id = 47, chapter = 6, title = "Quantum Cascade", subtitle = "Disappearing floor portal bridge",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#2..#..G#",
                    "###.###.#",
                    "#.DD.DD.#",
                    "#.#####.#",
                    "#.1.....#",
                    "#.#####.#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 15, targetMoves2Star = 19,
                mechanicsHint = "One step wrong on disappearing bridges drops you into the abyss."
            )
        )

        list.add(
            LevelDefinition(
                id = 48, chapter = 6, title = "The Iron Pendulum", subtitle = "Switch, bridge, and reverse sentry",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#S...B.G#",
                    "###.#####",
                    "#..M....#",
                    "#.#####.#",
                    "#..R..M.#",
                    "#.#####.#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 18, targetMoves2Star = 23,
                mechanicsHint = "Toggle the bridge switch while dodging the dual sentry sweeps."
            )
        )

        list.add(
            LevelDefinition(
                id = 49, chapter = 6, title = "The Nonagon of Peril", subtitle = "Nine chambers of traps",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#S.#2#.G#",
                    "###.###B#",
                    "#C.#..#C#",
                    "###.###.#",
                    "#R.#1#.M#",
                    "###.###.#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 20, targetMoves2Star = 26,
                mechanicsHint = "Navigate between 9 distinct chambers using warp portals and switches."
            )
        )

        list.add(
            LevelDefinition(
                id = 50, chapter = 6, title = "TRAPSHIFT: The Final Test", subtitle = "Mastery of all 12 mechanics",
                width = 9, height = 9, startPos = Position(1, 7), goalPos = Position(7, 1),
                rawGrid = listOf(
                    "#########",
                    "#S.1#2.G#",
                    "###.###B#",
                    "#.E..E..#",
                    "#.#####.#",
                    "#.W..M..#",
                    "#.#####.#",
                    "#.......#",
                    "#########"
                ),
                targetMoves3Star = 22, targetMoves2Star = 28,
                mechanicsHint = "Every trap mechanic combined. One more try!",
                memorySequence = listOf(Position(2, 3), Position(5, 3))
            )
        )

        return list
    }
}
