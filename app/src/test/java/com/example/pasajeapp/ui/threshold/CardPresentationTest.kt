package com.example.pasajeapp.ui.threshold

import com.example.pasajeapp.R
import com.example.pasajeapp.data.CardStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardPresentationTest {
    @Test
    fun `ACTIVE se presenta como Activa y sin candado`() {
        assertEquals(R.string.card_status_active, cardStatusLabelRes(CardStatus.ACTIVE))
        assertFalse(shouldShowCardLock(CardStatus.ACTIVE))
    }

    @Test
    fun `BLOCKED se presenta como Bloqueada y con candado`() {
        assertEquals(R.string.card_status_blocked, cardStatusLabelRes(CardStatus.BLOCKED))
        assertTrue(shouldShowCardLock(CardStatus.BLOCKED))
    }
}
