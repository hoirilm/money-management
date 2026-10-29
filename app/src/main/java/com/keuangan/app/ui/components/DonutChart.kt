package com.keuangan.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.data.model.FinanceConstants
import com.keuangan.app.data.model.Transaction
import com.keuangan.app.ui.theme.CoralExpense
import com.keuangan.app.ui.theme.EmeraldIncome
import com.keuangan.app.util.Formatters
import com.keuangan.app.util.bouncingClickable
import kotlin.math.atan2
import kotlin.math.sqrt

data class CategorySlice(
    val categoryName: String,
    val icon: String,
    val amount: Double,
    val percentage: Float,
    val color: Color,
    val startAngle: Float = 0f,
    val sweepAngle: Float = 0f
)

/**
 * Konten Diagram Donat Modern & Interaktif
 * - StrokeCap.Butt dengan angular gap mencegah tabrakan/benturan warna antar irisan
 * - Animasi seleksi halus dan layer z-index teratas untuk irisan aktif
 * - Dapat disematkan di dalam Slider Card atau Card tersendiri
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonutChartContent(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isExpenseView by remember { mutableStateOf(true) }
    var selectedSliceIndex by remember { mutableStateOf<Int?>(null) }

    val filteredList = remember(transactions, isExpenseView) {
        transactions.filter { if (isExpenseView) it.isExpense else it.isIncome }
    }

    val totalAmount = remember(filteredList) {
        filteredList.sumOf { it.nominal }
    }

    // Palette warna modern bergradasi harmonis tanpa benturan
    val sliceColors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFFEC4899), // Pink
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF06B6D4), // Cyan
        Color(0xFF8B5CF6), // Purple
        Color(0xFFF97316), // Orange
        Color(0xFF14B8A6), // Teal
        Color(0xFF3B82F6)  // Blue
    )

    // Hitung per kategori beserta sudut putar
    val slices = remember(filteredList, totalAmount) {
        if (totalAmount <= 0.0) emptyList()
        else {
            val groupMap = filteredList.groupBy { it.kategori }
                .mapValues { entry -> entry.value.sumOf { it.nominal } }
                .toList()
                .sortedByDescending { it.second }

            var curAngle = -90f
            groupMap.mapIndexed { index, (catName, sum) ->
                val emoji = (FinanceConstants.expenseCategories + FinanceConstants.incomeCategories)
                    .firstOrNull { it.name.equals(catName, ignoreCase = true) }?.icon ?: "📌"
                val pct = ((sum / totalAmount) * 100).toFloat()
                val sweep = (pct / 100f) * 360f
                val slice = CategorySlice(
                    categoryName = catName,
                    icon = emoji,
                    amount = sum,
                    percentage = pct,
                    color = sliceColors[index % sliceColors.size],
                    startAngle = curAngle,
                    sweepAngle = sweep
                )
                curAngle += sweep
                slice
            }
        }
    }

    // Reset seleksi ketika berganti mode pengeluaran/pemasukan
    LaunchedEffect(isExpenseView) {
        selectedSliceIndex = null
    }

    // Animasi putaran awal diagram
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(isExpenseView, slices) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    // Fungsi pembantu mendeteksi irisan yang disentuh
    fun findSliceAtTouch(x: Float, y: Float, centerX: Float, centerY: Float): Int? {
        val dx = x - centerX
        val dy = y - centerY
        val dist = sqrt(dx * dx + dy * dy)
        // Batas radius sentuhan yang valid
        if (dist < 32f || dist > 260f) return null

        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        var normalizedAngle = (angle + 90f)
        if (normalizedAngle < 0) normalizedAngle += 360f

        var acc = 0f
        slices.forEachIndexed { idx, s ->
            val nextAcc = acc + s.sweepAngle
            if (normalizedAngle in acc..nextAcc) {
                return idx
            }
            acc = nextAcc
        }
        return null
    }

    val density = LocalDensity.current
    val baseStrokePx = with(density) { 20.dp.toPx() }
    val expandedStrokePx = with(density) { 28.dp.toPx() }

    Column(modifier = modifier.fillMaxWidth()) {
        // Toggle Keluar / Masuk Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Diagram Distribusi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ketuk irisan untuk rincian",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mode Selector Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isExpenseView) CoralExpense else Color.Transparent)
                        .bouncingClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isExpenseView = true
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Keluar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpenseView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (!isExpenseView) EmeraldIncome else Color.Transparent)
                        .bouncingClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            isExpenseView = false
                        }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Masuk",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isExpenseView) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (totalAmount <= 0.0 || slices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Belum ada transaksi ${if (isExpenseView) "pengeluaran" else "pemasukan"} di periode ini",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Interactive Donut Chart Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(180.dp)
                        .pointerInput(slices) {
                            detectTapGestures { offset ->
                                val touchedIdx = findSliceAtTouch(
                                    offset.x, offset.y,
                                    size.width / 2f, size.height / 2f
                                )
                                if (touchedIdx != null) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    selectedSliceIndex = if (selectedSliceIndex == touchedIdx) null else touchedIdx
                                } else {
                                    selectedSliceIndex = null
                                }
                            }
                        }
                        .pointerInput(slices) {
                            detectDragGestures { change, _ ->
                                val touchedIdx = findSliceAtTouch(
                                    change.position.x, change.position.y,
                                    size.width / 2f, size.height / 2f
                                )
                                if (touchedIdx != null && touchedIdx != selectedSliceIndex) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedSliceIndex = touchedIdx
                                }
                            }
                        }
                ) {
                    val gapAngle = if (slices.size > 1) 2.5f else 0f
                    val anim = animationProgress.value

                    // Background track tipis di belakang irisan
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.12f),
                        radius = (size.minDimension - baseStrokePx) / 2f,
                        style = Stroke(width = baseStrokePx)
                    )

                    var curStart = -90f

                    // 1. Gambar irisan yang TIDAK dipilih terlebih dahulu (Butt cap = zero overlap!)
                    slices.forEachIndexed { idx, slice ->
                        val sweep = slice.sweepAngle * anim
                        val isSelected = selectedSliceIndex == idx
                        val effectiveSweep = (sweep - gapAngle).coerceAtLeast(0.4f)
                        val sliceStart = curStart + (gapAngle / 2f)

                        if (!isSelected) {
                            val colorAlpha = if (selectedSliceIndex == null) 1f else 0.35f
                            drawArc(
                                color = slice.color.copy(alpha = colorAlpha),
                                startAngle = sliceStart,
                                sweepAngle = effectiveSweep,
                                useCenter = false,
                                style = Stroke(width = baseStrokePx, cap = StrokeCap.Butt)
                            )
                        }
                        curStart += sweep
                    }

                    // 2. Gambar irisan yang DIPILIH di atas lapisan (smoothly highlighted without colliding)
                    selectedSliceIndex?.let { selIdx ->
                        val selSlice = slices.getOrNull(selIdx)
                        if (selSlice != null) {
                            val sweep = selSlice.sweepAngle * anim
                            val effectiveSweep = (sweep - gapAngle).coerceAtLeast(0.4f)
                            val sliceStart = selSlice.startAngle + (gapAngle / 2f)

                            // Highlight arc dengan ketebalan yang lebih menonjol
                            drawArc(
                                color = selSlice.color,
                                startAngle = sliceStart,
                                sweepAngle = effectiveSweep,
                                useCenter = false,
                                style = Stroke(width = expandedStrokePx, cap = StrokeCap.Butt)
                            )
                        }
                    }
                }

                // Dynamic Center Hole Information
                val activeSlice = selectedSliceIndex?.let { slices.getOrNull(it) }

                AnimatedContent(
                    targetState = activeSlice,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    label = "centerHoleContent"
                ) { target ->
                    if (target != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(horizontal = 24.dp)
                                .clickable { selectedSliceIndex = null }
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(target.color.copy(alpha = 0.15f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${target.icon} ${target.percentage.toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = target.color
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = target.categoryName,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = Formatters.formatRupiah(target.amount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = target.color
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = if (isExpenseView) "Total Keluar" else "Total Masuk",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = Formatters.formatRupiah(totalAmount),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isExpenseView) CoralExpense else EmeraldIncome,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = "Sentuh grafik",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Legend Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                slices.forEachIndexed { idx, slice ->
                    val isSelected = selectedSliceIndex == idx
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) slice.color.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = if (isSelected) slice.color else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .bouncingClickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedSliceIndex = if (selectedSliceIndex == idx) null else idx
                            }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(slice.color)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${slice.icon} ${slice.categoryName}",
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) slice.color else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${slice.percentage.toInt()}%",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) slice.color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Standalone DonutChartCard untuk backward-compatibility
 */
@Composable
fun DonutChartCard(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.padding(18.dp)) {
            DonutChartContent(transactions = transactions)
        }
    }
}
