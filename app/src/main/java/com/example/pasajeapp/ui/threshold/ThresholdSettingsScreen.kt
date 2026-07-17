package com.example.pasajeapp.ui.threshold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.pasajeapp.R
import com.example.pasajeapp.ui.components.FinancialSectionCard
import com.example.pasajeapp.ui.components.FinancialSectionHeader
import com.example.pasajeapp.ui.components.MoneyInputField
import com.example.pasajeapp.ui.components.PrimaryActionButton
import com.example.pasajeapp.ui.theme.AppDimensions
import com.example.pasajeapp.ui.theme.AppSpacing
import com.example.pasajeapp.ui.theme.FinancialCardContent
import com.example.pasajeapp.ui.theme.FinancialCardEnd
import com.example.pasajeapp.ui.theme.FinancialCardStart
import com.example.pasajeapp.ui.theme.PasajeAppTheme

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
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = {
            PasajeSnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        ThresholdSettingsContent(
            uiState = uiState,
            onAmountChanged = viewModel::onAmountChanged,
            onSaveClick = viewModel::saveThreshold,
            onAvailableBalanceChanged = viewModel::onAvailableBalanceChanged,
            onPurchaseAmountChanged = viewModel::onPurchaseAmountChanged,
            onRegisterPurchaseClick = viewModel::registerPurchase,
            contentPadding = innerPadding
        )
    }

    uiState.lowBalanceAlert?.let { alert ->
        LowBalanceAlertDialog(
            alert = alert,
            onDismiss = viewModel::dismissLowBalanceAlert
        )
    }
}

@Composable
private fun PasajeSnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState = hostState) { data ->
        Snackbar(
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield_24),
                    contentDescription = null,
                    modifier = Modifier.size(AppDimensions.smallIcon)
                )
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ThresholdSettingsContent(
    uiState: ThresholdUiState,
    onAmountChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    onAvailableBalanceChanged: (String) -> Unit,
    onPurchaseAmountChanged: (String) -> Unit,
    onRegisterPurchaseClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AppDimensions.contentMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(
                    horizontal = AppDimensions.screenHorizontalPadding,
                    vertical = AppSpacing.large
                ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.large)
        ) {
            AppHeader()
            FinancialSummaryCard(
                availableBalance = uiState.availableBalance,
                savedThreshold = uiState.savedThreshold,
                isThresholdLoaded = uiState.isThresholdLoaded
            )
            ThresholdSection(
                uiState = uiState,
                onAmountChanged = onAmountChanged,
                onSaveClick = onSaveClick
            )
            PurchaseSection(
                uiState = uiState,
                onAvailableBalanceChanged = onAvailableBalanceChanged,
                onPurchaseAmountChanged = onPurchaseAmountChanged,
                onRegisterPurchaseClick = onRegisterPurchaseClick
            )
            Spacer(modifier = Modifier.height(AppSpacing.small))
        }
    }
}

@Composable
private fun AppHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
        Text(
            text = stringResource(R.string.home_eyebrow),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = stringResource(R.string.home_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FinancialSummaryCard(
    availableBalance: Long?,
    savedThreshold: Long?,
    isThresholdLoaded: Boolean,
    modifier: Modifier = Modifier
) {
    val statusText = when {
        !isThresholdLoaded -> stringResource(R.string.threshold_loading)
        savedThreshold != null -> stringResource(R.string.threshold_active)
        else -> stringResource(R.string.threshold_inactive)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppDimensions.financialCardMinHeight)
            .clip(MaterialTheme.shapes.large)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(FinancialCardStart, FinancialCardEnd)
                )
            )
            .padding(AppSpacing.large)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.large)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wallet_24),
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.smallIcon),
                        tint = FinancialCardContent
                    )
                    Text(
                        text = stringResource(R.string.financial_product_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = FinancialCardContent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(AppSpacing.small))
                FinancialStatusPill(text = statusText)
            }

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)) {
                Text(
                    text = stringResource(R.string.available_balance),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinancialCardContent.copy(alpha = 0.78f)
                )
                Text(
                    text = availableBalance?.formatAsChileanPesos()
                        ?: stringResource(R.string.balance_not_entered),
                    style = MaterialTheme.typography.displaySmall,
                    color = FinancialCardContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield_24),
                    contentDescription = null,
                    modifier = Modifier.size(AppDimensions.smallIcon),
                    tint = FinancialCardContent.copy(alpha = 0.88f)
                )
                Text(
                    text = stringResource(
                        if (availableBalance == null) {
                            R.string.balance_card_hint
                        } else {
                            R.string.balance_card_ready
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinancialCardContent.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun FinancialStatusPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = FinancialCardContent.copy(alpha = 0.14f),
        contentColor = FinancialCardContent
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = AppSpacing.medium,
                vertical = AppSpacing.small
            ),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun ThresholdSection(
    uiState: ThresholdUiState,
    onAmountChanged: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FinancialSectionCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(AppSpacing.large),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.standard)
        ) {
            FinancialSectionHeader(
                title = stringResource(R.string.threshold_section_title),
                description = stringResource(R.string.threshold_section_description),
                icon = painterResource(R.drawable.ic_shield_24)
            )
            CurrentThresholdSummary(savedThreshold = uiState.savedThreshold)
            MoneyInputField(
                value = uiState.amountInput,
                onValueChange = onAmountChanged,
                label = stringResource(R.string.minimum_threshold),
                errorMessage = uiState.errorMessage
            )
            PrimaryActionButton(
                text = stringResource(R.string.save_threshold),
                onClick = onSaveClick,
                icon = painterResource(R.drawable.ic_shield_24)
            )
        }
    }
}

@Composable
private fun CurrentThresholdSummary(
    savedThreshold: Long?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.standard),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_shield_24),
                contentDescription = null,
                modifier = Modifier.size(AppDimensions.smallIcon)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.current_threshold),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = savedThreshold?.formatAsChileanPesos()
                        ?: stringResource(R.string.threshold_not_configured),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun PurchaseSection(
    uiState: ThresholdUiState,
    onAvailableBalanceChanged: (String) -> Unit,
    onPurchaseAmountChanged: (String) -> Unit,
    onRegisterPurchaseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FinancialSectionCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(AppSpacing.large),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.standard)
        ) {
            FinancialSectionHeader(
                title = stringResource(R.string.purchase_section_title),
                description = stringResource(R.string.purchase_section_description),
                icon = painterResource(R.drawable.ic_wallet_24)
            )
            AvailableBalanceSummary(availableBalance = uiState.availableBalance)
            MoneyInputField(
                value = uiState.availableBalanceInput,
                onValueChange = onAvailableBalanceChanged,
                label = stringResource(R.string.balance_before_purchase),
                errorMessage = uiState.balanceErrorMessage
            )
            MoneyInputField(
                value = uiState.purchaseAmountInput,
                onValueChange = onPurchaseAmountChanged,
                label = stringResource(R.string.purchase_amount),
                errorMessage = uiState.purchaseErrorMessage
            )
            PrimaryActionButton(
                text = stringResource(R.string.make_purchase),
                onClick = onRegisterPurchaseClick,
                enabled = uiState.isThresholdLoaded && uiState.isPurchaseSubmissionEnabled,
                icon = painterResource(R.drawable.ic_wallet_24)
            )
        }
    }
}

@Composable
private fun AvailableBalanceSummary(
    availableBalance: Long?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(AppSpacing.standard),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wallet_24),
                contentDescription = null,
                modifier = Modifier.size(AppDimensions.smallIcon)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.available_balance),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = availableBalance?.formatAsChileanPesos()
                        ?: stringResource(R.string.balance_not_entered),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun LowBalanceAlertDialog(
    alert: LowBalanceAlert,
    onDismiss: () -> Unit
) {
    val formattedBalance = alert.remainingBalance.formatAsChileanPesos()
    val formattedThreshold = alert.configuredThreshold.formatAsChileanPesos()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Box(
                    modifier = Modifier.size(AppDimensions.iconContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_warning_24),
                        contentDescription = stringResource(
                            R.string.warning_icon_description
                        ),
                        modifier = Modifier.size(AppDimensions.smallIcon)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.low_balance_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.standard)) {
                Text(
                    text = stringResource(
                        R.string.low_balance_message,
                        formattedBalance,
                        formattedThreshold
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                WarningAmountRow(
                    label = stringResource(R.string.remaining_balance_label),
                    amount = formattedBalance
                )
                WarningAmountRow(
                    label = stringResource(R.string.configured_threshold_label),
                    amount = formattedThreshold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.understood))
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun WarningAmountRow(
    label: String,
    amount: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(AppSpacing.standard))
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun Long.formatAsChileanPesos(): String {
    return "$" + toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
}

@Preview(name = "Pantalla principal", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ThresholdSettingsContentPreview() {
    PasajeAppTheme {
        ThresholdSettingsContent(
            uiState = ThresholdUiState(isThresholdLoaded = true),
            onAmountChanged = {},
            onSaveClick = {},
            onAvailableBalanceChanged = {},
            onPurchaseAmountChanged = {},
            onRegisterPurchaseClick = {},
            contentPadding = PaddingValues()
        )
    }
}

@Preview(name = "Tarjeta de saldo", showBackground = true)
@Composable
private fun FinancialSummaryCardPreview() {
    PasajeAppTheme {
        FinancialSummaryCard(
            availableBalance = null,
            savedThreshold = null,
            isThresholdLoaded = true,
            modifier = Modifier.padding(AppSpacing.standard)
        )
    }
}

@Preview(name = "Configuración de umbral", showBackground = true)
@Composable
private fun ThresholdSectionPreview() {
    PasajeAppTheme {
        ThresholdSection(
            uiState = ThresholdUiState(isThresholdLoaded = true),
            onAmountChanged = {},
            onSaveClick = {},
            modifier = Modifier.padding(AppSpacing.standard)
        )
    }
}

@Preview(name = "Formulario de compra", showBackground = true)
@Composable
private fun PurchaseSectionPreview() {
    PasajeAppTheme {
        PurchaseSection(
            uiState = ThresholdUiState(isThresholdLoaded = true),
            onAvailableBalanceChanged = {},
            onPurchaseAmountChanged = {},
            onRegisterPurchaseClick = {},
            modifier = Modifier.padding(AppSpacing.standard)
        )
    }
}

@Preview(name = "Alerta de saldo bajo", showBackground = true)
@Composable
private fun LowBalanceAlertDialogPreview() {
    PasajeAppTheme {
        LowBalanceAlertDialog(
            alert = LowBalanceAlert(
                remainingBalance = 3_500L,
                configuredThreshold = 5_000L
            ),
            onDismiss = {}
        )
    }
}
