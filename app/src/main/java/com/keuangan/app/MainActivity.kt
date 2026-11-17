package com.keuangan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.keuangan.app.ui.screens.HomeScreen
import com.keuangan.app.ui.screens.ProfileSelectionScreen
import com.keuangan.app.ui.screens.SplashScreen
import com.keuangan.app.ui.theme.KeuanganKeluargaTheme
import com.keuangan.app.ui.viewmodel.FinanceViewModel

private enum class AppScreen {
    SPLASH,
    PROFILE_SELECTION,
    HOME
}

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeuanganKeluargaTheme {
                var showSplash by remember { mutableStateOf(true) }
                val uiState by viewModel.uiState.collectAsState()

                val currentScreen = when {
                    showSplash || !uiState.isProfileLoaded -> AppScreen.SPLASH
                    !uiState.isProfileSelected -> AppScreen.PROFILE_SELECTION
                    else -> AppScreen.HOME
                }

                Crossfade(
                    targetState = currentScreen,
                    animationSpec = tween(400),
                    label = "appScreenTransition"
                ) { screen ->
                    when (screen) {
                        AppScreen.SPLASH -> {
                            SplashScreen(
                                onSplashFinished = { showSplash = false }
                            )
                        }
                        AppScreen.PROFILE_SELECTION -> {
                            ProfileSelectionScreen(
                                onProfileSelected = { profile ->
                                    viewModel.setActiveProfile(profile)
                                }
                            )
                        }
                        AppScreen.HOME -> {
                            HomeScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
