package com.example.pasajeapp.ui.threshold

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchaseProcessorTest {
    @Test
    fun `purchase subtracts amount from available balance`() {
        val result = processPurchase(
            availableBalanceInput = "20000",
            purchaseAmountInput = "3000",
            configuredThreshold = 5_000L
        )

        assertTrue(result is PurchaseProcessingResult.Success)
        assertEquals(17_000L, (result as PurchaseProcessingResult.Success).remainingBalance)
    }

    @Test
    fun `purchase emits alert data when remaining balance is below saved threshold`() {
        val result = processPurchase(
            availableBalanceInput = "6500",
            purchaseAmountInput = "3000",
            configuredThreshold = 5_000L
        ) as PurchaseProcessingResult.Success

        assertEquals(
            LowBalanceAlert(
                remainingBalance = 3_500L,
                configuredThreshold = 5_000L
            ),
            result.lowBalanceAlert
        )
    }

    @Test
    fun `purchase does not emit alert when remaining balance is above threshold`() {
        val result = processPurchase(
            availableBalanceInput = "20000",
            purchaseAmountInput = "3000",
            configuredThreshold = 5_000L
        ) as PurchaseProcessingResult.Success

        assertNull(result.lowBalanceAlert)
    }

    @Test
    fun `purchase does not emit alert when remaining balance equals threshold`() {
        val result = processPurchase(
            availableBalanceInput = "8000",
            purchaseAmountInput = "3000",
            configuredThreshold = 5_000L
        ) as PurchaseProcessingResult.Success

        assertNull(result.lowBalanceAlert)
    }

    @Test
    fun `purchase succeeds without alert when threshold is not configured`() {
        val result = processPurchase(
            availableBalanceInput = "6500",
            purchaseAmountInput = "3000",
            configuredThreshold = null
        ) as PurchaseProcessingResult.Success

        assertEquals(3_500L, result.remainingBalance)
        assertNull(result.lowBalanceAlert)
    }

    @Test
    fun `purchase succeeds without alert when stored threshold is invalid`() {
        val result = processPurchase(
            availableBalanceInput = "6500",
            purchaseAmountInput = "3000",
            configuredThreshold = 0L
        ) as PurchaseProcessingResult.Success

        assertEquals(3_500L, result.remainingBalance)
        assertNull(result.lowBalanceAlert)
    }

    @Test
    fun `empty purchase amount is rejected`() {
        assertPurchaseError(
            purchaseAmountInput = "",
            expectedError = PurchaseValidationError.PURCHASE_AMOUNT_REQUIRED
        )
    }

    @Test
    fun `zero purchase amount is rejected`() {
        assertPurchaseError(
            purchaseAmountInput = "0",
            expectedError = PurchaseValidationError.NON_POSITIVE_PURCHASE_AMOUNT
        )
    }

    @Test
    fun `negative purchase amount is rejected`() {
        assertPurchaseError(
            purchaseAmountInput = "-1",
            expectedError = PurchaseValidationError.NON_POSITIVE_PURCHASE_AMOUNT
        )
    }

    @Test
    fun `purchase above available balance is rejected`() {
        assertPurchaseError(
            purchaseAmountInput = "20001",
            expectedError = PurchaseValidationError.INSUFFICIENT_BALANCE
        )
    }

    @Test
    fun `empty available balance is rejected`() {
        val result = processPurchase(
            availableBalanceInput = "",
            purchaseAmountInput = "3000",
            configuredThreshold = 5_000L
        )

        assertEquals(
            PurchaseProcessingResult.Error(
                PurchaseValidationError.AVAILABLE_BALANCE_REQUIRED
            ),
            result
        )
    }

    @Test
    fun `negative available balance is rejected`() {
        val result = processPurchase(
            availableBalanceInput = "-1",
            purchaseAmountInput = "1",
            configuredThreshold = 5_000L
        )

        assertEquals(
            PurchaseProcessingResult.Error(
                PurchaseValidationError.INVALID_AVAILABLE_BALANCE
            ),
            result
        )
    }

    @Test
    fun `values larger than Long are rejected safely`() {
        val result = processPurchase(
            availableBalanceInput = "9223372036854775808",
            purchaseAmountInput = "1",
            configuredThreshold = 5_000L
        )

        assertEquals(
            PurchaseProcessingResult.Error(
                PurchaseValidationError.INVALID_AVAILABLE_BALANCE
            ),
            result
        )
    }

    @Test
    fun `maximum Long balance is processed without overflow`() {
        val result = processPurchase(
            availableBalanceInput = Long.MAX_VALUE.toString(),
            purchaseAmountInput = "1",
            configuredThreshold = 5_000L
        ) as PurchaseProcessingResult.Success

        assertEquals(Long.MAX_VALUE - 1L, result.remainingBalance)
        assertNull(result.lowBalanceAlert)
    }

    private fun assertPurchaseError(
        purchaseAmountInput: String,
        expectedError: PurchaseValidationError
    ) {
        val result = processPurchase(
            availableBalanceInput = "20000",
            purchaseAmountInput = purchaseAmountInput,
            configuredThreshold = 5_000L
        )

        assertEquals(PurchaseProcessingResult.Error(expectedError), result)
    }
}
