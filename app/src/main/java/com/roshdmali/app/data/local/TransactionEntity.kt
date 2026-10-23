package com.roshdmali.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["occurredAtMillis"])],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val amountToman: Long,
    val category: String,
    val occurredAtMillis: Long,
)