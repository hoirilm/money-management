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
import java.net.SocketTimeoutException
import java.util.UUID

class TransactionRepository(
    private val apiService: GoogleSheetApiService = RetrofitClient.apiService
) {

    suspend fun testConnection(url: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val cleanUrl = url.trim()
                if (cleanUrl.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("URL tidak boleh kosong"))
                }
                if (cleanUrl.contains("docs.google.com/spreadsheets")) {
                    return@withContext Result.failure(IllegalArgumentException("Anda memasukkan URL dokumen Google Sheets! Yang dibutuhkan adalah Web App URL dari Extensions > Apps Script > Deploy > Web app."))
                }
                if (!cleanUrl.startsWith("https://script.google.com/macros/s/")) {
                    return@withContext Result.failure(IllegalArgumentException("URL harus berawalan 'https://script.google.com/macros/s/'"))
                }
                if (cleanUrl.endsWith("/dev")) {
                    return@withContext Result.failure(IllegalArgumentException("URL berakhiran /dev membutuhkan login Google. Gunakan URL hasil 'New deployment' yang berakhiran /exec"))
                }

                val response = apiService.ping(cleanUrl)
                if (response.status.equals("success", ignoreCase = true)) {
                    Result.success("Koneksi Berhasil! Terhubung ke Google Sheets (${response.data.size} transaksi ditemukan)")
                } else {
                    Result.failure(Exception("Respons server: ${response.message ?: response.status}"))
                }
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("Koneksi timeout. Pastikan koneksi internet stabil dan opsi 'Who has access' di Apps Script sudah diatur ke 'Anyone'."))
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: e.message ?: "Gagal terhubung"
                if (msg.contains("Expected BEGIN_OBJECT but was STRING") || msg.contains("HTML")) {
                    Result.failure(Exception("Akses Ditolak oleh Google. Pastikan pengaturan Deploy Apps Script: 'Who has access' = 'Anyone'."))
                } else {
                    Result.failure(Exception(msg))
                }
            }
        }
    }

    suspend fun fetchTransactions(url: String): Result<SheetResponse> {
        return withContext(Dispatchers.IO) {
            try {
                if (url.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("URL Google Apps Script belum diisi di Pengaturan"))
                }
                val response = apiService.getTransactions(url.trim())
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
                val response = apiService.postTransaction(url.trim(), payload)
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
                val response = apiService.postTransaction(url.trim(), payload)
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
                val response = apiService.getBills(url.trim())
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
                val response = apiService.postBill(url.trim(), payload)
                if (response.status != "success") {
                    Result.failure(Exception(response.message.ifBlank { "Gagal menambahkan tagihan di Google Sheets" }))
                } else {
                    Result.success(response)
                }
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
        customNominal: Double? = null,
        updateDefaultNominal: Boolean = false,
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
                    nominal = customNominal ?: bill?.nominal,
                    kategori = bill?.kategori,
                    pencatat = pencatat,
                    akun = akun,
                    updateDefaultNominal = updateDefaultNominal
                )
                val response = apiService.postBill(url.trim(), payload)
                if (response.status != "success") {
                    Result.failure(Exception(response.message.ifBlank { "Gagal memperbarui status tagihan di Google Sheets" }))
                } else {
                    Result.success(response)
                }
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
                val response = apiService.postBill(url.trim(), payload)
                if (response.status != "success") {
                    Result.failure(Exception(response.message.ifBlank { "Gagal menghapus tagihan di Google Sheets" }))
                } else {
                    Result.success(response)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
