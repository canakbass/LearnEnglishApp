package com.app.wordlearn.presentation.words

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.wordlearn.domain.model.Word
import com.app.wordlearn.domain.model.WordSample
import com.app.wordlearn.domain.repository.ProgressRepository
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

/**
 * Kelime listesinin sekme filtresi:
 *  - [Unlearned] reviewStage = 0 ve totalAttempts = 0 (hiç başlanmamış kelimeler)
 *  - [InProgress] reviewStage 1-4 (öğrenmekte olduğun kelimeler) + reviewStage 5 (tamamen öğrenildi).
 *    "0 hariç tüm seviyeler" tek bir sekmede toplandı — hem başlangıç hem tam öğrenilmiş.
 */
enum class WordListTab { Unlearned, InProgress }

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val progressRepository: ProgressRepository,
    private val addWordUseCase: AddWordUseCase,
    private val ttsManager: TtsManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Tüm kelimeler + filtrelenmiş liste — UI sekme/aramaya göre filtrelenmiş halini gösterir.
    private val _allWords = MutableStateFlow<List<Word>>(emptyList())
    private val _progressByWordId = MutableStateFlow<Map<Int, com.app.wordlearn.domain.model.WordProgress>>(emptyMap())

    private val _words = MutableStateFlow<List<Word>>(emptyList())
    val words: StateFlow<List<Word>> = _words.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(WordListTab.Unlearned)
    val selectedTab: StateFlow<WordListTab> = _selectedTab.asStateFlow()

    /** Sekme rozetleri için sayılar. */
    private val _tabCounts = MutableStateFlow(0 to 0)
    val tabCounts: StateFlow<Pair<Int, Int>> = _tabCounts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadWords() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            _allWords.value = wordRepository.getAllWords()
            _progressByWordId.value = progressRepository.getAllProgress().associateBy { it.wordId }
            applyFilters()
            _isLoading.value = false
        }
    }

    fun searchWords(query: String) {
        _searchQuery.value = query
        applyFilters()
    }

    fun selectTab(tab: WordListTab) {
        _selectedTab.value = tab
        applyFilters()
    }

    /**
     * Sekme + arama filtresini bellekte uygular.
     * "Öğrenilmemiş" = progress yok VEYA reviewStage 0 AND totalAttempts 0.
     * "Öğrenmekte/Öğrenildi" = progress var AND (reviewStage > 0 OR totalAttempts > 0).
     */
    private fun applyFilters() {
        val all = _allWords.value
        val progressMap = _progressByWordId.value
        val query = _searchQuery.value.trim().lowercase()

        fun isUnlearned(w: Word): Boolean {
            val p = progressMap[w.wordId]
            return p == null || (p.reviewStage == 0 && p.totalAttempts == 0)
        }

        val unlearnedCount = all.count { isUnlearned(it) }
        val inProgressCount = all.size - unlearnedCount
        _tabCounts.value = unlearnedCount to inProgressCount

        val byTab = all.filter {
            when (_selectedTab.value) {
                WordListTab.Unlearned -> isUnlearned(it)
                WordListTab.InProgress -> !isUnlearned(it)
            }
        }
        _words.value = if (query.isEmpty()) byTab else byTab.filter {
            it.engWord.lowercase().contains(query) || it.turWord.lowercase().contains(query)
        }
    }

    /** Bir kelimenin progress'ini UI'da rozet/etiket göstermek için döner. */
    fun progressFor(wordId: Int): com.app.wordlearn.domain.model.WordProgress? =
        _progressByWordId.value[wordId]

    fun playAudio(word: String) {
        ttsManager.speak(word)
    }

    /**
     * User word'ü siler. Sistem kelimelerine dokunmaz (WordDao guard'ı ile).
     * Bağımlı kayıtlar (samples, progress, answers) FK CASCADE ile temizlenir.
     */
    fun deleteUserWord(word: Word) {
        if (word.source != "user") return
        viewModelScope.launch(Dispatchers.IO) {
            wordRepository.deleteUserWord(word.wordId)
            loadWords()
        }
    }

    fun addWord(engWord: String, turWord: String, level: String, category: String, uri: Uri?, samples: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            // Boş cümleleri at — AddWordScreen başlangıçta tek bir boş eleman ile başlıyor.
            val cleanedSamples = samples.map { it.trim() }.filter { it.isNotEmpty() }

            val localPicturePath: String? = uri?.let { copyImageToInternal(it) }

            val word = Word(
                engWord = engWord,
                turWord = turWord,
                picturePath = localPicturePath,
                level = level,
                category = category,
                source = "user"
            )
            addWordUseCase.executeWithSamples(word, cleanedSamples)
            loadWords()
        }
    }

    /**
     * Seçilen URI'yi internal storage'a kopyalar.
     * inputStream açılamazsa veya kopyalama 0 byte ile biterse null döner —
     * DB'de stale path bırakılmaz.
     */
    private fun copyImageToInternal(uri: Uri): String? {
        val fileName = "word_img_${UUID.randomUUID()}.jpg"
        val destFile = File(context.filesDir, fileName)
        return try {
            val input = context.contentResolver.openInputStream(uri)
                ?: run {
                    android.util.Log.w(TAG, "openInputStream(uri=$uri) null döndü — resim atlandı")
                    return null
                }
            val bytesCopied = input.use { ins ->
                FileOutputStream(destFile).use { out -> ins.copyTo(out) }
            }
            if (bytesCopied <= 0 || destFile.length() <= 0) {
                android.util.Log.w(TAG, "Image kopyası 0 byte — sil ve atla")
                destFile.delete()
                null
            } else {
                destFile.absolutePath
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Image kopyalama hatası: ${e.message}", e)
            destFile.delete()
            null
        }
    }

    companion object {
        private const val TAG = "WordListVM"
    }
}