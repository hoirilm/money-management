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

    fun formatDateDisplay(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateString) ?: return dateString
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
}
