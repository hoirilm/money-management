package com.keuangan.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.data.model.Transaction
import com.keuangan.app.ui.theme.EmeraldIncome
import com.keuangan.app.ui.theme.IndigoPrimary
import com.keuangan.app.util.Formatters
import com.keuangan.app.util.bouncingClickable
import java.util.Calendar

data class InsightItem(
    val title: String,
    val message: String,
    val iconEmoji: String,
    val highlightColor: Color
)

@Composable
fun ContextualInsightCard(
    transactions: List<Transaction>,
    budgetLimit: Double,
    modifier: Modifier = Modifier
) {
    var isDismissed by remember { mutableStateOf(false) }
    var currentInsightIndex by remember { mutableIntStateOf(0) }

    val insights = remember(transactions, budgetLimit) {
        val list = mutableListOf<InsightItem>()
        val expenseTransactions = transactions.filter { it.isExpense }
        val totalExpense = expenseTransactions.sumOf { it.nominal }

        // 1. Kategori Pengeluaran Terbesar
        val topCategory = expenseTransactions.groupBy { it.kategori }
            .maxByOrNull { entry -> entry.value.sumOf { it.nominal } }

        if (topCategory != null && totalExpense > 0.0) {
            val catTotal = topCategory.value.sumOf { it.nominal }
            val pct = ((catTotal / totalExpense) * 100).toInt()
            list.add(
                InsightItem(
                    title = "Kategori Dominan",
                    message = "Pengeluaran terbesar bulan ini ada pada '${topCategory.key}' sebesar ${Formatters.formatRupiah(catTotal)} ($pct% dari total pengeluaran).",
                    iconEmoji = "📊",
                    highlightColor = IndigoPrimary
                )
            )
        }

        // 2. Evaluasi Kuota Harian Sisa Bulan
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val remainingDays = (daysInMonth - currentDay + 1).coerceAtLeast(1)
        val safeBudget = if (budgetLimit > 0) budgetLimit else 5000000.0
        val remainingBudget = (safeBudget - totalExpense).coerceAtLeast(0.0)

        if (remainingBudget > 0.0) {
            val dailyLimit = remainingBudget / remainingDays
            list.add(
                InsightItem(
                    title = "Kuota Aman Harian",
                    message = "Sisa hari bulan ini ada $remainingDays hari. Batas belanja aman Anda rata-rata ${Formatters.formatRupiah(dailyLimit)} / hari.",
                    iconEmoji = "💡",
                    highlightColor = EmeraldIncome
                )
            )
        }

        // 3. Analisis Kopi & Makanan
        val fnbTotal = expenseTransactions
            .filter { it.kategori.contains("Makan", ignoreCase = true) || it.catatan.contains("kopi", ignoreCase = true) }
            .sumOf { it.nominal }
        if (fnbTotal > 0.0) {
            list.add(
                InsightItem(
                    title = "Tren Kuliner & Kopi",
                    message = "Total pos jajan & makan bulan ini ${Formatters.formatRupiah(fnbTotal)}. Siapkan bekal atau seduh kopi mandiri untuk ekstra hemat!",
                    iconEmoji = "☕",
                    highlightColor = Color(0xFFF59E0B)
                )
            )
        }

        // 4. Default Insight jika data masih sedikit
        if (list.isEmpty()) {
            list.add(
                InsightItem(
                    title = "Smart Tips Keuangan",
                    message = "Rutin mencatat setiap transaksi kecil membantu keluarga mengendalikan 20% pengeluaran tak terduga setiap bulan.",
                    iconEmoji = "🎯",
                    highlightColor = IndigoPrimary
                )
            )
        }

        list
    }

    if (insights.isEmpty() || isDismissed) return

    val currentInsight = insights[currentInsightIndex % insights.size]

    AnimatedVisibility(
        visible = !isDismissed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .bouncingClickable {
                    currentInsightIndex = (currentInsightIndex + 1) % insights.size
                },
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                currentInsight.highlightColor.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                currentInsight.highlightColor.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Emoji / Icon Container
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(currentInsight.highlightColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = currentInsight.iconEmoji, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentInsight.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = currentInsight.highlightColor
                            )
                            if (insights.size > 1) {
                                Text(
                                    text = "(${currentInsightIndex + 1}/${insights.size} • ketuk ganti)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Text(
                            text = currentInsight.message,
                            fontSize = 12.sp,
                            lineHeight = 16.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { isDismissed = true },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup Insight",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}
