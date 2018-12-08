package boonleng94.iguide.View

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.UtteranceProgressListener
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.TextView
import boonleng94.iguide.*
import boonleng94.iguide.Controller.Navigator
import boonleng94.iguide.Controller.ShakeDetector
import boonleng94.iguide.Controller.ShakeListener
import boonleng94.iguide.Controller.TTSController
import boonleng94.iguide.Model.*
import boonleng94.iguide.Model.Dictionary

import kotlin.collections.ArrayList
import kotlinx.android.synthetic.main.activity_dest.*

import java.util.*

//Activity class for the UI after reaching destination
class MainDestinationReachedActivity : AppCompatActivity() {
    private val debugTAG = "MainDestinationReachedActivity"

    private lateinit var shakeDetector: ShakeDetector
    private lateinit var TTSCtrl: TTSController
    private lateinit var compass: Compass
    private lateinit var userOrientation: Orientation
    private lateinit var mSpeechRecognizer: SpeechRecognizer
    private lateinit var mSpeechRecognizerIntent: Intent
    private lateinit var mSpeechRecognitionListener: SpeechRecognitionListener
    private lateinit var currentPos: Beacon
    private lateinit var beaconList: ArrayList<Beacon>

    private lateinit var destReachedTv: TextView

    private var displayList = ArrayList<String>()

    private var destOutputString = StringBuilder("Would you like to go to nearby? ")

    private var TTSOutput = "Eye Guide"

    private var doneSpeech = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dest_reached)

        destReachedTv = findViewById(R.id.tv_destination_reached)
        currentPos = intent.getSerializableExtra("currentPos") as Beacon
        destReachedTv.text = currentPos.name

        TTSCtrl = (application as MainApp).speech
        TTSOutput = "You have reached your destination! . . ."
        TTSCtrl.speakOut(TTSOutput)

        beaconList = (application as MainApp).beaconList

        initializeListeners()

        displayNearBeacons(beaconList, currentPos)

        Handler().postDelayed({
            readDestinations()
        }, 5000)
    }

    override fun onDestroy() {
        shakeDetector.stop()
        compass.stop()
        mSpeechRecognizer.stopListening()
        mSpeechRecognizer.destroy()
        Log.d(debugTAG, "MainDestinationsActivity destroyed")
        super.onDestroy()
    }

    private fun readDestinations() {
        var index = 1

        for (i in displayList) {
            destOutputString.append(". $index. ${i}. ")
            index++
        }

        val TTSUPL = object: UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                doneSpeech = true
            }
            override fun onError(utteranceId: String?) {
            }
            override fun onStart(utteranceId: String?) {
                doneSpeech = false
            }
        }

        TTSCtrl.talk.setOnUtteranceProgressListener(TTSUPL)

        TTSOutput = destOutputString.toString()
        TTSCtrl.speakOut(TTSOutput)

        while (true) {
            if (doneSpeech) {
                doneSpeech = false
                mSpeechRecognizer.startListening(mSpeechRecognizerIntent)
                break
            }
        }
    }

    //Start navigation, initialize Navigator, pass data to MainNavigationActivity
    private fun startNavigation(destBeaconName: String) {
        shakeDetector.stop()
        mSpeechRecognizer.destroy()

        Handler().postDelayed( {
            compass.stop()
        }, 2000)

        var destBeacon = Beacon("Beacon")

        if (destBeaconName == "Exit") {
            TTSOutput = "Thank you for using Eye Guide"
            TTSCtrl.speakOut(TTSOutput)

            finish()
            startActivity(Intent(applicationContext, MainMapActivity::class.java))
        }
        else {
            if (destBeaconName != "Original Location") {
                for (i in beaconList) {
                    if (i.name == destBeaconName) {
                        destBeacon = i
                    }
                }
            } else {
                destBeacon = Beacon("Original Spot")
                destBeacon.coordinate = (application as MainApp).startPos
            }

            Handler().postDelayed( {
                val currentPos =  currentPos.coordinate

                //done here and finish() to prevent memory error
                val nav = Navigator()

                var nextBeacon = nav.findNextNearest(currentPos, beaconList)
                nav.initialize(nextBeacon.coordinate, userOrientation, destBeacon.coordinate, beaconList)
                val idealQueue = nav.executeFastestPath() as LinkedList<Beacon>

                val intent = Intent(applicationContext, MainNavigationActivity::class.java)
                intent.putExtra("destination", destBeacon)
                intent.putExtra("currentPos", currentPos)
                intent.putExtra("currentOrientation", userOrientation)
                intent.putExtra("nextBeacon", nextBeacon)
                intent.putExtra("idealQueue", idealQueue)
                intent.putExtra("beaconList", beaconList)
                finish()
                startActivity(intent)
            }, 5000)
        }
    }

    //Initialize necessary listeners such as compass, STT, gestures
    private fun initializeListeners() {
        var azimuthCount = 0
        var tempAzimuth: Float = 0.toFloat()
        compass = Compass(this)

        val cl = object: CompassListener {
            override fun onAzimuthChange(azimuth: Float) {
                //Do something each time azimuth changes
                azimuthCount++
                tempAzimuth += azimuth
                if (azimuthCount == 200) {
                    tempAzimuth /= azimuthCount
                    userOrientation = compass.getOrientation(tempAzimuth)
                    azimuthCount = 0
                    //Log.d(debugTAG, "Azimuth: " + tempAzimuth)
                }
            }
        }

        compass.compassListener = cl
        compass.start()

        mSpeechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.packageName)

        mSpeechRecognitionListener = SpeechRecognitionListener()

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        mSpeechRecognizer.setRecognitionListener(mSpeechRecognitionListener)

        val gl = object: GestureDetector.OnGestureListener {
            override fun onDown(p0: MotionEvent?): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onLongPress(p0: MotionEvent?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onShowPress(p0: MotionEvent?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSingleTapUp(p0: MotionEvent?): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }

        shakeDetector = ShakeDetector(this)
        val sl = object: ShakeListener {
            override fun onShake(count: Int) {
                //Do shake event here
                if (count > 3) {
                    //Repeat audio
                    Log.d(debugTAG, "Shake detected: $count")
                    TTSCtrl.speakOut(TTSOutput)
                }
            }
        }
        shakeDetector.shakeListener = sl

        shakeDetector.start()
    }

    //Inner class to hold the STT listener
    private inner class SpeechRecognitionListener : RecognitionListener {
        val intMap = Dictionary().intMap

        override fun onBeginningOfSpeech() {
        }

        override fun onBufferReceived(buffer: ByteArray) {
        }

        override fun onEndOfSpeech() {
        }

        override fun onError(error: Int) {
            Log.d(debugTAG, "STT error code: " + error)
            val TTSUPL = object: UtteranceProgressListener() {
                override fun onDone(utteranceId: String?) {
                    doneSpeech = true
                }
                override fun onError(utteranceId: String?) {
                }
                override fun onStart(utteranceId: String?) {
                }
            }
            TTSCtrl.talk.setOnUtteranceProgressListener(TTSUPL)
            TTSCtrl.speakOut("Failed to detect any sound. Please try again")

            while (true) {
                if (doneSpeech) {
                    doneSpeech = false
                    mSpeechRecognizer.startListening(mSpeechRecognizerIntent)
                    break
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle) {
        }

        override fun onPartialResults(partialResults: Bundle) {
        }

        override fun onReadyForSpeech(params: Bundle) {
        }

        //Speech to text onResults
        override fun onResults(results: Bundle) {
            val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            var choice: Int = -1

            Log.d(debugTAG, "STT results: " + matches.toString())

            if (matches.isNotEmpty()) {
                for ((index,i) in displayList.withIndex()) {
                    if (matches.contains(i.toLowerCase()) || matches.contains(i)) {
                        choice = index
                        break
                    } else if (matches.contains(intMap.getValue(index+1)) || matches.contains((index+1).toString())) {
                        choice = index
                        break
                    }
                }

                if (choice != -1) {
                    val dest = displayList[choice]

                    //Start navigation to choice
                    choice += 1
                    val output = "You have chosen choice $choice ..., $dest ... Please wait as I find your orientation"
                    TTSCtrl.speakOut(output)

                    if (choice == displayList.size) {
                        startNavigation("Exit")
                    } else {
                        startNavigation(dest)
                    }
                } else {
                    val TTSUPL = object: UtteranceProgressListener() {
                        override fun onDone(utteranceId: String?) {
                            doneSpeech = true
                        }
                        override fun onError(utteranceId: String?) {
                        }
                        override fun onStart(utteranceId: String?) {
                        }
                    }
                    TTSCtrl.talk.setOnUtteranceProgressListener(TTSUPL)
                    TTSCtrl.speakOut("Failed to recognize a choice. Please try again")

                    while (true) {
                        if (doneSpeech) {
                            doneSpeech = false
                            mSpeechRecognizer.startListening(mSpeechRecognizerIntent)
                            break
                        }
                    }
                }
            } else {
                val TTSUPL = object: UtteranceProgressListener() {
                    override fun onDone(utteranceId: String?) {
                        doneSpeech = true
                    }
                    override fun onError(utteranceId: String?) {
                    }
                    override fun onStart(utteranceId: String?) {
                    }
                }
                TTSCtrl.talk.setOnUtteranceProgressListener(TTSUPL)
                TTSCtrl.speakOut("No sound detected. Please try again")

                while (true) {
                    if (doneSpeech) {
                        doneSpeech = false
                        mSpeechRecognizer.startListening(mSpeechRecognizerIntent)
                        break
                    }
                }
            }
        }

        override fun onRmsChanged(rmsdB: Float) {}
    }

    //Display only beacons near the current user
    private fun displayNearBeacons(beaconList: ArrayList<Beacon>, currentPos: Beacon) {
        var nearBeacons = ArrayList<String>()

        val index = beaconList.indexOf(currentPos)

        var beacon1 = beaconList[index+2].name
        var beacon2 = beaconList[index+1].name
        var beacon3 = beaconList[index-1].name
        var beacon4 = beaconList[index-2].name
        var originalPos = "GO back to original spot"
        var exit = "Exit the app"

        nearBeacons.add(beacon1)
        nearBeacons.add(beacon2)
        nearBeacons.add(beacon3)
        nearBeacons.add(beacon4)
        nearBeacons.add(originalPos)
        nearBeacons.add(exit)

        for (i in nearBeacons) {
            if (i != "N4-02c Placeholder") {
                val tvDest = TextView(this)
                tvDest.gravity = Gravity.CENTER_HORIZONTAL
                tvDest.textSize = 24f
                tvDest.isClickable = false
                tvDest.text = i

                displayList.add(i)
                sv_linear_layout.addView(tvDest)
            }
        }
    }
}