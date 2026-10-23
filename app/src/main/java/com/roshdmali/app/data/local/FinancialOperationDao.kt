package com.roshdmali.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FinancialOperationDao {
    @Query("SELECT * FROM financial_operations WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): FinancialOperationEntity?

    @Query("SELECT * FROM financial_operations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FinancialOperationEntity?

    @Query("SELECT * FROM financial_operations WHERE reversesOperationId = :operationId LIMIT 1")
    suspend fun getReversal(operationId: String): FinancialOperationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(operation: FinancialOperationEntity)
}
