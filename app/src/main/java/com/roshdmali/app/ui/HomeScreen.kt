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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.roshdmali.app.domain.TransactionDraft
import com.roshdmali.app.domain.TransactionType
import com.roshdmali.app.domain.formatTomanAmount
import com.roshdmali.app.domain.parseTomanAmount
import com.roshdmali.app.domain.summarizeTransactions
import com.roshdmali.app.ui.theme.Emerald
import com.roshdmali.app.ui.theme.MutedInk
import com.roshdmali.app.ui.theme.WarmBackground
import java.math.BigInteger

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
    val selectedPage = FinancePage.valueOf(selectedPageName)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                FinanceEvent.TransactionSaved -> {
                    entryType = null
                    snackbarHostState.showSnackbar("تراکنش ذخیره شد.")
                }
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
                onIncome = { entryType = TransactionType.INCOME },
                onExpense = { entryType = TransactionType.EXPENSE },
                modifier = Modifier.padding(padding),
            )
            FinancePage.TRANSACTIONS -> TransactionsPage(state, Modifier.padding(padding))
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

    entryType?.let { type ->
        TransactionEntryDialog(
            type = type,
            isSaving = state.isSaving,
            onDismiss = { if (!state.isSaving) entryType = null },
            onSave = viewModel::addTransaction,
        )
    }
}

@Composable
private fun DashboardPage(
    state: FinanceUiState,
    onIncome: () -> Unit,
    onExpense: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTransactions = state.transactions.isNotEmpty()
    val summary = remember(state.transactions) { summarizeTransactions(state.transactions) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SummaryCard("موجودی خالص", summary.balanceToman, hasTransactions, emphasized = true)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("مجموع درآمد", summary.totalIncomeToman, hasTransactions, Modifier.weight(1f))
            SummaryCard("مجموع هزینه", summary.totalExpenseToman, hasTransactions, Modifier.weight(1f))
        }
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

        when {
            state.isLoading -> Text("در حال خواندن اطلاعات ذخیره‌شده…", color = MutedInk)
            !hasTransactions -> Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("هنوز تراکنشی ثبت نشده", fontWeight = FontWeight.Bold)
                    Text(
                        "برای شروع، درآمد یا هزینه‌ات را ثبت کن. جمع‌ها فقط از اطلاعات خودت محاسبه می‌شوند.",
                        color = MutedInk,
                    )
                }
            }
            else -> {
                Text("تراکنش‌های اخیر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                state.transactions.take(5).forEach { TransactionRow(it) }
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
private fun TransactionsPage(state: FinanceUiState, modifier: Modifier = Modifier) {
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
            items(state.transactions, key = { it.id }) { TransactionRow(it) }
        }
    }
}

@Composable
private fun TransactionRow(transaction: FinancialTransaction) {
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
                Text(transaction.category, color = MutedInk, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "${if (isIncome) "+" else "−"}${formatTomanAmount(BigInteger.valueOf(transaction.amountToman))}",
                color = if (isIncome) Emerald else Color(0xFF8B4A3C),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
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

@Composable
private fun TransactionEntryDialog(
    type: TransactionType,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (TransactionDraft) -> Unit,
) {
    var title by rememberSaveable(type) { mutableStateOf("") }
    var amount by rememberSaveable(type) { mutableStateOf("") }
    var category by rememberSaveable(type) { mutableStateOf("") }
    var error by rememberSaveable(type) { mutableStateOf<String?>(null) }
    val isIncome = type == TransactionType.INCOME

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIncome) "ثبت درآمد" else "ثبت هزینه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; error = null },
                    label = { Text("عنوان") },
                    singleLine = true,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; error = null },
                    label = { Text("مبلغ به تومان") },
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
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !isSaving,
                onClick = {
                    val parsedAmount = parseTomanAmount(amount)
                    error = when {
                        title.isBlank() -> "عنوان را وارد کن."
                        parsedAmount == null -> "مبلغ باید عدد صحیح و بزرگ‌تر از صفر باشد."
                        category.isBlank() -> "دسته‌بندی را وارد کن."
                        else -> null
                    }
                    if (error == null && parsedAmount != null) {
                        onSave(TransactionDraft(type, title.trim(), parsedAmount, category.trim()))
                    }
                },
            ) { Text(if (isSaving) "در حال ذخیره…" else "ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("انصراف") } },
        containerColor = WarmBackground,
    )
}
