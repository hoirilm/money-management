package com.keuangan.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Today
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
    viewModel: FinanceViewModel,
    isAddSheetOpen: Boolean = false,
    onDismissAddSheet: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    // Navigasi Bulan untuk Tagihan
    val actualCurrentMonthKey = remember { Formatters.getCurrentMonthYearKey() }
    var selectedMonthKey by remember { mutableStateOf(actualCurrentMonthKey) }

    var billToToggle by remember { mutableStateOf<Bill?>(null) }
    var billToDelete by remember { mutableStateOf<Bill?>(null) }

    var recordExpenseInTx by remember { mutableStateOf(true) }
    var selectedPaymentAccount by remember { mutableStateOf(FinanceConstants.paymentAccounts.first()) }
    var customPaymentTfv by remember { mutableStateOf(TextFieldValue("")) }
    var updateDefaultNominal by remember { mutableStateOf(false) }

    LaunchedEffect(billToToggle) {
        billToToggle?.let { b ->
            val formatted = Formatters.formatCurrencyInput(b.nominal.toLong().toString())
            customPaymentTfv = TextFieldValue(formatted, TextRange(formatted.length))
            updateDefaultNominal = false
            selectedPaymentAccount = uiState.paymentAccounts.firstOrNull() ?: "Cash"
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Hitung statistik tagihan untuk bulan yang dipilih
    val totalBills = uiState.bills.size
    val paidBills = uiState.bills.count { it.isPaidForMonth(selectedMonthKey) }
    val unpaidBillsTotal = uiState.bills.filter { !it.isPaidForMonth(selectedMonthKey) }.sumOf { it.nominal }
    val totalAmount = uiState.bills.sumOf { it.nominal }

    val progress = if (totalBills > 0) paidBills.toFloat() / totalBills.toFloat() else 0f
    val percentage = (progress * 100).toInt()

    // Sort bills: belum lunas di atas, lalu lunas di bawah
    val sortedBills = remember(uiState.bills, selectedMonthKey) {
        uiState.bills.sortedWith(
            compareBy<Bill> { it.isPaidForMonth(selectedMonthKey) }
                .thenBy { it.jatuhTempo }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Interactive Month Selector Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                selectedMonthKey = Formatters.getShiftedMonthKey(selectedMonthKey, -1)
                            }
                        ) {
                            Icon(imageVector = Icons.Rounded.ChevronLeft, contentDescription = "Bulan Sebelumnya")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = IndigoPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = Formatters.formatMonthKeyDisplay(selectedMonthKey),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (selectedMonthKey != actualCurrentMonthKey) {
                                Text(
                                    text = "Kembali ke Bulan Ini",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IndigoPrimary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedMonthKey = actualCurrentMonthKey }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                selectedMonthKey = Formatters.getShiftedMonthKey(selectedMonthKey, 1)
                            }
                        ) {
                            Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = "Bulan Berikutnya")
                        }
                    }
                }
            }

            // 2. Month Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                                    text = "Status Pembayaran",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$paidBills dari $totalBills Selesai ($percentage%)",
                                        color = EmeraldIncome,
                                        fontSize = 12.sp,
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
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
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
                                        text = "Sisa Belum Dibayar",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = Formatters.formatRupiah(unpaidBillsTotal),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (unpaidBillsTotal > 0) AmberAccent else EmeraldIncome
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Komitmen Bulanan",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = Formatters.formatRupiah(totalAmount),
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

            // 3. Section Header
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Checklist Tagihan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Otomatis uncheck tiap bulan baru",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Bills List
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
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📝", fontSize = 44.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Belum Ada Tagihan Rutin",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tambahkan tagihan bulanan (Listrik, Wi-Fi, Cicilan, BPJS). Tagihan akan otomatis berulang tiap bulannya.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(sortedBills, key = { it.id.ifBlank { it.nama } }) { bill ->
                    BillItem(
                        bill = bill,
                        selectedMonthKey = selectedMonthKey,
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
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Modal Bottom Sheet: Tambah Tagihan
    if (isAddSheetOpen) {
        AddBillBottomSheet(
            sheetState = sheetState,
            isLoading = uiState.isActionLoading,
            onDismiss = onDismissAddSheet,
            onSubmit = { newBill ->
                viewModel.addBill(newBill) {
                    scope.launch {
                        sheetState.hide()
                        onDismissAddSheet()
                    }
                }
            }
        )
    }

    // Toggle Paid Confirmation Dialog
    billToToggle?.let { bill ->
        val isAlreadyPaid = bill.isPaidForMonth(selectedMonthKey)
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
                Text(
                    text = if (!isAlreadyPaid) "Tandai Lunas?" else "Batal Tandai Lunas?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(text = "Tagihan: ${bill.nama}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "Periode: ${Formatters.formatMonthKeyDisplay(selectedMonthKey)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isAlreadyPaid) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Nominal Pembayaran Bulan Ini",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customPaymentTfv,
                            onValueChange = { tfv ->
                                val digits = tfv.text.filter { it.isDigit() }
                                val fmt = Formatters.formatCurrencyInput(digits)
                                customPaymentTfv = TextFieldValue(fmt, TextRange(fmt.length))
                            },
                            prefix = {
                                Text("Rp ", fontWeight = FontWeight.Bold, color = IndigoPrimary)
                            },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = IndigoPrimary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Akun Pembayaran Chip Row
                        Text(
                            text = "Bayar Menggunakan:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.paymentAccounts.forEach { acc ->
                                val isChosen = selectedPaymentAccount == acc
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isChosen) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedPaymentAccount = acc }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = acc,
                                        fontSize = 11.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isChosen) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Checkbox Catat Otomatis ke Transaksi
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { recordExpenseInTx = !recordExpenseInTx }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = recordExpenseInTx,
                                onCheckedChange = { recordExpenseInTx = it },
                                colors = CheckboxDefaults.colors(checkedColor = IndigoPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Catat otomatis ke riwayat pengeluaran",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Checkbox Jadikan Nominal Ini Sebagai Acuan Default Baru
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable { updateDefaultNominal = !updateDefaultNominal }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = updateDefaultNominal,
                                onCheckedChange = { updateDefaultNominal = it },
                                colors = CheckboxDefaults.colors(checkedColor = IndigoPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Simpan nominal ini sebagai acuan tagihan rutin",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Apakah Anda yakin ingin membatalkan status lunas untuk bulan ini? Pengeluaran terkait di riwayat transaksi akan dihapus.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedCustom = customPaymentTfv.text.replace("[^\\d]".toRegex(), "").toDoubleOrNull() ?: bill.nominal
                        viewModel.toggleBillPaid(
                            bill = bill,
                            monthKey = selectedMonthKey,
                            recordExpense = recordExpenseInTx,
                            paymentAccount = selectedPaymentAccount,
                            customNominal = parsedCustom,
                            updateDefaultNominal = updateDefaultNominal
                        )
                        billToToggle = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isAlreadyPaid) EmeraldIncome else AmberAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (!isAlreadyPaid) "Ya, Tandai Lunas" else "Ubah Jadi Belum Bayar", fontWeight = FontWeight.Bold)
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
            shape = RoundedCornerShape(22.dp)
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
            title = { Text("Hapus Tagihan Rutin?", fontWeight = FontWeight.Bold) },
            text = { Text("Hapus tagihan bulanan '${bill.nama}' dari daftar tagihan rutin?") },
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
            shape = RoundedCornerShape(22.dp)
        )
    }
}
