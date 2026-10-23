package com.roshdmali.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AccountEntity::class,
        AccountOpeningEntity::class,
        FinancialOperationEntity::class,
        LedgerEntryEntity::class,
        TransactionEntity::class,
        LegacyTransactionV1Entity::class,
        CategoryEntity::class,
        CounterpartyEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class RoshdMaliDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun financialOperationDao(): FinancialOperationDao
    abstract fun ledgerEntryDao(): LedgerEntryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun counterpartyDao(): CounterpartyDao

    companion object {
        @Volatile
        private var instance: RoshdMaliDatabase? = null

        fun getInstance(context: Context): RoshdMaliDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RoshdMaliDatabase::class.java,
                    "roshd-mali.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}