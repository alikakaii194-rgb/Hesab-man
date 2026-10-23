package com.roshdmali.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TransactionDaoTest {
    private lateinit var database: RoshdMaliDatabase

    @Before
    fun createDatabase() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RoshdMaliDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertCanBeReadBackFromDao() = runBlocking {
        val account = AccountEntity(
            id = "account-1",
            name = "کیف پول",
            type = "CASH",
            isSystem = false,
            isActive = true,
            createdAtMillis = 1L,
            idempotencyKey = "account-create-1",
        )
        database.accountDao().insert(account)
        database.financialOperationDao().insert(
            FinancialOperationEntity(
                id = "operation-1",
                idempotencyKey = "transaction-create-1",
                kind = "INCOME",
                createdAtMillis = 1L,
                occurredOnEpochDay = 20_000L,
                accountId = account.id,
                reversesOperationId = null,
            ),
        )
        val transaction = TransactionEntity(
            id = "transaction-1",
            operationId = "operation-1",
            accountId = "account-1",
            type = "INCOME",
            title = "حقوق",
            amountToman = 25_000L,
            category = "حقوق",
            createdAtMillis = 1_700_000_000_000L,
            occurredOnEpochDay = 20_000L,
            reversesTransactionId = null,
        )

        database.transactionDao().insert(transaction)

        assertEquals(listOf(transaction), database.transactionDao().getAll())
    }
}