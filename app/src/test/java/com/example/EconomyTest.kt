package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class EconomyTest {

    private fun calculateLevelCoinDelta(starsEarned: Int, previousCoinsEarned: Int): Int {
        val maxCoinsForStars = (starsEarned * 10).coerceAtMost(30)
        return maxOf(0, maxCoinsForStars - previousCoinsEarned)
    }

    private fun calculateRageCoinDelta(isAlreadyCompleted: Boolean): Int {
        return if (isAlreadyCompleted) 0 else 30
    }

    private fun calculateDailyCoinDelta(isAlreadyCompleted: Boolean): Int {
        return if (isAlreadyCompleted) 0 else 25
    }

    @Test
    fun testLevelFirstWinOneStarAwardsTenCoins() {
        val delta = calculateLevelCoinDelta(starsEarned = 1, previousCoinsEarned = 0)
        assertEquals(10, delta)
    }

    @Test
    fun testLevelReplayWithSameStarsAwardsZeroCoins() {
        val delta = calculateLevelCoinDelta(starsEarned = 1, previousCoinsEarned = 10)
        assertEquals(0, delta)
    }

    @Test
    fun testLevelUpgradeFromOneStarToTwoStarsAwardsDifference() {
        val delta = calculateLevelCoinDelta(starsEarned = 2, previousCoinsEarned = 10)
        assertEquals(10, delta)
    }

    @Test
    fun testLevelUpgradeFromOneStarToThreeStarsAwardsTwentyCoins() {
        val delta = calculateLevelCoinDelta(starsEarned = 3, previousCoinsEarned = 10)
        assertEquals(20, delta)
    }

    @Test
    fun testLevelUpgradeFromTwoStarsToThreeStarsAwardsTenCoins() {
        val delta = calculateLevelCoinDelta(starsEarned = 3, previousCoinsEarned = 20)
        assertEquals(10, delta)
    }

    @Test
    fun testLevelReplayWithThreeStarsAwardsZeroCoins() {
        val delta = calculateLevelCoinDelta(starsEarned = 3, previousCoinsEarned = 30)
        assertEquals(0, delta)
    }

    @Test
    fun testLevelReplayWithFewerStarsAwardsZeroCoins() {
        val delta = calculateLevelCoinDelta(starsEarned = 1, previousCoinsEarned = 30)
        assertEquals(0, delta)
    }

    @Test
    fun testLevelTotalCoinsNeverExceedsThirty() {
        var totalLevelCoins = 0

        // Win 1: 1 star
        val d1 = calculateLevelCoinDelta(starsEarned = 1, previousCoinsEarned = totalLevelCoins)
        totalLevelCoins += d1
        assertEquals(10, totalLevelCoins)

        // Replay: 1 star (no change)
        val d2 = calculateLevelCoinDelta(starsEarned = 1, previousCoinsEarned = totalLevelCoins)
        totalLevelCoins += d2
        assertEquals(10, totalLevelCoins)

        // Replay: 3 stars (upgrade to max)
        val d3 = calculateLevelCoinDelta(starsEarned = 3, previousCoinsEarned = totalLevelCoins)
        totalLevelCoins += d3
        assertEquals(30, totalLevelCoins)

        // Replay again: 3 stars (no change)
        val d4 = calculateLevelCoinDelta(starsEarned = 3, previousCoinsEarned = totalLevelCoins)
        totalLevelCoins += d4
        assertEquals(30, totalLevelCoins)
    }

    @Test
    fun testRageModeAwardsCoinsOnlyOnFirstCompletion() {
        val firstWinDelta = calculateRageCoinDelta(isAlreadyCompleted = false)
        assertEquals(30, firstWinDelta)

        val replayDelta = calculateRageCoinDelta(isAlreadyCompleted = true)
        assertEquals(0, replayDelta)
    }

    @Test
    fun testDailyChallengeAwardsCoinsOnlyOncePerDay() {
        val firstDailyWin = calculateDailyCoinDelta(isAlreadyCompleted = false)
        assertEquals(25, firstDailyWin)

        val replayDailyWin = calculateDailyCoinDelta(isAlreadyCompleted = true)
        assertEquals(0, replayDailyWin)
    }
}
