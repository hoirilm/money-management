package com.keuangan.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.data.model.Bill
import com.keuangan.app.data.model.CategoryItem
import com.keuangan.app.data.model.FinanceConstants
import com.keuangan.app.ui.theme.CoralExpense
import com.keuangan.app.ui.theme.IndigoPrimary
import com.keuangan.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddBillBottomSheet(
    sheetState: SheetState,
    billCategories: List<CategoryItem> = FinanceConstants.billCategories,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAddCategory: (name: String, icon: String) -> Unit = { _, _ -> },
    onDeleteCategory: (name: String) -> Unit = {},
    onSubmit: (Bill) -> Unit
) {
    var billName by remember { mutableStateOf("") }
    var rawDigits by remember { mutableStateOf("") }
    var nominalTfv by remember { mutableStateOf(TextFieldValue("")) }
    var dueDay by remember { mutableIntStateOf(10) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isVariableBill by remember { mutableStateOf(false) }
    var selectedCategory by remember {
        mutableStateOf(billCategories.firstOrNull()?.name ?: "Tagihan & Listrik")
    }

    // Dialog & Mode Kelola Kategori Tagihan Custom
    var isManageCategoriesMode by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("⚡") }
    var categoryToDelete by remember { mutableStateOf<CategoryItem?>(null) }

    LaunchedEffect(billCategories) {
        if (billCategories.isNotEmpty() && billCategories.none { it.name.equals(selectedCategory, ignoreCase = true) }) {
            selectedCategory = billCategories.first().name
        } else if (billCategories.isEmpty()) {
            selectedCategory = "Lainnya"
            isManageCategoriesMode = false
        }
    }

    val parsedAmount = rawDigits.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tambah Tagihan Rutin Bulanan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Tutup")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nama Tagihan
            Text(
                text = "Nama Tagihan",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = billName,
                onValueChange = { billName = it },
                placeholder = { Text("Contoh: Wi-Fi Indihome, Listrik PLN...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Estimasi Nominal dengan Auto-Format Ribuan
            Text(
                text = "Estimasi Nominal (Rp)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = nominalTfv,
                onValueChange = { tfv ->
                    val digits = tfv.text.filter { it.isDigit() }
                    rawDigits = digits
                    val formatted = Formatters.formatCurrencyInput(digits)
                    nominalTfv = TextFieldValue(
                        text = formatted,
                        selection = TextRange(formatted.length)
                    )
                },
                placeholder = { Text("0", fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                prefix = {
                    Text("Rp ", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = IndigoPrimary)
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = IndigoPrimary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IndigoPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Add Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    50_000L to "+50rb",
                    100_000L to "+100rb",
                    250_000L to "+250rb",
                    500_000L to "+500rb"
                )
                presets.forEach { (addVal, label) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                val current = rawDigits.toLongOrNull() ?: 0L
                                val newTotal = (current + addVal).toString()
                                rawDigits = newTotal
                                val formatted = Formatters.formatCurrencyInput(newTotal)
                                nominalTfv = TextFieldValue(
                                    text = formatted,
                                    selection = TextRange(formatted.length)
                                )
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Opsi Tagihan Fleksibel / Jumlah Berubah
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        1.dp,
                        if (isVariableBill) IndigoPrimary.copy(alpha = 0.3f) else Color.Transparent,
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { isVariableBill = !isVariableBill }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isVariableBill,
                    onCheckedChange = { isVariableBill = it },
                    colors = CheckboxDefaults.colors(checkedColor = IndigoPrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Jumlah Tagihan Berubah Setiap Bulan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                    Text(
                        text = "Contoh: Listrik PLN, Air PDAM, atau Kartu Kredit. Anda bisa memasukkan nominal aktual saat mencentang lunas.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Pemilihan Tanggal Jatuh Tempo (Simpel DatePicker)
            Text(
                text = "Jatuh Tempo Tiap Bulan",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { showDatePicker = true },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndigoPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = IndigoPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Tanggal $dueDay Setiap Bulan",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Ketuk untuk memilih dari kalender",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IndigoPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Pilih",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Presets Ringkas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    1 to "Tgl 1",
                    10 to "Tgl 10",
                    20 to "Tgl 20",
                    25 to "Tgl 25"
                ).forEach { (presetDay, label) ->
                    val isChosen = dueDay == presetDay
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isChosen) IndigoPrimary.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = if (isChosen) 1.dp else 0.dp,
                                color = if (isChosen) IndigoPrimary else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { dueDay = presetDay }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                            color = if (isChosen) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Material 3 DatePickerDialog
            if (showDatePicker) {
                val initialMillis = remember(dueDay) {
                    java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                        set(java.util.Calendar.DAY_OF_MONTH, dueDay.coerceIn(1, 28))
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
                                        timeInMillis = millis
                                    }
                                    dueDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
                                }
                                showDatePicker = false
                            }
                        ) {
                            Text("Pilih", color = IndigoPrimary, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Batal")
                        }
                    }
                ) {
                    DatePicker(
                        state = datePickerState,
                        title = {
                            Text(
                                text = "Pilih Tanggal Jatuh Tempo",
                                modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        headline = {
                            Text(
                                text = "Pilih tanggal jatuh tempo bulanan",
                                modifier = Modifier.padding(start = 24.dp, bottom = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Kategori Tagihan (Custom: Bisa Ditambah & Dihapus)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kategori Tagihan",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isManageCategoriesMode) "Ketuk kategori untuk menghapus" else "Tekan lama kategori atau ketuk Hapus",
                        fontSize = 10.sp,
                        color = if (isManageCategoriesMode) CoralExpense else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                if (billCategories.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isManageCategoriesMode) CoralExpense.copy(alpha = 0.14f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { isManageCategoriesMode = !isManageCategoriesMode }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isManageCategoriesMode) "Selesai" else "Hapus",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isManageCategoriesMode) CoralExpense else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                billCategories.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isManageCategoriesMode -> CoralExpense.copy(alpha = 0.10f)
                                    isSelected -> IndigoPrimary.copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .border(
                                width = when {
                                    isManageCategoriesMode -> 1.dp
                                    isSelected -> 1.5.dp
                                    else -> 0.dp
                                },
                                color = when {
                                    isManageCategoriesMode -> CoralExpense.copy(alpha = 0.45f)
                                    isSelected -> IndigoPrimary
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    if (isManageCategoriesMode) {
                                        categoryToDelete = cat
                                    } else {
                                        selectedCategory = cat.name
                                    }
                                },
                                onLongClick = {
                                    categoryToDelete = cat
                                }
                            )
                            .padding(horizontal = 11.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = cat.icon, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = cat.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected && !isManageCategoriesMode) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isManageCategoriesMode) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Hapus ${cat.name}",
                                    tint = CoralExpense,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Tombol Tambah Kategori Tagihan Custom
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(IndigoPrimary.copy(alpha = 0.1f))
                        .border(1.dp, IndigoPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable {
                            isManageCategoriesMode = false
                            newCategoryName = ""
                            newCategoryIcon = "⚡"
                            showAddCategoryDialog = true
                        }
                        .padding(horizontal = 11.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Tambah Kategori Tagihan",
                            tint = IndigoPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Tambah",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Submit Button (Tagihan bisa diset meskipun nominal = 0)
            val isFormValid = billName.isNotBlank() && parsedAmount >= 0 && !isLoading

            Button(
                onClick = {
                    if (isFormValid) {
                        val displayName = if (isVariableBill && !billName.contains("(Fleksibel)", ignoreCase = true)) {
                            "${billName.trim()} (Fleksibel)"
                        } else {
                            billName.trim()
                        }
                        val newBill = Bill(
                            nama = displayName,
                            nominal = parsedAmount,
                            jatuhTempo = dueDay,
                            kategori = selectedCategory.ifBlank { "Lainnya" },
                            bulanLunas = ""
                        )
                        onSubmit(newBill)
                    }
                },
                enabled = isFormValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Menyimpan Tagihan...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Simpan Tagihan Bulanan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Dialog Tambah Kategori Tagihan Custom
    if (showAddCategoryDialog) {
        val billEmojis = listOf(
            "⚡", "🌐", "💳", "💧", "📚", "🎬", "🛡️", "🏠",
            "🏢", "📱", "🚗", "🏥", "🗑️", "🔒", "🏷️", "📦"
        )

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text("Tambah Kategori Tagihan", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Pilih Ikon:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        billEmojis.forEach { emoji ->
                            val isChosen = newCategoryIcon == emoji
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isChosen) IndigoPrimary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        width = if (isChosen) 2.dp else 0.dp,
                                        color = if (isChosen) IndigoPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { newCategoryIcon = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Nama Kategori Tagihan") },
                        placeholder = { Text("Contoh: Iuran Komplek, Gas, Hosting...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCategoryName.trim()
                        if (trimmed.isNotBlank()) {
                            onAddCategory(trimmed, newCategoryIcon)
                            selectedCategory = trimmed
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Tambah", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showAddCategoryDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }

    // Dialog Konfirmasi Hapus Kategori Tagihan
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = {
                Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, tint = CoralExpense)
            },
            title = { Text("Hapus Kategori Tagihan?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Hapus kategori '${cat.icon} ${cat.name}' dari daftar kategori tagihan?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCategory(cat.name)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralExpense),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { categoryToDelete = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }
}
