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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FinanceUiState(
    val scriptUrl: String = "",
    val activeProfile: String = "Suami",
    val filterUser: String = "Semua", // "Semua", "Suami", "Istri"
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
    val paymentAccounts: List<String> = FinanceConstants.paymentAccounts
) {
    val filteredTransactions: List<Transaction>
        get() = if (filterUser == "Semua") {
            transactions
        } else {
            transactions.filter { it.pencatat.equals(filterUser, ignoreCase = true) }
        }

    val filteredSummary: Summary
        get() {
            if (filterUser == "Semua") return summary
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
            userPreferences.accountsFlow.collect { list ->
                _uiState.update { it.copy(paymentAccounts = list) }
            }
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

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = repository.addTransaction(currentUrl, transaction)

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isActionLoading = false) }
                    showNotification("Transaksi berhasil dicatat ke Google Sheets!", NotificationType.SUCCESS)
                    onSuccess()
                    loadTransactions()
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isActionLoading = false) }
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
                    _uiState.update {
                        it.copy(
                            isBillsLoading = false,
                            bills = response.data
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
     * Memperbarui status centang/uncheck tagihan dengan OPTIMISTIC UPDATE
     * (Tampilan di HP berubah seketika, lalu disinkronkan ke Google Sheets di background).
     */
    fun toggleBillPaid(bill: Bill, monthKey: String, recordExpense: Boolean, paymentAccount: String = "BCA") {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) {
            showNotification("URL Google Apps Script belum diisi", NotificationType.ERROR)
            return
        }

        val previousBills = _uiState.value.bills
        val previousTransactions = _uiState.value.transactions
        val isCurrentlyPaid = bill.isPaidForMonth(monthKey)
        val newBulanLunas = bill.toggleMonth(monthKey)

        // 1. OPTIMISTIC UPDATE: Langsung update status di UI seketika!
        val updatedBills = previousBills.map { b ->
            if (b.id == bill.id || b.nama.equals(bill.nama, ignoreCase = true)) {
                b.copy(bulanLunas = newBulanLunas)
            } else {
                b
            }
        }

        // JIKA DI-UNCHECK (sebelumnya lunas):
        // Hapus catatan pengeluaran pembayaran tagihan ini seketika agar menu tagihan dan transaksi sinkron!
        val updatedTransactions = if (isCurrentlyPaid) {
            previousTransactions.filterNot { tx ->
                tx.catatan.contains(bill.nama, ignoreCase = true) ||
                tx.catatan.contains("pembayaran ${bill.nama}", ignoreCase = true)
            }
        } else {
            previousTransactions
        }

        _uiState.update { it.copy(bills = updatedBills, transactions = updatedTransactions) }

        // 2. Kirim update ke Google Sheets
        viewModelScope.launch {
            val result = repository.toggleBill(
                url = currentUrl,
                billId = bill.id,
                newBulanLunas = newBulanLunas,
                recordExpense = recordExpense && !isCurrentlyPaid,
                bill = bill,
                pencatat = _uiState.value.activeProfile,
                akun = paymentAccount
            )

            result.fold(
                onSuccess = {
                    val statusText = if (!isCurrentlyPaid) "ditandai Lunas" else "diubah menjadi Belum Bayar"
                    showNotification("Tagihan '${bill.nama}' $statusText!", NotificationType.SUCCESS)
                    // Selalu reload transaksi agar total saldo dan riwayat 100% akurat
                    loadTransactions()
                },
                onFailure = { error ->
                    // Revert perubahan jika gagal ke Google Sheets
                    _uiState.update { it.copy(bills = previousBills, transactions = previousTransactions) }
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
