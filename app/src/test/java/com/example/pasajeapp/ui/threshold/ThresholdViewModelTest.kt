package com.example.pasajeapp.ui.threshold

import com.example.pasajeapp.data.ApprovedPurchase
import com.example.pasajeapp.data.CardData
import com.example.pasajeapp.data.CardRepository
import com.example.pasajeapp.data.CardStatus
import com.example.pasajeapp.data.PurchaseRepositoryResult
import com.example.pasajeapp.data.RepositoryResult
import com.example.pasajeapp.data.ThresholdStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThresholdViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val activeCard = CardData(
        id = "00000000-0000-0000-0000-000000000001",
        lastFour = "1234",
        status = CardStatus.ACTIVE,
        balance = 20_000,
        lowBalanceThreshold = 5_000,
        blockedAt = null
    )

    @Test
    fun `confirmar bloqueo llama una sola vez y actualiza a BLOCKED`() = runTest {
        val repository = FakeCardRepository(activeCard)
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.requestTemporaryBlock()
        viewModel.confirmTemporaryBlock()
        viewModel.confirmTemporaryBlock()
        advanceUntilIdle()

        assertEquals(1, repository.blockCalls)
        assertEquals(CardStatus.BLOCKED, viewModel.uiState.value.card?.status)
        assertTrue(viewModel.uiState.value.blockOperation is BlockOperationState.Success)
    }

    @Test
    fun `cancelar confirmación no llama al backend y conserva ACTIVE`() = runTest {
        val repository = FakeCardRepository(activeCard)
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.requestTemporaryBlock()
        viewModel.cancelTemporaryBlock()
        advanceUntilIdle()

        assertEquals(0, repository.blockCalls)
        assertEquals(CardStatus.ACTIVE, viewModel.uiState.value.card?.status)
        assertEquals(BlockConfirmationState.HIDDEN, viewModel.uiState.value.blockConfirmation)
    }

    @Test
    fun `error de bloqueo conserva la tarjeta activa en un estado coherente`() = runTest {
        val repository = FakeCardRepository(activeCard).apply {
            blockResult = RepositoryResult.Error("NETWORK_ERROR", "Sin conexión")
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.requestTemporaryBlock()
        viewModel.confirmTemporaryBlock()
        advanceUntilIdle()

        assertEquals(CardStatus.ACTIVE, viewModel.uiState.value.card?.status)
        assertTrue(viewModel.uiState.value.blockOperation is BlockOperationState.Error)
    }

    @Test
    fun `CARD_BLOCKED muestra rechazo y no modifica saldo ni activa HU 2_2`() = runTest {
        val repository = FakeCardRepository(activeCard).apply {
            purchaseResult = PurchaseRepositoryResult.RejectedCardBlocked(
                message = "La tarjeta está bloqueada temporalmente.",
                currentBalance = 20_000
            )
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        viewModel.onPurchaseAmountChanged("3000")

        viewModel.registerPurchase()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.purchaseState is PurchaseUiState.RejectedCardBlocked)
        assertEquals(20_000L, viewModel.uiState.value.availableBalance)
        assertNull(viewModel.uiState.value.lowBalanceAlert)
        assertEquals(CardStatus.BLOCKED, viewModel.uiState.value.card?.status)
    }

    @Test
    fun `compra activa actualiza saldo y mantiene alerta HU 2_2`() = runTest {
        val repository = FakeCardRepository(activeCard).apply {
            purchaseResult = PurchaseRepositoryResult.Approved(
                ApprovedPurchase(
                    previousBalance = 20_000,
                    amount = 16_000,
                    currentBalance = 4_000,
                    lowBalance = true
                )
            )
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        viewModel.onPurchaseAmountChanged("16000")

        viewModel.registerPurchase()
        advanceUntilIdle()

        assertEquals(4_000L, viewModel.uiState.value.availableBalance)
        assertEquals(LowBalanceAlert(4_000, 5_000), viewModel.uiState.value.lowBalanceAlert)
        assertTrue(viewModel.uiState.value.purchaseState is PurchaseUiState.Approved)
    }

    @Test
    fun `compra activa sin saldo bajo no muestra alerta`() = runTest {
        val repository = FakeCardRepository(activeCard).apply {
            purchaseResult = PurchaseRepositoryResult.Approved(
                ApprovedPurchase(20_000, 3_000, 17_000, lowBalance = false)
            )
        }
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        viewModel.onPurchaseAmountChanged("3000")

        viewModel.registerPurchase()
        advanceUntilIdle()

        assertEquals(17_000L, viewModel.uiState.value.availableBalance)
        assertNull(viewModel.uiState.value.lowBalanceAlert)
        assertFalse(viewModel.uiState.value.purchaseState is PurchaseUiState.Error)
    }

    private fun createViewModel(repository: FakeCardRepository) = ThresholdViewModel(
        thresholdStore = FakeThresholdStore(),
        cardRepository = repository,
        requestIdFactory = { "request-test" }
    )

    private class FakeThresholdStore : ThresholdStore {
        private val state = MutableStateFlow<Long?>(null)
        override val savedThreshold: Flow<Long?> = state
        override suspend fun saveThreshold(threshold: Long) {
            state.value = threshold
        }
    }

    private class FakeCardRepository(private val card: CardData) : CardRepository {
        var blockCalls = 0
        var blockResult: RepositoryResult<CardData> = RepositoryResult.Success(
            card.copy(
                status = CardStatus.BLOCKED,
                blockedAt = "2026-07-16T12:00:00.000Z"
            )
        )
        var purchaseResult: PurchaseRepositoryResult = PurchaseRepositoryResult.Approved(
            ApprovedPurchase(card.balance, 3_000, card.balance - 3_000, lowBalance = false)
        )

        override suspend fun getCard(): RepositoryResult<CardData> = RepositoryResult.Success(card)

        override suspend fun blockCard(): RepositoryResult<CardData> {
            blockCalls += 1
            return blockResult
        }

        override suspend fun updateThreshold(threshold: Long): RepositoryResult<Long> =
            RepositoryResult.Success(threshold)

        override suspend fun processPurchase(
            amount: Long,
            merchant: String,
            requestId: String
        ): PurchaseRepositoryResult = purchaseResult
    }
}
