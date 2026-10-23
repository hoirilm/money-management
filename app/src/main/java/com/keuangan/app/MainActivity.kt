package com.keuangan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.keuangan.app.ui.screens.HomeScreen
import com.keuangan.app.ui.theme.KeuanganKeluargaTheme
import com.keuangan.app.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FinanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KeuanganKeluargaTheme {
                HomeScreen(viewModel = viewModel)
            }
        }
    }
}
