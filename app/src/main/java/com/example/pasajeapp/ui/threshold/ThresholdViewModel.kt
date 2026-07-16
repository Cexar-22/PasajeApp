package com.example.pasajeapp.ui.threshold

import android.util.Log
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
    val remainingBalanceInput: String = "",
    val remainingBalance: Long? = null,
    val savedThreshold: Long? = null,
    val errorMessage: String? = null,
    val purchaseErrorMessage: String? = null,
    val confirmationMessage: String? = null
) {
    val shouldShowLowBalanceAlert: Boolean
        get() = remainingBalance != null &&
            savedThreshold != null &&
            isLowBalance(remainingBalance, savedThreshold)
}

class ThresholdViewModel(
    private val thresholdPreferences: ThresholdPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(ThresholdUiState())
    val uiState: StateFlow<ThresholdUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            thresholdPreferences.savedThreshold.collect { savedThreshold ->
                Log.d("ThresholdViewModel", "Umbral recuperado: $savedThreshold")
                _uiState.update {
                    it.copy(savedThreshold = savedThreshold)
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

    fun onRemainingBalanceInputChanged(value: String) {
        _uiState.update {
            it.copy(
                remainingBalanceInput = value,
                purchaseErrorMessage = null
            )
        }
    }

    fun registerPurchaseResult() {
        val currentInput = uiState.value.remainingBalanceInput.trim()
        val remainingBalance = currentInput.toLongOrNull()

        if (remainingBalance == null || remainingBalance < 0L) {
            _uiState.update {
                it.copy(purchaseErrorMessage = "Ingrese un saldo restante válido")
            }
            return
        }

        onPurchaseCompleted(remainingBalance)
    }

    fun onPurchaseCompleted(remainingBalance: Long) {
        require(remainingBalance >= 0L) { "Remaining balance cannot be negative" }
        _uiState.update {
            it.copy(
                remainingBalanceInput = remainingBalance.toString(),
                remainingBalance = remainingBalance,
                purchaseErrorMessage = null
            )
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
                    errorMessage = null,
                    confirmationMessage = "Umbral guardado correctamente"
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
