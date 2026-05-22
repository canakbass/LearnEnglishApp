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
import com.app.wordlearn.domain.model.WordProgress
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
    val selectedTab by viewModel.selectedTab.collectAsState()
    val tabCounts by viewModel.tabCounts.collectAsState()
    val progressByWordId by viewModel.progressByWordId.collectAsState()
    var pendingDelete by remember { mutableStateOf<Word?>(null) }
    // Hot-path callback'leri remember'la sabitle — item composable'ları "stable" kalsın.
    val onPlay = remember(viewModel) { { word: Word -> viewModel.playAudio(word.engWord) } }
    val onDelete = remember { { word: Word -> pendingDelete = word } }

    // Her composition'da değil, ekran her geri geldiğinde de tazele — VM ayrı entry'de
    // olabileceği için kelime ekleme sonrası güncel listeyi göster.
    LaunchedEffect(Unit) { viewModel.loadWords() }

    pendingDelete?.let { word ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Kelimeyi sil") },
            text = {
                Text(
                    "\"${word.engWord}\" kelimesini ve buna bağlı tüm ilerleme/cevap kayıtlarını " +
                        "silmek istediğinize emin misiniz? Bu işlem geri alınamaz."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteUserWord(word)
                    pendingDelete = null
                }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("İptal") }
            }
        )
    }

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

                // Sekme şeridi — solda öğrenilmemiş, sağda öğrenmekte/öğrenildi.
                TabRow(
                    selectedTabIndex = if (selectedTab == WordListTab.Unlearned) 0 else 1
                ) {
                    Tab(
                        selected = selectedTab == WordListTab.Unlearned,
                        onClick = { viewModel.selectTab(WordListTab.Unlearned) },
                        text = { Text("Öğrenilmemiş (${tabCounts.first})") }
                    )
                    Tab(
                        selected = selectedTab == WordListTab.InProgress,
                        onClick = { viewModel.selectTab(WordListTab.InProgress) },
                        text = { Text("Öğreniyorum (${tabCounts.second})") }
                    )
                }

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
                        val msg = when (selectedTab) {
                            WordListTab.Unlearned -> "Hiç başlanmamış kelime kalmadı 🎉"
                            WordListTab.InProgress -> "Henüz hiçbir kelimeye başlamadın"
                        }
                        Text(msg, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(
                            items = words,
                            // Stable identity → scroll sırasında recomposition skip mümkün.
                            key = { it.wordId },
                            // Aynı layout (resimsiz vs resimli) recycle edilir.
                            contentType = { if (it.picturePath == null) "text" else "image" }
                        ) { word ->
                            WordRow(
                                word = word,
                                inProgressTab = selectedTab == WordListTab.InProgress,
                                progress = progressByWordId[word.wordId],
                                onPlay = onPlay,
                                onDelete = onDelete
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tek bir kelime kartı. Skippable composable — tüm parametreleri stable.
 *
 * Performans notları:
 *  - `word` ve `progress` data class'ları immutable (Compose otomatik stable).
 *  - Callback'ler dışarıda `remember` ile sabitlendi → kart parametreleri değişmedikçe
 *    Compose bu composable'ı SKIP eder (recomposition yok).
 *  - `picturePath` için `File` objesi `remember(path)` ile cache'lenir, allocation 1x.
 */
@Composable
private fun WordRow(
    word: Word,
    inProgressTab: Boolean,
    progress: WordProgress?,
    onPlay: (Word) -> Unit,
    onDelete: (Word) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            val path = word.picturePath
            if (path != null) {
                val file = remember(path) { File(path) }
                AsyncImage(
                    model = file,
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
                    Text(
                        word.turWord,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onPlay(word) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Sesli Oku")
                    }
                    if (word.source == "user") {
                        IconButton(onClick = { onDelete(word) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (inProgressTab && progress != null) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    stageBadge(progress.reviewStage, progress.isLearned),
                                    fontSize = 11.sp
                                )
                            }
                        )
                    } else {
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

/**
 * Stage'e göre rozet etiketi — algoritmadaki stage numarasını kullanıcıya görünür hale getirir.
 * Stage 0 görünmez (Öğrenilmemiş sekmesinde).
 */
private fun stageBadge(stage: Int, isLearned: Boolean): String = when {
    isLearned -> "✓ Öğrenildi"
    stage == 1 -> "🌱 1/6"
    stage == 2 -> "🌿 2/6"
    stage == 3 -> "🌳 3/6"
    stage == 4 -> "💪 4/6"
    stage == 5 -> "🏆 5/6"
    stage >= 6 -> "✓ Öğrenildi"
    else -> "Yeni"
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
                    label = { Text("Örnek Cümle ${index + 1}") },
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