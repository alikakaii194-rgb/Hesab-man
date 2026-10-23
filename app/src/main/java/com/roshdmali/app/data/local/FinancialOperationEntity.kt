package com.roshdmali.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "financial_operations",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = FinancialOperationEntity::class,
            parentColumns = ["id"],
            childColumns = ["reversesOperationId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["accountId"]),
        Index(value = ["reversesOperationId"], unique = true),
    ],
)
data class FinancialOperationEntity(
    @PrimaryKey val id: String,
    val idempotencyKey: String,
    val kind: String,
    val createdAtMillis: Long,
    val occurredOnEpochDay: Long?,
    val accountId: String?,
    val reversesOperationId: String?,
)
