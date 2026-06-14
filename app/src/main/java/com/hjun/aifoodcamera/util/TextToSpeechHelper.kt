package com.hjun.aifoodcamera.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.CHINESE)
            isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isReady) {
                tts?.setLanguage(Locale.getDefault())
                isReady = true
            }
        }
    }

    fun speakFoodResult(foodName: String, calories: String, description: String) {
        if (!isReady) return
        val text = "识别结果：$foodName，估算热量 $calories 千卡。$description"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "food_result")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
