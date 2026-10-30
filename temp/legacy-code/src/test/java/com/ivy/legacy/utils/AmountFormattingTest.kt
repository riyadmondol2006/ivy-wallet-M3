package com.ivy.legacy.utils

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

/** Amounts above Int.MAX_VALUE (2,147,483,647) used to be clamped/overflowed by toInt(). */
class AmountFormattingTest {
    private lateinit var originalLocale: Locale

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `typing a digit keeps amounts above the 32-bit limit intact`() {
        val result = formatInputAmount(currency = "USD", amount = "98765432198", newSymbol = "7")

        assertEquals("987,654,321,987", result)
    }

    @Test
    fun `typing keeps the decimal part of a large amount`() {
        val result = formatInputAmount(currency = "USD", amount = "5000000000.2", newSymbol = "5")

        assertEquals("5,000,000,000.25", result)
    }

    @Test
    fun `integer part of a large balance is not overflowed`() {
        assertEquals("12,345,678,901", integerPartFormatted(12_345_678_901.0))
        assertEquals("-3,000,000,000", integerPartFormatted(-3_000_000_000.0))
    }

    @Test
    fun `small negative values keep their sign`() {
        assertEquals("-0", integerPartFormatted(-0.5))
    }

    @Test
    fun `significant decimal part is detected for large amounts`() {
        assertTrue(hasSignificantDecimalPart(3_000_000_000.5))
        assertFalse(hasSignificantDecimalPart(3_000_000_000.0))
    }
}
