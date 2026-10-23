package com.roshdmali.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.roshdmali.app.data.TransactionRepository
import com.roshdmali.app.domain.AccountBalance
import com.roshdmali.app.domain.AccountDraft
import com.roshdmali.app.domain.CategoryDraft
import com.roshdmali.app.domain.Counterparty
import com.roshdmali.app.domain.CounterpartyDraft
import com.roshdmali.app.domain.FinancialTransaction
import com.roshdmali.app.domain.TransactionCategory
import com.roshdmali.app.domain.TransactionDraft
import com.roshdmali.app.domain.TransactionType
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class FinanceUiState(
    val isAccountsLoading: Boolean = true,
    val isTransactionsLoading: Boolean = true,
    val isMetadataLoading: Boolean = true,
    val isSaving: Boolean = false,
    val accounts: List<AccountBalance> = emptyList(),
    val transactions: List<FinancialTransaction> = emptyList(),
    val categories: List<TransactionCategory> = emptyList(),
    val counterparties: List<Counterparty> = emptyList(),
) {
    val isLoading: Boolean
        get() = isAccountsLoading || isTransactionsLoading || isMetadataLoading
}

sealed interface FinanceEvent {
    data object TransactionSaved : FinanceEvent
    data object TransactionReversed : FinanceEvent
    data object AccountCreated : FinanceEvent
    data object MetadataChanged : FinanceEvent
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
        viewModelScope.launch {
            combine(
                repository.observeCategories(),
                repository.observeCounterparties(),
            ) { categories, counterparties -> categories to counterparties }
                .catch {
                    mutableState.update { it.copy(isMetadataLoading = false) }
                    eventChannel.send(FinanceEvent.Error("خواندن دسته‌ها و طرف‌حساب‌ها ناموفق بود."))
                }
                .collect { (categories, counterparties) ->
                    mutableState.update {
                        it.copy(
                            isMetadataLoading = false,
                            categories = categories,
                            counterparties = counterparties,
                        )
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

    fun reverseTransaction(transactionId: String) {
        if (mutableState.value.isSaving) return

        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true) }
            try {
                repository.reverseTransaction(transactionId, UUID.randomUUID().toString())
                mutableState.update { it.copy(isSaving = false) }
                eventChannel.send(FinanceEvent.TransactionReversed)
            } catch (exception: Exception) {
                mutableState.update { it.copy(isSaving = false) }
                eventChannel.send(
                    FinanceEvent.Error(exception.localizedMessage ?: "لغو تراکنش انجام نشد."),
                )
            }
        }
    }

    fun createCategory(draft: CategoryDraft) = runMetadataChange {
        repository.createCategory(draft)
    }

    fun renameCategory(categoryId: String, name: String) = runMetadataChange {
        repository.renameCategory(categoryId, name)
    }

    fun setCategoryActive(categoryId: String, isActive: Boolean) = runMetadataChange {
        repository.setCategoryActive(categoryId, isActive)
    }

    fun createCounterparty(draft: CounterpartyDraft) = runMetadataChange {
        repository.createCounterparty(draft)
    }

    fun renameCounterparty(counterpartyId: String, name: String) = runMetadataChange {
        repository.renameCounterparty(counterpartyId, name)
    }

    fun setCounterpartyActive(counterpartyId: String, isActive: Boolean) = runMetadataChange {
        repository.setCounterpartyActive(counterpartyId, isActive)
    }

    private fun runMetadataChange(action: suspend () -> Any?) {
        if (mutableState.value.isSaving) return
        viewModelScope.launch {
            mutableState.update { it.copy(isSaving = true) }
            try {
                action()
                mutableState.update { it.copy(isSaving = false) }
                eventChannel.send(FinanceEvent.MetadataChanged)
            } catch (exception: Exception) {
                mutableState.update { it.copy(isSaving = false) }
                eventChannel.send(
                    FinanceEvent.Error(exception.localizedMessage ?: "ذخیره اطلاعات انجام نشد."),
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