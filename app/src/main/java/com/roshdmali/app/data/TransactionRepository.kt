package com.roshdmali.app.data

import androidx.room.withTransaction
import com.roshdmali.app.data.local.AccountEntity
import com.roshdmali.app.data.local.AccountOpeningEntity
import com.roshdmali.app.data.local.CategoryEntity
import com.roshdmali.app.data.local.CounterpartyEntity
import com.roshdmali.app.data.local.TransactionDao
import com.roshdmali.app.data.local.TransactionEntity
import com.roshdmali.app.data.local.FinancialOperationEntity
import com.roshdmali.app.data.local.LedgerEntryEntity
import com.roshdmali.app.data.local.RoshdMaliDatabase
import com.roshdmali.app.domain.Account
import com.roshdmali.app.domain.AccountBalance
import com.roshdmali.app.domain.AccountDraft
import com.roshdmali.app.domain.AccountType
import com.roshdmali.app.domain.CategoryDraft
import com.roshdmali.app.domain.Counterparty
import com.roshdmali.app.domain.CounterpartyDraft
import com.roshdmali.app.domain.FinancialTransaction
import com.roshdmali.app.domain.LedgerPosting
import com.roshdmali.app.domain.LedgerSide
import com.roshdmali.app.domain.TransactionDraft
import com.roshdmali.app.domain.TransactionType
import com.roshdmali.app.domain.TransactionCategory
import com.roshdmali.app.domain.calculateAccountBalance
import com.roshdmali.app.domain.isLedgerBalanced
import java.math.BigInteger
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface TransactionRepository {
    fun observeTransactions(): Flow<List<FinancialTransaction>>
    fun observeAccountBalances(): Flow<List<AccountBalance>>
    suspend fun getTransactions(): List<FinancialTransaction>
    suspend fun createAccount(draft: AccountDraft): Account
    fun observeCategories(type: TransactionType? = null): Flow<List<TransactionCategory>>
    fun observeCounterparties(): Flow<List<Counterparty>>
    suspend fun createCategory(draft: CategoryDraft): TransactionCategory
    suspend fun renameCategory(categoryId: String, name: String): TransactionCategory
    suspend fun setCategoryActive(categoryId: String, isActive: Boolean)
    suspend fun createCounterparty(draft: CounterpartyDraft): Counterparty
    suspend fun renameCounterparty(counterpartyId: String, name: String): Counterparty
    suspend fun setCounterpartyActive(counterpartyId: String, isActive: Boolean)
    suspend fun addTransaction(draft: TransactionDraft): FinancialTransaction
    suspend fun reverseTransaction(transactionId: String, idempotencyKey: String): FinancialTransaction
}

class RoomTransactionRepository(
    private val database: RoshdMaliDatabase,
) : TransactionRepository {
    private val transactionDao: TransactionDao = database.transactionDao()
    private val accountDao = database.accountDao()
    private val categoryDao = database.categoryDao()
    private val counterpartyDao = database.counterpartyDao()
    private val operationDao = database.financialOperationDao()
    private val ledgerEntryDao = database.ledgerEntryDao()

    override fun observeTransactions(): Flow<List<FinancialTransaction>> =
        transactionDao.observeAll().map { rows -> rows.map(TransactionEntity::toDomain) }

    override fun observeAccountBalances(): Flow<List<AccountBalance>> = combine(
        accountDao.observeUserAccounts(),
        ledgerEntryDao.observeEntriesThrough(LocalDate.now().toEpochDay()),
    ) { accounts, entries ->
        accounts.map { entity ->
            val postings = entries.asSequence()
                .filter { it.accountId == entity.id }
                .map { entry ->
                    LedgerPosting(
                        accountId = entry.accountId,
                        side = LedgerSide.valueOf(entry.side),
                        amountToman = entry.amountToman,
                    )
                }
                .toList()
            val balance = calculateAccountBalance(postings)
            AccountBalance(entity.toDomain(), balance)
        }
    }

    override suspend fun getTransactions(): List<FinancialTransaction> =
        transactionDao.getAll().map(TransactionEntity::toDomain)

    override fun observeCategories(type: TransactionType?): Flow<List<TransactionCategory>> =
        categoryDao.observeAll()
            .map { rows ->
                rows.asSequence()
                    .filter { type == null || it.transactionType == type.name }
                    .map(CategoryEntity::toDomain)
                    .toList()
            }

    override fun observeCounterparties(): Flow<List<Counterparty>> =
        counterpartyDao.observeAll().map { rows -> rows.map(CounterpartyEntity::toDomain) }

    override suspend fun createCategory(draft: CategoryDraft): TransactionCategory = database.withTransaction {
        val name = normalizeMetadataName(draft.name)
        require(name.isNotBlank()) { "Category name is required." }
        require(categoryDao.getByName(draft.transactionType.name, name) == null) {
            "A category with this name and transaction type already exists."
        }
        val category = CategoryEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            normalizedName = name.lowercase(Locale.ROOT),
            transactionType = draft.transactionType.name,
            isActive = true,
            createdAtMillis = System.currentTimeMillis(),
        )
        categoryDao.insert(category)
        category.toDomain()
    }

    override suspend fun renameCategory(categoryId: String, name: String): TransactionCategory = database.withTransaction {
        val current = requireNotNull(categoryDao.getById(categoryId)) { "Category does not exist." }
        val cleanName = normalizeMetadataName(name)
        require(cleanName.isNotBlank()) { "Category name is required." }
        val duplicate = categoryDao.getByName(current.transactionType, cleanName.lowercase(Locale.ROOT))
        require(duplicate == null || duplicate.id == categoryId) {
            "A category with this name and transaction type already exists."
        }
        val updated = current.copy(name = cleanName, normalizedName = cleanName.lowercase(Locale.ROOT))
        categoryDao.update(updated)
        updated.toDomain()
    }

    override suspend fun setCategoryActive(categoryId: String, isActive: Boolean) = database.withTransaction {
        val current = requireNotNull(categoryDao.getById(categoryId)) { "Category does not exist." }
        categoryDao.update(current.copy(isActive = isActive))
    }

    override suspend fun createCounterparty(draft: CounterpartyDraft): Counterparty = database.withTransaction {
        val name = normalizeMetadataName(draft.name)
        require(name.isNotBlank()) { "Counterparty name is required." }
        val normalizedName = name.lowercase(Locale.ROOT)
        require(counterpartyDao.getByName(normalizedName) == null) {
            "A counterparty with this name already exists."
        }
        val counterparty = CounterpartyEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            normalizedName = normalizedName,
            isActive = true,
            createdAtMillis = System.currentTimeMillis(),
        )
        counterpartyDao.insert(counterparty)
        counterparty.toDomain()
    }

    override suspend fun renameCounterparty(counterpartyId: String, name: String): Counterparty = database.withTransaction {
        val current = requireNotNull(counterpartyDao.getById(counterpartyId)) { "Counterparty does not exist." }
        val cleanName = normalizeMetadataName(name)
        require(cleanName.isNotBlank()) { "Counterparty name is required." }
        val normalizedName = cleanName.lowercase(Locale.ROOT)
        val duplicate = counterpartyDao.getByName(normalizedName)
        require(duplicate == null || duplicate.id == counterpartyId) {
            "A counterparty with this name already exists."
        }
        val updated = current.copy(name = cleanName, normalizedName = normalizedName)
        counterpartyDao.update(updated)
        updated.toDomain()
    }

    override suspend fun setCounterpartyActive(counterpartyId: String, isActive: Boolean) = database.withTransaction {
        val current = requireNotNull(counterpartyDao.getById(counterpartyId)) { "Counterparty does not exist." }
        counterpartyDao.update(current.copy(isActive = isActive))
    }

    override suspend fun createAccount(draft: AccountDraft): Account = database.withTransaction {
        require(draft.name.isNotBlank())
        require(draft.openingAmountToman >= 0L)
        ensureSystemAccounts()

        operationDao.getByIdempotencyKey(draft.idempotencyKey)?.let { existingOperation ->
            require(existingOperation.kind == ACCOUNT_OPENING_KIND) {
                "Idempotency key has already been used for another operation."
            }
            val existingAccount = requireNotNull(accountDao.getById(requireNotNull(existingOperation.accountId)))
            val existingOpening = requireNotNull(accountDao.getOpening(existingAccount.id))
            require(
                existingAccount.name == draft.name.trim() &&
                    existingAccount.type == draft.type.name &&
                    existingOpening.amountToman == draft.openingAmountToman &&
                    existingOpening.effectiveEpochDay == draft.effectiveEpochDay
            ) { "Idempotency key was reused with different account data." }
            return@withTransaction existingAccount.toDomain()
        }

        val accountId = UUID.randomUUID().toString()
        val operationId = UUID.randomUUID().toString()
        val account = AccountEntity(
            id = accountId,
            name = draft.name.trim(),
            type = draft.type.name,
            isSystem = false,
            isActive = true,
            createdAtMillis = System.currentTimeMillis(),
            idempotencyKey = "account:${draft.idempotencyKey}",
        )
        accountDao.insert(account)
        operationDao.insert(
            FinancialOperationEntity(
                id = operationId,
                idempotencyKey = draft.idempotencyKey,
                kind = ACCOUNT_OPENING_KIND,
                createdAtMillis = System.currentTimeMillis(),
                occurredOnEpochDay = draft.effectiveEpochDay,
                accountId = accountId,
                reversesOperationId = null,
            ),
        )
        accountDao.insertOpening(
            AccountOpeningEntity(
                accountId = accountId,
                amountToman = draft.openingAmountToman,
                effectiveEpochDay = draft.effectiveEpochDay,
                operationId = operationId,
            ),
        )
        if (draft.openingAmountToman > 0L) {
            insertBalancedEntries(
                operationId = operationId,
                entries = listOf(
                    LedgerEntryEntity(
                        id = UUID.randomUUID().toString(),
                        operationId = operationId,
                        accountId = accountId,
                        side = LEDGER_DEBIT,
                        amountToman = draft.openingAmountToman,
                    ),
                    LedgerEntryEntity(
                        id = UUID.randomUUID().toString(),
                        operationId = operationId,
                        accountId = SYSTEM_OPENING_EQUITY_ID,
                        side = LEDGER_CREDIT,
                        amountToman = draft.openingAmountToman,
                    ),
                ),
            )
        }
        account.toDomain()
    }

    override suspend fun addTransaction(draft: TransactionDraft): FinancialTransaction = database.withTransaction {
        require(draft.title.isNotBlank())
        require(draft.categoryId != null || draft.category.isNotBlank())
        require(draft.amountToman > 0L)
        require(draft.accountId.isNotBlank())
        ensureSystemAccounts()

        operationDao.getByIdempotencyKey(draft.idempotencyKey)?.let { existingOperation ->
            require(existingOperation.kind == draft.type.name) {
                "Idempotency key has already been used for another operation."
            }
            val existing = requireNotNull(
                transactionDao.getByOperationId(existingOperation.id),
            )
            require(
                existing.accountId == draft.accountId &&
                    existing.type == draft.type.name &&
                    existing.title == draft.title.trim() &&
                    existing.amountToman == draft.amountToman &&
                    existing.category == draft.category.trim() &&
                    existing.categoryId == draft.categoryId &&
                    existing.counterpartyId == draft.counterpartyId &&
                    existing.counterpartyNameSnapshot == draft.counterpartyNameSnapshot &&
                    existing.notes == draft.notes?.trim()?.ifBlank { null } &&
                    existing.occurredOnEpochDay == draft.occurredOnEpochDay
            ) { "Idempotency key was reused with different transaction data." }
            return@withTransaction existing.toDomain()
        }

        val account = requireNotNull(accountDao.getById(draft.accountId)) {
            "Selected account does not exist."
        }
        require(!account.isSystem && account.isActive) { "Selected account is unavailable." }
        val category = draft.categoryId?.let { categoryId ->
            requireNotNull(categoryDao.getById(categoryId)) { "Category does not exist." }.also {
                require(it.isActive && it.transactionType == draft.type.name) {
                    "Category is inactive or has a different transaction type."
                }
            }
        }
        val counterparty = draft.counterpartyId?.let { counterpartyId ->
            requireNotNull(counterpartyDao.getById(counterpartyId)) { "Counterparty does not exist." }.also {
                require(it.isActive) { "Counterparty is inactive." }
            }
        }

        val createdAtMillis = System.currentTimeMillis()
        val operationId = UUID.randomUUID().toString()
        val transactionId = UUID.randomUUID().toString()
        operationDao.insert(
            FinancialOperationEntity(
                id = operationId,
                idempotencyKey = draft.idempotencyKey,
                kind = draft.type.name,
                createdAtMillis = createdAtMillis,
                occurredOnEpochDay = draft.occurredOnEpochDay,
                accountId = draft.accountId,
                reversesOperationId = null,
            ),
        )

        val transaction = TransactionEntity(
            id = transactionId,
            operationId = operationId,
            accountId = draft.accountId,
            type = draft.type.name,
            title = draft.title.trim(),
            amountToman = draft.amountToman,
            category = category?.name ?: draft.category.trim(),
            createdAtMillis = createdAtMillis,
            occurredOnEpochDay = draft.occurredOnEpochDay,
            reversesTransactionId = null,
            categoryId = category?.id,
            counterpartyId = counterparty?.id,
            counterpartyNameSnapshot = counterparty?.name ?: draft.counterpartyNameSnapshot?.trim()?.ifBlank { null },
            notes = draft.notes?.trim()?.ifBlank { null },
        )
        transactionDao.insert(transaction)
        val ledgerEntries = when (draft.type) {
            TransactionType.INCOME -> listOf(
                ledgerEntry(operationId, draft.accountId, LEDGER_DEBIT, draft.amountToman),
                ledgerEntry(operationId, SYSTEM_INCOME_ID, LEDGER_CREDIT, draft.amountToman),
            )
            TransactionType.EXPENSE -> listOf(
                ledgerEntry(operationId, SYSTEM_EXPENSE_ID, LEDGER_DEBIT, draft.amountToman),
                ledgerEntry(operationId, draft.accountId, LEDGER_CREDIT, draft.amountToman),
            )
        }
        insertBalancedEntries(operationId, ledgerEntries)
        transaction.toDomain()
    }

    override suspend fun reverseTransaction(
        transactionId: String,
        idempotencyKey: String,
    ): FinancialTransaction = database.withTransaction {
        require(idempotencyKey.isNotBlank())
        val original = requireNotNull(transactionDao.getById(transactionId)) {
            "Transaction does not exist."
        }
        require(original.reversesTransactionId == null) {
            "A reversal transaction cannot itself be reversed."
        }
        operationDao.getByIdempotencyKey(idempotencyKey)?.let { existingOperation ->
            require(existingOperation.kind == REVERSAL_KIND) {
                "Idempotency key has already been used for another operation."
            }
            require(existingOperation.reversesOperationId == original.operationId) {
                "Idempotency key was reused for another reversal."
            }
            return@withTransaction requireNotNull(
                transactionDao.getByOperationId(existingOperation.id),
            ).toDomain()
        }

        val originalOperation = requireNotNull(operationDao.getById(original.operationId))
        require(operationDao.getReversal(original.operationId) == null) {
            "Transaction has already been reversed."
        }
        val todayEpochDay = LocalDate.now().toEpochDay()
        val reversalEpochDay = maxOf(original.occurredOnEpochDay ?: todayEpochDay, todayEpochDay)
        val originalEntries = ledgerEntryDao.getForOperation(original.operationId)
        require(originalEntries.isNotEmpty()) { "Transaction has no ledger entries to reverse." }
        require(isLedgerBalanced(originalEntries.map { entry ->
            LedgerPosting(entry.accountId, LedgerSide.valueOf(entry.side), entry.amountToman)
        })) { "Original transaction ledger is not balanced." }

        val createdAtMillis = System.currentTimeMillis()
        val reversalOperationId = UUID.randomUUID().toString()
        val reversalTransactionId = UUID.randomUUID().toString()
        operationDao.insert(
            FinancialOperationEntity(
                id = reversalOperationId,
                idempotencyKey = idempotencyKey,
                kind = REVERSAL_KIND,
                createdAtMillis = createdAtMillis,
                occurredOnEpochDay = reversalEpochDay,
                accountId = originalOperation.accountId,
                reversesOperationId = original.operationId,
            ),
        )
        transactionDao.insert(
            TransactionEntity(
                id = reversalTransactionId,
                operationId = reversalOperationId,
                accountId = original.accountId,
                type = if (original.type == TransactionType.INCOME.name) {
                    TransactionType.EXPENSE.name
                } else {
                    TransactionType.INCOME.name
                },
                title = "لغو: ${original.title}",
                amountToman = original.amountToman,
                category = original.category,
                createdAtMillis = createdAtMillis,
                occurredOnEpochDay = reversalEpochDay,
                reversesTransactionId = original.id,
                categoryId = original.categoryId,
                counterpartyId = original.counterpartyId,
                counterpartyNameSnapshot = original.counterpartyNameSnapshot,
                notes = original.notes,
            ),
        )
        val reversalEntries = originalEntries.map { entry ->
            LedgerEntryEntity(
                id = UUID.randomUUID().toString(),
                operationId = reversalOperationId,
                accountId = entry.accountId,
                side = if (entry.side == LEDGER_DEBIT) LEDGER_CREDIT else LEDGER_DEBIT,
                amountToman = entry.amountToman,
            )
        }
        insertBalancedEntries(reversalOperationId, reversalEntries)
        require(originalEntries.sumOf { it.amountToman } == reversalEntries.sumOf { it.amountToman })
        transactionDao.getById(reversalTransactionId).let(::requireNotNull).toDomain()
    }

    private suspend fun ensureSystemAccounts() {
        listOf(
            AccountEntity(SYSTEM_INCOME_ID, "درآمد", "SYSTEM_INCOME", true, true, null, null),
            AccountEntity(SYSTEM_EXPENSE_ID, "هزینه", "SYSTEM_EXPENSE", true, true, null, null),
            AccountEntity(SYSTEM_OPENING_EQUITY_ID, "موجودی اولیه", "SYSTEM_OPENING_EQUITY", true, true, null, null),
        ).forEach { account ->
            if (accountDao.getById(account.id) == null) accountDao.insert(account)
        }
    }

    private suspend fun insertBalancedEntries(operationId: String, entries: List<LedgerEntryEntity>) {
        require(entries.all { it.operationId == operationId && it.amountToman > 0L })
        require(
            isLedgerBalanced(entries.map { entry ->
                LedgerPosting(entry.accountId, LedgerSide.valueOf(entry.side), entry.amountToman)
            }),
        ) { "Ledger operation is not balanced." }
        ledgerEntryDao.insertAll(entries)
    }

    private fun ledgerEntry(
        operationId: String,
        accountId: String,
        side: String,
        amountToman: Long,
    ) = LedgerEntryEntity(
        id = UUID.randomUUID().toString(),
        operationId = operationId,
        accountId = accountId,
        side = side,
        amountToman = amountToman,
    )

    private companion object {
        const val ACCOUNT_OPENING_KIND = "ACCOUNT_OPENING"
        const val REVERSAL_KIND = "REVERSAL"
        const val SYSTEM_INCOME_ID = "system-income"
        const val SYSTEM_EXPENSE_ID = "system-expense"
        const val SYSTEM_OPENING_EQUITY_ID = "system-opening-equity"
        const val LEDGER_DEBIT = "DEBIT"
        const val LEDGER_CREDIT = "CREDIT"
    }
}

private val METADATA_WHITESPACE = Regex("\\s+")

private fun normalizeMetadataName(value: String): String =
    value.trim().replace(METADATA_WHITESPACE, " ")

private fun TransactionEntity.toDomain(): FinancialTransaction = FinancialTransaction(
    id = id,
    operationId = operationId,
    accountId = accountId,
    type = TransactionType.valueOf(type),
    title = title,
    amountToman = amountToman,
    category = category,
    createdAtMillis = createdAtMillis,
    occurredOnEpochDay = occurredOnEpochDay,
    reversesTransactionId = reversesTransactionId,
    categoryId = categoryId,
    counterpartyId = counterpartyId,
    counterpartyNameSnapshot = counterpartyNameSnapshot,
    notes = notes,
)

private fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
)

private fun CategoryEntity.toDomain(): TransactionCategory = TransactionCategory(
    id = id,
    name = name,
    transactionType = TransactionType.valueOf(transactionType),
    isActive = isActive,
)

private fun CounterpartyEntity.toDomain(): Counterparty = Counterparty(
    id = id,
    name = name,
    isActive = isActive,
)