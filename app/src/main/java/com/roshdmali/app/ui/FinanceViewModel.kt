package com.roshdmali.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.roshdmali.app.data.TransactionRepository
import com.roshdmali.app.domain.AccountBalance
import com.roshdmali.app.domain.AccountDraft
import com.roshdmali.app.domain.FinancialTransaction
import com.roshdmali.app.domain.TransactionDraft
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FinanceUiState(
    val isAccountsLoading: Boolean = true,
    val isTransactionsLoading: Boolean = true,
    val isSaving: Boolean = false,
    val accounts: List<AccountBalance> = emptyList(),
    val transactions: List<FinancialTransaction> = emptyList(),
) {
    val isLoading: Boolean
        get() = isAccountsLoading || isTransactionsLoading
}

sealed interface FinanceEvent {
    data object TransactionSaved : FinanceEvent
    data object AccountCreated : FinanceEvent
    data class Error(val message: String) : FinanceEvent
}

class FinanceViewModel(
    private val repository: TransactionRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FinanceUiState())
    val state = mutableState.asStateFlow()

    private val eventChannel = Channel<FinanceEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            repository.observeTransactions()
                .catch {
                    mutableState.update { it.copy(isTransactionsLoading = false) }
                    eventChannel.send(FinanceEvent.Error("خواندن تراکنش‌های ذخیره‌شده ناموفق بود."))
                }
                .collect { transactions ->
                    mutableState.update { state ->
                        state.copy(isTransactionsLoading = false, transactions = transactions)
                    }
                }
        }
        viewModelScope.launch {
            repository.observeAccountBalances()
                .catch {
                    mutableState.update { state -> state.copy(isAccountsLoading = false) }
                    eventChannel.send(FinanceEvent.Error("خواندن حساب‌های ذخیره‌شده ناموفق بود."))
                }
                .collect { accounts ->
                    mutableState.update { state ->
                        state.copy(isAccountsLoading = false, accounts = accounts)
                    }
                }
        }
    }

    fun createAccount(draft: AccountDraft) {
        if (mutableState.value.isSaving) return

        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true) }
            try {
                repository.createAccount(draft)
                mutableState.update { it.copy(isSaving = false) }
                eventChannel.send(FinanceEvent.AccountCreated)
            } catch (exception: Exception) {
                mutableState.update { it.copy(isSaving = false) }
                eventChannel.send(
                    FinanceEvent.Error(exception.localizedMessage ?: "ساخت حساب انجام نشد."),
                )
            }
        }
    }

    fun addTransaction(draft: TransactionDraft) {
        if (mutableState.value.isSaving) return

        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true) }
            try {
                repository.addTransaction(draft)
                mutableState.update { it.copy(isSaving = false) }
                eventChannel.send(FinanceEvent.TransactionSaved)
            } catch (exception: Exception) {
                mutableState.update { it.copy(isSaving = false) }
                eventChannel.send(
                    FinanceEvent.Error(exception.localizedMessage ?: "ذخیره تراکنش انجام نشد."),
                )
            }
        }
    }
}

class FinanceViewModelFactory(
    private val repository: TransactionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinanceViewModel::class.java)) {
            return FinanceViewModel(repository) as T
        }
        error("Unsupported ViewModel: ${modelClass.name}")
    }
}