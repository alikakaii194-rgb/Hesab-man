package com.roshdmali.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.roshdmali.app.data.RoomTransactionRepository
import com.roshdmali.app.data.local.RoshdMaliDatabase
import com.roshdmali.app.ui.HomeScreen
import com.roshdmali.app.ui.FinanceViewModel
import com.roshdmali.app.ui.FinanceViewModelFactory
import com.roshdmali.app.ui.theme.GrowthFinancialTheme

class MainActivity : ComponentActivity() {
    private val financeViewModel: FinanceViewModel by viewModels {
        FinanceViewModelFactory(
            RoomTransactionRepository(
                RoshdMaliDatabase.getInstance(applicationContext).transactionDao(),
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrowthFinancialTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HomeScreen(viewModel = financeViewModel)
                }
            }
        }
    }
}
