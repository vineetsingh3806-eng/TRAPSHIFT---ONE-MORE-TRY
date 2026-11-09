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

class InvisiblePathTest {

    private fun createInvisiblePathLevel(): LevelDefinition {
        return LevelDefinition(
            id = 202,
            chapter = 5,
            title = "Phantom Path Test",
            subtitle = "Testing invisible tiles",
            width = 5,
            height = 5,
            startPos = Position(1, 3),
            goalPos = Position(3, 1),
            rawGrid = listOf(
                "#####",
                "#..G#",
                "##I##",
                "#...#",
                "#####"
            ),
            targetMoves3Star = 4,
            targetMoves2Star = 6
        )
    }

    @Test
    fun testInvisiblePathIsWalkableAndRevealsOnStep() {
        val level = createInvisiblePathLevel()
        val engine = GameEngine(level)

        val phantomPos = Position(2, 2)
        assertFalse(engine.revealedInvisiblePaths.contains(phantomPos))

        // Step right to (2, 3)
        engine.move(Direction.RIGHT)
        assertEquals(Position(2, 3), engine.ballPosition)

        // Step up onto 'I' at (2, 2)
        val stepResult = engine.move(Direction.UP)
        assertTrue("Ball should be able to step onto invisible path", stepResult.ballMoved)
        assertEquals(Position(2, 2), engine.ballPosition)
        assertEquals(GameEvent.MOVE, stepResult.eventType)
        assertEquals(GameStatus.PLAYING, engine.gameStatus)
        assertTrue(
            "Invisible path at (2,2) should be recorded in revealedInvisiblePaths",
            engine.revealedInvisiblePaths.contains(phantomPos)
        )
    }

    @Test
    fun testResetClearsRevealedInvisiblePaths() {
        val level = createInvisiblePathLevel()
        val engine = GameEngine(level)

        engine.move(Direction.RIGHT)
        engine.move(Direction.UP) // stepped on I
        assertTrue(engine.revealedInvisiblePaths.isNotEmpty())

        engine.reset()
        assertTrue("Reset should clear revealed invisible paths", engine.revealedInvisiblePaths.isEmpty())
    }
}
