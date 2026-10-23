package com.roshdmali.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.roshdmali.app.domain.FinancialTransaction
import com.roshdmali.app.domain.AccountBalance
import com.roshdmali.app.domain.AccountDraft
import com.roshdmali.app.domain.AccountType
import com.roshdmali.app.domain.MoneyUnit
import com.roshdmali.app.domain.TransactionDraft
import com.roshdmali.app.domain.TransactionType
import com.roshdmali.app.domain.formatTomanAmount
import com.roshdmali.app.domain.formatPersianDate
import com.roshdmali.app.domain.parsePersianDateEpochDay
import com.roshdmali.app.domain.parseRialAmount
import com.roshdmali.app.domain.parseTomanAmount
import com.roshdmali.app.domain.summarizeTransactions
import com.roshdmali.app.ui.theme.Emerald
import com.roshdmali.app.ui.theme.MutedInk
import com.roshdmali.app.ui.theme.WarmBackground
import java.math.BigInteger
import java.time.LocalDate

private enum class FinancePage(val label: String, val icon: ImageVector) {
    HOME("خانه", Icons.Default.Home),
    TRANSACTIONS("تراکنش‌ها", Icons.AutoMirrored.Filled.List),
    BUDGET("بودجه", Icons.Default.DateRange),
    GOALS("اهداف", Icons.Default.Star),
    MORE("بیشتر", Icons.Default.MoreVert),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: FinanceViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPageName by rememberSaveable { mutableStateOf(FinancePage.HOME.name) }
    var entryType by remember { mutableStateOf<TransactionType?>(null) }
    var selectedAccountId by rememberSaveable { mutableStateOf("") }
    var showAccountDialog by rememberSaveable { mutableStateOf(false) }
    var transactionToReverse by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedPage = FinancePage.valueOf(selectedPageName)

    LaunchedEffect(state.accounts) {
        if (state.accounts.none { it.account.id == selectedAccountId }) {
            selectedAccountId = state.accounts.firstOrNull()?.account?.id.orEmpty()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                FinanceEvent.TransactionSaved -> {
                    entryType = null
                    snackbarHostState.showSnackbar("تراکنش ذخیره شد.")
                }
                FinanceEvent.TransactionReversed -> {
                    transactionToReverse = null
                    snackbarHostState.showSnackbar("تراکنش با ثبت معکوس لغو شد.")
                }
                FinanceEvent.AccountCreated -> {
                    showAccountDialog = false
                    snackbarHostState.showSnackbar("حساب ساخته شد.")
                }
                FinanceEvent.MetadataChanged -> snackbarHostState.showSnackbar("اطلاعات ذخیره شد.")
                is FinanceEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        containerColor = WarmBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("رشد مالی", fontWeight = FontWeight.Bold)
                        Text("مدیریت مالی شخصی", style = MaterialTheme.typography.labelMedium, color = MutedInk)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = WarmBackground),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                FinancePage.entries.forEach { page ->
                    NavigationBarItem(
                        selected = selectedPage == page,
                        onClick = { selectedPageName = page.name },
                        icon = { Icon(page.icon, contentDescription = null) },
                        label = { Text(page.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (selectedPage) {
            FinancePage.HOME -> DashboardPage(
                state = state,
                onCreateAccount = { showAccountDialog = true },
                onIncome = {
                    state.accounts.firstOrNull()?.let { account ->
                        selectedAccountId = account.account.id
                        entryType = TransactionType.INCOME
                    }
                },
                onExpense = {
                    state.accounts.firstOrNull()?.let { account ->
                        selectedAccountId = account.account.id
                        entryType = TransactionType.EXPENSE
                    }
                },
                modifier = Modifier.padding(padding),
            )
            FinancePage.TRANSACTIONS -> TransactionsPage(
                state = state,
                onReverse = { transactionToReverse = it },
                modifier = Modifier.padding(padding),
            )
            FinancePage.BUDGET -> PendingPage(
                "بودجه‌بندی",
                "بخش بودجه هنوز برای ثبت و پیگیری بودجه آماده نیست.",
                Modifier.padding(padding),
            )
            FinancePage.GOALS -> PendingPage(
                "اهداف مالی",
                "ثبت و پیگیری اهداف مالی هنوز پیاده‌سازی نشده است.",
                Modifier.padding(padding),
            )
            FinancePage.MORE -> PendingPage(
                "بیشتر",
                "تنظیمات و ابزارهای تکمیلی هنوز آماده نیستند.",
                Modifier.padding(padding),
            )
        }
    }

    if (showAccountDialog) {
        AccountCreationDialog(
            isSaving = state.isSaving,
            onDismiss = { if (!state.isSaving) showAccountDialog = false },
            onSave = viewModel::createAccount,
        )
    }

    transactionToReverse?.let { transactionId ->
        AlertDialog(
            onDismissRequest = { if (!state.isSaving) transactionToReverse = null },
            title = { Text("لغو تراکنش") },
            text = { Text("یک ثبت معکوس ساخته می‌شود و سابقه اصلی حذف نخواهد شد.") },
            confirmButton = {
                Button(
                    enabled = !state.isSaving,
                    onClick = { viewModel.reverseTransaction(transactionId) },
                ) { Text(if (state.isSaving) "در حال ثبت…" else "تأیید لغو") }
            },
            dismissButton = {
                TextButton(onClick = { transactionToReverse = null }, enabled = !state.isSaving) {
                    Text("بازگشت")
                }
            },
            containerColor = WarmBackground,
        )
    }

    entryType?.let { type ->
        TransactionEntryDialog(
            type = type,
            accountId = selectedAccountId,
            accounts = state.accounts,
            isSaving = state.isSaving,
            onDismiss = { if (!state.isSaving) entryType = null },
            onAccountChange = { selectedAccountId = it },
            onSave = viewModel::addTransaction,
        )
    }
}

@Composable
private fun DashboardPage(
    state: FinanceUiState,
    onCreateAccount: () -> Unit,
    onIncome: () -> Unit,
    onExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTransactions = state.transactions.isNotEmpty()
    val hasAccounts = state.accounts.isNotEmpty()
    val currentEpochDay = LocalDate.now().toEpochDay()
    val summaryTransactions = remember(state.transactions, currentEpochDay) {
        state.transactions.filter { transaction ->
            transaction.occurredOnEpochDay == null || transaction.occurredOnEpochDay <= currentEpochDay
        }
    }
    val hasSummaryTransactions = summaryTransactions.isNotEmpty()
    val summary = remember(summaryTransactions) { summarizeTransactions(summaryTransactions) }
    val totalAccountBalance = remember(state.accounts) {
        state.accounts.fold(BigInteger.ZERO) { total, account -> total + account.balanceToman }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SummaryCard("موجودی حساب‌ها", totalAccountBalance, hasAccounts, emphasized = true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("مجموع درآمد", summary.totalIncomeToman, hasSummaryTransactions, Modifier.weight(1f))
            SummaryCard("مجموع هزینه", summary.totalExpenseToman, hasSummaryTransactions, Modifier.weight(1f))
        }
        if (state.isLoading) {
            Text("در حال خواندن اطلاعات ذخیره‌شده…", color = MutedInk)
        } else if (!hasAccounts) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("برای شروع یک حساب بساز", fontWeight = FontWeight.Bold)
                    Text("نام و موجودی واقعی حساب را خودت وارد کن. موجودی اولیه درآمد نیست.", color = MutedInk)
                    Button(onClick = onCreateAccount, modifier = Modifier.fillMaxWidth()) {
                        Text("ساخت حساب")
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("حساب‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onCreateAccount) { Text("افزودن حساب") }
            }
            state.accounts.forEach { AccountBalanceRow(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onIncome,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald),
                ) { Text("ثبت درآمد") }
                Button(
                    onClick = onExpense,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4A3C)),
                ) { Text("ثبت هزینه") }
            }
        }

        when {
            state.isLoading -> Unit
            !hasTransactions -> Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("هنوز تراکنشی ثبت نشده", fontWeight = FontWeight.Bold)
                    Text(
                        if (hasAccounts) "پس از ثبت درآمد یا هزینه، جمع‌ها از اطلاعات خودت محاسبه می‌شوند."
                        else "پس از ساخت حساب، می‌توانی درآمد یا هزینه ثبت کنی.",
                        color = MutedInk,
                    )
                }
            }
            else -> {
                Text("تراکنش‌های اخیر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                state.transactions.take(5).forEach { transaction ->
                    val accountName = state.accounts.firstOrNull { it.account.id == transaction.accountId }
                        ?.account?.name
                    TransactionRow(transaction, accountName)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: BigInteger,
    hasTransactions: Boolean,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (emphasized) Emerald else Color.White),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = if (emphasized) Color.White.copy(alpha = 0.8f) else MutedInk)
            Text(
                text = if (hasTransactions) "${formatTomanAmount(amount)} تومان" else "—",
                color = if (emphasized) Color.White else MaterialTheme.colorScheme.onSurface,
                style = if (emphasized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AccountBalanceRow(accountBalance: AccountBalance) {
    val balance = accountBalance.balanceToman
    val typeLabel = when (accountBalance.account.type) {
        AccountType.CASH -> "کیف پول نقدی"
        AccountType.BANK -> "حساب بانکی"
        AccountType.BANK_CARD -> "کارت بانکی"
    }
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(accountBalance.account.name, fontWeight = FontWeight.SemiBold)
                Text("${formatTomanAmount(balance)} تومان", fontWeight = FontWeight.Bold)
            }
            Text(
                if (balance.signum() < 0) "$typeLabel · مانده منفی"
                else "$typeLabel · ${balance.signum().let { if (it == 0) "بدون مانده" else "موجود" }}",
                color = if (balance.signum() < 0) Color(0xFF8B4A3C) else MutedInk,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun AccountType.toPersianLabel(): String = when (this) {
    AccountType.CASH -> "نقدی"
    AccountType.BANK -> "بانک"
    AccountType.BANK_CARD -> "کارت"
}

@Composable
private fun TransactionsPage(
    state: FinanceUiState,
    onReverse: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("در حال خواندن اطلاعات ذخیره‌شده…", color = MutedInk)
        }
        state.transactions.isEmpty() -> PendingPage(
            "تراکنش‌ها",
            "فهرست خالی است. از خانه درآمد یا هزینه‌ای ثبت کن.",
            modifier,
        )
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("همه تراکنش‌ها", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            val reversedTransactionIds = state.transactions.mapNotNull(FinancialTransaction::reversesTransactionId).toSet()
            items(state.transactions, key = { it.id }) { transaction ->
                val accountName = state.accounts
                    .firstOrNull { it.account.id == transaction.accountId }
                    ?.account?.name
                    ?: if (transaction.accountId == LEGACY_UNASSIGNED_ACCOUNT_ID) {
                        "حساب قدیمی ـ تعیین‌نشده"
                    } else {
                        "حساب غیرفعال"
                    }
                val canReverse = transaction.reversesTransactionId == null && transaction.id !in reversedTransactionIds
                    if (canReverse) {
                        TransactionRow(transaction, accountName) { onReverse(transaction.id) }
                    } else {
                        TransactionRow(transaction, accountName)
                    }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: FinancialTransaction,
    accountName: String? = null,
    onReverse: (() -> Unit)? = null,
) {
    val isIncome = transaction.type == TransactionType.INCOME
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(if (isIncome) Color(0xFFE4F2EA) else Color(0xFFF6E9E5)),
                contentAlignment = Alignment.Center,
            ) { Text(if (isIncome) "+" else "−", color = if (isIncome) Emerald else Color(0xFF8B4A3C)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(transaction.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val dateLabel = transaction.occurredOnEpochDay?.formatPersianDate() ?: "تاریخ وقوع نامشخص"
                Text(
                    listOfNotNull(transaction.category, accountName, dateLabel).joinToString(" · "),
                    color = MutedInk,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "${if (isIncome) "+" else "−"}${formatTomanAmount(BigInteger.valueOf(transaction.amountToman))}",
                color = if (isIncome) Emerald else Color(0xFF8B4A3C),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            onReverse?.let { reverse ->
                TextButton(onClick = reverse) { Text("لغو") }
            }
        }
    }
}

@Composable
private fun PendingPage(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(message, color = MutedInk, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountCreationDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (AccountDraft) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("0") }
    var unit by rememberSaveable { mutableStateOf(MoneyUnit.TOMAN) }
    var type by rememberSaveable { mutableStateOf(AccountType.CASH) }
    var idempotencyKey by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var effectiveDate by rememberSaveable {
        mutableStateOf(LocalDate.now().toEpochDay().formatPersianDate())
    }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ساخت حساب") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = null },
                    label = { Text("نام حساب") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("نوع حساب", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AccountType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(option.toPersianLabel()) },
                            enabled = !isSaving,
                        )
                    }
                }
                MoneyUnitSelector(unit, enabled = !isSaving) {
                    if (unit != it) {
                        unit = it
                        amount = ""
                        error = null
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; error = null },
                    label = { Text(if (unit == MoneyUnit.TOMAN) "موجودی اولیه به تومان" else "موجودی اولیه به ریال") },
                    supportingText = { Text("صفر مجاز است؛ موجودی اولیه درآمد نیست.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = effectiveDate,
                    onValueChange = { effectiveDate = it; error = null },
                    label = { Text("تاریخ مؤثر (شمسی)") },
                    supportingText = { Text("مثال: ۱۴۰۴/۰۱/۰۱") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(enabled = !isSaving, onClick = {
                val openingAmount = parseMoneyAmount(amount, unit, allowZero = true)
                val effectiveEpochDay = effectiveDate.parsePersianDateEpochDay()
                error = when {
                    name.isBlank() -> "نام حساب را وارد کن."
                    openingAmount == null -> "موجودی نامعتبر است؛ مبلغ ریالی باید بر ۱۰ بخش‌پذیر باشد."
                    effectiveEpochDay == null -> "تاریخ شمسی معتبر وارد کن."
                    else -> null
                }
                if (error == null && openingAmount != null && effectiveEpochDay != null) {
                    onSave(AccountDraft(name, type, openingAmount, effectiveEpochDay, idempotencyKey))
                }
            }) { Text(if (isSaving) "در حال ذخیره…" else "ذخیره حساب") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("انصراف") } },
        containerColor = WarmBackground,
    )
}

@Composable
private fun MoneyUnitSelector(unit: MoneyUnit, enabled: Boolean, onSelect: (MoneyUnit) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = unit == MoneyUnit.TOMAN,
            onClick = { onSelect(MoneyUnit.TOMAN) },
            label = { Text("تومان") },
            enabled = enabled,
        )
        FilterChip(
            selected = unit == MoneyUnit.RIAL,
            onClick = { onSelect(MoneyUnit.RIAL) },
            label = { Text("ریال") },
            enabled = enabled,
        )
    }
}

private fun parseMoneyAmount(input: String, unit: MoneyUnit, allowZero: Boolean): Long? = when (unit) {
    MoneyUnit.TOMAN -> parseTomanAmount(input, allowZero)
    MoneyUnit.RIAL -> parseRialAmount(input, allowZero)
}

private const val LEGACY_UNASSIGNED_ACCOUNT_ID = "legacy-unassigned"

@Composable
private fun TransactionEntryDialog(
    type: TransactionType,
    accountId: String,
    accounts: List<AccountBalance>,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onAccountChange: (String) -> Unit,
    onSave: (TransactionDraft) -> Unit,
) {
    var title by rememberSaveable(type) { mutableStateOf("") }
    var amount by rememberSaveable(type) { mutableStateOf("") }
    var category by rememberSaveable(type) { mutableStateOf("") }
    var amountUnit by rememberSaveable(type) { mutableStateOf(MoneyUnit.TOMAN) }
    var occurredDate by rememberSaveable(type) {
        mutableStateOf(LocalDate.now().toEpochDay().formatPersianDate())
    }
    var idempotencyKey by rememberSaveable(type) { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var accountMenuExpanded by remember { mutableStateOf(false) }
    var error by rememberSaveable(type) { mutableStateOf<String?>(null) }
    val isIncome = type == TransactionType.INCOME

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIncome) "ثبت درآمد" else "ثبت هزینه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box {
                    val selectedAccount = accounts.firstOrNull { it.account.id == accountId }
                    TextButton(
                        onClick = { accountMenuExpanded = true },
                        enabled = !isSaving && accounts.size > 1,
                    ) {
                        Text(selectedAccount?.account?.name ?: "حساب انتخاب نشده")
                    }
                    DropdownMenu(
                        expanded = accountMenuExpanded,
                        onDismissRequest = { accountMenuExpanded = false },
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.account.name) },
                                onClick = {
                                    onAccountChange(account.account.id)
                                    accountMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("عنوان") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                MoneyUnitSelector(amountUnit, enabled = !isSaving) {
                    if (amountUnit != it) {
                        amountUnit = it
                        amount = ""
                        error = null
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; error = null },
                    label = { Text(if (amountUnit == MoneyUnit.TOMAN) "مبلغ به تومان" else "مبلغ به ریال") },
                    supportingText = { Text("عدد صحیح؛ رقم فارسی یا انگلیسی") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it; error = null },
                    label = { Text("دسته‌بندی") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = occurredDate,
                    onValueChange = { occurredDate = it; error = null },
                    label = { Text("تاریخ وقوع (شمسی)") },
                    supportingText = { Text("مثال: ۱۴۰۴/۰۱/۰۱") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val parsedAmount = parseMoneyAmount(amount, amountUnit, allowZero = false)
                    val occurredOnEpochDay = occurredDate.parsePersianDateEpochDay()
                    error = when {
                        accountId.isBlank() -> "یک حساب انتخاب کن."
                        title.isBlank() -> "عنوان را وارد کن."
                        parsedAmount == null -> "مبلغ نامعتبر است؛ مبلغ ریالی باید بر ۱۰ بخش‌پذیر باشد."
                        category.isBlank() -> "دسته‌بندی را وارد کن."
                        occurredOnEpochDay == null -> "تاریخ شمسی معتبر وارد کن."
                        else -> null
                    }
                    if (error == null && parsedAmount != null && occurredOnEpochDay != null) {
                        onSave(
                            TransactionDraft(
                                accountId = accountId,
                                type = type,
                                title = title.trim(),
                                amountToman = parsedAmount,
                                category = category.trim(),
                                occurredOnEpochDay = occurredOnEpochDay,
                                idempotencyKey = idempotencyKey,
                            ),
                        )
                    }
                },
            ) { Text(if (isSaving) "در حال ذخیره…" else "ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("انصراف") } },
        containerColor = WarmBackground,
    )
}
