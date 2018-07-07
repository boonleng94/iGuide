package boonleng94.iguide


import android.speech.tts.TextToSpeech
import android.content.Context
import android.util.Log

import java.util.Locale

class TTSController(context: Context, locale: Locale, callback: TTSListener) {
    private lateinit var talk: TextToSpeech

    init {
        talk = TextToSpeech(context, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = talk.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "This Language is not supported")
                }
                talk.setSpeechRate(0.8F)
                callback.onSuccess(talk)
            } else {
                callback.onFailure(talk)
                Log.e("TTS", "TTS Initialization Error")
            }
        })
    }

    fun speakOut(text:String) {
        talk.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
    }
}
