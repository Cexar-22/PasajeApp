package com.example.pasajeapp.ui.threshold

import androidx.annotation.StringRes
import com.example.pasajeapp.R
import com.example.pasajeapp.data.CardStatus

@StringRes
internal fun cardStatusLabelRes(status: CardStatus): Int = when (status) {
    CardStatus.ACTIVE -> R.string.card_status_active
    CardStatus.BLOCKED -> R.string.card_status_blocked
}

internal fun shouldShowCardLock(status: CardStatus): Boolean = status == CardStatus.BLOCKED
