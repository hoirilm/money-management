package com.keuangan.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.ui.theme.IndigoGradientEnd
import com.keuangan.app.ui.theme.IndigoGradientStart
import com.keuangan.app.ui.theme.IndigoPrimary
import com.keuangan.app.util.bouncingClickable

private data class ProfileOption(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)

@Composable
fun ProfileSelectionScreen(
    onProfileSelected: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var selectedProfile by remember { mutableStateOf<String?>(null) }

    val options = remember {
        listOf(
            ProfileOption(
                id = "Suami",
                emoji = "👤",
                title = "Suami",
                subtitle = "Semua transaksi & tagihan dari HP ini otomatis tercatat sebagai Suami",
                accentColor = IndigoPrimary
            ),
            ProfileOption(
                id = "Istri",
                emoji = "🌸",
                title = "Istri",
                subtitle = "Semua transaksi & tagihan dari HP ini otomatis tercatat sebagai Istri",
                accentColor = Color(0xFFEC4899)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.6f))

        // App Emblem
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(IndigoGradientStart, IndigoGradientEnd)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Siapa yang Menggunakan HP Ini?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Pilih profil pengguna sekali di awal. Anda tetap bisa mengubahnya kapan saja lewat menu Pengaturan.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Role Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            options.forEach { option ->
                val isSelected = selectedProfile == option.id
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) option.accentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    animationSpec = tween(200),
                    label = "profileBorder"
                )
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) option.accentColor.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surface,
                    animationSpec = tween(200),
                    label = "profileBg"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bouncingClickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedProfile = option.id
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(option.accentColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = option.emoji, fontSize = 26.sp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = option.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 17.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isSelected) option.accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                selectedProfile?.let { profile ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onProfileSelected(profile)
                }
            },
            enabled = selectedProfile != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = IndigoPrimary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = if (selectedProfile != null) "Lanjutkan sebagai $selectedProfile" else "Pilih Profil Terlebih Dahulu",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

