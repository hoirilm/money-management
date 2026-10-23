package com.keuangan.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.data.model.FinanceConstants
import com.keuangan.app.data.model.Transaction
import com.keuangan.app.ui.theme.CoralExpense
import com.keuangan.app.ui.theme.EmeraldIncome
import com.keuangan.app.util.Formatters

data class CategorySlice(
    val categoryName: String,
    val icon: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutChartCard(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    var isExpenseView by remember { mutableStateOf(true) }

    val filteredList = remember(transactions, isExpenseView) {
        transactions.filter { if (isExpenseView) it.isExpense else it.isIncome }
    }

    val totalAmount = remember(filteredList) {
        filteredList.sumOf { it.nominal }
    }

    // Palette warna modern untuk irisan diagram donut
    val sliceColors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFFEC4899), // Pink
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF06B6D4), // Cyan
        Color(0xFF8B5CF6), // Purple
        Color(0xFFF97316), // Orange
        Color(0xFF14B8A6)  // Teal
    )

    // Hitung per kategori
    val slices = remember(filteredList, totalAmount) {
        if (totalAmount <= 0.0) emptyList()
        else {
            val groupMap = filteredList.groupBy { it.kategori }
                .mapValues { entry -> entry.value.sumOf { it.nominal } }
                .toList()
                .sortedByDescending { it.second }

            groupMap.mapIndexed { index, (catName, sum) ->
                val emoji = (FinanceConstants.expenseCategories + FinanceConstants.incomeCategories)
                    .firstOrNull { it.name.equals(catName, ignoreCase = true) }?.icon ?: "📌"
                val pct = ((sum / totalAmount) * 100).toFloat()
                CategorySlice(
                    categoryName = catName,
                    icon = emoji,
                    amount = sum,
                    percentage = pct,
                    color = sliceColors[index % sliceColors.size]
                )
            }
        }
    }

    // Animasi putaran diagram
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(isExpenseView, slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header Card & Toggle Pengeluaran / Pemasukan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analisis Diagram",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Toggle Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isExpenseView) CoralExpense else Color.Transparent)
                            .clickable { isExpenseView = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Pengeluaran",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isExpenseView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isExpenseView) EmeraldIncome else Color.Transparent)
                            .clickable { isExpenseView = false }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Pemasukan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isExpenseView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (totalAmount <= 0.0 || slices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada data ${if (isExpenseView) "pengeluaran" else "pemasukan"} untuk dianalisis",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Donut Chart Canvas with Center Hole
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(170.dp)) {
                        val strokeWidth = 24.dp.toPx()
                        var startAngle = -90f

                        slices.forEach { slice ->
                            val sweepAngle = (slice.percentage / 100f) * 360f * animationProgress.value
                            drawArc(
                                color = slice.color,
                                startAngle = startAngle + 2f, // Sedikit jeda antar irisan
                                sweepAngle = (sweepAngle - 4f).coerceAtLeast(0.1f),
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Teks di lubang tengah Donut
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 30.dp)
                    ) {
                        Text(
                            text = if (isExpenseView) "Total Keluar" else "Total Masuk",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Formatters.formatRupiah(totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isExpenseView) CoralExpense else EmeraldIncome,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Legend Kategori (Chips)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    slices.take(6).forEach { slice ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${slice.icon} ${slice.categoryName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${slice.percentage.toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
