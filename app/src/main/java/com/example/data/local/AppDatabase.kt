package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        LevelProgressEntity::class,
        DailyRecordEntity::class,
        UserStatsEntity::class,
        CosmeticEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun levelProgressDao(): LevelProgressDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun userStatsDao(): UserStatsDao
    abstract fun cosmeticDao(): CosmeticDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trapshift_database.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Initialize Level 1 as unlocked
                            CoroutineScope(Dispatchers.IO).launch {
                                val database = getDatabase(context)
                                database.levelProgressDao().insertOrUpdate(
                                    LevelProgressEntity(levelId = 1, isUnlocked = true)
                                )
                                database.userStatsDao().insertOrUpdate(
                                    UserStatsEntity(id = 1, totalCoins = 0)
                                )
                                database.cosmeticDao().insertOrUpdate(
                                    CosmeticEntity(skinId = "classic", isUnlocked = true)
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
