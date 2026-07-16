package com.example.pasajeapp.ui.threshold

fun isLowBalance(
    remainingBalance: Long,
    configuredThreshold: Long
): Boolean = remainingBalance < configuredThreshold
