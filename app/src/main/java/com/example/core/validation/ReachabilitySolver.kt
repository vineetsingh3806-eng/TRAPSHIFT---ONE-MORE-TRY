package com.example.core.validation

import com.example.core.model.Direction
import com.example.core.model.LevelDefinition
import com.example.core.model.Position
import java.util.ArrayDeque

data class SolutionPath(
    val isSolvable: Boolean,
    val optimalMoves: Int,
    val path: List<Position>,
    val nextDirection: Direction?
)

private data class SolverState(
    val pos: Position,
    val switchActive: Boolean,
    val memoryStep: Int
)

object ReachabilitySolver {

    fun solve(level: LevelDefinition): SolutionPath {
        val width = level.width
        val height = level.height
        val start = level.startPos
        val goal = level.goalPos

        var portal1: Position? = null
        var portal2: Position? = null
        val switches = mutableSetOf<Position>()
        val closedBridges = mutableSetOf<Position>()
        val openBridges = mutableSetOf<Position>()

        for (y in 0 until height) {
            val row = level.rawGrid.getOrNull(y) ?: ""
            for (x in 0 until width) {
                val c = row.getOrNull(x) ?: '.'
                val p = Position(x, y)
                when (c) {
                    '1' -> portal1 = p
                    '2' -> portal2 = p
                    'S' -> switches.add(p)
                    'B' -> closedBridges.add(p)
                    'O' -> openBridges.add(p)
                }
            }
        }

        fun isTilePassable(x: Int, y: Int, switchActive: Boolean): Boolean {
            if (x !in 0 until width || y !in 0 until height) return false
            val c = level.rawGrid.getOrNull(y)?.getOrNull(x) ?: '.'
            val p = Position(x, y)

            if (p in closedBridges) {
                return switchActive
            }
            if (p in openBridges) {
                return !switchActive
            }

            return when (c) {
                '#', 'X', 'K' -> false // Wall, spike hazard, or fake floor
                else -> true
            }
        }

        val queue = ArrayDeque<Pair<SolverState, List<Position>>>()
        val visited = HashSet<SolverState>()

        val initialState = SolverState(pos = start, switchActive = false, memoryStep = 0)
        queue.add(Pair(initialState, listOf(start)))
        visited.add(initialState)

        var statesExplored = 0
        val maxStates = 15000

        while (queue.isNotEmpty() && statesExplored < maxStates) {
            statesExplored++
            val item = queue.poll() ?: continue
            val (state, currentPath) = item

            if (state.pos == goal) {
                val memoryRequired = level.memorySequence.isNotEmpty()
                val memoryDone = state.memoryStep >= level.memorySequence.size
                if (!memoryRequired || memoryDone) {
                    val nextDir = if (currentPath.size > 1) {
                        val nextStep = currentPath[1]
                        when {
                            nextStep.x > start.x -> Direction.RIGHT
                            nextStep.x < start.x -> Direction.LEFT
                            nextStep.y > start.y -> Direction.DOWN
                            else -> Direction.UP
                        }
                    } else null

                    return SolutionPath(
                        isSolvable = true,
                        optimalMoves = currentPath.size - 1,
                        path = currentPath,
                        nextDirection = nextDir
                    )
                }
            }

            for (dir in Direction.entries) {
                val nextPos = state.pos.step(dir)
                if (isTilePassable(nextPos.x, nextPos.y, state.switchActive)) {
                    val actualNextPos = when {
                        nextPos == portal1 && portal2 != null -> portal2
                        nextPos == portal2 && portal1 != null -> portal1
                        else -> nextPos
                    }

                    val nextSwitchActive = if (actualNextPos in switches) !state.switchActive else state.switchActive

                    val expectedMemPos = level.memorySequence.getOrNull(state.memoryStep)
                    val nextMemStep = if (actualNextPos == expectedMemPos) {
                        state.memoryStep + 1
                    } else if (level.memorySequence.contains(actualNextPos) && actualNextPos != expectedMemPos) {
                        0
                    } else {
                        state.memoryStep
                    }

                    val nextState = SolverState(
                        pos = actualNextPos,
                        switchActive = nextSwitchActive,
                        memoryStep = nextMemStep
                    )

                    if (visited.add(nextState)) {
                        val updatedPath = if (actualNextPos != nextPos) {
                            currentPath + nextPos + actualNextPos
                        } else {
                            currentPath + nextPos
                        }
                        queue.add(Pair(nextState, updatedPath))
                    }
                }
            }
        }

        return SolutionPath(
            isSolvable = false,
            optimalMoves = -1,
            path = emptyList(),
            nextDirection = null
        )
    }

    fun findNextHint(currentPos: Position, goalPos: Position, level: LevelDefinition): Direction? {
        val dynamicLevel = level.copy(startPos = currentPos, goalPos = goalPos)
        val solution = solve(dynamicLevel)
        return solution.nextDirection
    }
}
