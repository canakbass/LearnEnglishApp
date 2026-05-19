package com.app.wordlearn.presentation.words

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.wordlearn.domain.model.Word
import java.io.File

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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
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
                Text("📚 Kelime Listesi", fontSize = 24.sp, fontWeight = FontWeight.Bold)

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

                if (words.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Henüz kelime yok", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(words) { word ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    // Kelimeye ait resim varsa göster
                                    word.picturePath?.let { path ->
                                        AsyncImage(
                                            model = File(path),
                                            contentDescription = word.engWord,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp)
                                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(word.engWord, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(word.turWord, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.7f))
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { viewModel.playAudio(word.engWord) }) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Sesli Oku")
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(viewModel: WordListViewModel, onBack: () -> Unit) {
    var engWord by remember { mutableStateOf("") }
    var turWord by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("A1") }
    var category by remember { mutableStateOf("Genel") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val samples = remember { mutableStateListOf<String>("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
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

        Button(
            onClick = {
                photoPicker.launch(
                    androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (selectedImageUri == null) "Fotoğraf Ekle (Opsiyonel)" else "Fotoğraf Seçildi")
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text("Örnek Cümleler", fontWeight = FontWeight.Medium)
        samples.forEachIndexed { index, sample ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = sample,
                    onValueChange = { samples[index] = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Örnek Cümle $(index + 1)") },
                    singleLine = true
                )
                IconButton(onClick = { samples.removeAt(index) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Sil")
                }
            }
        }
        TextButton(onClick = { samples.add("") }) {
            Text("+ Cümle Ekle")
        }
        Spacer(modifier = Modifier.height(12.dp))

        Text("Seviye", fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("A1", "A2", "B1", "B2", "C1", "C2").forEach { l ->
                FilterChip(
                    selected = level == l,
                    onClick = { level = l },
                    label = { Text(l) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

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
                        viewModel.addWord(engWord, turWord, level, category, selectedImageUri, samples.toList())
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