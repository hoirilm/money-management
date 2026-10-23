package com.keuangan.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keuangan.app.ui.theme.AmberAccent
import com.keuangan.app.ui.theme.CoralExpense
import com.keuangan.app.ui.theme.EmeraldIncome
import com.keuangan.app.ui.theme.IndigoPrimary

@Composable
fun SettingsDialog(
    initialUrl: String,
    initialProfile: String,
    excelExportUrl: String? = null,
    spreadsheetUrl: String? = null,
    onDismiss: () -> Unit,
    onTestConnection: (url: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onSave: (url: String, profile: String) -> Unit
) {
    val context = LocalContext.current
    var urlText by remember { mutableStateOf(initialUrl) }
    var selectedProfile by remember { mutableStateOf(initialProfile) }

    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    val isDocsUrl = urlText.contains("docs.google.com/spreadsheets")
    val isDevUrl = urlText.trim().endsWith("/dev")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Pengaturan & Ekspor Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section 1: Profil HP ini
                Text(
                    text = "Profil Pengguna di HP Ini",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Suami", "Istri").forEach { profile ->
                        val isSelected = selectedProfile.equals(profile, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) IndigoPrimary else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedProfile = profile }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (profile == "Istri") "🌸 Istri" else "👤 Suami",
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 2: Google Apps Script Web App URL
                Text(
                    text = "Google Apps Script Web App URL",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        testResult = null
                    },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Link, contentDescription = null)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isDocsUrl) CoralExpense else IndigoPrimary,
                        unfocusedBorderColor = if (isDocsUrl) CoralExpense else MaterialTheme.colorScheme.outline
                    )
                )

                if (isDocsUrl) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CoralExpense.copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = CoralExpense,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠️ Anda memasukkan URL file Spreadsheet, bukan Web App URL! URL harus berawalan 'https://script.google.com/macros/s/.../exec'.",
                                color = CoralExpense,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else if (isDevUrl) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AmberAccent.copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "⚠️ URL berakhiran /dev membutuhkan login Google. Gunakan URL hasil 'New deployment' yang berakhiran /exec.",
                                color = AmberAccent,
                                fontSize = 11.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tes Koneksi Button
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testResult = null
                        onTestConnection(urlText.trim()) { success, msg ->
                            isTesting = false
                            testResult = Pair(success, msg)
                        }
                    },
                    enabled = urlText.isNotBlank() && !isTesting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = IndigoPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Menguji koneksi...", fontSize = 13.sp)
                    } else {
                        Icon(imageVector = Icons.Rounded.Power, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tes Koneksi ke Google Sheets", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                testResult?.let { (success, message) ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (success) EmeraldIncome.copy(alpha = 0.12f) else CoralExpense.copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = if (success) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                contentDescription = null,
                                tint = if (success) EmeraldIncome else CoralExpense,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = message,
                                color = if (success) EmeraldIncome else CoralExpense,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Section 3: Unduh File Excel (.xlsx) & Link Google Sheets
                val downloadUrl = excelExportUrl ?: if (urlText.isNotBlank()) "$urlText?export=xlsx" else null
                val openSheetUrl = spreadsheetUrl

                if (!downloadUrl.isNullOrBlank() || !openSheetUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Ekspor & File Database",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!downloadUrl.isNullOrBlank()) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldIncome)
                        ) {
                            Icon(imageVector = Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download File Excel (.xlsx)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    if (!openSheetUrl.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(openSheetUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Buka di Google Sheets", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(urlText.trim(), selectedProfile)
                    onDismiss()
                },
                enabled = urlText.isNotBlank() && !isDocsUrl,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
            ) {
                Icon(imageVector = Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Tutup")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
