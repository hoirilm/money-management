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
    val bulanLunas: String = "" // Menyimpan riwayat bulan yang sudah lunas, contoh: "2026-09,2026-10"
) {
    /**
     * Memeriksa apakah tagihan ini sudah dicentang lunas pada bulan tertentu (format YYYY-MM).
     * Jika berganti bulan, otomatis kembali menjadi false (uncheck).
     */
    fun isPaidForMonth(monthYear: String): Boolean {
        if (bulanLunas.isBlank()) return false
        val target = monthYear.trim()
        val list = bulanLunas.split(",").map { it.trim() }
        if (list.contains(target)) return true
        if (bulanLunas.contains(target)) return true

        // Cek jika Google Sheets mengembalikan string tanggal objek (misal: "Thu Oct 01 2026" / "2026/10")
        val parts = target.split("-")
        if (parts.size >= 2) {
            val year = parts[0]
            val month = parts[1]
            val monthInt = month.toIntOrNull()
            if (monthInt != null && monthInt in 1..12) {
                val enNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val idNames = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
                val enName = enNames[monthInt - 1]
                val idName = idNames[monthInt - 1]
                if (bulanLunas.contains(year, ignoreCase = true) &&
                    (bulanLunas.contains(enName, ignoreCase = true) || bulanLunas.contains(idName, ignoreCase = true) || bulanLunas.contains("/$month/") || bulanLunas.contains("-$month-"))) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Menghasilkan string bulanLunas baru saat dicentang/batal centang.
     */
    fun toggleMonth(monthYear: String): String {
        val currentList = if (bulanLunas.isBlank()) emptyList() else bulanLunas.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val target = monthYear.trim()
        val newList = if (currentList.contains(target)) {
            currentList.filter { it != target }
        } else {
            currentList + target
        }
        return newList.joinToString(",")
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
    val akun: String? = "Cash",

    @SerializedName("updateDefaultNominal")
    val updateDefaultNominal: Boolean = false
)

data class BillsResponse(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("data")
    val data: List<Bill> = emptyList()
)
