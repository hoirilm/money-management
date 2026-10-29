package com.keuangan.app.util

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

data class AutoCategoryResult(
    val categoryName: String,
    val isExpense: Boolean,
    val icon: String
)

object AutoCategorizer {

    private val expenseKeywords = mapOf(
        "Makanan & Minuman" to listOf(
            "kopi", "starbucks", "janji jiwa", "kenangan", "fore", "tomoro", "point coffee", "cafe", "caffe", "teh", "boba",
            "makan", "sarapan", "lunch", "siang", "dinner", "malam", "nasi", "padang", "resto", "restoran", "warteg", "soto",
            "bakso", "mie", "ayam", "bebek", "burger", "pizza", "mcd", "kfc", "richeese", "roti", "jajan", "cemilan", "snack",
            "kue", "haus", "chatime", "gofood", "grabfood", "shopeefood", "kantin", "angkringan", "pecel"
        ),
        "Transportasi" to listOf(
            "bensin", "pertalite", "pertamax", "spbu", "shell", "bp", "solar", "parkir", "tol", "e-toll", "ojol", "gojek",
            "grab", "maxim", "kereta", "krl", "mrt", "lrt", "busway", "transjakarta", "tiket", "bus", "travel", "cuci motor",
            "cuci mobil", "bengkel", "oli", "tambal", "ban", "service", "helm"
        ),
        "Belanja & Dapur" to listOf(
            "indomaret", "alfamart", "alfamidi", "supermarket", "hypermart", "superindo", "transmart", "pasar", "sayur", "buah",
            "beras", "minyak", "telur", "sabun", "odol", "shampoo", "detergen", "belanja", "shopee", "tokopedia", "lazada",
            "tiktok shop", "mall", "pakaian", "baju", "celana", "sepatu", "toko", "daging", "ikan", "bumbu"
        ),
        "Tagihan & Listrik" to listOf(
            "listrik", "pln", "token", "pdam", "air", "wifi", "indihome", "firstmedia", "biznet", "oxygen", "myrepublic",
            "pulsa", "kuota", "paket data", "telkomsel", "xl", "indosat", "tri", "smartfren", "bpjs", "pbb", "iuran", "sampah", "keamanan"
        ),
        "Cicilan & Pinjaman" to listOf(
            "kpr", "cicilan", "angsuran", "kredit", "kartu kredit", "paylater", "spaylater", "pinjaman", "hutang", "leasing", "oto", "baf"
        ),
        "Pendidikan / SPP" to listOf(
            "spp", "les", "kursus", "buku", "alat tulis", "seragam", "susu", "pampers", "popok", "sekolah", "kuliah", "dospem", "skripsi"
        ),
        "Hiburan & Langganan" to listOf(
            "bioskop", "cinema", "xxi", "cgv", "nonton", "netflix", "spotify", "youtube", "disney", "vidio", "steam", "game",
            "playstation", "liburan", "hotel", "villa", "rekreasi", "jalan-jalan", "karaoke"
        ),
        "Asuransi & BPJS" to listOf(
            "asuransi", "prudential", "allianz", "manulife", "axa", "bpjs kesehatan", "bpjs ketenagakerjaan"
        ),
        "Kesehatan & Medis" to listOf(
            "obat", "apotek", "kimia farma", "k24", "dokter", "klinik", "rumah sakit", "lab", "vitamin", "panadol", "paracetamol"
        )
    )

    private val incomeKeywords = mapOf(
        "Gaji Utama" to listOf("gaji", "salary", "upah", "payroll", "gajian", "bulanan"),
        "Bonus & THR" to listOf("bonus", "thr", "insentif", "komisi", "lembur", "tunjangan", "reward"),
        "Usaha & Freelance" to listOf("jualan", "orderan", "omset", "dagang", "toko", "freelance", "proyek", "project", "honor", "klien"),
        "Investasi / Bunga" to listOf("dividen", "reksadana", "saham", "crypto", "bunga", "deposito", "imbal hasil"),
        "Hadiah / Lainnya" to listOf("hadiah", "gift", "amplop", "cashback", "undian", "arisan", "kembalian")
    )

    fun detect(text: String): AutoCategoryResult? {
        val query = text.trim().lowercase()
        if (query.length < 2) return null

        // Check Income keywords first
        for ((catName, keywords) in incomeKeywords) {
            for (kw in keywords) {
                if (query.contains(kw)) {
                    val icon = when (catName) {
                        "Gaji Utama" -> "💼"
                        "Bonus & THR" -> "🎉"
                        "Usaha & Freelance" -> "🚀"
                        "Investasi / Bunga" -> "📈"
                        else -> "🎁"
                    }
                    return AutoCategoryResult(catName, false, icon)
                }
            }
        }

        // Check Expense keywords
        for ((catName, keywords) in expenseKeywords) {
            for (kw in keywords) {
                if (query.contains(kw)) {
                    val icon = when (catName) {
                        "Makanan & Minuman" -> "🍔"
                        "Transportasi" -> "🚗"
                        "Belanja & Dapur" -> "🛒"
                        "Tagihan & Listrik" -> "⚡"
                        "Cicilan & Pinjaman" -> "💳"
                        "Pendidikan / SPP" -> "📚"
                        "Hiburan & Langganan" -> "🎬"
                        "Asuransi & BPJS" -> "🛡️"
                        "Kesehatan & Medis" -> "💊"
                        else -> "🏷️"
                    }
                    return AutoCategoryResult(catName, true, icon)
                }
            }
        }

        return null
    }
}

/**
 * Micro-interaction: Bouncing spring effect on tap + Tactile Haptic Feedback
 */
@Composable
fun Modifier.bouncingClickable(
    enabled: Boolean = true,
    scaleDown: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "bounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
}

/**
 * Skeleton Shimmer Effect: Animated loading shimmer for modern placeholder visuals
 */
@Composable
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.35f),
        Color.Gray.copy(alpha = 0.12f)
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim - 300f, y = translateAnim - 300f),
        end = Offset(x = translateAnim, y = translateAnim)
    )

    this.background(brush)
}
