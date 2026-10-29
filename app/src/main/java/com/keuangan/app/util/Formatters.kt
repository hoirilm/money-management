package com.keuangan.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

    private val indonesianLocale = Locale("in", "ID")

    fun formatRupiah(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(indonesianLocale).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        return formatter.format(amount).replace("Rp", "Rp ")
    }

    /**
     * Memformat string angka mentah menjadi berpemisah titik secara real-time.
     * Contoh: "1000" -> "1.000", "1500000" -> "1.500.000"
     */
    fun formatCurrencyInput(rawDigits: String): String {
        val clean = rawDigits.replace("[^\\d]".toRegex(), "").trimStart('0')
        if (clean.isEmpty()) return ""
        val parsed = clean.toLongOrNull() ?: return clean
        val formatter = NumberFormat.getNumberInstance(indonesianLocale)
        return formatter.format(parsed)
    }

    fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getCurrentMonthYearKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return sdf.format(Date())
    }

    fun getCurrentMonthYearDisplay(): String {
        val sdf = SimpleDateFormat("MMMM yyyy", indonesianLocale)
        return sdf.format(Date())
    }

    fun formatMonthKeyDisplay(monthKey: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = sdfInput.parse(monthKey) ?: return monthKey
            val sdfOutput = SimpleDateFormat("MMMM yyyy", indonesianLocale)
            sdfOutput.format(date)
        } catch (e: Exception) {
            monthKey
        }
    }

    fun getShiftedMonthKey(monthKey: String, shift: Int): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = sdf.parse(monthKey) ?: return monthKey
            val cal = Calendar.getInstance().apply {
                time = date
                add(Calendar.MONTH, shift)
            }
            sdf.format(cal.time)
        } catch (e: Exception) {
            monthKey
        }
    }

    /**
     * Format waktu ringkas dan hemat tempat untuk daftar transaksi:
     * - "Hari ini, 12:30"
     * - "Kemarin, 19:45"
     * - "23 Okt, 14:00"
     */
    fun formatCompactDate(dateString: String): String {
        return try {
            val date = parseDate(dateString) ?: return dateString.take(16)

            val nowCal = Calendar.getInstance()
            val targetCal = Calendar.getInstance().apply { time = date }

            val timeFormat = SimpleDateFormat("HH:mm", indonesianLocale)
            val timeText = timeFormat.format(date)

            val isToday = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    nowCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR)
            val isYesterday = nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                    nowCal.get(Calendar.DAY_OF_YEAR) - targetCal.get(Calendar.DAY_OF_YEAR) == 1

            when {
                isToday -> "Hari ini, $timeText"
                isYesterday -> "Kemarin, $timeText"
                nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) -> {
                    val dayMonthFormat = SimpleDateFormat("dd MMM", indonesianLocale)
                    "${dayMonthFormat.format(date)}, $timeText"
                }
                else -> {
                    val fullFormat = SimpleDateFormat("dd/MM/yy", indonesianLocale)
                    "${fullFormat.format(date)}, $timeText"
                }
            }
        } catch (e: Exception) {
            dateString.take(16)
        }
    }

    fun formatDateDisplay(dateString: String): String {
        return try {
            val date = parseDate(dateString) ?: return dateString
            val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", indonesianLocale)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    fun parseAmount(text: String): Double {
        val cleanString = text.replace("[^\\d]".toRegex(), "")
        return cleanString.toDoubleOrNull() ?: 0.0
    }

    fun parseDate(dateString: String): Date? {
        if (dateString.isBlank()) return null
        val clean = dateString.replace(Regex("\\s*\\([^)]*\\)"), "").trim()

        // 1. Coba parse dengan kandidat pola umum (termasuk format JS Date dari Google Sheets)
        val candidates = listOf(
            Pair("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            Pair("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US),
            Pair("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US),
            Pair("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
            Pair("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US),
            Pair("EEE MMM dd yyyy HH:mm:ss", Locale.US),
            Pair("EEE, dd MMM yyyy HH:mm:ss", Locale.US),
            Pair("yyyy-MM-dd HH:mm", Locale.getDefault()),
            Pair("yyyy-MM-dd", Locale.getDefault()),
            Pair("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
            Pair("dd/MM/yyyy HH:mm", Locale.getDefault()),
            Pair("dd/MM/yyyy", Locale.getDefault()),
            Pair("d/M/yyyy HH:mm:ss", Locale.getDefault()),
            Pair("d/M/yyyy", Locale.getDefault()),
            Pair("MM/dd/yyyy HH:mm:ss", Locale.US),
            Pair("MM/dd/yyyy", Locale.US),
            Pair("dd-MM-yyyy HH:mm:ss", Locale.getDefault()),
            Pair("dd-MM-yyyy", Locale.getDefault())
        )
        for ((pattern, locale) in candidates) {
            try {
                val sdf = SimpleDateFormat(pattern, locale)
                val d = sdf.parse(clean)
                if (d != null) return d
            } catch (_: Exception) {}
        }

        // 2. Fallback: Ekstraksi angka tahun, bulan, tanggal dengan regex
        try {
            val ymdMatch = Regex("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").find(clean)
            if (ymdMatch != null) {
                val year = ymdMatch.groupValues[1].toInt()
                val month = ymdMatch.groupValues[2].toInt() - 1
                val day = ymdMatch.groupValues[3].toInt()
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                return cal.time
            }

            val dmyMatch = Regex("(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})").find(clean)
            if (dmyMatch != null) {
                val day = dmyMatch.groupValues[1].toInt()
                val month = dmyMatch.groupValues[2].toInt() - 1
                val year = dmyMatch.groupValues[3].toInt()
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                return cal.time
            }
        } catch (_: Exception) {}

        return null
    }

    fun isDateInPeriod(dateString: String, period: TimePeriod): Boolean {
        if (period == TimePeriod.ALL_TIME) return true
        if (dateString.isBlank()) return true

        val date = parseDate(dateString)
        // JANGAN pernah sembunyikan transaksi jika format tanggal tidak dapat diparsing
        if (date == null) return true

        val targetCal = Calendar.getInstance().apply { time = date }
        val nowCal = Calendar.getInstance()

        return when (period) {
            TimePeriod.THIS_MONTH -> {
                targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        targetCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
            }
            TimePeriod.LAST_MONTH -> {
                val lastMonthCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                }
                targetCal.get(Calendar.YEAR) == lastMonthCal.get(Calendar.YEAR) &&
                        targetCal.get(Calendar.MONTH) == lastMonthCal.get(Calendar.MONTH)
            }
            TimePeriod.LAST_7_DAYS -> {
                val sevenDaysAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                date.time >= sevenDaysAgo
            }
            TimePeriod.LAST_30_DAYS -> {
                val thirtyDaysAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -30)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                date.time >= thirtyDaysAgo
            }
            TimePeriod.ALL_TIME -> true
        }
    }
}

enum class TimePeriod(val label: String, val shortLabel: String) {
    ALL_TIME("Semua", "Semua"),
    THIS_MONTH("Bulan Ini", "Bulan Ini"),
    LAST_MONTH("Bulan Lalu", "Bulan Lalu"),
    LAST_7_DAYS("7 Hari", "7 Hari"),
    LAST_30_DAYS("30 Hari", "30 Hari")
}

