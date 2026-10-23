package com.roshdmali.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
        val transaction = TransactionEntity(
            id = "transaction-1",
            type = "INCOME",
            title = "حقوق",
            amountToman = 25_000L,
            category = "حقوق",
            occurredAtMillis = 1_700_000_000_000L,
        )

        database.transactionDao().insert(transaction)

        assertEquals(listOf(transaction), database.transactionDao().getAll())
    }
}