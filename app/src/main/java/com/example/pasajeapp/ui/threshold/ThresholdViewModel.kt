package com.example.pasajeapp.ui.threshold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pasajeapp.data.CardData
import com.example.pasajeapp.data.CardRepository
import com.example.pasajeapp.data.CardStatus
import com.example.pasajeapp.data.PurchaseRepositoryResult
import com.example.pasajeapp.data.RepositoryResult
import com.example.pasajeapp.data.ThresholdStore
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CardUiState {
    data object Loading : CardUiState
    data class Ready(val card: CardData) : CardUiState
    data class Error(val message: String) : CardUiState
}

enum class BlockConfirmationState { HIDDEN, VISIBLE }

sealed interface BlockOperationState {
    data object Idle : BlockOperationState
    data object Blocking : BlockOperationState
    data object Success : BlockOperationState
    data class Error(val message: String) : BlockOperationState
}

sealed interface PurchaseUiState {
    data object Idle : PurchaseUiState
    data object Submitting : PurchaseUiState
    data class Approved(val currentBalance: Long) : PurchaseUiState
    data class RejectedCardBlocked(val message: String) : PurchaseUiState
    data class Error(val message: String) : PurchaseUiState
}

data class ThresholdUiState(
    val amountInput: String = "",
    val purchaseAmountInput: String = "",
    val savedThreshold: Long? = null,
    val isThresholdLoaded: Boolean = false,
    val cardState: CardUiState = CardUiState.Loading,
    val blockConfirmation: BlockConfirmationState = BlockConfirmationState.HIDDEN,
    val blockOperation: BlockOperationState = BlockOperationState.Idle,
    val purchaseState: PurchaseUiState = PurchaseUiState.Idle,
    val errorMessage: String? = null,
    val purchaseErrorMessage: String? = null,
    val confirmationMessage: String? = null,
    val lowBalanceAlert: LowBalanceAlert? = null
) {
    val card: CardData?
        get() = (cardState as? CardUiState.Ready)?.card
    val availableBalance: Long?
        get() = card?.balance
    val isPurchaseSubmissionEnabled: Boolean
        get() = cardState is CardUiState.Ready && purchaseState !is PurchaseUiState.Submitting
}

class ThresholdViewModel(
    private val thresholdStore: ThresholdStore,
    private val cardRepository: CardRepository,
    private val requestIdFactory: () -> String = { UUID.randomUUID().toString() }
) : ViewModel() {
    private val _uiState = MutableStateFlow(ThresholdUiState())
    val uiState: StateFlow<ThresholdUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            thresholdStore.savedThreshold.collect { savedThreshold ->
                _uiState.update {
                    it.copy(
                        savedThreshold = savedThreshold?.takeIf { value -> value > 0L },
                        isThresholdLoaded = true
                    )
                }
            }
        }
        loadCard()
    }

    fun loadCard() {
        if (_uiState.value.cardState is CardUiState.Loading &&
            _uiState.value.card != null
        ) return

        _uiState.update { it.copy(cardState = CardUiState.Loading) }
        viewModelScope.launch {
            when (val result = cardRepository.getCard()) {
                is RepositoryResult.Success -> {
                    val card = result.value
                    _uiState.update {
                        it.copy(
                            cardState = CardUiState.Ready(card),
                            savedThreshold = card.lowBalanceThreshold,
                            amountInput = card.lowBalanceThreshold.toString(),
                            isThresholdLoaded = true
                        )
                    }
                    thresholdStore.saveThreshold(card.lowBalanceThreshold)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(cardState = CardUiState.Error(result.message))
                }
            }
        }
    }

    fun onAmountChanged(value: String) {
        _uiState.update { it.copy(amountInput = value, errorMessage = null) }
    }

    fun onPurchaseAmountChanged(value: String) {
        _uiState.update {
            it.copy(
                purchaseAmountInput = value,
                purchaseErrorMessage = null,
                purchaseState = PurchaseUiState.Idle
            )
        }
    }

    fun requestTemporaryBlock() {
        val state = _uiState.value
        if (state.card?.status != CardStatus.ACTIVE || state.blockOperation is BlockOperationState.Blocking) {
            return
        }
        _uiState.update { it.copy(blockConfirmation = BlockConfirmationState.VISIBLE) }
    }

    fun cancelTemporaryBlock() {
        _uiState.update { it.copy(blockConfirmation = BlockConfirmationState.HIDDEN) }
    }

    fun confirmTemporaryBlock() {
        val state = _uiState.value
        if (state.blockConfirmation != BlockConfirmationState.VISIBLE ||
            state.blockOperation is BlockOperationState.Blocking
        ) return

        _uiState.update {
            it.copy(
                blockConfirmation = BlockConfirmationState.HIDDEN,
                blockOperation = BlockOperationState.Blocking
            )
        }
        viewModelScope.launch {
            when (val result = cardRepository.blockCard()) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(
                        cardState = CardUiState.Ready(result.value),
                        blockOperation = BlockOperationState.Success,
                        confirmationMessage = "Tarjeta bloqueada temporalmente"
                    )
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(blockOperation = BlockOperationState.Error(result.message))
                }
            }
        }
    }

    fun registerPurchase() {
        val currentState = _uiState.value
        if (!currentState.isPurchaseSubmissionEnabled) return

        val purchaseAmount = currentState.purchaseAmountInput.trim().toLongOrNull()
        val error = when {
            currentState.purchaseAmountInput.trim().isEmpty() -> "Ingrese el monto de la compra"
            purchaseAmount == null -> "Ingrese un monto de compra válido"
            purchaseAmount <= 0L -> "El monto de la compra debe ser mayor que cero"
            else -> null
        }
        if (error != null || purchaseAmount == null) {
            _uiState.update { it.copy(purchaseErrorMessage = error) }
            return
        }

        _uiState.update {
            it.copy(
                purchaseState = PurchaseUiState.Submitting,
                purchaseErrorMessage = null,
                lowBalanceAlert = null
            )
        }
        val requestId = requestIdFactory()
        viewModelScope.launch {
            when (
                val result = cardRepository.processPurchase(
                    amount = purchaseAmount,
                    merchant = "Comercio de prueba",
                    requestId = requestId
                )
            ) {
                is PurchaseRepositoryResult.Approved -> {
                    val purchase = result.purchase
                    _uiState.update { state ->
                        val currentCard = state.card
                        val threshold = currentCard?.lowBalanceThreshold ?: state.savedThreshold
                        state.copy(
                            cardState = currentCard?.let {
                                CardUiState.Ready(it.copy(balance = purchase.currentBalance))
                            } ?: state.cardState,
                            purchaseAmountInput = "",
                            purchaseState = PurchaseUiState.Approved(purchase.currentBalance),
                            confirmationMessage = "Compra aprobada",
                            lowBalanceAlert = threshold
                                ?.takeIf { purchase.lowBalance && purchase.currentBalance < it }
                                ?.let { LowBalanceAlert(purchase.currentBalance, it) }
                        )
                    }
                }
                is PurchaseRepositoryResult.RejectedCardBlocked -> _uiState.update { state ->
                    val currentCard = state.card
                    state.copy(
                        cardState = currentCard?.let {
                            CardUiState.Ready(it.copy(status = CardStatus.BLOCKED))
                        } ?: state.cardState,
                        purchaseState = PurchaseUiState.RejectedCardBlocked(result.message),
                        lowBalanceAlert = null
                    )
                }
                is PurchaseRepositoryResult.Rejected -> _uiState.update {
                    it.copy(
                        purchaseState = PurchaseUiState.Error(result.message),
                        purchaseErrorMessage = result.message,
                        lowBalanceAlert = null
                    )
                }
                is PurchaseRepositoryResult.Error -> _uiState.update {
                    it.copy(
                        purchaseState = PurchaseUiState.Error(result.message),
                        purchaseErrorMessage = result.message,
                        lowBalanceAlert = null
                    )
                }
            }
        }
    }

    fun dismissLowBalanceAlert() {
        _uiState.update { it.copy(lowBalanceAlert = null) }
    }

    fun dismissBlockedPurchaseAlert() {
        _uiState.update {
            if (it.purchaseState is PurchaseUiState.RejectedCardBlocked) {
                it.copy(purchaseState = PurchaseUiState.Idle)
            } else it
        }
    }

    fun saveThreshold() {
        val currentInput = uiState.value.amountInput.trim()
        val validationError = validate(currentInput)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        val threshold = currentInput.toLong()
        viewModelScope.launch {
            when (val result = cardRepository.updateThreshold(threshold)) {
                is RepositoryResult.Success -> {
                    thresholdStore.saveThreshold(result.value)
                    _uiState.update { state ->
                        val currentCard = state.card
                        state.copy(
                            cardState = currentCard?.let {
                                CardUiState.Ready(it.copy(lowBalanceThreshold = result.value))
                            } ?: state.cardState,
                            amountInput = result.value.toString(),
                            savedThreshold = result.value,
                            isThresholdLoaded = true,
                            errorMessage = null,
                            confirmationMessage = "Umbral guardado correctamente"
                        )
                    }
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(errorMessage = result.message)
                }
            }
        }
    }

    fun onConfirmationShown() {
        _uiState.update { it.copy(confirmationMessage = null) }
    }

    private fun validate(value: String): String? {
        if (value.isEmpty()) return "Ingrese un monto"
        val amount = value.toLongOrNull() ?: return "Ingrese un monto válido"
        if (amount <= 0L) return "El umbral debe ser mayor que cero"
        return null
    }

    class Factory(
        private val thresholdStore: ThresholdStore,
        private val cardRepository: CardRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ThresholdViewModel::class.java)) {
                return ThresholdViewModel(thresholdStore, cardRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
