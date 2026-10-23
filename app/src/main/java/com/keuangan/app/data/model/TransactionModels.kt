package com.keuangan.app.data.model

import com.google.gson.annotations.SerializedName

enum class TransactionType {
    EXPENSE,
    INCOME
}

data class Transaction(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("tanggal")
    val tanggal: String = "",

    @SerializedName("tipe")
    val tipe: String = "EXPENSE", // "EXPENSE" or "INCOME"

    @SerializedName("kategori")
    val kategori: String = "Umum",

    @SerializedName("nominal")
    val nominal: Double = 0.0,

    @SerializedName("akun")
    val akun: String = "Cash",

    @SerializedName("catatan")
    val catatan: String = "",

    @SerializedName("pencatat")
    val pencatat: String = "Suami"
) {
    val isExpense: Boolean get() = tipe.equals("EXPENSE", ignoreCase = true)
    val isIncome: Boolean get() = tipe.equals("INCOME", ignoreCase = true)
}

data class Summary(
    @SerializedName("totalIncome")
    val totalIncome: Double = 0.0,

    @SerializedName("totalExpense")
    val totalExpense: Double = 0.0,

    @SerializedName("balance")
    val balance: Double = 0.0,

    @SerializedName("totalTransactions")
    val totalTransactions: Int = 0
)

data class SheetResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("summary")
    val summary: Summary? = null,

    @SerializedName("data")
    val data: List<Transaction> = emptyList()
)

data class TransactionPostPayload(
    @SerializedName("action")
    val action: String = "add", // "add" or "delete"

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("tanggal")
    val tanggal: String? = null,

    @SerializedName("tipe")
    val tipe: String? = null,

    @SerializedName("kategori")
    val kategori: String? = null,

    @SerializedName("nominal")
    val nominal: Double? = null,

    @SerializedName("akun")
    val akun: String? = null,

    @SerializedName("catatan")
    val catatan: String? = null,

    @SerializedName("pencatat")
    val pencatat: String? = null
)

data class ActionResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("message")
    val message: String = "",

    @SerializedName("id")
    val id: String? = null
)
