package com.example.pasajeapp.ui.threshold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pasajeapp.data.ThresholdPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ThresholdUiState(
    val amountInput: String = "",
    val availableBalanceInput: String = "",
    val purchaseAmountInput: String = "",
    val availableBalance: Long? = null,
    val savedThreshold: Long? = null,
    val isThresholdLoaded: Boolean = false,
    val isPurchaseSubmissionEnabled: Boolean = true,
    val errorMessage: String? = null,
    val balanceErrorMessage: String? = null,
    val purchaseErrorMessage: String? = null,
    val confirmationMessage: String? = null,
    val lowBalanceAlert: LowBalanceAlert? = null
)

class ThresholdViewModel(
    private val thresholdPreferences: ThresholdPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(ThresholdUiState())
    val uiState: StateFlow<ThresholdUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            thresholdPreferences.savedThreshold.collect { savedThreshold ->
                _uiState.update {
                    it.copy(
                        savedThreshold = savedThreshold?.takeIf { value -> value > 0L },
                        isThresholdLoaded = true
                    )
                }
            }
        }
    }

    fun onAmountChanged(value: String) {
        _uiState.update {
            it.copy(
                amountInput = value,
                errorMessage = null
            )
        }
    }

    fun onAvailableBalanceChanged(value: String) {
        _uiState.update {
            it.copy(
                availableBalanceInput = value,
                availableBalance = value.trim().toLongOrNull()?.takeIf { balance ->
                    balance >= 0L
                },
                isPurchaseSubmissionEnabled = true,
                balanceErrorMessage = null
            )
        }
    }

    fun onPurchaseAmountChanged(value: String) {
        _uiState.update {
            it.copy(
                purchaseAmountInput = value,
                isPurchaseSubmissionEnabled = true,
                purchaseErrorMessage = null
            )
        }
    }

    fun registerPurchase() {
        val currentState = uiState.value
        if (!currentState.isThresholdLoaded || !currentState.isPurchaseSubmissionEnabled) {
            return
        }

        when (
            val result = processPurchase(
                availableBalanceInput = currentState.availableBalanceInput,
                purchaseAmountInput = currentState.purchaseAmountInput,
                configuredThreshold = currentState.savedThreshold
            )
        ) {
            is PurchaseProcessingResult.Success -> {
                _uiState.update {
                    it.copy(
                        availableBalanceInput = result.remainingBalance.toString(),
                        purchaseAmountInput = "",
                        availableBalance = result.remainingBalance,
                        isPurchaseSubmissionEnabled = false,
                        balanceErrorMessage = null,
                        purchaseErrorMessage = null,
                        lowBalanceAlert = result.lowBalanceAlert
                    )
                }
            }

            is PurchaseProcessingResult.Error -> showPurchaseError(result.reason)
        }
    }

    fun dismissLowBalanceAlert() {
        _uiState.update {
            it.copy(lowBalanceAlert = null)
        }
    }

    fun saveThreshold() {
        val currentInput = uiState.value.amountInput.trim()
        val validationError = validate(currentInput)

        if (validationError != null) {
            _uiState.update {
                it.copy(errorMessage = validationError)
            }
            return
        }

        val threshold = currentInput.toLong()
        viewModelScope.launch {
            thresholdPreferences.saveThreshold(threshold)
            _uiState.update {
                it.copy(
                    amountInput = threshold.toString(),
                    savedThreshold = threshold,
                    isThresholdLoaded = true,
                    errorMessage = null,
                    confirmationMessage = "Umbral guardado correctamente"
                )
            }
        }
    }

    private fun showPurchaseError(error: PurchaseValidationError) {
        _uiState.update {
            when (error) {
                PurchaseValidationError.AVAILABLE_BALANCE_REQUIRED -> it.copy(
                    balanceErrorMessage = "Ingrese el saldo disponible",
                    purchaseErrorMessage = null
                )

                PurchaseValidationError.INVALID_AVAILABLE_BALANCE -> it.copy(
                    balanceErrorMessage = "Ingrese un saldo disponible válido",
                    purchaseErrorMessage = null
                )

                PurchaseValidationError.PURCHASE_AMOUNT_REQUIRED -> it.copy(
                    balanceErrorMessage = null,
                    purchaseErrorMessage = "Ingrese el monto de la compra"
                )

                PurchaseValidationError.INVALID_PURCHASE_AMOUNT -> it.copy(
                    balanceErrorMessage = null,
                    purchaseErrorMessage = "Ingrese un monto de compra válido"
                )

                PurchaseValidationError.NON_POSITIVE_PURCHASE_AMOUNT -> it.copy(
                    balanceErrorMessage = null,
                    purchaseErrorMessage = "El monto de la compra debe ser mayor que cero"
                )

                PurchaseValidationError.INSUFFICIENT_BALANCE -> it.copy(
                    balanceErrorMessage = null,
                    purchaseErrorMessage = "El monto de la compra supera el saldo disponible"
                )
            }
        }
    }

    fun onConfirmationShown() {
        _uiState.update {
            it.copy(confirmationMessage = null)
        }
    }

    private fun validate(value: String): String? {
        if (value.isEmpty()) {
            return "Ingrese un monto"
        }

        val amount = value.toLongOrNull()
            ?: return "Ingrese un monto válido"

        if (amount <= 0L) {
            return "El umbral debe ser mayor que cero"
        }

        return null
    }

    class Factory(
        private val thresholdPreferences: ThresholdPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ThresholdViewModel::class.java)) {
                return ThresholdViewModel(thresholdPreferences) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
