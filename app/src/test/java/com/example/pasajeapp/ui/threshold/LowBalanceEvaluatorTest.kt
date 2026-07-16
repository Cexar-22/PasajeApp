package com.example.pasajeapp.ui.threshold

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LowBalanceEvaluatorTest {
    @Test
    fun `returns true when remaining balance is below threshold`() {
        assertTrue(isLowBalance(remainingBalance = 3_500L, configuredThreshold = 5_000L))
    }

    @Test
    fun `returns false when remaining balance is above threshold`() {
        assertFalse(isLowBalance(remainingBalance = 17_000L, configuredThreshold = 5_000L))
    }

    @Test
    fun `returns false when remaining balance equals threshold`() {
        assertFalse(isLowBalance(remainingBalance = 5_000L, configuredThreshold = 5_000L))
    }

    @Test
    fun `does not show alert before a purchase result is registered`() {
        val uiState = ThresholdUiState(savedThreshold = 5_000L)

        assertFalse(uiState.shouldShowLowBalanceAlert)
    }

    @Test
    fun `shows alert after a purchase leaves balance below saved threshold`() {
        val uiState = ThresholdUiState(
            remainingBalance = 3_500L,
            savedThreshold = 5_000L
        )

        assertTrue(uiState.shouldShowLowBalanceAlert)
    }

    @Test
    fun `does not show alert after a purchase leaves balance above saved threshold`() {
        val uiState = ThresholdUiState(
            remainingBalance = 17_000L,
            savedThreshold = 5_000L
        )

        assertFalse(uiState.shouldShowLowBalanceAlert)
    }
}
