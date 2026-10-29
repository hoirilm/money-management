package com.keuangan.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.keuangan.app.data.model.Summary
import com.keuangan.app.data.model.Transaction
import com.keuangan.app.ui.theme.EmeraldIncome
import com.keuangan.app.ui.theme.IndigoPrimary
import com.keuangan.app.util.Formatters

data class MilestoneBadge(
    val title: String,
    val description: String,
    val badgeIcon: String,
    val badgeColor: Color
)

@Composable
fun MilestoneCard(
    summary: Summary,
    transactions: List<Transaction>,
    budgetLimit: Double,
    modifier: Modifier = Modifier
) {
    var isDismissed by remember { mutableStateOf(false) }

    val activeBadge = remember(summary, transactions, budgetLimit) {
        val totalInc = summary.totalIncome
        val totalExp = summary.totalExpense
        val balance = summary.balance

        when {
            // 1. Saving Hero: Tabungan surplus > 25% dari pemasukan
            totalInc > 0 && (balance / totalInc) >= 0.25 -> {
                val savingsRate = ((balance / totalInc) * 100).toInt()
                MilestoneBadge(
                    title = "🏆 Juara Menabung!",
                    description = "Luar biasa! Keluarga berhasil menyisihkan $savingsRate% dari total pemasukan bulan ini (${Formatters.formatRupiah(balance)}).",
                    badgeIcon = "💎",
                    badgeColor = EmeraldIncome
                )
            }
            // 2. Budget Champion: Pengeluaran masih di bawah 60% anggaran
            totalExp > 0 && totalExp < (budgetLimit * 0.60) -> {
                MilestoneBadge(
                    title = "🛡️ Hemat Terjaga!",
                    description = "Pengeluaran Anda terkendali sangat baik, masih di bawah 60% dari batas anggaran bulanan.",
                    badgeIcon = "⭐",
                    badgeColor = Color(0xFFF59E0B)
                )
            }
            // 3. Pencatat Disiplin: Mencatat lebih dari 5 transaksi
            transactions.size >= 5 -> {
                MilestoneBadge(
                    title = "✨ Pencatat Disiplin",
                    description = "Konsistensi yang hebat! Sudah ada ${transactions.size} transaksi tercatat rapi.",
                    badgeIcon = "🎖️",
                    badgeColor = IndigoPrimary
                )
            }
            else -> null
        }
    }

    if (activeBadge == null || isDismissed) return

    AnimatedVisibility(
        visible = !isDismissed,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, activeBadge.badgeColor.copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                activeBadge.badgeColor.copy(alpha = 0.12f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge Container with gold/colored ring
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(activeBadge.badgeColor.copy(alpha = 0.2f))
                            .border(1.5.dp, activeBadge.badgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = activeBadge.badgeIcon, fontSize = 22.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeBadge.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeBadge.badgeColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeBadge.description,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { isDismissed = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Tutup Badge",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
