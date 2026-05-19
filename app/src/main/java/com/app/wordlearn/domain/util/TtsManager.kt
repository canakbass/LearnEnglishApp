package com.app.wordlearn.domain.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class TtsManager @Inject constructor(@ApplicationContext context: Context) : TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = false
            } else {
                isInitialized = true
            }
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
