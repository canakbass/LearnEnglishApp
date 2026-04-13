package com.app.wordlearn.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadSettings() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("⚙️ Ayarlar", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Günlük kelime sayısı
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Günlük Yeni Kelime Sayısı", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${settings.dailyNewWordCount} kelime",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = settings.dailyNewWordCount.toFloat(),
                    onValueChange = { viewModel.updateDailyWordCount(it.toInt()) },
                    valueRange = 5f..30f,
                    steps = 4
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("5", fontSize = 12.sp)
                    Text("30", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Seviye seçimi
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Bilgi Düzeyi", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Başlangıç", "Orta", "İleri").forEach { level ->
                        FilterChip(
                            selected = settings.userLevel == level,
                            onClick = { viewModel.updateUserLevel(level) },
                            label = { Text(level) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Kullanıcı adı
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Kullanıcı Adı", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                var name by remember { mutableStateOf(settings.displayName) }
                LaunchedEffect(settings.displayName) { name = settings.displayName }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.updateDisplayName(name) }) {
                    Text("Kaydet")
                }
            }
        }
    }
}
