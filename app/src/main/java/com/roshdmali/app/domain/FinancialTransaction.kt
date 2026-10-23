package com.roshdmali.app.domain

import java.math.BigInteger
import java.util.Locale
import java.util.UUID

enum class TransactionType {
    INCOME,
    EXPENSE,
}

data class TransactionCategory(
    val id: String,
    val name: String,
    val transactionType: TransactionType,
    val isActive: Boolean,
)

data class Counterparty(
    val id: String,
    val name: String,
    val isActive: Boolean,
)

data class TransactionFilters(
    val query: String = "",
    val type: TransactionType? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val counterpartyId: String? = null,
    val startEpochDay: Long? = null,
    val endEpochDay: Long? = null,
)

data class AmountInputFormatting(
    val text: String,
    val cursorPosition: Int,
)

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
    val categoryId: String? = null,
    val counterpartyId: String? = null,
    val counterpartyNameSnapshot: String? = null,
    val notes: String? = null,
)

data class TransactionDraft(
    val accountId: String,
    val type: TransactionType,
    val title: String,
    val amountToman: Long,
    val category: String,
    val occurredOnEpochDay: Long,
    val idempotencyKey: String = UUID.randomUUID().toString(),
    val categoryId: String? = null,
    val counterpartyId: String? = null,
    val counterpartyNameSnapshot: String? = null,
    val notes: String? = null,
)

data class CategoryDraft(
    val name: String,
    val transactionType: TransactionType,
)

data class CounterpartyDraft(
    val name: String,
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
    val byId = transactions.associateBy(FinancialTransaction::id)

    transactions.forEach { transaction ->
        val amount = BigInteger.valueOf(transaction.amountToman)
        val reversedTransactionId = transaction.reversesTransactionId
        if (reversedTransactionId != null) {
            val original = requireNotNull(byId[reversedTransactionId]) {
                "Reversal transaction references a missing original transaction."
            }
            when (original.type) {
                TransactionType.INCOME -> income -= amount
                TransactionType.EXPENSE -> expense -= amount
            }
            return@forEach
        }
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

fun filterTransactions(
    transactions: List<FinancialTransaction>,
    filters: TransactionFilters,
): List<FinancialTransaction> {
    val query = filters.query.trim().lowercase(Locale.ROOT)
    return transactions.filter { transaction ->
        val matchesQuery = query.isEmpty() || listOfNotNull(
            transaction.title,
            transaction.category,
            transaction.counterpartyNameSnapshot,
            transaction.notes,
        ).any { it.lowercase(Locale.ROOT).contains(query) }
        val matchesDate = if (filters.startEpochDay == null && filters.endEpochDay == null) {
            true
        } else {
            transaction.occurredOnEpochDay?.let { day ->
                (filters.startEpochDay == null || day >= filters.startEpochDay) &&
                    (filters.endEpochDay == null || day <= filters.endEpochDay)
            } ?: false
        }
        matchesQuery &&
            (filters.type == null || transaction.type == filters.type) &&
            (filters.accountId == null || transaction.accountId == filters.accountId) &&
            (filters.categoryId == null || transaction.categoryId == filters.categoryId) &&
            (filters.counterpartyId == null || transaction.counterpartyId == filters.counterpartyId) &&
            matchesDate
    }
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
    val source = input.trim()
    if (source.isEmpty()) return null
    val digits = StringBuilder(source.length)
    var groupLength = 0
    var hasGrouping = false
    var previousWasSeparator = false

    source.forEach { character ->
        val normalizedDigit = when (character) {
            in '۰'..'۹' -> character.code - '۰'.code
            in '٠'..'٩' -> character.code - '٠'.code
            in '0'..'9' -> character.code - '0'.code
            else -> null
        }
        if (normalizedDigit != null) {
            digits.append(('0'.code + normalizedDigit).toChar())
            groupLength += 1
            previousWasSeparator = false
        } else if (character == ',' || character == '٬' || character == ' ' || character == '\u00a0') {
            if (previousWasSeparator) return null
            if (!hasGrouping) {
                if (groupLength !in 1..3) return null
                hasGrouping = true
            } else if (groupLength != 3) {
                return null
            }
            groupLength = 0
            previousWasSeparator = true
        } else {
            return null
        }
    }

    if (digits.isEmpty() || (hasGrouping && groupLength != 3)) return null
    return digits.toString().toLongOrNull()
}

fun formatAmountInput(input: String, cursorPosition: Int): AmountInputFormatting {
    val parsed = parseUnsignedInteger(input)
    if (parsed == null) return AmountInputFormatting(input, cursorPosition.coerceIn(0, input.length))

    val normalizedDigits = buildString(input.length) {
        input.forEach { character ->
            when (character) {
                in '۰'..'۹' -> append(('0'.code + character.code - '۰'.code).toChar())
                in '٠'..'٩' -> append(('0'.code + character.code - '٠'.code).toChar())
                in '0'..'9' -> append(character)
            }
        }
    }
    val formatted = normalizedDigits.reversed().chunked(3).joinToString(",").reversed()
    val digitsBeforeCursor = input.take(cursorPosition.coerceIn(0, input.length)).count(Char::isDigit)
    var formattedCursor = 0
    var countedDigits = 0
    while (formattedCursor < formatted.length && countedDigits < digitsBeforeCursor) {
        if (formatted[formattedCursor].isDigit()) countedDigits += 1
        formattedCursor += 1
    }
    if (digitsBeforeCursor == 0 && formattedCursor < formatted.length && formattedCursor > 0 && formatted[formattedCursor - 1] == ',') {
        formattedCursor -= 1
    }
    return AmountInputFormatting(formatted, formattedCursor)
}

fun tomanAmountInPersianWords(amountToman: Long): String? {
    if (amountToman <= 0L) return null
    val scales = listOf("", "هزار", "میلیون", "میلیارد", "هزار میلیارد", "میلیون میلیارد", "میلیارد میلیارد")
    var remaining = amountToman
    var scaleIndex = 0
    val parts = mutableListOf<String>()
    while (remaining > 0L) {
        val group = (remaining % 1000L).toInt()
        if (group != 0) {
            val scale = scales.getOrNull(scaleIndex) ?: return null
            val groupWords = numberUnderOneThousandInPersian(group)
            parts += listOf(groupWords, scale).filter(String::isNotEmpty).joinToString(" ")
        }
        remaining /= 1000L
        scaleIndex += 1
    }
    return parts.asReversed().joinToString(" و ") + " تومان"
}

private fun numberUnderOneThousandInPersian(number: Int): String {
    val hundreds = listOf("", "صد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    val teens = listOf("ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
    val tens = listOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    val units = listOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    val parts = mutableListOf<String>()
    val remainderAfterHundreds = number % 100
    if (number / 100 > 0) parts += hundreds[number / 100]
    when {
        remainderAfterHundreds in 10..19 -> parts += teens[remainderAfterHundreds - 10]
        remainderAfterHundreds >= 20 -> {
            parts += tens[remainderAfterHundreds / 10]
            if (remainderAfterHundreds % 10 > 0) parts += units[remainderAfterHundreds % 10]
        }
        remainderAfterHundreds > 0 -> parts += units[remainderAfterHundreds]
    }
    return parts.joinToString(" و ")
}

fun formatTomanAmount(amount: BigInteger): String {
    val grouped = amount.abs().toString().reversed().chunked(3).joinToString(",").reversed()
    val signed = if (amount.signum() < 0) "-$grouped" else grouped
    return signed.map { character ->
        if (character in '0'..'9') (character.code + ('۰'.code - '0'.code)).toChar() else character
    }.joinToString("")
}