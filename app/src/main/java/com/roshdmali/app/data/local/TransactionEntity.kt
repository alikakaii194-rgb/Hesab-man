package com.roshdmali.app.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
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
            childColumns = ["operationId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["reversesTransactionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CounterpartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["counterpartyId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["createdAtMillis"]),
        Index(value = ["accountId"]),
        Index(value = ["operationId"], unique = true),
        Index(value = ["reversesTransactionId"], unique = true),
        Index(value = ["categoryId"]),
        Index(value = ["counterpartyId"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val operationId: String,
    val accountId: String,
    val type: String,
    val title: String,
    val amountToman: Long,
    val category: String,
    val createdAtMillis: Long,
    val occurredOnEpochDay: Long?,
    val reversesTransactionId: String?,
    val categoryId: String?,
    val counterpartyId: String?,
    val counterpartyNameSnapshot: String?,
    val notes: String?,
)

@Entity(
    tableName = "transactions_legacy_v1",
    indices = [Index(value = ["occurredAtMillis"])],
)
data class LegacyTransactionV1Entity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val amountToman: Long,
    val category: String,
    val occurredAtMillis: Long,
)