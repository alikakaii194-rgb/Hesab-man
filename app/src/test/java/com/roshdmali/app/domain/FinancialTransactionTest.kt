package com.roshdmali.app.domain

import java.math.BigInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialTransactionTest {
    @Test
    fun parsesPersianAndArabicDigitsAndGroupingSeparators() {
        assertEquals(12_345L, parseTomanAmount("۱۲٬۳۴۵"))
        assertEquals(12_345L, parseTomanAmount("١٢,٣٤٥"))
        assertEquals(12_345L, parseTomanAmount("12 345"))
        assertEquals(12_345L, parseRialAmount("۱۲۳۴۵۰"))
        assertNull(parseRialAmount("۱۲۳۴۵۱"))
    }

    @Test
    fun rejectsEmptyZeroDecimalNegativeAndOverflowAmounts() {
        assertNull(parseTomanAmount(""))
        assertNull(parseTomanAmount("۰"))
        assertNull(parseTomanAmount("12.5"))
        assertNull(parseTomanAmount("-12"))
        assertNull(parseTomanAmount("1,2"))
        assertNull(parseTomanAmount("12,34,567"))
        assertNull(parseTomanAmount("۹۲۲۳۳۷۲۰۳۶۸۵۴۷۷۵۸۰۸"))
        assertEquals(0L, parseTomanAmount("۰", allowZero = true))
        assertEquals(0L, parseRialAmount("۰", allowZero = true))
        assertNull(parseRialAmount("۹۲۲۳۳۷۲۰۳۶۸۵۴۷۷۵۸۰۸۰"))
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
    fun accountBalanceIncludesOpeningIncomeAndExpensePostings() {
        val postings = listOf(
            LedgerPosting("cash", LedgerSide.DEBIT, 100_000L),
            LedgerPosting("cash", LedgerSide.DEBIT, 25_000L),
            LedgerPosting("cash", LedgerSide.CREDIT, 40_000L),
        )

        assertEquals(BigInteger.valueOf(85_000L), calculateAccountBalance(postings))
        assertEquals(BigInteger.ZERO, calculateAccountBalance(emptyList()))
    }

    @Test
    fun accountBalanceCanBeNegativeWithoutOverflow() {
        val postings = listOf(
            LedgerPosting("cash", LedgerSide.DEBIT, 10L),
            LedgerPosting("cash", LedgerSide.CREDIT, 30L),
        )

        assertEquals(BigInteger.valueOf(-20L), calculateAccountBalance(postings))
    }

    @Test
    fun reversedIncomeAndExpenseDoNotCountAsNewPnl() {
        val income = transaction(TransactionType.INCOME, 10_000L)
        val expense = transaction(TransactionType.EXPENSE, 3_000L)
        val reversedIncome = transaction(TransactionType.EXPENSE, 10_000L).copy(
            id = "reverse-income",
            reversesTransactionId = income.id,
        )
        val reversedExpense = transaction(TransactionType.INCOME, 3_000L).copy(
            id = "reverse-expense",
            reversesTransactionId = expense.id,
        )

        val summary = summarizeTransactions(listOf(income, expense, reversedIncome, reversedExpense))

        assertEquals(BigInteger.ZERO, summary.totalIncomeToman)
        assertEquals(BigInteger.ZERO, summary.totalExpenseToman)
    }

    @Test
    fun ledgerOperationsRequireEqualDebitAndCreditTotals() {
        assertTrue(
            isLedgerBalanced(
                listOf(
                    LedgerPosting("cash", LedgerSide.DEBIT, Long.MAX_VALUE),
                    LedgerPosting("income", LedgerSide.CREDIT, Long.MAX_VALUE),
                ),
            ),
        )
        assertFalse(isLedgerBalanced(listOf(LedgerPosting("cash", LedgerSide.DEBIT, 100L))))
        assertFalse(isLedgerBalanced(listOf(LedgerPosting("cash", LedgerSide.DEBIT, 0L))))
    }

    @Test
    fun formatsTomanWithPersianDigitsAndGrouping() {
        assertEquals("۱,۲۳۴,۵۶۷", formatTomanAmount(BigInteger.valueOf(1_234_567L)))
        assertEquals("-۱۲", formatTomanAmount(BigInteger.valueOf(-12L)))
    }

    @Test
    fun convertsPersianCalendarDatesToAndFromEpochDays() {
        val nowruz = PersianDate(1403, 1, 1)

        assertEquals(java.time.LocalDate.of(2024, 3, 20).toEpochDay(), nowruz.toEpochDayOrNull())
        assertEquals(nowruz, requireNotNull(nowruz.toEpochDayOrNull()).toPersianDate())
        assertEquals(
            java.time.LocalDate.of(2024, 3, 19).toEpochDay(),
            PersianDate(1402, 12, 29).toEpochDayOrNull(),
        )
        assertEquals(
            java.time.LocalDate.of(2025, 3, 20).toEpochDay(),
            PersianDate(1403, 12, 30).toEpochDayOrNull(),
        )
        assertEquals(
            PersianDate(1403, 12, 30),
            requireNotNull(PersianDate(1403, 12, 30).toEpochDayOrNull()).toPersianDate(),
        )
        assertEquals(
            PersianDate(1404, 1, 1),
            (requireNotNull(PersianDate(1403, 12, 30).toEpochDayOrNull()) + 1).toPersianDate(),
        )
        assertNull(PersianDate(1402, 12, 30).toEpochDayOrNull())
        assertEquals(1403, "۱۴۰۳/۰۱/۰۱".parsePersianDateEpochDay()?.toPersianDate()?.year)
    }

    private fun transaction(type: TransactionType, amountToman: Long) = FinancialTransaction(
        id = "test-${type.name}-$amountToman",
        operationId = "operation-${type.name}-$amountToman",
        accountId = "account-test",
        type = type,
        title = "تست",
        amountToman = amountToman,
        category = "تست",
        createdAtMillis = 0L,
        occurredOnEpochDay = 0L,
        reversesTransactionId = null,
    )
}