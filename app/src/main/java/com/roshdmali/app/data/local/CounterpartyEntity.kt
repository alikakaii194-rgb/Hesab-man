package com.roshdmali.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "counterparties",
    indices = [Index(value = ["normalizedName"], unique = true)],
)
data class CounterpartyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val isActive: Boolean,
    val createdAtMillis: Long?,
)