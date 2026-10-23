package com.roshdmali.app.domain

import java.math.BigInteger

enum class TransactionType {
    INCOME,
    EXPENSE,
}

data class FinancialTransaction(
    val id: String,
    val type: TransactionType,
    val title: String,
    val amountToman: Long,
    val category: String,
    val occurredAtMillis: Long,
)

data class TransactionDraft(
    val type: TransactionType,
    val title: String,
    val amountToman: Long,
    val category: String,
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

fun parseTomanAmount(input: String): Long? {
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
    return normalized.toLongOrNull()?.takeIf { it > 0L }
}

fun formatTomanAmount(amount: BigInteger): String {
    val grouped = amount.abs().toString().reversed().chunked(3).joinToString(",").reversed()
    val signed = if (amount.signum() < 0) "-$grouped" else grouped
    return signed.map { character ->
        if (character in '0'..'9') (character.code + ('۰'.code - '0'.code)).toChar() else character
    }.joinToString("")
}