package com.roshdmali.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roshdmali.app.ui.theme.GrowthFinancialTheme
import com.roshdmali.app.data.TransactionRepository
import com.roshdmali.app.domain.Account
import com.roshdmali.app.domain.AccountBalance
import com.roshdmali.app.domain.AccountDraft
import com.roshdmali.app.domain.FinancialTransaction
import com.roshdmali.app.domain.TransactionDraft
import com.roshdmali.app.domain.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun incomeFormValidatesAndPersistsTransactionInRepository() {
        val repository = FakeTransactionRepository()

        composeRule.setContent {
            GrowthFinancialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    val financeViewModel: FinanceViewModel = viewModel(
                        factory = FinanceViewModelFactory(repository),
                    )
                    HomeScreen(financeViewModel)
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("رشد مالی").assertIsDisplayed()
        composeRule.onNodeWithText("ساخت حساب").performClick()
        composeRule.onNodeWithText("نام حساب").performTextInput("کیف پول")
        composeRule.onNodeWithText("ذخیره حساب").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { repository.accounts.value.size == 1 }

        composeRule.onNodeWithText("هنوز تراکنشی ثبت نشده").assertIsDisplayed()
        composeRule.onNodeWithText("ثبت درآمد").performClick()
        composeRule.onNodeWithText("ذخیره").performClick()
        composeRule.onNodeWithText("عنوان را وارد کن.").assertIsDisplayed()

        composeRule.onNodeWithText("عنوان").performTextInput("حقوق ماهانه")
        composeRule.onNodeWithText("مبلغ به تومان").performTextInput("۱۲۳۴۵")
        composeRule.onNodeWithText("دسته‌بندی").performTextInput("حقوق")
        composeRule.onNodeWithText("ذخیره").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.transactions.value.size == 1
        }
        composeRule.onNodeWithText("حقوق ماهانه").assertIsDisplayed()
        composeRule.onNodeWithText("تراکنش‌ها").performClick()
        composeRule.onNodeWithText("لغو").performClick()
        composeRule.onNodeWithText("تأیید لغو").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            repository.transactions.value.size == 2
        }
        composeRule.onNodeWithText("لغو: حقوق ماهانه").assertIsDisplayed()
    }

    private class FakeTransactionRepository : TransactionRepository {
        val transactions = MutableStateFlow<List<FinancialTransaction>>(emptyList())
        val accounts = MutableStateFlow<List<AccountBalance>>(emptyList())

        override fun observeTransactions(): Flow<List<FinancialTransaction>> = transactions
        override fun observeAccountBalances(): Flow<List<AccountBalance>> = accounts

        override suspend fun getTransactions(): List<FinancialTransaction> = transactions.value

        override suspend fun createAccount(draft: AccountDraft): Account {
            val account = Account(
                id = "account-${accounts.value.size + 1}",
                name = draft.name,
                type = draft.type,
            )
            accounts.value += AccountBalance(account, java.math.BigInteger.valueOf(draft.openingAmountToman))
            return account
        }

        override suspend fun addTransaction(draft: TransactionDraft): FinancialTransaction {
            val transaction = FinancialTransaction(
                id = "test-${transactions.value.size + 1}",
                operationId = "operation-${transactions.value.size + 1}",
                accountId = draft.accountId,
                type = draft.type,
                title = draft.title,
                amountToman = draft.amountToman,
                category = draft.category,
                createdAtMillis = 1L,
                occurredOnEpochDay = draft.occurredOnEpochDay,
                reversesTransactionId = null,
            )
            transactions.value += transaction
            return transaction
        }

        override suspend fun reverseTransaction(
            transactionId: String,
            idempotencyKey: String,
        ): FinancialTransaction {
            transactions.value.firstOrNull { it.reversesTransactionId == transactionId }?.let { return it }
            val original = requireNotNull(transactions.value.firstOrNull { it.id == transactionId })
            val reversal = original.copy(
                id = "reversal-${original.id}",
                operationId = "reversal-operation-${original.operationId}",
                type = if (original.type == TransactionType.INCOME) TransactionType.EXPENSE else TransactionType.INCOME,
                title = "لغو: ${original.title}",
                reversesTransactionId = original.id,
            )
            transactions.value += reversal
            return reversal
        }
    }
}
