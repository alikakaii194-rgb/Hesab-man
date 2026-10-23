package com.roshdmali.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.roshdmali.app.data.local.RoshdMaliDatabase
import com.roshdmali.app.domain.AccountDraft
import com.roshdmali.app.domain.AccountType
import com.roshdmali.app.domain.TransactionDraft
import com.roshdmali.app.domain.TransactionType
import com.roshdmali.app.domain.summarizeTransactions
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RoomTransactionRepositoryTest {
    private lateinit var database: RoshdMaliDatabase
    private lateinit var repository: RoomTransactionRepository

    @Before
    fun createDatabase() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RoshdMaliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomTransactionRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun openingIncomeExpenseAndRetriesKeepAccountBalanceAndLedgerCorrect() = runBlocking {
        val effectiveDay = LocalDate.of(2025, 3, 21).toEpochDay()
        val accountDraft = AccountDraft(
            name = "کیف پول",
            type = AccountType.CASH,
            openingAmountToman = 100_000L,
            effectiveEpochDay = effectiveDay,
            idempotencyKey = "account-create-key",
        )

        val account = repository.createAccount(accountDraft)
        val repeatedAccount = repository.createAccount(accountDraft)
        assertEquals(account.id, repeatedAccount.id)
        assertEquals(emptyList<Any>(), repository.getTransactions())

        val incomeDraft = TransactionDraft(
            accountId = account.id,
            type = TransactionType.INCOME,
            title = "درآمد",
            amountToman = 25_000L,
            category = "حقوق",
            occurredOnEpochDay = effectiveDay,
            idempotencyKey = "income-key",
        )
        val income = repository.addTransaction(incomeDraft)
        val repeatedIncome = repository.addTransaction(incomeDraft)
        assertEquals(income.id, repeatedIncome.id)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.addTransaction(incomeDraft.copy(amountToman = 26_000L))
            }
        }

        val expense = repository.addTransaction(
            TransactionDraft(
                accountId = account.id,
                type = TransactionType.EXPENSE,
                title = "هزینه",
                amountToman = 40_000L,
                category = "خوراک",
                occurredOnEpochDay = effectiveDay,
                idempotencyKey = "expense-key",
            ),
        )

        assertNotEquals(income.id, expense.id)
        assertEquals(2, repository.getTransactions().size)
        assertEquals(85_000L, repository.observeAccountBalances().first().single().balanceToman.longValueExact())

        listOf(income, expense).forEach { transaction ->
            val entries = database.ledgerEntryDao().getForOperation(transaction.operationId)
            val debits = entries.filter { it.side == "DEBIT" }.sumOf { it.amountToman }
            val credits = entries.filter { it.side == "CREDIT" }.sumOf { it.amountToman }
            assertEquals(debits, credits)
        }

        val reversal = repository.reverseTransaction(income.id, "reverse-income-key")
        val repeatedReversal = repository.reverseTransaction(income.id, "reverse-income-key")
        assertEquals(reversal.id, repeatedReversal.id)
        assertEquals(60_000L, repository.observeAccountBalances().first().single().balanceToman.longValueExact())
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.reverseTransaction(income.id, "second-reversal-key") }
        }
        val summary = summarizeTransactions(repository.getTransactions())
        assertEquals(0L, summary.totalIncomeToman.longValueExact())
        assertEquals(40_000L, summary.totalExpenseToman.longValueExact())
    }

    @Test
    fun zeroOpeningCreatesAccountWithoutIncomeOrLedgerPosting() = runBlocking {
        val account = repository.createAccount(
            AccountDraft(
                name = "حساب صفر",
                type = AccountType.BANK,
                openingAmountToman = 0L,
                effectiveEpochDay = LocalDate.of(2025, 3, 21).toEpochDay(),
                idempotencyKey = "zero-account-key",
            ),
        )

        assertEquals(0L, repository.observeAccountBalances().first().single().balanceToman.longValueExact())
        assertEquals(emptyList<Any>(), repository.getTransactions())
        val opening = requireNotNull(database.accountDao().getOpening(account.id))
        assertEquals(0L, opening.amountToman)
        assertEquals(0, database.ledgerEntryDao().getForOperation(opening.operationId).size)
    }

    @Test
    fun futureEffectiveOpeningDoesNotChangeTodaysBalance() = runBlocking {
        val account = repository.createAccount(
            AccountDraft(
                name = "حساب با افتتاحیه آینده",
                type = AccountType.BANK,
                openingAmountToman = 80_000L,
                effectiveEpochDay = LocalDate.of(2030, 1, 1).toEpochDay(),
                idempotencyKey = "future-opening-key",
            ),
        )

        assertEquals(0L, repository.observeAccountBalances().first()
            .single { it.account.id == account.id }.balanceToman.longValueExact())
    }
}