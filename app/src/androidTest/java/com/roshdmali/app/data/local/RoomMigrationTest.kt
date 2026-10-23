package com.roshdmali.app.data.local

import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.testing.MigrationTestHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoomMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        RoshdMaliDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrationOneToThreePreservesLegacyTransactionsAndAddsCategoryLinks() {
        val oldDatabase = helper.createDatabase(TEST_DATABASE, 1)
        oldDatabase.execSQL(
            """INSERT INTO transactions
                (id, type, title, amountToman, category, occurredAtMillis)
                VALUES ('legacy-income', 'INCOME', 'حقوق قدیمی', 12000000, 'حقوق', 1700000000123)""",
        )
        oldDatabase.execSQL(
            """INSERT INTO transactions
                (id, type, title, amountToman, category, occurredAtMillis)
                VALUES ('legacy-expense', 'EXPENSE', 'هزینه قدیمی', 350000, 'خوراک', 1700000000456)""",
        )
        oldDatabase.close()

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            MIGRATION_1_2,
            MIGRATION_2_3,
        )
        try {
            migrated.query(
                "SELECT id, type, title, amountToman, category, createdAtMillis, occurredOnEpochDay, accountId, categoryId, counterpartyId, counterpartyNameSnapshot, notes FROM transactions ORDER BY id",
            ).use { cursor ->
                assertTrue(cursor.moveToNext())
                assertEquals("legacy-expense", cursor.getString(0))
                assertEquals("EXPENSE", cursor.getString(1))
                assertEquals("هزینه قدیمی", cursor.getString(2))
                assertEquals(350000L, cursor.getLong(3))
                assertEquals("خوراک", cursor.getString(4))
                assertEquals(1700000000456L, cursor.getLong(5))
                assertTrue(cursor.isNull(6))
                assertEquals(LEGACY_ACCOUNT_ID, cursor.getString(7))
                assertEquals("legacy-category:EXPENSE:خوراک", cursor.getString(8))
                assertTrue(cursor.isNull(9))
                assertTrue(cursor.isNull(10))
                assertTrue(cursor.isNull(11))

                assertTrue(cursor.moveToNext())
                assertEquals("legacy-income", cursor.getString(0))
                assertEquals("INCOME", cursor.getString(1))
                assertEquals("حقوق قدیمی", cursor.getString(2))
                assertEquals(12000000L, cursor.getLong(3))
                assertEquals("حقوق", cursor.getString(4))
                assertEquals(1700000000123L, cursor.getLong(5))
                assertTrue(cursor.isNull(6))
                assertEquals(LEGACY_ACCOUNT_ID, cursor.getString(7))
                assertEquals("legacy-category:INCOME:حقوق", cursor.getString(8))
                assertTrue(cursor.isNull(9))
                assertTrue(cursor.isNull(10))
                assertTrue(cursor.isNull(11))
                assertFalse(cursor.moveToNext())
            }

            migrated.query(
                "SELECT id, type, title, amountToman, category, occurredAtMillis FROM transactions_legacy_v1 ORDER BY id",
            ).use { cursor ->
                assertEquals(2, cursor.count)
            }
            migrated.query(
                "SELECT amountToman FROM account_openings WHERE accountId = '$LEGACY_ACCOUNT_ID'",
            ).use { cursor -> assertEquals(0, cursor.count) }
            migrated.query("SELECT COUNT(*) FROM categories").use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(2, cursor.getInt(0))
            }

            migrated.query(
                """SELECT SUM(CASE WHEN side = 'DEBIT' THEN amountToman ELSE 0 END),
                    SUM(CASE WHEN side = 'CREDIT' THEN amountToman ELSE 0 END)
                    FROM ledger_entries""",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(cursor.getLong(0), cursor.getLong(1))
                assertEquals(12350000L, cursor.getLong(0))
            }
        } finally {
            migrated.close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "migration-1-to-3-test"
        const val LEGACY_ACCOUNT_ID = "legacy-unassigned"
    }
}