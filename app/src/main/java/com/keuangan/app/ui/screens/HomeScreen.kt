package com.keuangan.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.data.model.Transaction
import com.keuangan.app.ui.components.BudgetAndChartCard
import com.keuangan.app.ui.components.ContextualInsightCard
import com.keuangan.app.ui.components.ExportTransactionsDialog
import com.keuangan.app.ui.components.FilterPills
import com.keuangan.app.ui.components.HomeScreenSkeleton
import com.keuangan.app.ui.components.MilestoneCard
import com.keuangan.app.ui.components.ModernFloatingNotification
import com.keuangan.app.ui.components.PeriodFilterRow
import com.keuangan.app.ui.components.QuickAddBar
import com.keuangan.app.ui.components.QuickAddPreset
import com.keuangan.app.ui.components.SummaryCard
import com.keuangan.app.ui.components.TransactionItem
import com.keuangan.app.ui.theme.CoralExpense
import com.keuangan.app.ui.theme.IndigoPrimary
import com.keuangan.app.ui.viewmodel.FinanceViewModel
import com.keuangan.app.util.bouncingClickable
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FinanceViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Transaksi, 1: Tagihan

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddBillSheet by remember { mutableStateOf(false) }
    var activeQuickPreset by remember { mutableStateOf<QuickAddPreset?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (selectedTabIndex == 0) "Keuangan Keluarga" else "Tagihan Bulanan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        // Profile Switcher Chip
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(IndigoPrimary.copy(alpha = 0.12f))
                                .clickable { showSettingsDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = uiState.activeProfile,
                                    color = IndigoPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showExportDialog = true },
                            enabled = uiState.transactions.isNotEmpty()
                        ) {
                            Icon(imageVector = Icons.Rounded.FileDownload, contentDescription = "Unduh Riwayat")
                        }
                        IconButton(
                            onClick = { viewModel.loadAllData() },
                            enabled = !uiState.isLoading && !uiState.isBillsLoading
                        ) {
                            if (uiState.isLoading || uiState.isBillsLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = IndigoPrimary
                                )
                            } else {
                                Icon(imageVector = Icons.Rounded.Refresh, contentDescription = "Refresh Data")
                            }
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(imageVector = Icons.Rounded.Settings, contentDescription = "Pengaturan")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tab 1: Transaksi
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .bouncingClickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTabIndex = 0
                                }
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Payment,
                                contentDescription = "Transaksi",
                                tint = if (selectedTabIndex == 0) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Transaksi",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTabIndex == 0) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }

                        // Central Elevated Action Button (+) with Micro-interaction
                        FloatingActionButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (selectedTabIndex == 0) {
                                    activeQuickPreset = null
                                    showAddSheet = true
                                } else {
                                    showAddBillSheet = true
                                }
                            },
                            shape = CircleShape,
                            containerColor = IndigoPrimary,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 10.dp
                            ),
                            modifier = Modifier
                                .size(52.dp)
                                .bouncingClickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (selectedTabIndex == 0) {
                                        activeQuickPreset = null
                                        showAddSheet = true
                                    } else {
                                        showAddBillSheet = true
                                    }
                                }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = if (selectedTabIndex == 0) "Catat Transaksi" else "Tambah Tagihan",
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Tab 2: Tagihan
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .bouncingClickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTabIndex = 1
                                }
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Checklist,
                                contentDescription = "Tagihan",
                                tint = if (selectedTabIndex == 1) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Tagihan",
                                fontSize = 11.sp,
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTabIndex == 1) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            if (selectedTabIndex == 1) {
                // Render BillsScreen
                Box(modifier = Modifier.padding(innerPadding)) {
                    BillsScreen(
                        viewModel = viewModel,
                        isAddSheetOpen = showAddBillSheet,
                        onDismissAddSheet = { showAddBillSheet = false }
                    )
                }
            } else {
                // Skeleton Loading saat pertama kali memuat data transaksi
                if (uiState.isLoading && uiState.transactions.isEmpty()) {
                    Box(modifier = Modifier.padding(innerPadding)) {
                        HomeScreenSkeleton()
                    }
                } else {
                    // Render Transaksi Screen
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. Summary Card
                        // 0. Period Filter Row (Bulan Ini, Bulan Lalu, 7 Hari, 30 Hari, Semua)
                        item {
                            PeriodFilterRow(
                                selectedPeriod = uiState.filterPeriod,
                                onPeriodSelected = { viewModel.setFilterPeriod(it) }
                            )
                        }

                        // 1. Summary Card (Menghitung Total Masuk, Total Keluar, Saldo per Periode & User)
                        item {
                            SummaryCard(
                                summary = uiState.filteredSummary,
                                filterName = "${uiState.filterUser} • ${uiState.filterPeriod.shortLabel}"
                            )
                        }

                        // 2. Budget and Chart Combined Slider Card (Smart Micro-Budget & Diagram Donat dalam 1 tempat)
                        item {
                            BudgetAndChartCard(
                                currentExpense = uiState.filteredSummary.totalExpense,
                                budgetLimit = uiState.monthlyBudget,
                                onUpdateBudget = { viewModel.updateMonthlyBudget(it) },
                                transactions = uiState.filteredTransactions
                            )
                        }

                        // 3. Quick Add Bar (1-Ketukan Catat Cepat)
                        item {
                            QuickAddBar(
                                onSelectPreset = { preset ->
                                    activeQuickPreset = preset
                                    showAddSheet = true
                                }
                            )
                        }

                        // 4. Contextual Insights Cerdas
                        item {
                            ContextualInsightCard(
                                transactions = uiState.filteredTransactions,
                                budgetLimit = uiState.monthlyBudget
                            )
                        }

                        // 5. Gamifikasi & Milestone Badge
                        item {
                            MilestoneCard(
                                summary = uiState.filteredSummary,
                                transactions = uiState.filteredTransactions,
                                budgetLimit = uiState.monthlyBudget
                            )
                        }

                        // 6. Filter Pills (Semua, Suami, Istri)
                        item {
                            Spacer(modifier = Modifier.height(2.dp))
                            FilterPills(
                                selectedFilter = uiState.filterUser,
                                onFilterSelected = { viewModel.setFilterUser(it) }
                            )
                        }

                        // 7. Section Title Riwayat Transaksi + Tombol Unduh Cepat
                        item {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Riwayat Transaksi",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${uiState.filterPeriod.label} • ${uiState.filteredTransactions.size} transaksi",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(IndigoPrimary.copy(alpha = 0.12f))
                                        .bouncingClickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            showExportDialog = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.FileDownload,
                                            contentDescription = "Unduh",
                                            tint = IndigoPrimary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Unduh",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IndigoPrimary
                                        )
                                    }
                                }
                            }
                        }

                        // 9. Empty State or Transactions List
                        val list = uiState.filteredTransactions
                        if (list.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "📊", fontSize = 48.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = if (uiState.scriptUrl.isBlank())
                                                "URL Google Sheets belum diatur"
                                            else
                                                "Belum ada catatan transaksi",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (uiState.scriptUrl.isBlank())
                                                "Buka Pengaturan di pojok kanan atas untuk memasukkan URL Web App."
                                            else
                                                "Tekan tombol '+' di tengah bawah atau pilih menu 'Catat Cepat'.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = list,
                                key = { it.id.ifBlank { it.tanggal + it.nominal.toString() } }
                            ) { item ->
                                TransactionItem(
                                    transaction = item,
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        transactionToDelete = item
                                    }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }

        // Floating Modern Notification (Top Dynamic Island Style)
        ModernFloatingNotification(
            notification = uiState.notification,
            onDismiss = { viewModel.dismissNotification() }
        )
    }

    // Modal Bottom Sheet: Add Transaction (Dengan Auto-Categorization & Quick Add)
    if (showAddSheet) {
        AddTransactionBottomSheet(
            sheetState = sheetState,
            activeProfile = uiState.activeProfile,
            expenseCategories = uiState.expenseCategories,
            incomeCategories = uiState.incomeCategories,
            paymentAccounts = uiState.paymentAccounts,
            isLoading = uiState.isActionLoading,
            initialPreset = activeQuickPreset,
            onDismiss = {
                activeQuickPreset = null
                showAddSheet = false
            },
            onAddCategory = { isExp, name, icon -> viewModel.addCategory(isExp, name, icon) },
            onDeleteCategory = { isExp, name -> viewModel.deleteCategory(isExp, name) },
            onAddAccount = { name -> viewModel.addAccount(name) },
            onDeleteAccount = { name -> viewModel.deleteAccount(name) },
            onSubmit = { transaction ->
                viewModel.addTransaction(transaction) {
                    scope.launch {
                        sheetState.hide()
                        activeQuickPreset = null
                        showAddSheet = false
                    }
                }
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            initialUrl = uiState.scriptUrl,
            initialProfile = uiState.activeProfile,
            excelExportUrl = uiState.excelExportUrl,
            spreadsheetUrl = uiState.spreadsheetUrl,
            onDismiss = { showSettingsDialog = false },
            onTestConnection = { url, onResult ->
                viewModel.testConnection(url, onResult)
            },
            onSave = { newUrl, newProfile ->
                viewModel.saveScriptUrl(newUrl)
                viewModel.setActiveProfile(newProfile)
            }
        )
    }

    // Delete Confirmation Dialog
    transactionToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = CoralExpense
                )
            },
            title = {
                Text("Hapus Transaksi?")
            },
            text = {
                Text("Hapus transaksi '${target.kategori}' senilai ${target.nominal} dari Google Sheets?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(target.id)
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralExpense),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { transactionToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Export Transactions Dialog
    if (showExportDialog) {
        ExportTransactionsDialog(
            transactions = uiState.transactions,
            excelExportUrl = uiState.excelExportUrl,
            initialPeriod = uiState.filterPeriod,
            onDismiss = { showExportDialog = false }
        )
    }
}

