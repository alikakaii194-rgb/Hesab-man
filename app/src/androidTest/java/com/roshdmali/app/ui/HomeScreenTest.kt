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
import com.roshdmali.app.domain.FinancialTransaction
import com.roshdmali.app.domain.TransactionDraft
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
    }

    private class FakeTransactionRepository : TransactionRepository {
        val transactions = MutableStateFlow<List<FinancialTransaction>>(emptyList())

        override fun observeTransactions(): Flow<List<FinancialTransaction>> = transactions

        override suspend fun getTransactions(): List<FinancialTransaction> = transactions.value

        override suspend fun addTransaction(draft: TransactionDraft) {
            transactions.value += FinancialTransaction(
                id = "test-${transactions.value.size + 1}",
                type = draft.type,
                title = draft.title,
                amountToman = draft.amountToman,
                category = draft.category,
                occurredAtMillis = 1L,
            )
        }
    }
}
