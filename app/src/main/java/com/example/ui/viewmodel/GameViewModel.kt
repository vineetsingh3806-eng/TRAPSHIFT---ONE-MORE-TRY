package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.HapticManager
import com.example.audio.SoundManager
import com.example.core.engine.GameEngine
import com.example.core.engine.GameEvent
import com.example.core.engine.MoveResult
import com.example.core.levels.LevelRepository
import com.example.core.model.BallSkin
import com.example.core.model.BoardTheme
import com.example.core.model.ControlScheme
import com.example.core.model.Direction
import com.example.core.model.GameMode
import com.example.core.model.GameStatus
import com.example.core.model.LevelDefinition
import com.example.core.validation.ReachabilitySolver
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.CosmeticEntity
import com.example.data.local.DailyRecordEntity
import com.example.data.local.LevelProgressEntity
import com.example.data.local.UserStatsEntity
import com.example.data.repository.GamePreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GameUiState(
    val currentMode: GameMode = GameMode.LEVELS,
    val currentLevel: LevelDefinition = LevelRepository.getLevel(1),
    val endlessRound: Int = 1,
    val endlessBest: Int = 0,
    val dailyDateString: String = "",
    val dailyCompletedToday: Boolean = false,
    val activeHintDirection: Direction? = null,
    val isPaused: Boolean = false,
    val selectedSkin: BallSkin = BallSkin.CLASSIC,
    val selectedTheme: BoardTheme = BoardTheme.MIDNIGHT,
    val controlScheme: ControlScheme = ControlScheme.BOTH,
    val soundEnabled: Boolean = true,
    val musicEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val totalCoins: Int = 0,
    val unlockedSkins: Set<String> = setOf("classic"),
    val lastEarnedCoins: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val prefs = GamePreferencesRepository(application)
    val soundManager = SoundManager(application)
    val hapticManager = HapticManager(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    var engine: GameEngine = GameEngine(LevelRepository.getLevel(1))
        private set

    val allProgress = db.levelProgressDao().getAllProgress().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val userStats = db.userStatsDao().getUserStatsFlow().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    init {
        // Collect preferences
        viewModelScope.launch {
            prefs.soundEnabled.collect { sound ->
                soundManager.soundEnabled = sound
                _uiState.update { it.copy(soundEnabled = sound) }
            }
        }
        viewModelScope.launch {
            prefs.musicEnabled.collect { music ->
                soundManager.musicEnabled = music
                _uiState.update { it.copy(musicEnabled = music) }
            }
        }
        viewModelScope.launch {
            prefs.vibrationEnabled.collect { vibration ->
                hapticManager.vibrationEnabled = vibration
                _uiState.update { it.copy(vibrationEnabled = vibration) }
            }
        }
        viewModelScope.launch {
            prefs.controlScheme.collect { controls ->
                _uiState.update { it.copy(controlScheme = controls) }
            }
        }
        viewModelScope.launch {
            prefs.selectedSkin.collect { skin ->
                _uiState.update { it.copy(selectedSkin = skin) }
            }
        }
        viewModelScope.launch {
            prefs.selectedTheme.collect { theme ->
                _uiState.update { it.copy(selectedTheme = theme) }
            }
        }

        // Collect stats & cosmetics
        viewModelScope.launch {
            db.userStatsDao().getUserStatsFlow().collect { stats ->
                _uiState.update {
                    it.copy(
                        totalCoins = stats?.totalCoins ?: 0,
                        endlessBest = stats?.endlessHighScore ?: 0
                    )
                }
            }
        }

        viewModelScope.launch {
            db.cosmeticDao().getAllCosmetics().collect { cosmetics ->
                val set = cosmetics.filter { it.isUnlocked }.map { it.skinId }.toSet() + "classic"
                _uiState.update { it.copy(unlockedSkins = set) }
            }
        }

        // Check today's daily
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        _uiState.update { it.copy(dailyDateString = todayKey) }
        viewModelScope.launch {
            val record = db.dailyRecordDao().getDailyRecord(todayKey)
            _uiState.update { it.copy(dailyCompletedToday = record?.isCompleted == true) }
        }

        soundManager.startAmbientMusic()
    }

    fun startLevel(levelId: Int) {
        val level = LevelRepository.getLevel(levelId)
        engine = GameEngine(level)
        _uiState.update {
            it.copy(
                currentMode = GameMode.LEVELS,
                currentLevel = level,
                activeHintDirection = null,
                isPaused = false,
                lastEarnedCoins = 0
            )
        }
    }

    fun startEndless() {
        val level = LevelRepository.generateEndlessLevel(1)
        engine = GameEngine(level)
        _uiState.update {
            it.copy(
                currentMode = GameMode.ENDLESS,
                currentLevel = level,
                endlessRound = 1,
                activeHintDirection = null,
                isPaused = false,
                lastEarnedCoins = 0
            )
        }
    }

    fun startDaily() {
        val level = LevelRepository.generateDailyChallenge()
        engine = GameEngine(level)
        _uiState.update {
            it.copy(
                currentMode = GameMode.DAILY,
                currentLevel = level,
                activeHintDirection = null,
                isPaused = false,
                lastEarnedCoins = 0
            )
        }
    }

    fun startRage(levelId: Int) {
        val level = LevelRepository.getRageLevels().find { it.id == levelId }
            ?: LevelRepository.getRageLevels().first()
        engine = GameEngine(level)
        _uiState.update {
            it.copy(
                currentMode = GameMode.RAGE,
                currentLevel = level,
                activeHintDirection = null,
                isPaused = false,
                lastEarnedCoins = 0
            )
        }
    }

    fun handleMove(direction: Direction) {
        if (_uiState.value.isPaused || engine.gameStatus != GameStatus.PLAYING) return

        val result = engine.move(direction)

        when (result.eventType) {
            GameEvent.MOVE -> {
                soundManager.playMove()
                hapticManager.tick()
            }
            GameEvent.WALL_BUMP -> {
                soundManager.playWallBump()
                hapticManager.bump()
            }
            GameEvent.SWITCH_TOGGLE -> {
                soundManager.playSwitch()
                hapticManager.tick()
            }
            GameEvent.PORTAL_WARP -> {
                soundManager.playPortal()
                hapticManager.tick()
            }
            GameEvent.FAKE_GOAL_BAMBOOZLE -> {
                soundManager.playTrapTrigger()
                hapticManager.bump()
            }
            GameEvent.REVERSE_CONTROLS_TRIGGER -> {
                soundManager.playTrapTrigger()
                hapticManager.bump()
            }
            GameEvent.MEMORY_STEP -> {
                soundManager.playMemoryStep()
                hapticManager.tick()
            }
            GameEvent.MEMORY_FAIL -> {
                soundManager.playTrapTrigger()
                hapticManager.bump()
            }
            GameEvent.DEATH -> {
                soundManager.playFailure()
                hapticManager.failure()
                recordDeath()
            }
            GameEvent.VICTORY -> {
                soundManager.playVictory()
                hapticManager.victory()
                recordVictory(result.movesCount, result.starsEarned)
            }
            else -> {}
        }

        // Clear active hint on move
        if (_uiState.value.activeHintDirection != null) {
            _uiState.update { it.copy(activeHintDirection = null) }
        }
    }

    fun restartCurrentLevel() {
        soundManager.playButtonClick()
        engine.reset()
        _uiState.update { it.copy(activeHintDirection = null, isPaused = false, lastEarnedCoins = 0) }
    }

    fun nextLevel() {
        soundManager.playButtonClick()
        val current = _uiState.value.currentLevel
        when (_uiState.value.currentMode) {
            GameMode.LEVELS -> {
                val nextId = (current.id + 1).coerceAtMost(50)
                startLevel(nextId)
            }
            GameMode.ENDLESS -> {
                val nextRound = _uiState.value.endlessRound + 1
                val newLevel = LevelRepository.generateEndlessLevel(nextRound)
                engine = GameEngine(newLevel)
                _uiState.update {
                    it.copy(
                        currentLevel = newLevel,
                        endlessRound = nextRound,
                        activeHintDirection = null,
                        isPaused = false
                    )
                }
            }
            GameMode.RAGE -> {
                val nextId = if (current.id < 510) current.id + 1 else 501
                startRage(nextId)
            }
            GameMode.DAILY -> {
                // Done for the day
            }
        }
    }

    fun requestHint() {
        val hint = ReachabilitySolver.findNextHint(engine.ballPosition, engine.level.goalPos, engine.level)
        if (hint != null) {
            soundManager.playSwitch()
            _uiState.update { it.copy(activeHintDirection = hint) }
        }
    }

    fun setPaused(paused: Boolean) {
        soundManager.playButtonClick()
        _uiState.update { it.copy(isPaused = paused) }
    }

    fun toggleSound() {
        viewModelScope.launch {
            prefs.setSoundEnabled(!_uiState.value.soundEnabled)
        }
    }

    fun toggleMusic() {
        viewModelScope.launch {
            val newVal = !_uiState.value.musicEnabled
            prefs.setMusicEnabled(newVal)
            if (newVal) soundManager.startAmbientMusic() else soundManager.stopAmbientMusic()
        }
    }

    fun toggleVibration() {
        viewModelScope.launch {
            prefs.setVibrationEnabled(!_uiState.value.vibrationEnabled)
        }
    }

    fun setControlScheme(scheme: ControlScheme) {
        viewModelScope.launch {
            prefs.setControlScheme(scheme)
        }
    }

    fun selectSkin(skin: BallSkin) {
        if (skin.id in _uiState.value.unlockedSkins) {
            viewModelScope.launch {
                prefs.setSelectedSkin(skin)
            }
        } else {
            // Purchase skin with coins
            if (_uiState.value.totalCoins >= skin.costCoins) {
                viewModelScope.launch {
                    val currentStats = db.userStatsDao().getUserStats() ?: UserStatsEntity()
                    db.userStatsDao().insertOrUpdate(
                        currentStats.copy(totalCoins = currentStats.totalCoins - skin.costCoins)
                    )
                    db.cosmeticDao().insertOrUpdate(
                        CosmeticEntity(skinId = skin.id, isUnlocked = true, unlockedAt = System.currentTimeMillis())
                    )
                    prefs.setSelectedSkin(skin)
                }
            }
        }
    }

    fun selectTheme(theme: BoardTheme) {
        viewModelScope.launch {
            prefs.setSelectedTheme(theme)
        }
    }

    private fun recordVictory(moves: Int, stars: Int) {
        val level = _uiState.value.currentLevel
        val mode = _uiState.value.currentMode

        viewModelScope.launch {
            db.withTransaction {
                var coinsDelta = 0
                val currentStats = db.userStatsDao().getUserStats() ?: UserStatsEntity()

                when (mode) {
                    GameMode.LEVELS -> {
                        val existing = db.levelProgressDao().getProgressForLevel(level.id)
                        val prevCoins = existing?.coinsEarned ?: 0
                        val maxCoinsForStars = (stars * 10).coerceAtMost(30)
                        coinsDelta = maxOf(0, maxCoinsForStars - prevCoins)

                        val newStars = maxOf(existing?.stars ?: 0, stars)
                        val newBest = if (existing == null || existing.bestMoves == 0) moves else minOf(existing.bestMoves, moves)

                        db.levelProgressDao().insertOrUpdate(
                            LevelProgressEntity(
                                levelId = level.id,
                                isUnlocked = true,
                                isCompleted = true,
                                stars = newStars,
                                bestMoves = newBest,
                                coinsEarned = prevCoins + coinsDelta
                            )
                        )

                        // Unlock next level
                        if (level.id < 50) {
                            val nextExisting = db.levelProgressDao().getProgressForLevel(level.id + 1)
                            if (nextExisting == null || !nextExisting.isUnlocked) {
                                db.levelProgressDao().insertOrUpdate(
                                    nextExisting?.copy(isUnlocked = true)
                                        ?: LevelProgressEntity(levelId = level.id + 1, isUnlocked = true)
                                )
                            }
                        }
                    }

                    GameMode.RAGE -> {
                        val existing = db.levelProgressDao().getProgressForLevel(level.id)
                        if (existing == null || !existing.isCompleted) {
                            coinsDelta = 30
                            db.levelProgressDao().insertOrUpdate(
                                LevelProgressEntity(
                                    levelId = level.id,
                                    isUnlocked = true,
                                    isCompleted = true,
                                    stars = 3,
                                    bestMoves = moves,
                                    coinsEarned = 30
                                )
                            )
                        } else {
                            coinsDelta = 0
                        }
                    }

                    GameMode.DAILY -> {
                        val dateKey = _uiState.value.dailyDateString
                        val existingDaily = db.dailyRecordDao().getDailyRecord(dateKey)
                        if (existingDaily == null || !existingDaily.isCompleted) {
                            coinsDelta = 25
                            db.dailyRecordDao().insertOrUpdate(
                                DailyRecordEntity(
                                    dateKey = dateKey,
                                    isCompleted = true,
                                    moves = moves,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        } else {
                            coinsDelta = 0
                        }
                        _uiState.update { it.copy(dailyCompletedToday = true) }
                    }

                    GameMode.ENDLESS -> {
                        // 5 coins milestone reward every 5 rounds
                        coinsDelta = if (_uiState.value.endlessRound % 5 == 0) 5 else 0
                    }
                }

                // Update aggregate user stats atomically
                val newStats = currentStats.copy(
                    totalGamesPlayed = currentStats.totalGamesPlayed + 1,
                    totalWins = currentStats.totalWins + 1,
                    totalMoves = currentStats.totalMoves + moves,
                    totalCoins = currentStats.totalCoins + coinsDelta,
                    endlessHighScore = if (mode == GameMode.ENDLESS) {
                        maxOf(currentStats.endlessHighScore, _uiState.value.endlessRound)
                    } else currentStats.endlessHighScore,
                    rageLevelsCompleted = if (mode == GameMode.RAGE && coinsDelta > 0) {
                        currentStats.rageLevelsCompleted + 1
                    } else currentStats.rageLevelsCompleted
                )
                db.userStatsDao().insertOrUpdate(newStats)

                _uiState.update { it.copy(lastEarnedCoins = coinsDelta) }
            }
        }
    }

    private fun recordDeath() {
        viewModelScope.launch {
            val currentStats = db.userStatsDao().getUserStats() ?: UserStatsEntity()
            db.userStatsDao().insertOrUpdate(
                currentStats.copy(
                    totalGamesPlayed = currentStats.totalGamesPlayed + 1,
                    totalDeaths = currentStats.totalDeaths + 1,
                    totalMoves = currentStats.totalMoves + engine.moveCount
                )
            )
        }
    }
}
