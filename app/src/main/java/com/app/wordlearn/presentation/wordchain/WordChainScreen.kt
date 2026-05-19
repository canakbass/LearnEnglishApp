package com.app.wordlearn.presentation.wordchain

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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

import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon

@Composable
fun WordChainScreen(
    viewModel: WordChainViewModel,
    onNavigateToSavedStories: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.generateChain() },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                enabled = uiState !is WordChainUiState.Loading
            ) {
                if (uiState is WordChainUiState.Loading) {
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

            OutlinedButton(
                onClick = onNavigateToSavedStories,
                modifier = Modifier.height(50.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = "Kayıtlı Hikayeler")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (val state = uiState) {
            is WordChainUiState.Idle -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
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

            is WordChainUiState.Loading -> {
                // Loading is handled by the button state
            }

            is WordChainUiState.Success -> {
                // Kelime zinciri
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "🔗 Kelime Zinciri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Her kelimeyi ayrı chip olarak göster
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            state.result.words.forEachIndexed { index, word ->
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            word,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                )
                                if (index < state.result.words.size - 1) {
                                    Text(
                                        " → ",
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // AI Image
                if (state.result.imagePath != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "🎨 AI Görseli",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            AsyncImage(
                                model = state.result.imagePath,
                                contentDescription = "AI Generated Story Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Hikaye
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "📖 AI Hikaye",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            state.result.story,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tekrar oluştur butonu
                OutlinedButton(
                    onClick = { viewModel.generateChain() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔄 Yeni Zincir Oluştur")
                }
            }

            is WordChainUiState.Error -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.generateChain() }) {
                            Text("Tekrar Dene")
                        }
                    }
                }
            }
        }
    }
}
