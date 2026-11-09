package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelSelectScreen
import com.example.ui.screens.RageSelectScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.BgDarkNavy
import com.example.ui.theme.TrapshiftTheme
import com.example.ui.viewmodel.GameViewModel

enum class Screen {
    HOME,
    LEVEL_SELECT,
    GAMEPLAY,
    STATS,
    SETTINGS,
    RAGE_SELECT
}

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TrapshiftTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BgDarkNavy
                ) {
                    TrapshiftApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.soundManager.stopAmbientMusic()
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.uiState.value.musicEnabled) {
            viewModel.soundManager.startAmbientMusic()
        }
    }
}

@Composable
fun TrapshiftApp(viewModel: GameViewModel) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    when (currentScreen) {
        Screen.HOME -> {
            HomeScreen(
                viewModel = viewModel,
                onPlayLevels = { currentScreen = Screen.LEVEL_SELECT },
                onPlayEndless = {
                    viewModel.startEndless()
                    currentScreen = Screen.GAMEPLAY
                },
                onPlayDaily = {
                    viewModel.startDaily()
                    currentScreen = Screen.GAMEPLAY
                },
                onPlayRage = { currentScreen = Screen.RAGE_SELECT },
                onOpenStats = { currentScreen = Screen.STATS },
                onOpenSettings = { currentScreen = Screen.SETTINGS }
            )
        }

        Screen.LEVEL_SELECT -> {
            LevelSelectScreen(
                viewModel = viewModel,
                onSelectLevel = { levelId ->
                    viewModel.startLevel(levelId)
                    currentScreen = Screen.GAMEPLAY
                },
                onBack = { currentScreen = Screen.HOME }
            )
        }

        Screen.RAGE_SELECT -> {
            RageSelectScreen(
                onSelectRageLevel = { levelId ->
                    viewModel.startRage(levelId)
                    currentScreen = Screen.GAMEPLAY
                },
                onBack = { currentScreen = Screen.HOME }
            )
        }

        Screen.GAMEPLAY -> {
            GameplayScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    currentScreen = when (viewModel.uiState.value.currentMode) {
                        com.example.core.model.GameMode.LEVELS -> Screen.LEVEL_SELECT
                        com.example.core.model.GameMode.RAGE -> Screen.RAGE_SELECT
                        else -> Screen.HOME
                    }
                }
            )
        }

        Screen.STATS -> {
            StatsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = Screen.HOME }
            )
        }

        Screen.SETTINGS -> {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { currentScreen = Screen.HOME }
            )
        }
    }
}
