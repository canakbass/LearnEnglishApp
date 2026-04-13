package com.app.wordlearn.presentation.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.repository.WordRepository
import com.app.wordlearn.domain.usecase.AddWordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val addWordUseCase: AddWordUseCase
) : ViewModel() {

    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadWords() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _words.value = wordRepository.getAllWords()
            _isLoading.value = false
        }
    }

    fun searchWords(query: String) {
        _searchQuery.value = query
        viewModelScope.launch(Dispatchers.IO) {
            _words.value = if (query.isBlank()) {
                wordRepository.getAllWords()
            } else {
                wordRepository.searchWords(query)
            }
        }
    }

    fun addWord(engWord: String, turWord: String, level: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val word = Word(
                engWord = engWord,
                turWord = turWord,
                level = level,
                category = category,
                source = "user"
            )
            addWordUseCase.execute(word)
            loadWords()
        }
    }
}
