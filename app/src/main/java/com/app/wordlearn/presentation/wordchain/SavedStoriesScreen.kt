package com.app.wordlearn.presentation.wordchain

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.app.wordlearn.domain.model.Story
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedStoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: SavedStoriesViewModel = hiltViewModel()
) {
    val stories by viewModel.stories.collectAsState()

    Scaffold(topBar = { SavedStoriesTopBar(onNavigateBack) }) { padding ->
        if (stories.isEmpty()) {
            EmptyStoriesPlaceholder(padding)
        } else {
            StoriesList(
                stories = stories,
                padding = padding,
                onDelete = viewModel::deleteStory
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedStoriesTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = { Text("Kayıtlı Hikayeler") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
private fun EmptyStoriesPlaceholder(padding: PaddingValues) {
    Box(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Henüz kaydedilmiş hikaye yok.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StoriesList(
    stories: List<Story>,
    padding: PaddingValues,
    onDelete: (Story) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(stories, key = { it.id }) { story ->
            StoryCard(story = story, onDelete = onDelete)
        }
    }
}

@Composable
private fun StoryCard(story: Story, onDelete: (Story) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            StoryCardHeader(story = story, onDelete = onDelete)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Kelimeler: " + story.words.joinToString(", "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = story.storyText, style = MaterialTheme.typography.bodyMedium)
            story.imagePath?.let { path -> StoryImage(path = path, isLocal = story.isLocalImage) }
        }
    }
}

@Composable
private fun StoryCardHeader(story: Story, onDelete: (Story) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dateFormatter = remember {
            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("tr"))
        }
        Text(
            text = dateFormatter.format(Date(story.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        IconButton(onClick = { onDelete(story) }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Hikayeyi Sil",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun StoryImage(path: String, isLocal: Boolean) {
    Spacer(modifier = Modifier.height(16.dp))
    val model = remember(path, isLocal) { if (isLocal) File(path) else path }
    AsyncImage(
        model = model,
        contentDescription = "Hikaye Görseli",
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentScale = ContentScale.Crop
    )
}
