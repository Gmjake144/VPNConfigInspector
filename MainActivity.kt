package com.example.vpnconfiginspector

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Base64

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { InspectorScreen { uri ->
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        } } }
    }
}

@Composable
private fun InspectorScreen(readFile: (Uri) -> String) {
    var fileName by remember { mutableStateOf("No file selected") }
    var raw by remember { mutableStateOf("") }
    var report by remember { mutableStateOf("Choose a VPN configuration file to inspect it locally.") }
    var decoded by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            raw = runCatching { readFile(uri) }.getOrElse { "Could not read file: ${it.message}" }
            fileName = uri.lastPathSegment ?: "Selected file"
            decoded = ""
            report = analyzeConfig(raw, fileName)
        }
    }

    Column(Modifier.fillMaxSize().padding(18.dp).verticalScroll(rememberScrollState())) {
        Text("VPN Config Inspector", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text("Offline config reader • no VPN connection or traffic interception")
        Spacer(Modifier.height(16.dp))
        Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Open config file") }
        Text(fileName, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(14.dp))
        Text("Analysis", style = MaterialTheme.typography.titleMedium)
        SelectionText(report)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = {
            decoded = decodeBase64IfPossible(raw)
        }, enabled = raw.isNotBlank()) { Text("Try Base64 decode") }
        if (decoded.isNotBlank()) {
            Text("Decoded preview", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp))
            SelectionText(decoded.take(12000))
        }
        Spacer(Modifier.height(14.dp))
        Text("Safety: This app only reads files you select. It does not crack passwords, extract private keys, bypass VPN restrictions, or decrypt live traffic. Treat config files as secrets and do not share them.")
    }
}

@Composable
private fun SelectionText(value: String) {
    SelectionContainer {
        Text(value, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
    }
}

private fun analyzeConfig(text: String, fileName: String): String {
    if (text.isBlank()) return "The file is empty or unreadable."
    val lines = text.lines()
    val lower = text.lowercase()
    val findings = mutableListOf<String>()
    findings += "Size: ${text.toByteArray().size} bytes"
    findings += "Lines: ${lines.size}"
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val type = when {
        ext == "hc" -> "HTTP Custom (.hc) — proprietary/app-specific; content may be opaque"
        ext == "ehi" -> "HTTP Injector (.ehi) — proprietary/app-specific; content may be opaque"
        ext == "hat" -> "HA Tunnel Plus (.hat) — proprietary/app-specific; content may be opaque"
        lower.contains("<client>") || (lower.contains("client") && lower.contains("remote ")) -> "Possible OpenVPN profile"
        lower.contains("[interface]") && lower.contains("[peer]") -> "WireGuard configuration"
        lower.contains("ss://") || lower.contains("shadowsocks") -> "Possible Shadowsocks URI/config"
        lower.contains("vmess://") -> "Possible VMess URI"
        lower.contains("vless://") -> "Possible VLESS URI"
        lower.contains("trojan://") -> "Possible Trojan URI"
        lower.trimStart().startsWith("{") -> "Possible JSON configuration"
        else -> "Unknown / app-specific configuration"
    }
    findings += "Detected format: $type"
    val markers = listOf("remote ", "proto ", "dev ", "server =", "endpoint =", "[Interface]", "[Peer]", "cipher", "auth", "tls")
    val found = markers.filter { lower.contains(it) }
    findings += "Recognized setting markers: ${if (found.isEmpty()) "None" else found.joinToString(", ")}"
    val secretHints = listOf("privatekey", "password", "passwd", "token", "secret", "auth-user-pass")
    val count = secretHints.count { lower.contains(it) }
    findings += "Potential credential markers: $count (values intentionally not displayed)"
    findings += "Recognized file extensions: .hc, .ehi, .hat, .ovpn, .conf, .json, .txt"
    findings += "Note: .hc, .ehi, and .hat are proprietary formats. This app can identify the extension but cannot claim to decode their protected payloads. Use the official app/export or documented format support where available."
    findings += "Detection is heuristic; proprietary or encrypted formats may not be readable."
    return findings.joinToString("\n")
}

private fun decodeBase64IfPossible(input: String): String {
    val candidate = input.trim().replace("\\s".toRegex(), "")
    if (candidate.isEmpty()) return "No content to decode."
    return try {
        val normalized = candidate.replace('-', '+').replace('_', '/')
        val padded = normalized + "=".repeat((4 - normalized.length % 4) % 4)
        val bytes = Base64.getDecoder().decode(padded)
        val result = bytes.toString(Charsets.UTF_8)
        if (result.any { it == '\uFFFD' }) "Decoded bytes are not valid readable UTF-8 text." else result
    } catch (_: IllegalArgumentException) {
        "This content is not valid Base64 (or contains extra non-Base64 text)."
    }
}
