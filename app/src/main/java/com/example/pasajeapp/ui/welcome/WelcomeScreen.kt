package com.example.pasajeapp.ui.welcome

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.pasajeapp.R
import com.example.pasajeapp.ui.components.FinancialSectionCard
import com.example.pasajeapp.ui.components.PrimaryActionButton
import com.example.pasajeapp.ui.theme.AppDimensions
import com.example.pasajeapp.ui.theme.AppSpacing
import com.example.pasajeapp.ui.theme.FinancialCardContent
import com.example.pasajeapp.ui.theme.FinancialCardEnd
import com.example.pasajeapp.ui.theme.FinancialCardStart
import com.example.pasajeapp.ui.theme.PasajeAppTheme

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
    startEnabled: Boolean = true
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { contentPadding ->
        WelcomeContent(
            onStartClick = onStartClick,
            startEnabled = startEnabled,
            contentPadding = contentPadding
        )
    }
}

@Composable
private fun WelcomeContent(
    onStartClick: () -> Unit,
    startEnabled: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
            WelcomeBrand()

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                Text(
                    text = stringResource(R.string.welcome_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.welcome_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            WelcomeFinancialVisual()
            WelcomeBenefits()

            PrimaryActionButton(
                text = stringResource(R.string.welcome_start),
                onClick = onStartClick,
                enabled = startEnabled,
                icon = painterResource(R.drawable.ic_wallet_24)
            )

            Text(
                text = stringResource(R.string.welcome_secondary_text),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(AppSpacing.small))
        }
    }
}

@Composable
private fun WelcomeBrand() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Box(
                modifier = Modifier.size(AppDimensions.iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_wallet_24),
                    contentDescription = stringResource(R.string.welcome_logo_description),
                    modifier = Modifier.size(AppDimensions.smallIcon)
                )
            }
        }
        Column {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.welcome_brand_tagline),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun WelcomeFinancialVisual(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = AppDimensions.welcomeHeroMinHeight)
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
            verticalArrangement = Arrangement.spacedBy(AppSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.financial_product_name),
                    style = MaterialTheme.typography.titleMedium,
                    color = FinancialCardContent
                )
                Surface(
                    shape = CircleShape,
                    color = FinancialCardContent.copy(alpha = 0.14f),
                    contentColor = FinancialCardContent
                ) {
                    Text(
                        text = stringResource(R.string.welcome_visual_status),
                        modifier = Modifier.padding(
                            horizontal = AppSpacing.medium,
                            vertical = AppSpacing.small
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = FinancialCardContent.copy(alpha = 0.14f),
                contentColor = FinancialCardContent
            ) {
                Box(
                    modifier = Modifier.size(AppDimensions.welcomeHeroIconContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_wallet_24),
                        contentDescription = null,
                        modifier = Modifier.size(AppDimensions.iconContainer)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)
            ) {
                Text(
                    text = stringResource(R.string.welcome_visual_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = FinancialCardContent,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.welcome_visual_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = FinancialCardContent.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun WelcomeBenefits(
    modifier: Modifier = Modifier
) {
    FinancialSectionCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(AppSpacing.large),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.standard)
        ) {
            WelcomeBenefitRow(
                icon = painterResource(R.drawable.ic_wallet_24),
                text = stringResource(R.string.welcome_benefit_balance)
            )
            WelcomeBenefitRow(
                icon = painterResource(R.drawable.ic_shield_24),
                text = stringResource(R.string.welcome_benefit_threshold)
            )
            WelcomeBenefitRow(
                icon = painterResource(R.drawable.ic_warning_24),
                text = stringResource(R.string.welcome_benefit_warning)
            )
        }
    }
}

@Composable
private fun WelcomeBenefitRow(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Box(
                modifier = Modifier.size(AppDimensions.iconContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(AppDimensions.smallIcon)
                )
            }
        }
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(name = "Bienvenida", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun WelcomeScreenPreview() {
    PasajeAppTheme {
        WelcomeScreen(onStartClick = {})
    }
}

@Preview(name = "Bienvenida pequeña", showBackground = true, widthDp = 320, heightDp = 568)
@Composable
private fun WelcomeScreenSmallPreview() {
    PasajeAppTheme {
        WelcomeScreen(onStartClick = {})
    }
}

@Preview(name = "Bienvenida oscura", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun WelcomeScreenDarkPreview() {
    PasajeAppTheme(darkTheme = true) {
        WelcomeScreen(onStartClick = {})
    }
}
