package com.example.pasajeapp.ui.threshold

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThresholdSettingsScreen(
    viewModel: ThresholdViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.confirmationMessage) {
        val message = uiState.confirmationMessage
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.onConfirmationShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Configuración de umbral")
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        ThresholdSettingsContent(
            uiState = uiState,
            onAmountChanged = viewModel::onAmountChanged,
            onSaveClick = viewModel::saveThreshold,
            onRemainingBalanceChanged = viewModel::onRemainingBalanceInputChanged,
            onRegisterPurchaseClick = viewModel::registerPurchaseResult,
            contentPadding = innerPadding
        )
    }
}

@Composable
private fun ThresholdSettingsContent(
    uiState: ThresholdUiState,
    onAmountChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onRemainingBalanceChanged: (String) -> Unit,
    onRegisterPurchaseClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Recibirás una alerta cuando tu saldo sea inferior al monto configurado"
        )
        Spacer(modifier = Modifier.height(24.dp))
        val remainingBalance = uiState.remainingBalance
        val savedThreshold = uiState.savedThreshold
        if (
            uiState.shouldShowLowBalanceAlert &&
            remainingBalance != null &&
            savedThreshold != null
        ) {
            LowBalanceAlertCard(
                remainingBalance = remainingBalance,
                configuredThreshold = savedThreshold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        SavedThresholdCard(
            savedThreshold = uiState.savedThreshold,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = uiState.amountInput,
            onValueChange = onAmountChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = "Umbral mínimo")
            },
            prefix = {
                Text(text = "$")
            },
            isError = uiState.errorMessage != null,
            supportingText = {
                val errorMessage = uiState.errorMessage
                if (errorMessage != null) {
                    Text(text = errorMessage)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onSaveClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Guardar umbral")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Resultado de la compra",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = uiState.remainingBalanceInput,
            onValueChange = onRemainingBalanceChanged,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = "Saldo restante después de la compra")
            },
            prefix = {
                Text(text = "$")
            },
            isError = uiState.purchaseErrorMessage != null,
            supportingText = {
                val errorMessage = uiState.purchaseErrorMessage
                if (errorMessage != null) {
                    Text(text = errorMessage)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRegisterPurchaseClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Registrar compra")
        }
    }
}

@Composable
private fun LowBalanceAlertCard(
    remainingBalance: Long,
    configuredThreshold: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Alerta de saldo bajo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Saldo restante: ${remainingBalance.formatAsChileanPesos()}")
            Text(text = "Umbral configurado: ${configuredThreshold.formatAsChileanPesos()}")
        }
    }
}

@Composable
private fun SavedThresholdCard(
    savedThreshold: Long?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Umbral actual guardado",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = savedThreshold?.formatAsChileanPesos() ?: "No configurado",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun Long.formatAsChileanPesos(): String {
    return "$" + toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
}
