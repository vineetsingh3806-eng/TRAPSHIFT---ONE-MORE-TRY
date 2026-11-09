package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_progress")
data class LevelProgressEntity(
    @PrimaryKey val levelId: Int,
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val stars: Int = 0,
    val bestMoves: Int = 0,
    val coinsEarned: Int = 0
)

@Entity(tableName = "daily_records")
data class DailyRecordEntity(
    @PrimaryKey val dateKey: String, // e.g. "2026-11-08"
    val isCompleted: Boolean = false,
    val moves: Int = 0,
    val timestamp: Long = 0L
)

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: Int = 1,
    val totalGamesPlayed: Int = 0,
    val totalWins: Int = 0,
    val totalDeaths: Int = 0,
    val totalMoves: Long = 0L,
    val totalCoins: Int = 0,
    val endlessHighScore: Int = 0,
    val rageLevelsCompleted: Int = 0
)

@Entity(tableName = "cosmetics")
data class CosmeticEntity(
    @PrimaryKey val skinId: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L
)
