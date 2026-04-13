package com.app.wordlearn

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.usecase.SyncWordsUseCase
import com.app.wordlearn.presentation.navigation.NavGraph
import com.app.wordlearn.presentation.theme.WordLearnTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val syncWordsUseCase: SyncWordsUseCase
) : ViewModel() {
    fun syncWords(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = context.assets.open("system_words.json").bufferedReader().use { it.readText() }
                val typeToken = object : TypeToken<List<Word>>() {}.type
                val words: List<Word> = Gson().fromJson(jsonString, typeToken)
                syncWordsUseCase.execute(words)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Trigger sync on startup
        viewModel.syncWords(this)
        
        enableEdgeToEdge()
        setContent {
            WordLearnTheme {
                NavGraph()
            }
        }
    }
}
