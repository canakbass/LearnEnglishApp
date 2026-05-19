package com.app.wordlearn.presentation.words

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.model.WordSample
import com.app.wordlearn.domain.repository.WordRepository
import com.app.wordlearn.domain.usecase.AddWordUseCase
import com.app.wordlearn.domain.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val addWordUseCase: AddWordUseCase,
    private val ttsManager: TtsManager,
    @ApplicationContext private val context: Context
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

    fun playAudio(word: String) {
        ttsManager.speak(word)
    }

    fun addWord(engWord: String, turWord: String, level: String, category: String, uri: Uri?, samples: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            var localPicturePath: String? = null
            
            // If image given, copy it to internal storage
            uri?.let {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val fileName = "word_img_${UUID.randomUUID()}.jpg"
                    val file = File(context.filesDir, fileName)
                    val outputStream = FileOutputStream(file)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    localPicturePath = file.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val word = Word(
                engWord = engWord,
                turWord = turWord,
                picturePath = localPicturePath,
                level = level,
                category = category,
                source = "user"
            )
            addWordUseCase.executeWithSamples(word, samples)
            loadWords()
        }
    }
}