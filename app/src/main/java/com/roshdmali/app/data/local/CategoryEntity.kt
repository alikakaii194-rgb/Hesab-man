package com.roshdmali.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["transactionType", "normalizedName"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val normalizedName: String,
    val transactionType: String,
    val isActive: Boolean,
    val createdAtMillis: Long?,
)