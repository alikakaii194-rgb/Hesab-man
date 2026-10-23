package com.roshdmali.app.data

import com.roshdmali.app.data.local.TransactionDao
import com.roshdmali.app.data.local.TransactionEntity
import com.roshdmali.app.domain.FinancialTransaction
import com.roshdmali.app.domain.TransactionDraft
import com.roshdmali.app.domain.TransactionType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TransactionRepository {
    fun observeTransactions(): Flow<List<FinancialTransaction>>
    suspend fun getTransactions(): List<FinancialTransaction>
    suspend fun addTransaction(draft: TransactionDraft)
}

class RoomTransactionRepository(
    private val transactionDao: TransactionDao,
) : TransactionRepository {
    override fun observeTransactions(): Flow<List<FinancialTransaction>> =
        transactionDao.observeAll().map { rows -> rows.map(TransactionEntity::toDomain) }

    override suspend fun getTransactions(): List<FinancialTransaction> =
        transactionDao.getAll().map(TransactionEntity::toDomain)

    override suspend fun addTransaction(draft: TransactionDraft) {
        require(draft.title.isNotBlank())
        require(draft.category.isNotBlank())
        require(draft.amountToman > 0L)

        transactionDao.insert(
            TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = draft.type.name,
                title = draft.title.trim(),
                amountToman = draft.amountToman,
                category = draft.category.trim(),
                occurredAtMillis = System.currentTimeMillis(),
            ),
        )
    }
}

private fun TransactionEntity.toDomain(): FinancialTransaction = FinancialTransaction(
    id = id,
    type = TransactionType.valueOf(type),
    title = title,
    amountToman = amountToman,
    category = category,
    occurredAtMillis = occurredAtMillis,
)