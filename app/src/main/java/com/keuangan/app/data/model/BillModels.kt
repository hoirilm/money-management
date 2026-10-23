package com.keuangan.app.data.model

import com.google.gson.annotations.SerializedName

data class Bill(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("nama")
    val nama: String = "",

    @SerializedName("nominal")
    val nominal: Double = 0.0,

    @SerializedName("jatuhTempo")
    val jatuhTempo: Int = 1, // Tanggal 1 - 31

    @SerializedName("kategori")
    val kategori: String = "Tagihan & Listrik",

    @SerializedName("bulanLunas")
    val bulanLunas: String = "" // Contoh "2026-10"
) {
    fun isPaidForMonth(currentMonthYear: String): Boolean {
        return bulanLunas.trim() == currentMonthYear.trim()
    }
}

data class BillPostPayload(
    @SerializedName("action")
    val action: String = "add_bill", // "add_bill", "toggle_bill", "delete_bill"

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("nama")
    val nama: String? = null,

    @SerializedName("nominal")
    val nominal: Double? = null,

    @SerializedName("jatuhTempo")
    val jatuhTempo: Int? = null,

    @SerializedName("kategori")
    val kategori: String? = null,

    @SerializedName("bulanLunas")
    val bulanLunas: String? = null,

    // Optional: auto-record to transaction
    @SerializedName("recordTransaction")
    val recordTransaction: Boolean = false,

    @SerializedName("pencatat")
    val pencatat: String? = null,

    @SerializedName("akun")
    val akun: String? = "Cash"
)

data class BillsResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<Bill> = emptyList()
)
