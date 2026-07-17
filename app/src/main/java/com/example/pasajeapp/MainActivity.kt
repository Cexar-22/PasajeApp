package com.example.pasajeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.pasajeapp.data.ThresholdPreferences
import com.example.pasajeapp.data.RemoteCardRepository
import com.example.pasajeapp.data.network.ApiProvider
import com.example.pasajeapp.ui.navigation.PasajeAppNavigation
import com.example.pasajeapp.ui.threshold.ThresholdViewModel
import com.example.pasajeapp.ui.theme.PasajeAppTheme

class MainActivity : ComponentActivity() {
    private val thresholdViewModel: ThresholdViewModel by viewModels {
        ThresholdViewModel.Factory(
            thresholdStore = ThresholdPreferences(applicationContext),
            cardRepository = RemoteCardRepository(
                api = ApiProvider.cardApi,
                cardId = BuildConfig.DEMO_CARD_ID
            )
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PasajeAppTheme {
                PasajeAppNavigation(thresholdViewModel = thresholdViewModel)
            }
        }
    }
}
