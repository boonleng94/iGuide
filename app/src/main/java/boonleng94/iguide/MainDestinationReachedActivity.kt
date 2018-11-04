package boonleng94.iguide

import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.UtteranceProgressListener
import android.support.v4.app.NotificationCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.TextView

import com.estimote.proximity_sdk.api.ProximityObserver
import com.estimote.proximity_sdk.api.ProximityObserverBuilder
import com.estimote.proximity_sdk.api.ProximityZoneBuilder
import kotlinx.android.synthetic.main.activity_dest.*

import java.util.*
import kotlin.collections.ArrayList

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

        createNotificationChannel()

        val notif = NotificationCompat.Builder(this, (application as MainApp).channelID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Destination reached!")
                .setSmallIcon(R.drawable.iguide_logo)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .build()

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel((application as MainApp).channelID, name, importance)
            channel.description = getString(R.string.app_desc)

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    fun generateN42cMap(): Map {
        var beaconList = ArrayList<Beacon>()

        val cf1 = Beacon("08521b848f630526cdf23fe40044913d")
        cf1.name = "N4-02c Placeholder"
        cf1.coordinate = Coordinate(40.0,0.0)

        val cf2 = Beacon("bf96d1619008c20716ddafbf69747424")
        cf2.name = "N4-02c Placeholder"
        cf2.coordinate = Coordinate(40.0,2.0)

        val cf3 = Beacon("d137249e154e746b32fe0f25c89cfa05")
        cf3.name = "N4-02c Placeholder"
        cf3.coordinate = Coordinate(0.0,2.0)

        val cf4 = Beacon("21406f9c93238e88db98ec7fca351d20")
        cf4.name = "Entrance of Block B"
        cf4.coordinate = Coordinate(0.0,0.0)

        val lt1 = Beacon("c99857c5c5a01cdc348e02bb878c3b1d")
        lt1.name = "N4-02c-88"
        lt1.coordinate = Coordinate(35.0,2.0)

        val lt2 = Beacon("c449d2e64acc028dc214a74d53087827")
        lt2.name = "N4-02c-86"
        lt2.coordinate = Coordinate(31.0,2.0)

        val lt3 = Beacon("9176b3b9b95e4e6d69212c56fa21fe20")
        lt3.name = "N4-02c-84"
        lt3.coordinate = Coordinate(27.0,2.0)

        val lt4 = Beacon("881c05b08a25c096cbd4deaedfc6c70f")
        lt4.name = "N4-02c-82"
        lt4.coordinate = Coordinate(23.0,2.0)

        val br1 = Beacon("7d682761535f52f943994e8c8ef57613")
        br1.name = "N4-02c-80"
        br1.coordinate = Coordinate(19.0,2.0)

        val br2 = Beacon("99d3734cf5f35999cddc66e499b0f51e")
        br2.name = "N4-02c-78"
        br2.coordinate = Coordinate(15.0,2.0)

        val br3 = Beacon("4698eda62f2def1aef341553fa41b51c")
        br3.name = "N4-02c-76"
        br3.coordinate = Coordinate(11.0,2.0)

        val br4 = Beacon("e65c0c815eb675f11cad88bef67e1335")
        br4.name = "N4-02c-74"
        br4.coordinate = Coordinate(7.0,2.0)

        beaconList.add(cf1)
        beaconList.add(cf2)
        beaconList.add(cf3)
        beaconList.add(cf4)
        beaconList.add(lt1)
        beaconList.add(lt2)
        beaconList.add(lt3)
        beaconList.add(lt4)
        beaconList.add(br1)
        beaconList.add(br2)
        beaconList.add(br3)
        beaconList.add(br4)

        val map = Map("N4-02c", beaconList)

        return map
    }

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