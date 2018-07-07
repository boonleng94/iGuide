package boonleng94.iguide

import android.speech.tts.TextToSpeech

interface TTSListener {
    fun onSuccess(tts: TextToSpeech)

    fun onFailure(tts: TextToSpeech)
}