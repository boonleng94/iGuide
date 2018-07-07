package boonleng94.iguide


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Menu
import android.widget.ImageButton
import android.widget.TextView
import com.estimote.iguide.R

class STTTest : Activity() {
    private lateinit var txtSpeechInput: TextView
    private lateinit var btnSpeak: ImageButton
    private lateinit var mSpeechRecognizer: SpeechRecognizer
    private lateinit var mSpeechRecognizerIntent: Intent
    private lateinit var mSpeechRecognitionListener: SpeechRecognitionListener

    private var mIsListening: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stt_test)

        txtSpeechInput = findViewById(R.id.tvSpeechInput)
        btnSpeak = findViewById(R.id.btnSpeak)

        btnSpeak.setOnClickListener {
            if (!mIsListening)
            {
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
            }
        }

        mSpeechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.packageName)

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        mSpeechRecognitionListener = SpeechRecognitionListener()
        mSpeechRecognizer.setRecognitionListener(mSpeechRecognitionListener)

//      promptSpeechInput()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        //menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onDestroy() {
        mSpeechRecognizer.destroy();
        super.onDestroy()
    }

    private inner class SpeechRecognitionListener : RecognitionListener {
        override fun onBeginningOfSpeech() {
        }

        override fun onBufferReceived(buffer: ByteArray) {
        }

        override fun onEndOfSpeech() {
        }

        override fun onError(error: Int) {
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent)
        }

        override fun onEvent(eventType: Int, params: Bundle) {
        }

        override fun onPartialResults(partialResults: Bundle) {
        }

        override fun onReadyForSpeech(params: Bundle) {
        }

        override fun onResults(results: Bundle) {
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            txtSpeechInput.text = matches[0]
        }

        override fun onRmsChanged(rmsdB: Float) {}
    }
}