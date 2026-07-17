package com.example.pasajeapp.data

import com.example.pasajeapp.data.network.BlockCardRequestDto
import com.example.pasajeapp.data.network.CardApi
import com.example.pasajeapp.data.network.PurchaseRequestDto
import com.example.pasajeapp.data.network.PurchaseResponseDto
import com.example.pasajeapp.data.network.ThresholdRequestDto
import com.google.gson.Gson
import java.io.IOException

enum class CardStatus { ACTIVE, BLOCKED }

data class CardData(
    val id: String,
    val lastFour: String,
    val status: CardStatus,
    val balance: Long,
    val lowBalanceThreshold: Long,
    val blockedAt: String?
)

sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data class Error(val code: String, val message: String) : RepositoryResult<Nothing>
}

data class ApprovedPurchase(
    val previousBalance: Long,
    val amount: Long,
    val currentBalance: Long,
    val lowBalance: Boolean
)

sealed interface PurchaseRepositoryResult {
    data class Approved(val purchase: ApprovedPurchase) : PurchaseRepositoryResult
    data class RejectedCardBlocked(val message: String, val currentBalance: Long?) : PurchaseRepositoryResult
    data class Rejected(val code: String, val message: String, val currentBalance: Long?) : PurchaseRepositoryResult
    data class Error(val message: String) : PurchaseRepositoryResult
}

interface CardRepository {
    suspend fun getCard(): RepositoryResult<CardData>
    suspend fun blockCard(): RepositoryResult<CardData>
    suspend fun updateThreshold(threshold: Long): RepositoryResult<Long>
    suspend fun processPurchase(amount: Long, merchant: String, requestId: String): PurchaseRepositoryResult
}

class RemoteCardRepository(
    private val api: CardApi,
    private val cardId: String
) : CardRepository {
    override suspend fun getCard(): RepositoryResult<CardData> = safeRequest {
        api.getCard(cardId).let { response ->
            CardData(
                id = response.id,
                lastFour = response.lastFour,
                status = response.status.toCardStatus(),
                balance = response.balance,
                lowBalanceThreshold = response.lowBalanceThreshold,
                blockedAt = response.blockedAt
            )
        }
    }

    override suspend fun blockCard(): RepositoryResult<CardData> = safeRequest {
        val current = when (val result = getCard()) {
            is RepositoryResult.Success -> result.value
            is RepositoryResult.Error -> throw ApiRepositoryException(result.code, result.message)
        }
        val response = api.blockCard(cardId, BlockCardRequestDto(confirmed = true))
        current.copy(status = response.status.toCardStatus(), blockedAt = response.blockedAt)
    }

    override suspend fun updateThreshold(threshold: Long): RepositoryResult<Long> = safeRequest {
        api.updateThreshold(cardId, ThresholdRequestDto(threshold)).lowBalanceThreshold
    }

    override suspend fun processPurchase(
        amount: Long,
        merchant: String,
        requestId: String
    ): PurchaseRepositoryResult {
        return try {
            val response = api.processPurchase(
                cardId,
                PurchaseRequestDto(amount = amount, merchant = merchant, requestId = requestId)
            )
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val previousBalance = body.previousBalance
                val currentBalance = body.currentBalance
                if (body.status == "APPROVED" && previousBalance != null && currentBalance != null) {
                    PurchaseRepositoryResult.Approved(
                        ApprovedPurchase(
                            previousBalance = previousBalance,
                            amount = body.amount ?: amount,
                            currentBalance = currentBalance,
                            lowBalance = body.lowBalance == true
                        )
                    )
                } else {
                    PurchaseRepositoryResult.Error("La respuesta de compra no es válida.")
                }
            } else {
                val error = response.errorBody()?.charStream()?.use { reader ->
                    Gson().fromJson(reader, PurchaseResponseDto::class.java)
                }
                if (error?.code == "CARD_BLOCKED") {
                    PurchaseRepositoryResult.RejectedCardBlocked(
                        message = error.message ?: "La tarjeta está bloqueada temporalmente.",
                        currentBalance = error.currentBalance
                    )
                } else {
                    PurchaseRepositoryResult.Rejected(
                        code = error?.code ?: "PURCHASE_REJECTED",
                        message = error?.message ?: "La compra fue rechazada.",
                        currentBalance = error?.currentBalance
                    )
                }
            }
        } catch (_: IOException) {
            PurchaseRepositoryResult.Error("No fue posible conectar con el servidor.")
        } catch (_: Exception) {
            PurchaseRepositoryResult.Error("No fue posible procesar la compra.")
        }
    }

    private suspend fun <T> safeRequest(block: suspend () -> T): RepositoryResult<T> {
        return try {
            RepositoryResult.Success(block())
        } catch (error: ApiRepositoryException) {
            RepositoryResult.Error(error.code, error.message ?: "Error de API")
        } catch (_: IOException) {
            RepositoryResult.Error("NETWORK_ERROR", "No fue posible conectar con el servidor.")
        } catch (_: Exception) {
            RepositoryResult.Error("API_ERROR", "No fue posible completar la operación.")
        }
    }

    private fun String.toCardStatus(): CardStatus = when (this) {
        "ACTIVE" -> CardStatus.ACTIVE
        "BLOCKED" -> CardStatus.BLOCKED
        else -> throw ApiRepositoryException("INVALID_CARD_STATUS", "Estado de tarjeta no reconocido")
    }
}

private class ApiRepositoryException(val code: String, message: String) : Exception(message)
