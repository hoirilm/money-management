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
    val bulanLunas: String = "", // Menyimpan riwayat bulan yang sudah lunas, contoh: "2026-09,2026-10"

    @SerializedName("autoTxMap")
    val autoTxMap: String = "" // Menyimpan mapping periode -> ID transaksi otomatis, contoh: "2026-10:billtx_...,2026-09:NONE"
) {

    companion object {
        private val ymdRegex = Regex("""(\d{4})[-/](\d{1,2})(?:[-/]\d{1,2})?""")
        private val yearRegex = Regex("""\b(19\d{2}|20\d{2})\b""")

        private val monthKeywords = listOf(
            listOf("jan") to 1,
            listOf("feb") to 2,
            listOf("mar") to 3,
            listOf("apr") to 4,
            listOf("may", "mei") to 5,
            listOf("jun") to 6,
            listOf("jul") to 7,
            listOf("aug", "agu") to 8,
            listOf("sep") to 9,
            listOf("oct", "okt") to 10,
            listOf("nov") to 11,
            listOf("dec", "des") to 12
        )

        /**
         * Menormalisasi token periode bulan (misal "2026-10", "2026/9", "Thu Oct 01 2026") menjadi format "YYYY-MM".
         */
        fun normalizeMonthToken(raw: String): String? {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return null

            val ymdMatch = ymdRegex.find(trimmed)
            if (ymdMatch != null) {
                val year = ymdMatch.groupValues[1]
                val month = ymdMatch.groupValues[2].toIntOrNull()
                if (month != null && month in 1..12) {
                    return "%s-%02d".format(year, month)
                }
            }

            val yearMatch = yearRegex.find(trimmed)
            if (yearMatch != null) {
                val year = yearMatch.groupValues[1]
                val lower = trimmed.lowercase()
                for ((keys, monthNum) in monthKeywords) {
                    if (keys.any { lower.contains(it) }) {
                        return "%s-%02d".format(year, monthNum)
                    }
                }
            }

            return null
        }
    }

    /**
     * Daftar periode bulan ("YYYY-MM") yang sudah lunas secara kanonik.
     */
    fun paidMonths(): List<String> {
        if (bulanLunas.isBlank()) return emptyList()
        return bulanLunas.split(",")
            .mapNotNull { normalizeMonthToken(it) }
            .distinct()
    }

    /**
     * Memeriksa apakah tagihan ini sudah dicentang lunas pada bulan tertentu (format YYYY-MM).
     * Jika berganti bulan, otomatis kembali menjadi false (uncheck).
     */
    fun isPaidForMonth(monthYear: String): Boolean {
        if (bulanLunas.isBlank()) return false
        val target = normalizeMonthToken(monthYear) ?: monthYear.trim()
        return paidMonths().contains(target)
    }

    /**
     * Menghasilkan string bulanLunas baru dengan status [paid] eksplisit pada [monthYear].
     */
    fun withPaidForMonth(monthYear: String, paid: Boolean): String {
        val target = normalizeMonthToken(monthYear) ?: monthYear.trim()
        val current = paidMonths()
        val updated = if (paid) {
            (current + target).distinct()
        } else {
            current.filter { it != target }
        }
        return updated.joinToString(",")
    }

    /**
     * Menghasilkan string bulanLunas baru saat dicentang/batal centang.
     */
    fun toggleMonth(monthYear: String): String {
        return withPaidForMonth(monthYear, !isPaidForMonth(monthYear))
    }

    /**
     * Mengurai mapping bulan -> transactionId dari [autoTxMap].
     */
    fun parsedAutoTxMap(): Map<String, String> {
        if (autoTxMap.isBlank()) return emptyMap()
        val result = linkedMapOf<String, String>()
        autoTxMap.split(",").forEach { entry ->
            val trimmed = entry.trim()
            val colonIdx = trimmed.indexOf(':')
            if (colonIdx > 0 && colonIdx < trimmed.length - 1) {
                val rawMonth = trimmed.substring(0, colonIdx).trim()
                val txId = trimmed.substring(colonIdx + 1).trim()
                val normMonth = normalizeMonthToken(rawMonth) ?: rawMonth
                if (normMonth.isNotBlank() && txId.isNotBlank()) {
                    result[normMonth] = txId
                }
            }
        }
        return result
    }

    /**
     * Mengambil ID transaksi spesifik yang tercatat otomatis saat tagihan dicentang pada [monthYear].
     * Mengembalikan `"NONE"` jika tagihan dicentang tanpa opsi catat otomatis, atau `null` jika tidak ada metadata.
     */
    fun getAutoTxIdForMonth(monthYear: String): String? {
        val target = normalizeMonthToken(monthYear) ?: monthYear.trim()
        return parsedAutoTxMap()[target]
    }

    fun hasAutoRecordedTxForMonth(monthYear: String): Boolean {
        val txId = getAutoTxIdForMonth(monthYear)
        return !txId.isNullOrBlank() && !txId.equals("NONE", ignoreCase = true)
    }

    /**
     * Memperbarui mapping [autoTxMap] untuk periode [monthYear].
     */
    fun withAutoTxForMonth(monthYear: String, txIdOrNull: String?): String {
        val target = normalizeMonthToken(monthYear) ?: monthYear.trim()
        val mutable = LinkedHashMap(parsedAutoTxMap())
        if (txIdOrNull.isNullOrBlank()) {
            mutable.remove(target)
        } else {
            mutable[target] = txIdOrNull.trim()
        }
        return mutable.entries.joinToString(",") { "${it.key}:${it.value}" }
    }
}

data class BillPostPayload(
    @SerializedName("action")
    val action: String = "add_bill", // "add_bill", "toggle_bill", "delete_bill", "add_bill_category", "delete_bill_category"

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("nama")
    val nama: String? = null,

    @SerializedName("icon")
    val icon: String? = null,

    @SerializedName("nominal")
    val nominal: Double? = null,

    @SerializedName("jatuhTempo")
    val jatuhTempo: Int? = null,

    @SerializedName("kategori")
    val kategori: String? = null,

    @SerializedName("bulanLunas")
    val bulanLunas: String? = null,

    @SerializedName("monthKey")
    val monthKey: String? = null,

    @SerializedName("isPaid")
    val isPaid: Boolean? = null,

    // Optional: auto-record / reverse specific transaction
    @SerializedName("recordTransaction")
    val recordTransaction: Boolean = false,

    @SerializedName("transactionId")
    val transactionId: String? = null,

    @SerializedName("tanggal")
    val tanggal: String? = null,

    @SerializedName("catatan")
    val catatan: String? = null,

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
    val data: List<Bill> = emptyList(),

    @SerializedName("categories")
    val categories: List<CategoryItem>? = null
)
