package com.roshdmali.app.domain

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FinancialTransactionTest {
    @Test
    fun parsesPersianAndArabicDigitsAndGroupingSeparators() {
        assertEquals(12_345L, parseTomanAmount("۱۲٬۳۴۵"))
        assertEquals(12_345L, parseTomanAmount("١٢,٣٤٥"))
        assertEquals(12_345L, parseTomanAmount("12 345"))
    }

    @Test
    fun rejectsEmptyZeroDecimalNegativeAndOverflowAmounts() {
        assertNull(parseTomanAmount(""))
        assertNull(parseTomanAmount("۰"))
        assertNull(parseTomanAmount("12.5"))
        assertNull(parseTomanAmount("-12"))
        assertNull(parseTomanAmount("۹۲۲۳۳۷۲۰۳۶۸۵۴۷۷۵۸۰۸"))
    }

    @Test
    fun summarizesTransactionsWithoutLongOverflow() {
        val transactions = listOf(
            transaction(TransactionType.INCOME, Long.MAX_VALUE),
            transaction(TransactionType.INCOME, Long.MAX_VALUE),
            transaction(TransactionType.EXPENSE, 250L),
        )

        val summary = summarizeTransactions(transactions)

        assertEquals(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO), summary.totalIncomeToman)
        assertEquals(BigInteger.valueOf(250L), summary.totalExpenseToman)
        assertEquals(summary.totalIncomeToman - summary.totalExpenseToman, summary.balanceToman)
    }

    @Test
    fun formatsTomanWithPersianDigitsAndGrouping() {
        assertEquals("۱,۲۳۴,۵۶۷", formatTomanAmount(BigInteger.valueOf(1_234_567L)))
        assertEquals("-۱۲", formatTomanAmount(BigInteger.valueOf(-12L)))
    }

    private fun transaction(type: TransactionType, amountToman: Long) = FinancialTransaction(
        id = "test-${type.name}-$amountToman",
        type = type,
        title = "تست",
        amountToman = amountToman,
        category = "تست",
        occurredAtMillis = 0L,
    )
}