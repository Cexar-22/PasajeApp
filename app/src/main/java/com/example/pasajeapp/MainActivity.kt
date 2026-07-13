package com.example.pasajeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.pasajeapp.data.ThresholdPreferences
import com.example.pasajeapp.ui.threshold.ThresholdSettingsScreen
import com.example.pasajeapp.ui.threshold.ThresholdViewModel
import com.example.pasajeapp.ui.theme.PasajeAppTheme

class MainActivity : ComponentActivity() {
    private val thresholdViewModel: ThresholdViewModel by viewModels {
        ThresholdViewModel.Factory(ThresholdPreferences(applicationContext))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PasajeAppTheme {
                ThresholdSettingsScreen(viewModel = thresholdViewModel)
            }
        }
    }
}
