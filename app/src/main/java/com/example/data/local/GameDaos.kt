package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress ORDER BY levelId ASC")
    fun getAllProgress(): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE levelId = :levelId LIMIT 1")
    suspend fun getProgressForLevel(levelId: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(list: List<LevelProgressEntity>)
}

@Dao
interface DailyRecordDao {
    @Query("SELECT * FROM daily_records ORDER BY dateKey DESC")
    fun getAllDailyRecords(): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_records WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyRecord(dateKey: String): DailyRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyRecordEntity)

    @Query("SELECT COUNT(*) FROM daily_records WHERE isCompleted = 1")
    suspend fun getCompletedDailyCount(): Int
}

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStatsEntity?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStats(): UserStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(stats: UserStatsEntity)
}

@Dao
interface CosmeticDao {
    @Query("SELECT * FROM cosmetics")
    fun getAllCosmetics(): Flow<List<CosmeticEntity>>

    @Query("SELECT * FROM cosmetics WHERE skinId = :skinId LIMIT 1")
    suspend fun getCosmetic(skinId: String): CosmeticEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cosmetic: CosmeticEntity)
}
