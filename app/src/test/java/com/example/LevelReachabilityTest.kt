package com.example

import com.example.core.levels.LevelRepository
import com.example.core.validation.ReachabilitySolver
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelReachabilityTest {

    @Test
    fun verifyAll50HandcraftedLevelsAreSolvable() {
        val allLevels = LevelRepository.getAllLevels()
        org.junit.Assert.assertEquals("Must have 50 handcrafted levels", 50, allLevels.size)

        for (level in allLevels) {
            val solution = ReachabilitySolver.solve(level)
            assertTrue(
                "Level ${level.id} (${level.title}) must be solvable, but ReachabilitySolver returned unsolvable!",
                solution.isSolvable
            )
            assertTrue(
                "Level ${level.id} optimal moves must be >= 1",
                solution.optimalMoves >= 1
            )
        }
    }

    @Test
    fun verifyAllRageLevelsAreSolvable() {
        val rageLevels = LevelRepository.getRageLevels()
        org.junit.Assert.assertEquals("Must have 10 rage levels", 10, rageLevels.size)

        for (level in rageLevels) {
            val solution = ReachabilitySolver.solve(level)
            assertTrue(
                "Rage Level ${level.id} (${level.title}) must be solvable!",
                solution.isSolvable
            )
            level.maxMovesAllowed?.let { maxMoves ->
                assertTrue(
                    "Rage Level ${level.id} optimal moves (${solution.optimalMoves}) must not exceed max allowed ($maxMoves)",
                    solution.optimalMoves <= maxMoves
                )
            }
        }
    }

    @Test
    fun verifyDailyChallengeIsDeterministic() {
        val daily1 = LevelRepository.generateDailyChallenge()
        val daily2 = LevelRepository.generateDailyChallenge()
        org.junit.Assert.assertEquals(daily1.rawGrid, daily2.rawGrid)
        org.junit.Assert.assertEquals(daily1.startPos, daily2.startPos)
        org.junit.Assert.assertEquals(daily1.goalPos, daily2.goalPos)
    }

    @Test
    fun verifyEndlessLevelsAreSolvable() {
        for (round in 1..15) {
            val endlessLevel = LevelRepository.generateEndlessLevel(round)
            val solution = ReachabilitySolver.solve(endlessLevel)
            assertTrue(
                "Endless Round $round must be solvable!",
                solution.isSolvable
            )
        }
    }
}
