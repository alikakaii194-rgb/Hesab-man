package com.roshdmali.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LedgerEntryDao {
    @Query("SELECT * FROM ledger_entries")
    fun observeAll(): Flow<List<LedgerEntryEntity>>

    @Query(
        """SELECT ledger_entries.* FROM ledger_entries
            INNER JOIN financial_operations ON financial_operations.id = ledger_entries.operationId
            WHERE financial_operations.occurredOnEpochDay IS NULL
                OR financial_operations.occurredOnEpochDay <= :epochDay""",
    )
    fun observeEntriesThrough(epochDay: Long): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries WHERE operationId = :operationId")
    suspend fun getForOperation(operationId: String): List<LedgerEntryEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entries: List<LedgerEntryEntity>)
}