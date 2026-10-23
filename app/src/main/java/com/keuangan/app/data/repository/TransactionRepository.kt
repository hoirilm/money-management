package com.keuangan.app.data.repository

import com.keuangan.app.data.api.GoogleSheetApiService
import com.keuangan.app.data.api.RetrofitClient
import com.keuangan.app.data.model.ActionResponse
import com.keuangan.app.data.model.Bill
import com.keuangan.app.data.model.BillPostPayload
import com.keuangan.app.data.model.BillsResponse
import com.keuangan.app.data.model.SheetResponse
import com.keuangan.app.data.model.Transaction
import com.keuangan.app.data.model.TransactionPostPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class TransactionRepository(
    private val apiService: GoogleSheetApiService = RetrofitClient.apiService
) {

    suspend fun fetchTransactions(url: String): Result<SheetResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (url.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("URL Google Apps Script belum diisi di Pengaturan"))
                }
                val response = apiService.getTransactions(url)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun addTransaction(url: String, transaction: Transaction): Result<ActionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (url.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("URL Google Apps Script belum diisi di Pengaturan"))
                }
                val payload = TransactionPostPayload(
                    action = "add",
                    id = if (transaction.id.isBlank()) UUID.randomUUID().toString() else transaction.id,
                    tanggal = transaction.tanggal,
                    tipe = transaction.tipe,
                    kategori = transaction.kategori,
                    nominal = transaction.nominal,
                    akun = transaction.akun,
                    catatan = transaction.catatan,
                    pencatat = transaction.pencatat
                )
                val response = apiService.postTransaction(url, payload)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteTransaction(url: String, transactionId: String): Result<ActionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (url.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("URL Google Apps Script belum diisi di Pengaturan"))
                }
                val payload = TransactionPostPayload(
                    action = "delete",
                    id = transactionId
                )
                val response = apiService.postTransaction(url, payload)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // --- BILLS (TAGIHAN BULANAN) ---

    suspend fun fetchBills(url: String): Result<BillsResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (url.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("URL Google Apps Script belum diisi"))
                }
                val response = apiService.getBills(url)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun addBill(url: String, bill: Bill): Result<ActionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (url.isBlank()) return@withContext Result.failure(IllegalArgumentException("URL belum diisi"))
                val payload = BillPostPayload(
                    action = "add_bill",
                    id = if (bill.id.isBlank()) UUID.randomUUID().toString() else bill.id,
                    nama = bill.nama,
                    nominal = bill.nominal,
                    jatuhTempo = bill.jatuhTempo,
                    kategori = bill.kategori,
                    bulanLunas = bill.bulanLunas
                )
                val response = apiService.postBill(url, payload)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun toggleBill(
        url: String,
        billId: String,
        newBulanLunas: String,
        recordExpense: Boolean,
        bill: Bill?,
        pencatat: String,
        akun: String
    ): Result<ActionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (url.isBlank()) return@withContext Result.failure(IllegalArgumentException("URL belum diisi"))
                val payload = BillPostPayload(
                    action = "toggle_bill",
                    id = billId,
                    bulanLunas = newBulanLunas,
                    recordTransaction = recordExpense,
                    nama = bill?.nama,
                    nominal = bill?.nominal,
                    kategori = bill?.kategori,
                    pencatat = pencatat,
                    akun = akun
                )
                val response = apiService.postBill(url, payload)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun deleteBill(url: String, billId: String): Result<ActionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (url.isBlank()) return@withContext Result.failure(IllegalArgumentException("URL belum diisi"))
                val payload = BillPostPayload(
                    action = "delete_bill",
                    id = billId
                )
                val response = apiService.postBill(url, payload)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
