package com.example

import com.example.core.engine.GameEngine
import com.example.core.engine.GameEvent
import com.example.core.model.Direction
import com.example.core.model.GameStatus
import com.example.core.model.LevelDefinition
import com.example.core.model.Position
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private fun createTestLevel(): LevelDefinition {
        return LevelDefinition(
            id = 99,
            chapter = 1,
            title = "Test Level",
            subtitle = "Testing",
            width = 5,
            height = 5,
            startPos = Position(1, 3),
            goalPos = Position(3, 1),
            rawGrid = listOf(
                "#####",
                "#..G#",
                "#.X.#",
                "#...#",
                "#####"
            ),
            targetMoves3Star = 4,
            targetMoves2Star = 6
        )
    }

    @Test
    fun testInitialState() {
        val level = createTestLevel()
        val engine = GameEngine(level)
        assertEquals(Position(1, 3), engine.ballPosition)
        assertEquals(GameStatus.PLAYING, engine.gameStatus)
        assertEquals(0, engine.moveCount)
    }

    @Test
    fun testWallCollisionDoesNotAdvanceBall() {
        val level = createTestLevel()
        val engine = GameEngine(level)
        val result = engine.move(Direction.DOWN)
        assertFalse(result.ballMoved)
        assertEquals(Position(1, 3), engine.ballPosition)
        assertEquals(GameEvent.WALL_BUMP, result.eventType)
        assertEquals(0, engine.moveCount)
    }

    @Test
    fun testMoveToGoalTriggersVictory() {
        val level = createTestLevel()
        val engine = GameEngine(level)
        // From (1,3): Right to (2,3), Right to (3,3), Up to (3,2), Up to (3,1)[GOAL]
        engine.move(Direction.RIGHT)
        assertEquals(Position(2, 3), engine.ballPosition)
        engine.move(Direction.RIGHT)
        assertEquals(Position(3, 3), engine.ballPosition)
        engine.move(Direction.UP)
        assertEquals(Position(3, 2), engine.ballPosition)
        val victoryResult = engine.move(Direction.UP)

        assertEquals(Position(3, 1), engine.ballPosition)
        assertEquals(GameStatus.WON, engine.gameStatus)
        assertEquals(GameEvent.VICTORY, victoryResult.eventType)
        assertEquals(4, engine.moveCount)
        assertEquals(3, victoryResult.starsEarned)
    }

    @Test
    fun testHazardCollisionTriggersFailure() {
        val level = createTestLevel()
        val engine = GameEngine(level)
        // From (1,3): Right to (2,3), Up to (2,2)[HAZARD X]
        engine.move(Direction.RIGHT)
        val failResult = engine.move(Direction.UP)

        assertEquals(GameStatus.FAILED, engine.gameStatus)
        assertEquals(GameEvent.DEATH, failResult.eventType)
        assertNotNull(failResult.failureReason)
    }

    @Test
    fun testInstantResetRestoresState() {
        val level = createTestLevel()
        val engine = GameEngine(level)
        engine.move(Direction.RIGHT)
        engine.move(Direction.UP) // died
        assertEquals(GameStatus.FAILED, engine.gameStatus)

        engine.reset()
        assertEquals(Position(1, 3), engine.ballPosition)
        assertEquals(0, engine.moveCount)
        assertEquals(GameStatus.PLAYING, engine.gameStatus)
    }

    @Test
    fun testReverseControlsTileInvertsNextMoves() {
        val level = LevelDefinition(
            id = 98,
            chapter = 1,
            title = "Reverse Test",
            subtitle = "Testing",
            width = 5,
            height = 5,
            startPos = Position(1, 2),
            goalPos = Position(3, 1),
            rawGrid = listOf(
                "#####",
                "#..G#",
                "#.R.#",
                "#...#",
                "#####"
            ),
            targetMoves3Star = 5,
            targetMoves2Star = 7
        )
        val engine = GameEngine(level)
        // Step onto R at (2,2)
        val stepR = engine.move(Direction.RIGHT)
        assertEquals(Position(2, 2), engine.ballPosition)
        assertEquals(GameEvent.REVERSE_CONTROLS_TRIGGER, stepR.eventType)
        assertTrue(engine.confusionTurns > 0)

        // Player inputs UP -> should actually move DOWN to (2,3)
        engine.move(Direction.UP)
        assertEquals(Position(2, 3), engine.ballPosition)
    }
}
