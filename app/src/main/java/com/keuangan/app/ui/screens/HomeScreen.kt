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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.data.model.Transaction
import com.keuangan.app.ui.components.FilterPills
import com.keuangan.app.ui.components.SummaryCard
import com.keuangan.app.ui.components.TransactionItem
import com.keuangan.app.ui.theme.CoralExpense
import com.keuangan.app.ui.theme.IndigoPrimary
import com.keuangan.app.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: FinanceViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0: Transaksi, 1: Tagihan

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.scriptUrl) {
        if (uiState.scriptUrl.isBlank()) {
            showSettingsDialog = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    icon = { Icon(Icons.Rounded.Payment, contentDescription = "Transaksi") },
                    label = { Text("Transaksi", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndigoPrimary,
                        selectedTextColor = IndigoPrimary,
                        indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    icon = { Icon(Icons.Rounded.Checklist, contentDescription = "Tagihan") },
                    label = { Text("Tagihan", fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IndigoPrimary,
                        selectedTextColor = IndigoPrimary,
                        indicatorColor = IndigoPrimary.copy(alpha = 0.15f)
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showAddSheet = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Catat Transaksi", fontWeight = FontWeight.Bold) },
                    containerColor = IndigoPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (selectedTabIndex == 1) {
            // Render BillsScreen
            Box(modifier = Modifier.padding(innerPadding)) {
                BillsScreen(viewModel = viewModel)
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
                item {
                    SummaryCard(
                        summary = uiState.filteredSummary,
                        filterName = uiState.filterUser
                    )
                }

                // 2. Filter Pills (Semua, Suami, Istri)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    FilterPills(
                        selectedFilter = uiState.filterUser,
                        onFilterSelected = { viewModel.setFilterUser(it) }
                    )
                }

                // 3. Section Title
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Riwayat Transaksi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.filteredTransactions.size} item",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 4. Empty State or Transactions List
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
                                        "Tekan tombol '+ Catat Transaksi' untuk memulai.",
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

    // Modal Bottom Sheet: Add Transaction
    if (showAddSheet) {
        AddTransactionBottomSheet(
            sheetState = sheetState,
            activeProfile = uiState.activeProfile,
            isLoading = uiState.isActionLoading,
            onDismiss = { showAddSheet = false },
            onSubmit = { transaction ->
                viewModel.addTransaction(transaction) {
                    scope.launch {
                        sheetState.hide()
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
}
