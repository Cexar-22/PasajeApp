package com.example.pasajeapp.ui.threshold

data class LowBalanceAlert(
    val remainingBalance: Long,
    val configuredThreshold: Long
)

sealed interface PurchaseProcessingResult {
    data class Success(
        val remainingBalance: Long,
        val lowBalanceAlert: LowBalanceAlert?
    ) : PurchaseProcessingResult

    data class Error(
        val reason: PurchaseValidationError
    ) : PurchaseProcessingResult
}

enum class PurchaseValidationError {
    AVAILABLE_BALANCE_REQUIRED,
    INVALID_AVAILABLE_BALANCE,
    PURCHASE_AMOUNT_REQUIRED,
    INVALID_PURCHASE_AMOUNT,
    NON_POSITIVE_PURCHASE_AMOUNT,
    INSUFFICIENT_BALANCE
}

fun processPurchase(
    availableBalanceInput: String,
    purchaseAmountInput: String,
    configuredThreshold: Long?
): PurchaseProcessingResult {
    val normalizedBalance = availableBalanceInput.trim()
    if (normalizedBalance.isEmpty()) {
        return PurchaseProcessingResult.Error(
            PurchaseValidationError.AVAILABLE_BALANCE_REQUIRED
        )
    }

    val availableBalance = normalizedBalance.toLongOrNull()
    if (availableBalance == null || availableBalance < 0L) {
        return PurchaseProcessingResult.Error(
            PurchaseValidationError.INVALID_AVAILABLE_BALANCE
        )
    }

    val normalizedPurchaseAmount = purchaseAmountInput.trim()
    if (normalizedPurchaseAmount.isEmpty()) {
        return PurchaseProcessingResult.Error(
            PurchaseValidationError.PURCHASE_AMOUNT_REQUIRED
        )
    }

    val purchaseAmount = normalizedPurchaseAmount.toLongOrNull()
        ?: return PurchaseProcessingResult.Error(
            PurchaseValidationError.INVALID_PURCHASE_AMOUNT
        )

    if (purchaseAmount <= 0L) {
        return PurchaseProcessingResult.Error(
            PurchaseValidationError.NON_POSITIVE_PURCHASE_AMOUNT
        )
    }

    if (purchaseAmount > availableBalance) {
        return PurchaseProcessingResult.Error(
            PurchaseValidationError.INSUFFICIENT_BALANCE
        )
    }

    val remainingBalance = availableBalance - purchaseAmount
    val validThreshold = configuredThreshold?.takeIf { it > 0L }
    val lowBalanceAlert = validThreshold
        ?.takeIf { isLowBalance(remainingBalance, it) }
        ?.let {
            LowBalanceAlert(
                remainingBalance = remainingBalance,
                configuredThreshold = it
            )
        }

    return PurchaseProcessingResult.Success(
        remainingBalance = remainingBalance,
        lowBalanceAlert = lowBalanceAlert
    )
}
