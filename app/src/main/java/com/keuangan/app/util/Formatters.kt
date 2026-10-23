package com.keuangan.app.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
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
