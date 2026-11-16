package com.keuangan.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keuangan.app.data.local.UserPreferences
import com.keuangan.app.data.model.Bill
import com.keuangan.app.data.model.CategoryItem
import com.keuangan.app.data.model.FinanceConstants
import com.keuangan.app.data.model.Summary
import com.keuangan.app.data.model.Transaction
import com.keuangan.app.data.repository.TransactionRepository
import com.keuangan.app.ui.components.ModernNotificationData
import com.keuangan.app.ui.components.NotificationType
import com.keuangan.app.util.Formatters
import com.keuangan.app.util.TimePeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class FinanceUiState(
    val scriptUrl: String = "",
    val activeProfile: String = "Suami",
    val filterUser: String = "Semua", // "Semua", "Suami", "Istri"
    val filterPeriod: TimePeriod = TimePeriod.ALL_TIME,
    val transactions: List<Transaction> = emptyList(),
    val summary: Summary = Summary(),
    val bills: List<Bill> = emptyList(),
    val isLoading: Boolean = false,
    val isBillsLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val excelExportUrl: String? = null,
    val spreadsheetUrl: String? = null,
    val notification: ModernNotificationData? = null,
    val expenseCategories: List<CategoryItem> = FinanceConstants.expenseCategories,
    val incomeCategories: List<CategoryItem> = FinanceConstants.incomeCategories,
    val billCategories: List<CategoryItem> = FinanceConstants.billCategories,
    val paymentAccounts: List<String> = FinanceConstants.paymentAccounts,
    val monthlyBudget: Double = 5000000.0
) {
    val filteredTransactions: List<Transaction>
        get() {
            var list = transactions
            if (filterUser != "Semua") {
                list = list.filter { it.pencatat.equals(filterUser, ignoreCase = true) }
            }
            if (filterPeriod != TimePeriod.ALL_TIME) {
                list = list.filter { Formatters.isDateInPeriod(it.tanggal, filterPeriod) }
            }
            return list
        }

    val filteredSummary: Summary
        get() {
            val filtered = filteredTransactions
            var inc = 0.0
            var exp = 0.0
            filtered.forEach {
                if (it.isIncome) inc += it.nominal
                else if (it.isExpense) exp += it.nominal
            }
            return Summary(
                totalIncome = inc,
                totalExpense = exp,
                balance = inc - exp,
                totalTransactions = filtered.size
            )
        }

    val currentMonthKey: String get() = Formatters.getCurrentMonthYearKey()

    val totalBillsCount: Int get() = bills.size

    val paidBillsCount: Int
        get() = bills.count { it.isPaidForMonth(currentMonthKey) }

    val unpaidBillsTotalAmount: Double
        get() = bills.filter { !it.isPaidForMonth(currentMonthKey) }.sumOf { it.nominal }

    val totalBillsAmount: Double
        get() = bills.sumOf { it.nominal }
}

class FinanceViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val repository = TransactionRepository()

    private val _uiState = MutableStateFlow(FinanceUiState())
    val uiState: StateFlow<FinanceUiState> = _uiState.asStateFlow()

    // Melindungi state optimistis tagihan yang sedang diproses agar tidak tertimpa fetch lama
    private val pendingBillToggleKeys: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private fun billKey(bill: Bill): String = bill.id.ifBlank { bill.nama.trim().lowercase() }

    init {
        viewModelScope.launch {
            val savedUrl = userPreferences.scriptUrlFlow.first()
            val savedUser = userPreferences.currentUserFlow.first()

            _uiState.update {
                it.copy(
                    scriptUrl = savedUrl,
                    activeProfile = savedUser
                )
            }

            if (savedUrl.isNotBlank()) {
                loadAllData()
            }
        }

        viewModelScope.launch {
            userPreferences.expenseCategoriesFlow.collect { list ->
                _uiState.update { it.copy(expenseCategories = list) }
            }
        }
        viewModelScope.launch {
            userPreferences.incomeCategoriesFlow.collect { list ->
                _uiState.update { it.copy(incomeCategories = list) }
            }
        }
        viewModelScope.launch {
            userPreferences.billCategoriesFlow.collect { list ->
                _uiState.update { it.copy(billCategories = list) }
            }
        }
        viewModelScope.launch {
            userPreferences.accountsFlow.collect { list ->
                _uiState.update { it.copy(paymentAccounts = list) }
            }
        }
        viewModelScope.launch {
            userPreferences.monthlyBudgetFlow.collect { budget ->
                _uiState.update { it.copy(monthlyBudget = budget) }
            }
        }
    }

    fun updateMonthlyBudget(newBudget: Double) {
        viewModelScope.launch {
            userPreferences.saveMonthlyBudget(newBudget)
            _uiState.update { it.copy(monthlyBudget = newBudget) }
            showNotification("Target anggaran berhasil diperbarui!", NotificationType.SUCCESS)
        }
    }

    fun showNotification(message: String, type: NotificationType = NotificationType.SUCCESS) {
        _uiState.update { it.copy(notification = ModernNotificationData(message, type)) }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notification = null) }
    }

    fun loadAllData() {
        loadTransactions()
        loadBills()
    }

    fun loadTransactions() {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.fetchTransactions(currentUrl)

            result.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            transactions = response.data,
                            summary = response.summary ?: Summary(),
                            excelExportUrl = response.excelExportUrl,
                            spreadsheetUrl = response.spreadsheetUrl
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    showNotification(error.localizedMessage ?: "Gagal memuat transaksi", NotificationType.ERROR)
                }
            )
        }
    }

    fun addTransaction(transaction: Transaction, onSuccess: () -> Unit) {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) {
            showNotification("URL Google Apps Script belum diisi", NotificationType.ERROR)
            return
        }

        val previousTransactions = _uiState.value.transactions
        val newTx = if (transaction.id.isBlank()) {
            transaction.copy(id = UUID.randomUUID().toString())
        } else {
            transaction
        }

        // Optimistic update: langsung munculkan transaksi di list HP
        _uiState.update {
            it.copy(transactions = listOf(newTx) + previousTransactions)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = repository.addTransaction(currentUrl, newTx)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isActionLoading = false) }
                    showNotification("Transaksi berhasil dicatat ke Google Sheets!", NotificationType.SUCCESS)
                    onSuccess()
                    loadTransactions()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            transactions = previousTransactions
                        )
                    }
                    showNotification(error.localizedMessage ?: "Gagal menambahkan transaksi", NotificationType.ERROR)
                }
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = repository.deleteTransaction(currentUrl, transactionId)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isActionLoading = false) }
                    showNotification("Transaksi berhasil dihapus", NotificationType.SUCCESS)
                    loadTransactions()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isActionLoading = false) }
                    showNotification(error.localizedMessage ?: "Gagal menghapus transaksi", NotificationType.ERROR)
                }
            )
        }
    }

    // --- BILLS (TAGIHAN BULANAN) ---

    fun loadBills() {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBillsLoading = true) }
            val result = repository.fetchBills(currentUrl)

            result.fold(
                onSuccess = { response ->
                    val currentLocalBills = _uiState.value.bills
                    val mergedBills = response.data.map { remoteBill ->
                        val key = billKey(remoteBill)
                        if (pendingBillToggleKeys.contains(key)) {
                            currentLocalBills.firstOrNull { billKey(it) == key } ?: remoteBill
                        } else {
                            remoteBill
                        }
                    }

                    val remoteCategories = response.categories
                    if (remoteCategories != null) {
                        userPreferences.saveBillCategories(remoteCategories)
                    }

                    _uiState.update {
                        it.copy(
                            isBillsLoading = false,
                            bills = mergedBills,
                            billCategories = remoteCategories ?: it.billCategories
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isBillsLoading = false) }
                    showNotification(error.localizedMessage ?: "Gagal memuat daftar tagihan", NotificationType.ERROR)
                }
            )
        }
    }

    fun addBill(bill: Bill, onSuccess: () -> Unit) {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) {
            showNotification("URL Google Apps Script belum diisi", NotificationType.ERROR)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = repository.addBill(currentUrl, bill)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isActionLoading = false) }
                    showNotification("Tagihan '${bill.nama}' berhasil ditambahkan!", NotificationType.SUCCESS)
                    onSuccess()
                    loadBills()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isActionLoading = false) }
                    showNotification(error.localizedMessage ?: "Gagal menambahkan tagihan", NotificationType.ERROR)
                }
            )
        }
    }

    /**
     * Memperbarui status centang/uncheck tagihan dengan OPTIMISTIC UPDATE & SINKRONISASI SPESIFIK:
     * - Saat dicentang dengan opsi catat otomatis: checklist tercentang & transaksi pengeluaran otomatis masuk ke riwayat.
     * - Saat di-uncheck: transaksi spesifik yang sebelumnya tercatat otomatis di-reverse (dihapus) & checklist kembali uncheck.
     */
    fun toggleBillPaid(
        bill: Bill,
        monthKey: String,
        recordExpense: Boolean,
        paymentAccount: String = "BCA",
        customNominal: Double = bill.nominal,
        updateDefaultNominal: Boolean = false
    ) {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) {
            showNotification("URL Google Apps Script belum diisi", NotificationType.ERROR)
            return
        }

        val key = billKey(bill)
        val isCurrentlyPaid = bill.isPaidForMonth(monthKey)
        val targetIsPaid = !isCurrentlyPaid
        val shouldRecordTx = targetIsPaid && recordExpense
        val actualNominal = if (customNominal > 0.0) customNominal else bill.nominal
        val activeUser = _uiState.value.activeProfile

        val newBulanLunas = bill.withPaidForMonth(monthKey, targetIsPaid)

        var optimisticCreatedTx: Transaction? = null
        var optimisticRemovedTx: Transaction? = null
        var optimisticRemovedIndex = -1
        val payloadTxId: String?
        val newAutoTxMap: String

        if (targetIsPaid) {
            if (shouldRecordTx) {
                val generatedTxId = "billtx_${UUID.randomUUID()}"
                val timestamp = Formatters.getCurrentTimestamp()
                val note = "Pembayaran ${bill.nama} (${Formatters.formatMonthKeyDisplay(monthKey)})"
                optimisticCreatedTx = Transaction(
                    id = generatedTxId,
                    tanggal = timestamp,
                    tipe = "EXPENSE",
                    kategori = bill.kategori,
                    nominal = actualNominal,
                    akun = paymentAccount,
                    catatan = note,
                    pencatat = activeUser
                )
                payloadTxId = generatedTxId
                newAutoTxMap = bill.withAutoTxForMonth(monthKey, generatedTxId)
            } else {
                payloadTxId = null
                newAutoTxMap = bill.withAutoTxForMonth(monthKey, "NONE")
            }
        } else {
            val linkedTxId = bill.getAutoTxIdForMonth(monthKey)
            payloadTxId = linkedTxId
            newAutoTxMap = bill.withAutoTxForMonth(monthKey, null)

            val currentTxList = _uiState.value.transactions
            if (linkedTxId.equals("NONE", ignoreCase = true)) {
                // Tagihan sebelumnya dicentang tanpa catat otomatis -> jangan hapus transaksi apa pun
            } else if (!linkedTxId.isNullOrBlank()) {
                val idx = currentTxList.indexOfFirst { it.id == linkedTxId }
                if (idx != -1) {
                    optimisticRemovedIndex = idx
                    optimisticRemovedTx = currentTxList[idx]
                }
            } else {
                // Fallback data lama (sebelum autoTxMap ada): cari maksimal 1 transaksi pembayaran tagihan ini
                val exactNote = "Pembayaran ${bill.nama}"
                val exactNoteDisplay = "Pembayaran ${bill.nama} (${Formatters.formatMonthKeyDisplay(monthKey)})"
                val exactNoteKey = "Pembayaran ${bill.nama} ($monthKey)"
                val idx = currentTxList.indexOfFirst { tx ->
                    tx.isExpense && (
                        tx.catatan.equals(exactNoteDisplay, ignoreCase = true) ||
                            tx.catatan.equals(exactNoteKey, ignoreCase = true) ||
                            (tx.catatan.equals(exactNote, ignoreCase = true) && tx.tanggal.startsWith(monthKey))
                        )
                }.let { found ->
                    if (found != -1) found
                    else currentTxList.indexOfFirst { tx ->
                        tx.isExpense && tx.catatan.equals(exactNote, ignoreCase = true)
                    }
                }
                if (idx != -1) {
                    optimisticRemovedIndex = idx
                    optimisticRemovedTx = currentTxList[idx]
                }
            }
        }

        pendingBillToggleKeys.add(key)

        // 1. OPTIMISTIC UPDATE: Langsung perbarui checklist tagihan & riwayat transaksi seketika
        _uiState.update { state ->
            val updatedBills = state.bills.map { b ->
                if (billKey(b) == key) {
                    b.copy(
                        bulanLunas = newBulanLunas,
                        autoTxMap = newAutoTxMap,
                        nominal = if (targetIsPaid && updateDefaultNominal) actualNominal else b.nominal
                    )
                } else {
                    b
                }
            }

            val updatedTransactions = when {
                optimisticCreatedTx != null -> listOf(optimisticCreatedTx) + state.transactions
                optimisticRemovedTx != null -> state.transactions.filterIndexed { index, tx ->
                    if (optimisticRemovedTx.id.isNotBlank()) tx.id != optimisticRemovedTx.id
                    else index != optimisticRemovedIndex
                }
                else -> state.transactions
            }

            state.copy(
                bills = updatedBills,
                transactions = updatedTransactions
            )
        }

        // 2. Kirim update ke Google Sheets API
        viewModelScope.launch {
            val result = repository.toggleBill(
                url = currentUrl,
                billId = bill.id,
                monthKey = monthKey,
                isPaid = targetIsPaid,
                newBulanLunas = newBulanLunas,
                recordExpense = shouldRecordTx,
                transactionId = payloadTxId,
                tanggal = optimisticCreatedTx?.tanggal,
                catatan = optimisticCreatedTx?.catatan,
                bill = bill,
                customNominal = actualNominal,
                updateDefaultNominal = targetIsPaid && updateDefaultNominal,
                pencatat = activeUser,
                akun = paymentAccount
            )

            pendingBillToggleKeys.remove(key)

            result.fold(
                onSuccess = { response ->
                    if (response.bulanLunas != null || response.autoTxMap != null) {
                        _uiState.update { state ->
                            state.copy(
                                bills = state.bills.map { b ->
                                    if (billKey(b) == key) {
                                        b.copy(
                                            bulanLunas = response.bulanLunas ?: b.bulanLunas,
                                            autoTxMap = response.autoTxMap ?: b.autoTxMap
                                        )
                                    } else {
                                        b
                                    }
                                }
                            )
                        }
                    }

                    val wasRecordedWithoutTx = !targetIsPaid &&
                        bill.getAutoTxIdForMonth(monthKey).equals("NONE", ignoreCase = true)
                    val statusText = when {
                        targetIsPaid && shouldRecordTx -> "ditandai Lunas & tercatat di Riwayat Pengeluaran"
                        targetIsPaid -> "ditandai Lunas"
                        wasRecordedWithoutTx -> "diubah menjadi Belum Bayar"
                        else -> "diubah menjadi Belum Bayar & pengeluaran di-reverse"
                    }
                    showNotification("Tagihan '${bill.nama}' $statusText!", NotificationType.SUCCESS)

                    loadTransactions()
                    loadBills()
                },
                onFailure = { error ->
                    // Revert spesifik perubahan pada tagihan & transaksi ini jika gagal ke Google Sheets
                    _uiState.update { state ->
                        val revertedBills = state.bills.map { b ->
                            if (billKey(b) == key) bill else b
                        }
                        val revertedTransactions = when {
                            optimisticCreatedTx != null -> state.transactions.filterNot { it.id == optimisticCreatedTx.id }
                            optimisticRemovedTx != null -> {
                                val insertIdx = optimisticRemovedIndex.coerceIn(0, state.transactions.size)
                                state.transactions.toMutableList().apply {
                                    add(insertIdx, optimisticRemovedTx)
                                }
                            }
                            else -> state.transactions
                        }
                        state.copy(
                            bills = revertedBills,
                            transactions = revertedTransactions
                        )
                    }
                    showNotification(error.localizedMessage ?: "Gagal memperbarui status di Google Sheets", NotificationType.ERROR)
                }
            )
        }
    }

    fun deleteBill(billId: String) {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) return

        val previousBills = _uiState.value.bills
        // Optimistic delete
        _uiState.update { it.copy(bills = previousBills.filter { it.id != billId }) }

        viewModelScope.launch {
            val result = repository.deleteBill(currentUrl, billId)

            result.fold(
                onSuccess = {
                    showNotification("Tagihan berhasil dihapus", NotificationType.SUCCESS)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(bills = previousBills) }
                    showNotification(error.localizedMessage ?: "Gagal menghapus tagihan", NotificationType.ERROR)
                }
            )
        }
    }

    fun testConnection(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = repository.testConnection(url)
            _uiState.update { it.copy(isActionLoading = false) }

            result.fold(
                onSuccess = { message ->
                    onResult(true, message)
                },
                onFailure = { error ->
                    onResult(false, error.localizedMessage ?: "Gagal terhubung")
                }
            )
        }
    }

    fun saveScriptUrl(url: String) {
        viewModelScope.launch {
            userPreferences.saveScriptUrl(url)
            _uiState.update { it.copy(scriptUrl = url) }
            loadAllData()
        }
    }

    fun setActiveProfile(profile: String) {
        viewModelScope.launch {
            userPreferences.saveCurrentUser(profile)
            _uiState.update { it.copy(activeProfile = profile) }
        }
    }

    fun setFilterUser(filter: String) {
        _uiState.update { it.copy(filterUser = filter) }
    }

    fun setFilterPeriod(period: TimePeriod) {
        _uiState.update { it.copy(filterPeriod = period) }
    }

    fun addCategory(isExpense: Boolean, name: String, icon: String = "🏷️") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            if (isExpense) {
                val current = _uiState.value.expenseCategories
                if (current.none { it.name.equals(trimmed, ignoreCase = true) }) {
                    val updated = current + CategoryItem(trimmed, icon.ifBlank { "🏷️" })
                    userPreferences.saveExpenseCategories(updated)
                    showNotification("Kategori '$trimmed' berhasil ditambahkan", NotificationType.SUCCESS)
                } else {
                    showNotification("Kategori '$trimmed' sudah ada", NotificationType.WARNING)
                }
            } else {
                val current = _uiState.value.incomeCategories
                if (current.none { it.name.equals(trimmed, ignoreCase = true) }) {
                    val updated = current + CategoryItem(trimmed, icon.ifBlank { "💰" })
                    userPreferences.saveIncomeCategories(updated)
                    showNotification("Kategori '$trimmed' berhasil ditambahkan", NotificationType.SUCCESS)
                } else {
                    showNotification("Kategori '$trimmed' sudah ada", NotificationType.WARNING)
                }
            }
        }
    }

    fun deleteCategory(isExpense: Boolean, name: String) {
        viewModelScope.launch {
            if (isExpense) {
                val current = _uiState.value.expenseCategories
                val updated = current.filterNot { it.name.equals(name, ignoreCase = true) }
                userPreferences.saveExpenseCategories(updated)
                showNotification("Kategori '$name' berhasil dihapus", NotificationType.INFO)
            } else {
                val current = _uiState.value.incomeCategories
                val updated = current.filterNot { it.name.equals(name, ignoreCase = true) }
                userPreferences.saveIncomeCategories(updated)
                showNotification("Kategori '$name' berhasil dihapus", NotificationType.INFO)
            }
        }
    }

    // --- KATEGORI TAGIHAN (CUSTOM: TAMBAH & HAPUS) ---

    fun addBillCategory(name: String, icon: String = "🏷️") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val chosenIcon = icon.ifBlank { "🏷️" }

        val current = _uiState.value.billCategories
        if (current.any { it.name.equals(trimmed, ignoreCase = true) }) {
            showNotification("Kategori tagihan '$trimmed' sudah ada", NotificationType.WARNING)
            return
        }

        val updated = current + CategoryItem(trimmed, chosenIcon)
        _uiState.update { it.copy(billCategories = updated) }

        viewModelScope.launch {
            userPreferences.saveBillCategories(updated)
            showNotification("Kategori tagihan '$trimmed' berhasil ditambahkan", NotificationType.SUCCESS)

            val currentUrl = _uiState.value.scriptUrl
            if (currentUrl.isNotBlank()) {
                repository.addBillCategory(currentUrl, trimmed, chosenIcon).onSuccess { response ->
                    response.categories?.let { remoteCats ->
                        userPreferences.saveBillCategories(remoteCats)
                        _uiState.update { it.copy(billCategories = remoteCats) }
                    }
                }
            }
        }
    }

    fun deleteBillCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return

        val current = _uiState.value.billCategories
        val updated = current.filterNot { it.name.equals(trimmed, ignoreCase = true) }
        _uiState.update { it.copy(billCategories = updated) }

        viewModelScope.launch {
            userPreferences.saveBillCategories(updated)
            showNotification("Kategori tagihan '$trimmed' berhasil dihapus", NotificationType.INFO)

            val currentUrl = _uiState.value.scriptUrl
            if (currentUrl.isNotBlank()) {
                repository.deleteBillCategory(currentUrl, trimmed).onSuccess { response ->
                    response.categories?.let { remoteCats ->
                        userPreferences.saveBillCategories(remoteCats)
                        _uiState.update { it.copy(billCategories = remoteCats) }
                    }
                }
            }
        }
    }

    fun addAccount(accountName: String) {
        val trimmed = accountName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val current = _uiState.value.paymentAccounts
            if (current.none { it.equals(trimmed, ignoreCase = true) }) {
                val updated = current + trimmed
                userPreferences.saveAccounts(updated)
                showNotification("Akun '$trimmed' berhasil ditambahkan", NotificationType.SUCCESS)
            } else {
                showNotification("Akun '$trimmed' sudah ada", NotificationType.WARNING)
            }
        }
    }

    fun deleteAccount(accountName: String) {
        viewModelScope.launch {
            val current = _uiState.value.paymentAccounts
            val updated = current.filterNot { it.equals(accountName, ignoreCase = true) }
            userPreferences.saveAccounts(updated)
            showNotification("Akun '$accountName' berhasil dihapus", NotificationType.INFO)
        }
    }
}
