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
    fun `returns true when remaining balance is one peso below threshold`() {
        assertTrue(isLowBalance(remainingBalance = 4_999L, configuredThreshold = 5_000L))
    }

    @Test
    fun `returns false when remaining balance is one peso above threshold`() {
        assertFalse(isLowBalance(remainingBalance = 5_001L, configuredThreshold = 5_000L))
    }
}
