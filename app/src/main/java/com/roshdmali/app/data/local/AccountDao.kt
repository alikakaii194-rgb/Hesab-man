package com.roshdmali.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE isSystem = 0 AND isActive = 1 ORDER BY createdAtMillis, id")
    fun observeUserAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM account_openings")
    fun observeOpenings(): Flow<List<AccountOpeningEntity>>

    @Query("SELECT * FROM account_openings WHERE accountId = :accountId LIMIT 1")
    suspend fun getOpening(accountId: String): AccountOpeningEntity?

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOpening(opening: AccountOpeningEntity)
}
