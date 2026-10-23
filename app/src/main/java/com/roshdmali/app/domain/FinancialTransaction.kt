package com.roshdmali.app.domain

import java.math.BigInteger
import java.util.UUID

enum class TransactionType {
    INCOME,
    EXPENSE,
}

enum class AccountType {
    CASH,
    BANK,
    BANK_CARD,
}

enum class MoneyUnit {
    TOMAN,
    RIAL,
}

enum class LedgerSide {
    DEBIT,
    CREDIT,
}

data class LedgerPosting(
    val accountId: String,
    val side: LedgerSide,
    val amountToman: Long,
)

fun calculateAccountBalance(postings: Iterable<LedgerPosting>): BigInteger =
    postings.fold(BigInteger.ZERO) { balance, posting ->
        require(posting.amountToman > 0L)
        val amount = BigInteger.valueOf(posting.amountToman)
        balance + if (posting.side == LedgerSide.DEBIT) amount else -amount
    }

fun isLedgerBalanced(postings: Iterable<LedgerPosting>): Boolean {
    var debits = BigInteger.ZERO
    var credits = BigInteger.ZERO
    postings.forEach { posting ->
        if (posting.amountToman <= 0L) return false
        val amount = BigInteger.valueOf(posting.amountToman)
        when (posting.side) {
            LedgerSide.DEBIT -> debits += amount
            LedgerSide.CREDIT -> credits += amount
        }
    }
    return debits == credits
}

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
)

data class AccountBalance(
    val account: Account,
    val balanceToman: BigInteger,
)

data class AccountDraft(
    val name: String,
    val type: AccountType,
    val openingAmountToman: Long,
    val effectiveEpochDay: Long,
    val idempotencyKey: String = UUID.randomUUID().toString(),
)

data class FinancialTransaction(
    val id: String,
    val operationId: String,
    val accountId: String,
    val type: TransactionType,
    val title: String,
    val amountToman: Long,
    val category: String,
    val createdAtMillis: Long,
    val occurredOnEpochDay: Long?,
    val reversesTransactionId: String?,
)

data class TransactionDraft(
    val accountId: String,
    val type: TransactionType,
    val title: String,
    val amountToman: Long,
    val category: String,
    val occurredOnEpochDay: Long,
    val idempotencyKey: String = UUID.randomUUID().toString(),
)

data class FinancialSummary(
    val totalIncomeToman: BigInteger,
    val totalExpenseToman: BigInteger,
) {
    val balanceToman: BigInteger
        get() = totalIncomeToman - totalExpenseToman
}

fun summarizeTransactions(transactions: List<FinancialTransaction>): FinancialSummary {
    var income = BigInteger.ZERO
    var expense = BigInteger.ZERO

    transactions.forEach { transaction ->
        val amount = BigInteger.valueOf(transaction.amountToman)
        when (transaction.type) {
            TransactionType.INCOME -> income += amount
            TransactionType.EXPENSE -> expense += amount
        }
    }

    return FinancialSummary(
        totalIncomeToman = income,
        totalExpenseToman = expense,
    )
}

fun parseTomanAmount(input: String, allowZero: Boolean = false): Long? =
    parseUnsignedInteger(input)?.takeIf { if (allowZero) true else it > 0L }

fun parseRialAmount(input: String, allowZero: Boolean = false): Long? {
    val rialAmount = parseUnsignedInteger(input) ?: return null
    if (rialAmount % 10L != 0L) return null
    val tomanAmount = rialAmount / 10L
    return tomanAmount.takeIf { if (allowZero) true else it > 0L }
}

fun parseUnsignedInteger(input: String): Long? {
    val normalized = buildString(input.length) {
        input.forEach { character ->
            when (character) {
                in '۰'..'۹' -> append(character.code - '۰'.code)
                in '٠'..'٩' -> append(character.code - '٠'.code)
                ',', '٬', ' ', '\u00a0' -> Unit
                else -> append(character)
            }
        }
    }

    if (normalized.isEmpty() || normalized.any { !it.isDigit() }) return null
    return normalized.toLongOrNull()
}

fun formatTomanAmount(amount: BigInteger): String {
    val grouped = amount.abs().toString().reversed().chunked(3).joinToString(",").reversed()
    val signed = if (amount.signum() < 0) "-$grouped" else grouped
    return signed.map { character ->
        if (character in '0'..'9') (character.code + ('۰'.code - '0'.code)).toChar() else character
    }.joinToString("")
}