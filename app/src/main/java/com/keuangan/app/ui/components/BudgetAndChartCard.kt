package com.keuangan.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.data.model.Transaction
import com.keuangan.app.ui.theme.IndigoPrimary
import com.keuangan.app.util.bouncingClickable
import kotlinx.coroutines.launch

/**
 * Komponen terpadu yang menggabungkan Smart Micro-Budget dan Diagram Donat
 * dalam 1 container hemat tempat dengan slider / swipe horizontal (HorizontalPager).
 */
@Composable
fun BudgetAndChartCard(
    currentExpense: Double,
    budgetLimit: Double,
    onUpdateBudget: (Double) -> Unit,
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Segmented Tabs Slider: [ 🎯 Smart Budget ] | [ 📊 Diagram Donat ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 0: Smart Budget
                val isTab0Selected = pagerState.currentPage == 0
                val tab0Bg by animateColorAsState(
                    targetValue = if (isTab0Selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = tween(220),
                    label = "tab0Bg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(tab0Bg)
                        .bouncingClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch { pagerState.animateScrollToPage(0) }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🎯", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Smart Budget",
                            fontSize = 12.sp,
                            fontWeight = if (isTab0Selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTab0Selected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Tab 1: Diagram Donut
                val isTab1Selected = pagerState.currentPage == 1
                val tab1Bg by animateColorAsState(
                    targetValue = if (isTab1Selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    animationSpec = tween(220),
                    label = "tab1Bg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(tab1Bg)
                        .bouncingClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "📊", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Diagram Donat",
                            fontSize = 12.sp,
                            fontWeight = if (isTab1Selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTab1Selected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Slider Kanan Kiri (HorizontalPager)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> {
                        SmartMicroBudgetContent(
                            currentExpense = currentExpense,
                            budgetLimit = budgetLimit,
                            onUpdateBudget = onUpdateBudget
                        )
                    }
                    1 -> {
                        DonutChartContent(
                            transactions = transactions
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Page Indicator Dots Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..1).forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotColor = if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    val dotWidth = if (isSelected) 18.dp else 6.dp

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                    )
                }
            }
        }
    }
}
