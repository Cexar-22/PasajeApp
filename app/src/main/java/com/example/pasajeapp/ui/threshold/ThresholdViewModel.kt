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
    val savedThreshold: Long? = null,
    val errorMessage: String? = null,
    val confirmationMessage: String? = null
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
