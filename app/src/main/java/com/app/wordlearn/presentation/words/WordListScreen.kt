package com.app.wordlearn.presentation.words

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordListScreen(
    viewModel: WordListViewModel,
    onAddWordClick: () -> Unit
) {
    val words by viewModel.words.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadWords() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddWordClick) {
                Icon(Icons.Default.Add, "Kelime Ekle")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("📖 Kelime Listesi", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchWords(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Kelime ara...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (words.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Henüz kelime yok", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(words) { word ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(word.engWord, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(word.turWord, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                                }
                                AssistChip(
                                    onClick = {},
                                    label = { Text(word.level, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddWordScreen(viewModel: WordListViewModel, onBack: () -> Unit) {
    var engWord by remember { mutableStateOf("") }
    var turWord by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("Orta") }
    var category by remember { mutableStateOf("Genel") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Yeni Kelime Ekle", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = engWord, onValueChange = { engWord = it },
            label = { Text("İngilizce Kelime") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = turWord, onValueChange = { turWord = it },
            label = { Text("Türkçe Karşılığı") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Seviye seçimi
        Text("Seviye", fontWeight = FontWeight.Medium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Kolay", "Orta", "Zor").forEach { l ->
                FilterChip(
                    selected = level == l,
                    onClick = { level = l },
                    label = { Text(l) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Kategori
        OutlinedTextField(
            value = category, onValueChange = { category = it },
            label = { Text("Kategori") },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("İptal")
            }
            Button(
                onClick = {
                    if (engWord.isNotBlank() && turWord.isNotBlank()) {
                        viewModel.addWord(engWord, turWord, level, category)
                        onBack()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = engWord.isNotBlank() && turWord.isNotBlank()
            ) {
                Text("Kaydet")
            }
        }
    }
}
