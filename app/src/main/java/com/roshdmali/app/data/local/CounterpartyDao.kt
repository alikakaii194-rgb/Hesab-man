package com.roshdmali.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterpartyDao {
    @Query("SELECT * FROM counterparties ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CounterpartyEntity>>

    @Query("SELECT * FROM counterparties WHERE isActive = 1 ORDER BY name COLLATE NOCASE")
    fun observeActive(): Flow<List<CounterpartyEntity>>

    @Query("SELECT * FROM counterparties WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CounterpartyEntity?

    @Query("SELECT * FROM counterparties WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun getByName(normalizedName: String): CounterpartyEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(counterparty: CounterpartyEntity)

    @Update
    suspend fun update(counterparty: CounterpartyEntity)
}