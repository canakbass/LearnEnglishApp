package com.app.wordlearn.presentation.wordchain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.model.Story
import com.app.wordlearn.domain.repository.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedStoriesViewModel @Inject constructor(
    private val storyRepository: StoryRepository
) : ViewModel() {

    private val _stories = MutableStateFlow<List<Story>>(emptyList())
    val stories: StateFlow<List<Story>> = _stories.asStateFlow()

    init {
        viewModelScope.launch {
            storyRepository.getAllStories().collect { storyList ->
                _stories.value = storyList
            }
        }
    }

    fun deleteStory(story: Story) {
        viewModelScope.launch {
            storyRepository.deleteStory(story)
        }
    }
}