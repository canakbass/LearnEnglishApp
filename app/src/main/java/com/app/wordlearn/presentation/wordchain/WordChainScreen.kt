package com.app.wordlearn.presentation.wordchain

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.app.wordlearn.domain.model.ChainResult

@Composable
fun WordChainScreen(
    viewModel: WordChainViewModel,
    onNavigateToSavedStories: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        WordChainActions(
            isLoading = uiState is WordChainUiState.Loading,
            onGenerate = viewModel::generateChain,
            onNavigateToSaved = onNavigateToSavedStories
        )
        Spacer(modifier = Modifier.height(24.dp))

        when (val state = uiState) {
            is WordChainUiState.Idle -> IdleHint()
            is WordChainUiState.Loading -> Unit // button içinde gösteriliyor
            is WordChainUiState.Success -> SuccessSection(state.result, viewModel::generateChain)
            is WordChainUiState.Error -> ErrorSection(state.message, viewModel::generateChain)
        }
    }
}

@Composable
private fun WordChainActions(
    isLoading: Boolean,
    onGenerate: () -> Unit,
    onNavigateToSaved: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onGenerate,
            modifier = Modifier.weight(1f).height(50.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("AI Üretiyor...", fontSize = 16.sp)
            } else {
                Text("Zincir Oluştur", fontSize = 16.sp)
            }
        }
        OutlinedButton(onClick = onNavigateToSaved, modifier = Modifier.height(50.dp)) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Kayıtlı Hikayeler")
        }
    }
}

@Composable
private fun IdleHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🧩", fontSize = 40.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Butona basarak rastgele kelimelerden\nbir zincir, AI hikayesi ve görseli oluşturun.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SuccessSection(result: ChainResult, onRegenerate: () -> Unit) {
    WordChainChips(result.words)
    Spacer(modifier = Modifier.height(16.dp))
    result.imagePath?.let { path -> StoryImageCard(path) }
    StoryTextCard(result.story)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(onClick = onRegenerate, modifier = Modifier.fillMaxWidth()) {
        Text("🔄 Yeni Zincir Oluştur")
    }
}

@Composable
private fun WordChainChips(words: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("🔗 Kelime Zinciri", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                words.forEachIndexed { index, word ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(word, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                    if (index < words.size - 1) {
                        Text(" → ", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryImageCard(imagePath: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🎨 AI Görseli", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            AsyncImage(
                model = imagePath,
                contentDescription = "AI Generated Story Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(8.dp))
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun StoryTextCard(story: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("📖 AI Hikaye", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(story, fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun ErrorSection(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) { Text("Tekrar Dene") }
        }
    }
}
