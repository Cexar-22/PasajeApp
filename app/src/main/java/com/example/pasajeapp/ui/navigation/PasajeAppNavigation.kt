package com.example.pasajeapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.pasajeapp.ui.threshold.ThresholdSettingsScreen
import com.example.pasajeapp.ui.threshold.ThresholdViewModel
import com.example.pasajeapp.ui.welcome.WelcomeScreen

private const val WELCOME_DESTINATION = "welcome"
private const val HOME_DESTINATION = "home"

@Composable
fun PasajeAppNavigation(
    thresholdViewModel: ThresholdViewModel
) {
    var currentDestination by rememberSaveable {
        mutableStateOf(WELCOME_DESTINATION)
    }
    var navigationStarted by rememberSaveable {
        mutableStateOf(false)
    }

    when (currentDestination) {
        WELCOME_DESTINATION -> WelcomeScreen(
            startEnabled = !navigationStarted,
            onStartClick = {
                if (!navigationStarted) {
                    navigationStarted = true
                    currentDestination = HOME_DESTINATION
                }
            }
        )

        else -> ThresholdSettingsScreen(viewModel = thresholdViewModel)
    }
}
