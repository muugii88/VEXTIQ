package com.vextiq.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vextiq.optimizer.SmartFixBackup
import com.vextiq.optimizer.StarCitizenDiagnostics
import com.vextiq.optimizer.StarCitizenFix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SmartFixState { IDLE, DIAGNOSING, DIAGNOSED, CONFIRMING, RUNNING, DONE }

@Composable
fun TroubleshootPage() {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf(SmartFixState.IDLE) }
    var report by remember { mutableStateOf<StarCitizenDiagnostics.Report?>(null) }
    var preview by remember { mutableStateOf<StarCitizenFix.Preview?>(null) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var confirmText by remember { mutableStateOf("") }
    val backups = remember { mutableStateOf(SmartFixBackup.listBackups(StarCitizenFix.FIX_ID)) }

    fun log(line: String) { logs = logs + line }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with warning banner
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(VextiqColors.Warning.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .border(1.dp, VextiqColors.Warning.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text("⚠  TROUBLESHOOT — DESTRUCTIVE OPERATIONS",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Warning)
            Spacer(Modifier.height(6.dp))
            Text(
                "These tools modify launcher cache and game files to fix errors. " +
                    "Every fix is fully backed up and reversible, but only run them when you have a specific problem.",
                fontSize = 11.sp, color = VextiqColors.TextSecondary
            )
        }

        // Star Citizen card
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(VextiqColors.Card, RoundedCornerShape(12.dp))
                .border(1.dp, VextiqColors.Border, RoundedCornerShape(12.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🛸", fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("Star Citizen", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Primary)
                    Text("Fix Error 5007 without redownloading 150 GB", fontSize = 10.sp, color = VextiqColors.TextMuted)
                }
            }

            Divider(color = VextiqColors.Border)

            // Step 1: Diagnose
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        logs = emptyList()
                        state = SmartFixState.DIAGNOSING
                        scope.launch {
                            log("[>>] Running diagnostic scan...")
                            val r = withContext(Dispatchers.IO) { StarCitizenDiagnostics.diagnose() }
                            report = r
                            preview = StarCitizenFix().preview(r)
                            r.checks.forEach { check ->
                                val prefix = when (check.status) {
                                    StarCitizenDiagnostics.Status.OK -> "[OK]"
                                    StarCitizenDiagnostics.Status.WARN -> "[!] "
                                    StarCitizenDiagnostics.Status.PROBLEM -> "[✗] "
                                    StarCitizenDiagnostics.Status.INFO -> "[i] "
                                }
                                log("$prefix ${check.label}: ${check.detail}")
                            }
                            log("")
                            if (r.recommendFix) {
                                log("Recommended: Smart Fix  (${r.problemCount} problem(s) detected)")
                            } else {
                                log("No problems detected — Smart Fix is disabled.")
                            }
                            state = SmartFixState.DIAGNOSED
                        }
                    },
                    enabled = state != SmartFixState.DIAGNOSING && state != SmartFixState.RUNNING,
                    colors = ButtonDefaults.buttonColors(containerColor = VextiqColors.Primary)
                ) { Text("🔍  Diagnose", color = VextiqColors.Background) }

                val canFix = report?.recommendFix == true && state == SmartFixState.DIAGNOSED
                Button(
                    onClick = { state = SmartFixState.CONFIRMING; confirmText = "" },
                    enabled = canFix,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canFix) VextiqColors.Error else VextiqColors.Hover
                    )
                ) { Text("⚙  Smart Fix...", color = Color.White) }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            logs = emptyList()
                            log("[>>] Restoring from latest backup...")
                            val ok = withContext(Dispatchers.IO) {
                                StarCitizenFix().restoreLatest { log(it) }
                            }
                            log(if (ok) "[OK] Restore complete" else "[!] Restore finished with issues")
                            backups.value = SmartFixBackup.listBackups(StarCitizenFix.FIX_ID)
                        }
                    },
                    enabled = backups.value.isNotEmpty() && state != SmartFixState.RUNNING,
                ) { Text("↶  Restore Backup", color = VextiqColors.Primary) }
            }

            if (backups.value.isNotEmpty()) {
                Text("${backups.value.size} backup(s) available", fontSize = 10.sp, color = VextiqColors.TextMuted)
            }

            // Confirmation dialog inline
            if (state == SmartFixState.CONFIRMING) {
                ConfirmationCard(
                    preview = preview!!,
                    confirmText = confirmText,
                    onConfirmTextChange = { confirmText = it },
                    onCancel = { state = SmartFixState.DIAGNOSED },
                    onProceed = {
                        state = SmartFixState.RUNNING
                        logs = emptyList()
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                StarCitizenFix().execute(report!!) { line -> log(line) }
                            }
                            backups.value = SmartFixBackup.listBackups(StarCitizenFix.FIX_ID)
                            state = SmartFixState.DONE
                        }
                    }
                )
            }
        }

        // Log output
        if (logs.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .background(VextiqColors.Background, RoundedCornerShape(10.dp))
                    .border(1.dp, VextiqColors.Border, RoundedCornerShape(10.dp))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                logs.forEach { line ->
                    val color = when {
                        line.startsWith("[OK]") || line.contains("✓") -> VextiqColors.Success
                        line.startsWith("[!]") -> VextiqColors.Warning
                        line.startsWith("[✗]") -> VextiqColors.Error
                        line.startsWith("[i]") -> VextiqColors.TextMuted
                        line.startsWith("[>>]") -> VextiqColors.Primary
                        else -> VextiqColors.TextSecondary
                    }
                    Text(line, fontSize = 11.sp, color = color, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun ConfirmationCard(
    preview: StarCitizenFix.Preview,
    confirmText: String,
    onConfirmTextChange: (String) -> Unit,
    onCancel: () -> Unit,
    onProceed: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(VextiqColors.Error.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .border(1.dp, VextiqColors.Error, RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("⚠  Confirm Smart Fix", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Error)
        Text("VEXTIQ will:", fontSize = 11.sp, color = VextiqColors.TextSecondary)

        val totalDelete = StarCitizenDiagnostics.formatBytes(preview.totalDeleteBytes)
        DeleteRow("DELETE", "${preview.partialFilesToDelete.size} partial patch files " +
            "(${StarCitizenDiagnostics.formatBytes(preview.partialBytes)})")
        DeleteRow("DELETE", "RSI Launcher cache (${StarCitizenDiagnostics.formatBytes(preview.launcherCacheBytes)})")
        preview.shaderCacheToClear?.let {
            DeleteRow("DELETE", "SC shader cache (${StarCitizenDiagnostics.formatBytes(preview.shaderCacheBytes)})")
        }
        if (preview.gpuCachesToClear.isNotEmpty()) {
            DeleteRow("DELETE", "${preview.gpuCachesToClear.size} GPU shader caches")
        }
        KeepRow("BACKUP", "Keybinds + launcher state (fully reversible)")
        KeepRow("KEEP",   "All game files (~150 GB untouched)")
        KeepRow("ADD",    "Firewall rules for RSI endpoints")

        Text("Total deletion: $totalDelete", fontSize = 11.sp, color = VextiqColors.Warning, fontWeight = FontWeight.Bold)
        Text("RSI Launcher will redownload partial files on next start.", fontSize = 10.sp, color = VextiqColors.TextMuted)

        Spacer(Modifier.height(4.dp))
        Text("Type FIX to confirm:", fontSize = 11.sp, color = VextiqColors.TextSecondary)
        BasicTextField(
            value = confirmText,
            onValueChange = onConfirmTextChange,
            textStyle = TextStyle(color = VextiqColors.Primary, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
                .background(VextiqColors.Background, RoundedCornerShape(6.dp))
                .border(1.dp, VextiqColors.Border, RoundedCornerShape(6.dp))
                .padding(8.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel", color = VextiqColors.Primary)
            }
            Button(
                onClick = onProceed,
                enabled = confirmText == "FIX",
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (confirmText == "FIX") VextiqColors.Error else VextiqColors.Hover
                )
            ) { Text("Proceed with Fix", color = Color.White) }
        }
    }
}

@Composable
private fun DeleteRow(tag: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("✗ $tag", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Error,
            fontFamily = FontFamily.Monospace, modifier = Modifier.width(80.dp))
        Text(text, fontSize = 11.sp, color = VextiqColors.TextSecondary)
    }
}

@Composable
private fun KeepRow(tag: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("✓ $tag", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = VextiqColors.Success,
            fontFamily = FontFamily.Monospace, modifier = Modifier.width(80.dp))
        Text(text, fontSize = 11.sp, color = VextiqColors.TextSecondary)
    }
}
