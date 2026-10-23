package com.keuangan.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keuangan.app.data.local.UserPreferences
import com.keuangan.app.data.model.Bill
import com.keuangan.app.data.model.Summary
import com.keuangan.app.data.model.Transaction
import com.keuangan.app.data.repository.TransactionRepository
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
    val errorMessage: String? = null,
    val successMessage: String? = null
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

    // Bills statistics for current month
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
    }

    fun loadAllData() {
        loadTransactions()
        loadBills()
    }

    fun loadTransactions() {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.fetchTransactions(currentUrl)

            result.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            transactions = response.data,
                            summary = response.summary ?: Summary()
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Gagal memuat transaksi"
                        )
                    }
                }
            )
        }
    }

    fun addTransaction(transaction: Transaction, onSuccess: () -> Unit) {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = repository.addTransaction(currentUrl, transaction)

            result.fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(
                            isActionLoading = false,
                            successMessage = "Transaksi berhasil dicatat ke Google Sheets!"
                        )
                    }
                    onSuccess()
                    loadTransactions()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Gagal menambahkan transaksi"
                        )
                    }
                }
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true, errorMessage = null) }
            val result = repository.deleteTransaction(currentUrl, transactionId)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = "Transaksi berhasil dihapus"
                        )
                    }
                    loadTransactions()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Gagal menghapus transaksi"
                        )
                    }
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
                    _uiState.update {
                        it.copy(
                            isBillsLoading = false,
                            errorMessage = error.localizedMessage ?: "Gagal memuat daftar tagihan"
                        )
                    }
                }
            )
        }
    }

    fun addBill(bill: Bill, onSuccess: () -> Unit) {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = repository.addBill(currentUrl, bill)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = "Tagihan '${bill.nama}' berhasil ditambahkan!"
                        )
                    }
                    onSuccess()
                    loadBills()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Gagal menambahkan tagihan"
                        )
                    }
                }
            )
        }
    }

    fun toggleBillPaid(bill: Bill, recordExpense: Boolean, paymentAccount: String = "BCA") {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) return

        val currentMonthKey = Formatters.getCurrentMonthYearKey()
        val isCurrentlyPaid = bill.isPaidForMonth(currentMonthKey)
        val newBulanLunas = if (isCurrentlyPaid) "" else currentMonthKey

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
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
                    val statusText = if (newBulanLunas.isNotBlank()) "ditandai Lunas" else "diubah menjadi Belum Bayar"
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = "Tagihan '${bill.nama}' $statusText!"
                        )
                    }
                    loadBills()
                    if (recordExpense && !isCurrentlyPaid) {
                        loadTransactions() // Refresh transaction list too
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Gagal memperbarui status tagihan"
                        )
                    }
                }
            )
        }
    }

    fun deleteBill(billId: String) {
        val currentUrl = _uiState.value.scriptUrl
        if (currentUrl.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }
            val result = repository.deleteBill(currentUrl, billId)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            successMessage = "Tagihan berhasil dihapus"
                        )
                    }
                    loadBills()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isActionLoading = false,
                            errorMessage = error.localizedMessage ?: "Gagal menghapus tagihan"
                        )
                    }
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

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
