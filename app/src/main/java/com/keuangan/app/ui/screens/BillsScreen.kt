package com.keuangan.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.data.model.Bill
import com.keuangan.app.data.model.FinanceConstants
import com.keuangan.app.ui.components.BillItem
import com.keuangan.app.ui.theme.AmberAccent
import com.keuangan.app.ui.theme.CoralExpense
import com.keuangan.app.ui.theme.EmeraldIncome
import com.keuangan.app.ui.theme.IndigoPrimary
import com.keuangan.app.ui.viewmodel.FinanceViewModel
import com.keuangan.app.util.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    viewModel: FinanceViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    var billToToggle by remember { mutableStateOf<Bill?>(null) }
    var billToDelete by remember { mutableStateOf<Bill?>(null) }

    var recordExpenseInTx by remember { mutableStateOf(true) }
    var selectedPaymentAccount by remember { mutableStateOf(FinanceConstants.paymentAccounts.first()) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val currentMonthKey = uiState.currentMonthKey
    val progress = if (uiState.totalBillsCount > 0) {
        uiState.paidBillsCount.toFloat() / uiState.totalBillsCount.toFloat()
    } else 0f

    // Sort bills: unpaid first, then paid
    val sortedBills = remember(uiState.bills, currentMonthKey) {
        uiState.bills.sortedWith(
            compareBy<Bill> { it.isPaidForMonth(currentMonthKey) }
                .thenBy { it.jatuhTempo }
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Tambah Tagihan", fontWeight = FontWeight.Bold) },
                containerColor = IndigoPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Month Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1E293B),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tagihan ${Formatters.getCurrentMonthYearDisplay()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${uiState.paidBillsCount}/${uiState.totalBillsCount} Terbayar",
                                        color = EmeraldIncome,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = EmeraldIncome,
                                trackColor = Color.White.copy(alpha = 0.15f),
                                strokeCap = StrokeCap.Round
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Sisa Belum Bayar",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = Formatters.formatRupiah(uiState.unpaidBillsTotalAmount),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.unpaidBillsTotalAmount > 0) AmberAccent else EmeraldIncome
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Komitmen",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = Formatters.formatRupiah(uiState.totalBillsAmount),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Section Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Checklist Tagihan Bulanan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Auto-reset tiap bulan",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. Bills List
            if (uiState.isBillsLoading && uiState.bills.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = IndigoPrimary)
                    }
                }
            } else if (sortedBills.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📝", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum ada daftar tagihan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tambahkan tagihan bulanan seperti Listrik, Wi-Fi, BPJS, dll.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sortedBills, key = { it.id.ifBlank { it.nama } }) { bill ->
                    BillItem(
                        bill = bill,
                        currentMonthKey = currentMonthKey,
                        onToggleCheck = {
                            billToToggle = bill
                        },
                        onLongClick = {
                            billToDelete = bill
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Modal Bottom Sheet: Add Bill
    if (showAddSheet) {
        AddBillBottomSheet(
            sheetState = sheetState,
            isLoading = uiState.isActionLoading,
            onDismiss = { showAddSheet = false },
            onSubmit = { newBill ->
                viewModel.addBill(newBill) {
                    scope.launch {
                        sheetState.hide()
                        showAddSheet = false
                    }
                }
            }
        )
    }

    // Toggle Paid Confirmation Dialog
    billToToggle?.let { bill ->
        val isAlreadyPaid = bill.isPaidForMonth(currentMonthKey)
        AlertDialog(
            onDismissRequest = { billToToggle = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = if (!isAlreadyPaid) EmeraldIncome else AmberAccent
                )
            },
            title = {
                Text(if (!isAlreadyPaid) "Tandai Tagihan Lunas?" else "Batal Tandai Lunas?")
            },
            text = {
                Column {
                    Text("Tagihan: ${bill.nama}")
                    Text(
                        text = "Nominal: ${Formatters.formatRupiah(bill.nominal)}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!isAlreadyPaid) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(8.dp)
                        ) {
                            Checkbox(
                                checked = recordExpenseInTx,
                                onCheckedChange = { recordExpenseInTx = it },
                                colors = CheckboxDefaults.colors(checkedColor = IndigoPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Catat otomatis ke riwayat pengeluaran",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.toggleBillPaid(
                            bill = bill,
                            recordExpense = recordExpenseInTx,
                            paymentAccount = selectedPaymentAccount
                        )
                        billToToggle = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isAlreadyPaid) EmeraldIncome else AmberAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (!isAlreadyPaid) "Ya, Lunas" else "Ubah Belum Bayar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { billToToggle = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Bill Confirmation Dialog
    billToDelete?.let { bill ->
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = CoralExpense
                )
            },
            title = { Text("Hapus Tagihan?") },
            text = { Text("Hapus tagihan bulanan '${bill.nama}' dari daftar?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(bill.id)
                        billToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralExpense),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { billToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
