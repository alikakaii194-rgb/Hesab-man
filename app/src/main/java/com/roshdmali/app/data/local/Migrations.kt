package com.roshdmali.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE transactions RENAME TO transactions_legacy_v1")
        database.execSQL("DROP INDEX IF EXISTS index_transactions_occurredAtMillis")
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_transactions_legacy_v1_occurredAtMillis " +
                "ON transactions_legacy_v1(occurredAtMillis)",
        )

        database.execSQL(
            """CREATE TABLE IF NOT EXISTS accounts (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                isSystem INTEGER NOT NULL,
                isActive INTEGER NOT NULL,
                createdAtMillis INTEGER,
                idempotencyKey TEXT
            )""".trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_accounts_isSystem_isActive ON accounts(isSystem, isActive)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accounts_idempotencyKey ON accounts(idempotencyKey)")
        database.execSQL(
            """INSERT INTO accounts (
                id, name, type, isSystem, isActive, createdAtMillis, idempotencyKey
            ) VALUES (
                'legacy-unassigned', 'حساب قدیمی ـ تعیین‌نشده', 'LEGACY_UNASSIGNED', 1, 1, NULL, NULL
            ), (
                'system-income', 'درآمد', 'SYSTEM_INCOME', 1, 1, NULL, NULL
            ), (
                'system-expense', 'هزینه', 'SYSTEM_EXPENSE', 1, 1, NULL, NULL
            ), (
                'system-opening-equity', 'موجودی اولیه', 'SYSTEM_OPENING_EQUITY', 1, 1, NULL, NULL
            )""".trimIndent(),
        )

        database.execSQL(
            """CREATE TABLE IF NOT EXISTS financial_operations (
                id TEXT NOT NULL PRIMARY KEY,
                idempotencyKey TEXT NOT NULL,
                kind TEXT NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                occurredOnEpochDay INTEGER,
                accountId TEXT,
                reversesOperationId TEXT,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(reversesOperationId) REFERENCES financial_operations(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )""".trimIndent(),
        )
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_financial_operations_idempotencyKey ON financial_operations(idempotencyKey)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_financial_operations_accountId ON financial_operations(accountId)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_financial_operations_reversesOperationId ON financial_operations(reversesOperationId)")

        database.execSQL(
            """CREATE TABLE IF NOT EXISTS ledger_entries (
                id TEXT NOT NULL PRIMARY KEY,
                operationId TEXT NOT NULL,
                accountId TEXT NOT NULL,
                side TEXT NOT NULL,
                amountToman INTEGER NOT NULL,
                FOREIGN KEY(operationId) REFERENCES financial_operations(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )""".trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_entries_operationId ON ledger_entries(operationId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_ledger_entries_accountId ON ledger_entries(accountId)")

        database.execSQL(
            """CREATE TABLE IF NOT EXISTS account_openings (
                accountId TEXT NOT NULL PRIMARY KEY,
                amountToman INTEGER NOT NULL,
                effectiveEpochDay INTEGER NOT NULL,
                operationId TEXT NOT NULL,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(operationId) REFERENCES financial_operations(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )""".trimIndent(),
        )
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_account_openings_operationId ON account_openings(operationId)")

        database.execSQL(
            """CREATE TABLE IF NOT EXISTS transactions (
                id TEXT NOT NULL PRIMARY KEY,
                operationId TEXT NOT NULL,
                accountId TEXT NOT NULL,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                amountToman INTEGER NOT NULL,
                category TEXT NOT NULL,
                createdAtMillis INTEGER NOT NULL,
                occurredOnEpochDay INTEGER,
                reversesTransactionId TEXT,
                FOREIGN KEY(operationId) REFERENCES financial_operations(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(accountId) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(reversesTransactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE RESTRICT
            )""".trimIndent(),
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_createdAtMillis ON transactions(createdAtMillis)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId ON transactions(accountId)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_operationId ON transactions(operationId)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_transactions_reversesTransactionId ON transactions(reversesTransactionId)")

        database.execSQL(
            """INSERT INTO financial_operations (
                id, idempotencyKey, kind, createdAtMillis, occurredOnEpochDay, accountId, reversesOperationId
            )
            SELECT 'legacy:' || id, 'legacy:' || id, type, occurredAtMillis, NULL,
                'legacy-unassigned', NULL
            FROM transactions_legacy_v1""".trimIndent(),
        )
        database.execSQL(
            """INSERT INTO transactions (
                id, operationId, accountId, type, title, amountToman, category,
                createdAtMillis, occurredOnEpochDay, reversesTransactionId
            )
            SELECT id, 'legacy:' || id, 'legacy-unassigned', type, title, amountToman,
                category, occurredAtMillis, NULL, NULL
            FROM transactions_legacy_v1""".trimIndent(),
        )
        database.execSQL(
            """INSERT INTO ledger_entries (id, operationId, accountId, side, amountToman)
            SELECT 'legacy-entry-debit:' || id, 'legacy:' || id,
                CASE WHEN type = 'INCOME' THEN 'legacy-unassigned' ELSE 'system-expense' END,
                'DEBIT', amountToman
            FROM transactions_legacy_v1""".trimIndent(),
        )
        database.execSQL(
            """INSERT INTO ledger_entries (id, operationId, accountId, side, amountToman)
            SELECT 'legacy-entry-credit:' || id, 'legacy:' || id,
                CASE WHEN type = 'INCOME' THEN 'system-income' ELSE 'legacy-unassigned' END,
                'CREDIT', amountToman
            FROM transactions_legacy_v1""".trimIndent(),
        )
    }
}