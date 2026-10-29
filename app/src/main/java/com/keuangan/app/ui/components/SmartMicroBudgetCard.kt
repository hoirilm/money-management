package com.keuangan.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.ui.theme.CoralExpense
import com.keuangan.app.ui.theme.EmeraldIncome
import com.keuangan.app.ui.theme.IndigoPrimary
import com.keuangan.app.util.Formatters
import com.keuangan.app.util.bouncingClickable
import java.util.Calendar

@Composable
fun SmartMicroBudgetContent(
    currentExpense: Double,
    budgetLimit: Double,
    onUpdateBudget: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var showAdjustDialog by remember { mutableStateOf(false) }

    val safeBudget = if (budgetLimit > 0.0) budgetLimit else 5000000.0
    val ratio = (currentExpense / safeBudget).toFloat().coerceIn(0f, 1.25f)
    val percentage = ((currentExpense / safeBudget) * 100).toInt()

    // Status Warna Dinamis
    val dynamicColor by animateColorAsState(
        targetValue = when {
            ratio < 0.70f -> EmeraldIncome        // Aman: Hijau
            ratio < 0.90f -> Color(0xFFF59E0B)    // Peringatan: Kuning / Amber
            else -> CoralExpense                 // Kritis / Bahaya: Merah / Coral
        },
        label = "budgetColor"
    )

    val statusText = when {
        ratio < 0.70f -> "Aman • Pengeluaran Terkendali"
        ratio < 0.90f -> "Perhatian • Sudah $percentage% terpakai"
        ratio <= 1.0f -> "Mendekati Batas • Sisa menipis!"
        else -> "Overbudget! • Melampaui target anggaran"
    }

    // Hitung Sisa Hari & Kuota Belanja Harian
    val calendar = Calendar.getInstance()
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val remainingDays = (daysInMonth - currentDay + 1).coerceAtLeast(1)
    val remainingBudget = (safeBudget - currentExpense).coerceAtLeast(0.0)
    val dailyAllowance = remainingBudget / remainingDays

    // Animasi Progress Gauge
    val animatedRatio = remember { Animatable(0f) }
    LaunchedEffect(ratio) {
        animatedRatio.animateTo(
            targetValue = ratio.coerceAtMost(1f),
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(dynamicColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (ratio >= 0.90f) Icons.Rounded.WarningAmber else Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = dynamicColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Smart Micro-Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = dynamicColor
                    )
                }
            }

            // Tombol Atur Anggaran (Bouncing Micro-interaction)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .bouncingClickable {
                        showAdjustDialog = true
                    }
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Atur",
                        tint = IndigoPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Atur",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IndigoPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gauge Busur Melengkung Dinamis (Curved Arc Indicator)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = 210.dp, height = 110.dp)
                    .padding(bottom = 10.dp)
            ) {
                val strokeWidth = 14.dp.toPx()
                // Busur background abu-abu transparan (180 derajat)
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.25f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Busur progress dinamis berwarna (Hijau -> Kuning -> Merah)
                val sweepProgress = 180f * animatedRatio.value
                drawArc(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            EmeraldIncome,
                            if (ratio > 0.7f) Color(0xFFF59E0B) else EmeraldIncome,
                            dynamicColor
                        )
                    ),
                    startAngle = 180f,
                    sweepAngle = sweepProgress.coerceAtLeast(1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Teks di tengah busur
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 18.dp)
            ) {
                Text(
                    text = "$percentage%",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = dynamicColor
                )
                Text(
                    text = "Terpakai",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Rincian Angka: Terpakai vs Batas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Pengeluaran",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Formatters.formatRupiah(currentExpense),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Batas Anggaran",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = Formatters.formatRupiah(safeBudget),
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Kuota Harian (Smart Daily Allowance)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(dynamicColor.copy(alpha = 0.08f))
                .border(1.dp, dynamicColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "💡 Kuota Belanja Aman:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${Formatters.formatRupiah(dailyAllowance)} / hari ($remainingDays hari lagi)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = dynamicColor
                )
            }
        }
    }

    // Modal / Dialog Atur Anggaran
    if (showAdjustDialog) {
        val initialAmount = safeBudget.toLong()
        var rawDigits by remember { mutableStateOf(if (initialAmount > 0) initialAmount.toString() else "") }
        val initialFormatted = remember { Formatters.formatCurrencyInput(rawDigits) }
        var textFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    text = initialFormatted,
                    selection = TextRange(initialFormatted.length)
                )
            )
        }
        var sliderValue by remember {
            mutableFloatStateOf((safeBudget / 1000000.0).toFloat().coerceIn(1f, 30f))
        }

        val currentAmount = rawDigits.toDoubleOrNull() ?: (sliderValue.toDouble() * 1000000.0)

        AlertDialog(
            onDismissRequest = { showAdjustDialog = false },
            title = {
                Text(
                    text = "🎯 Atur Target Anggaran Bulanan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tentukan batas pengeluaran maksimal keluarga Anda per bulan. Smart Micro-Budget akan memantau kuota belanja harian Anda secara real-time.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = Formatters.formatRupiah(currentAmount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = IndigoPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            if ((newValue * 2).toInt() != (sliderValue * 2).toInt()) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            sliderValue = newValue
                            val amountLong = (newValue * 1000000).toLong()
                            rawDigits = amountLong.toString()
                            val formatted = Formatters.formatCurrencyInput(rawDigits)
                            textFieldValue = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                        },
                        valueRange = 1f..30f,
                        steps = 29,
                        colors = SliderDefaults.colors(
                            thumbColor = IndigoPrimary,
                            activeTrackColor = IndigoPrimary
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rp 1 Juta", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Rp 30 Juta", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = textFieldValue,
                        onValueChange = { tfv ->
                            val digits = tfv.text.filter { it.isDigit() }
                            rawDigits = digits
                            val formatted = Formatters.formatCurrencyInput(digits)
                            textFieldValue = TextFieldValue(
                                text = formatted,
                                selection = TextRange(formatted.length)
                            )
                            val num = digits.toFloatOrNull() ?: 0f
                            sliderValue = (num / 1000000f).coerceIn(1f, 30f)
                        },
                        prefix = { Text("Rp ", fontWeight = FontWeight.Bold, color = IndigoPrimary) },
                        label = { Text("Atau ketik nominal:") },
                        placeholder = { Text("0") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndigoPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = rawDigits.toDoubleOrNull() ?: (sliderValue.toDouble() * 1000000.0)
                        if (parsed > 0.0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onUpdateBudget(parsed)
                        }
                        showAdjustDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simpan Anggaran", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                IconButton(onClick = { showAdjustDialog = false }) {
                    Icon(imageVector = Icons.Rounded.Close, contentDescription = "Batal")
                }
            },
            shape = RoundedCornerShape(22.dp)
        )
    }
}

@Composable
fun SmartMicroBudgetCard(
    currentExpense: Double,
    budgetLimit: Double,
    onUpdateBudget: (Double) -> Unit,
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
            SmartMicroBudgetContent(
                currentExpense = currentExpense,
                budgetLimit = budgetLimit,
                onUpdateBudget = onUpdateBudget
            )
        }
    }
}
