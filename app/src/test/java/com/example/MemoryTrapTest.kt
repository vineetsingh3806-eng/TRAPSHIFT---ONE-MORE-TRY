package com.example

import com.example.core.engine.GameEngine
import com.example.core.engine.GameEvent
import com.example.core.model.Direction
import com.example.core.model.GameStatus
import com.example.core.model.LevelDefinition
import com.example.core.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryTrapTest {

    private fun createMemoryTestLevel(): LevelDefinition {
        // Start at (1, 3). Memory sequence: (2, 3), (3, 3), (3, 2). Goal at (3, 1).
        return LevelDefinition(
            id = 200,
            chapter = 5,
            title = "Memory Test",
            subtitle = "Sequence verification",
            width = 5,
            height = 5,
            startPos = Position(1, 3),
            goalPos = Position(3, 1),
            rawGrid = listOf(
                "#####",
                "#..G#",
                "#..E#",
                "#.EE#",
                "#####"
            ),
            targetMoves3Star = 4,
            targetMoves2Star = 6,
            memorySequence = listOf(Position(2, 3), Position(3, 3), Position(3, 2))
        )
    }

    @Test
    fun testSteppingInCorrectSequenceAdvancesProgress() {
        val level = createMemoryTestLevel()
        val engine = GameEngine(level)

        assertEquals(0, engine.memoryProgress)
        assertFalse(engine.isMemoryCompleted)

        // Step 1: Right to (2, 3) (First memory tile)
        val res1 = engine.move(Direction.RIGHT)
        assertEquals(Position(2, 3), engine.ballPosition)
        assertEquals(GameEvent.MEMORY_STEP, res1.eventType)
        assertEquals(1, engine.memoryProgress)
        assertFalse(engine.isMemoryCompleted)

        // Step 2: Right to (3, 3) (Second memory tile)
        val res2 = engine.move(Direction.RIGHT)
        assertEquals(Position(3, 3), engine.ballPosition)
        assertEquals(GameEvent.MEMORY_STEP, res2.eventType)
        assertEquals(2, engine.memoryProgress)
        assertFalse(engine.isMemoryCompleted)

        // Step 3: Up to (3, 2) (Third memory tile)
        val res3 = engine.move(Direction.UP)
        assertEquals(Position(3, 2), engine.ballPosition)
        assertEquals(GameEvent.MEMORY_STEP, res3.eventType)
        assertEquals(3, engine.memoryProgress)
        assertTrue(engine.isMemoryCompleted)

        // Step 4: Up to (3, 1) [GOAL]
        val resGoal = engine.move(Direction.UP)
        assertEquals(GameStatus.WON, engine.gameStatus)
        assertEquals(GameEvent.VICTORY, resGoal.eventType)
    }

    @Test
    fun testSteppingOutOfSequenceResetsProgress() {
        // Level where player can skip step 1 and directly reach step 2
        val level = LevelDefinition(
            id = 201,
            chapter = 5,
            title = "Out of Order Test",
            subtitle = "Testing sequence reset",
            width = 5,
            height = 5,
            startPos = Position(1, 1),
            goalPos = Position(3, 3),
            rawGrid = listOf(
                "#####",
                "#...#",
                "#.E.#",
                "#.E.#",
                "#####"
            ),
            targetMoves3Star = 6,
            targetMoves2Star = 8,
            memorySequence = listOf(Position(2, 2), Position(2, 3))
        )
        val engine = GameEngine(level)

        // Move to (1, 3) then directly to E2 at (2, 3) (skipping E1 at 2, 2)
        engine.move(Direction.DOWN)
        engine.move(Direction.DOWN)
        val wrongStep = engine.move(Direction.RIGHT)

        assertEquals(Position(2, 3), engine.ballPosition)
        assertEquals(GameEvent.MEMORY_FAIL, wrongStep.eventType)
        assertEquals(0, engine.memoryProgress)
        assertFalse(engine.isMemoryCompleted)
    }

    @Test
    fun testResetRestoresMemoryState() {
        val level = createMemoryTestLevel()
        val engine = GameEngine(level)

        engine.move(Direction.RIGHT) // stepped on E1
        assertEquals(1, engine.memoryProgress)

        engine.reset()
        assertEquals(0, engine.memoryProgress)
        assertFalse(engine.isMemoryCompleted)
    }
}
