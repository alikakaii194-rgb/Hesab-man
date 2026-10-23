package com.roshdmali.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["isSystem", "isActive"]),
        Index(value = ["idempotencyKey"], unique = true),
    ],
)
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val isSystem: Boolean,
    val isActive: Boolean,
    val createdAtMillis: Long?,
    val idempotencyKey: String?,
)
