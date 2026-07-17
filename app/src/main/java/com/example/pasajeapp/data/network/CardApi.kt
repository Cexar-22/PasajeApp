package com.example.pasajeapp.data.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface CardApi {
    @GET("api/cards/{cardId}")
    suspend fun getCard(@Path("cardId") cardId: String): CardResponseDto

    @POST("api/cards/{cardId}/block")
    suspend fun blockCard(
        @Path("cardId") cardId: String,
        @Body request: BlockCardRequestDto
    ): BlockCardResponseDto

    @POST("api/cards/{cardId}/purchases")
    suspend fun processPurchase(
        @Path("cardId") cardId: String,
        @Body request: PurchaseRequestDto
    ): Response<PurchaseResponseDto>

    @PATCH("api/cards/{cardId}/threshold")
    suspend fun updateThreshold(
        @Path("cardId") cardId: String,
        @Body request: ThresholdRequestDto
    ): ThresholdResponseDto
}

data class CardResponseDto(
    val id: String,
    val lastFour: String,
    val status: String,
    val balance: Long,
    val lowBalanceThreshold: Long,
    val blockedAt: String?
)

data class BlockCardRequestDto(val confirmed: Boolean)

data class BlockCardResponseDto(
    val id: String,
    val status: String,
    val blockedAt: String?
)

data class PurchaseRequestDto(
    val amount: Long,
    val merchant: String,
    val requestId: String
)

data class PurchaseResponseDto(
    val status: String? = null,
    val previousBalance: Long? = null,
    val amount: Long? = null,
    val currentBalance: Long? = null,
    val lowBalance: Boolean? = null,
    val code: String? = null,
    val message: String? = null,
    val balanceChanged: Boolean? = null
)

data class ThresholdRequestDto(val threshold: Long)

data class ThresholdResponseDto(
    val id: String,
    val lowBalanceThreshold: Long
)
