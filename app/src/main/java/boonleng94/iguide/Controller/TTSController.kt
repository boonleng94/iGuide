package boonleng94.iguide.Controller

import android.speech.tts.TextToSpeech
import android.content.Context
import android.util.Log

import java.util.Locale

//Controller class for the TTS
class TTSController {
    private val debugTAG = "TTSController"

    lateinit var talk: TextToSpeech

    fun initialize(context: Context, locale: Locale) {
        talk = TextToSpeech(context, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = talk.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(debugTAG, "This Language is not supported")
                }
                talk.setSpeechRate(0.8F)
                speakOut("Welcome to EyeGuide... ... Please wait and stay still as I find your current position...")
            } else {
                Log.e(debugTAG, "TTS Initialization Error")
            }
        })
    }

    fun speakOut(text:String) {
        talk.speak(text, TextToSpeech.QUEUE_FLUSH, null,"")
    }
}
