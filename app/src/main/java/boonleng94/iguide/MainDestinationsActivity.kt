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
import android.widget.Toast

import com.estimote.proximity_sdk.api.ProximityObserver
import com.estimote.proximity_sdk.api.ProximityObserverBuilder
import com.estimote.proximity_sdk.api.ProximityZoneBuilder

import com.lemmingapex.trilateration.LinearLeastSquaresSolver
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction

import kotlinx.android.synthetic.main.activity_dest.*
import kotlin.math.roundToInt

import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import org.apache.commons.math3.linear.SingularMatrixException

import java.util.*

class MainDestinationsActivity : AppCompatActivity() {
    private lateinit var destObsHandler: ProximityObserver.Handler
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var TTSCtrl: TTSController
    private lateinit var compass: Compass
    private lateinit var userOrientation: Orientation
    private lateinit var mSpeechRecognizer: SpeechRecognizer
    private lateinit var mSpeechRecognizerIntent: Intent
    private lateinit var mSpeechRecognitionListener: SpeechRecognitionListener

    private var destList = ArrayList<DestinationBeacon>()
    private var viewList = ArrayList<TextView>()
    private var displayList = ArrayList<String>()

    private var destOutputString = StringBuilder("Where would you like to go? ")

    private var TTSOutput = "iGuide"

    private var doneSpeech = false

    private val debugTAG = "MainDestinationsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dest)

        createNotificationChannel()

        TTSCtrl = (application as MainApp).speech
        TTSOutput = "Please wait as I find destinations..."
        TTSCtrl.speakOut(TTSOutput)

        destList = (application as MainApp).destList

        initializeListeners()

        val notif = NotificationCompat.Builder(this, (application as MainApp).channelID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Fetching destinations...")
                .setSmallIcon(R.drawable.iguide_logo)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .build()

        //Build a ProximityObserver for ranging Beacons
        val destProxObserver = ProximityObserverBuilder(applicationContext, (application as MainApp).proximityCloudCredentials)
                .withLowLatencyPowerMode()
                .withScannerInForegroundService(notif)
                .onError { /* handle errors here */
                    throwable ->
                    Log.d(debugTAG, "error msg: $throwable")
                }
                .build()

        //Build A ProximityZone to find Beacons with tag 'corridor' within 20m distance
        //2. Get beacons in the corridor, get the coordinate of the beacon and put into destList of beacons that match the deviceID
        val corridorZone = ProximityZoneBuilder()
                .forTag("corridor")
                .inCustomRange(20.0)
                .onEnter {
                    //only nearest beacon
                    beacon ->
                    val name = beacon.attachments["name"]
                    val description = beacon.attachments["description"]

                    Toast.makeText(this, "Entered destination beacon $name, description: $description", Toast.LENGTH_SHORT).show()
                    Log.d(debugTAG, "Entered destination beacon $name, description: $description")
                }
                .onExit {
                    Toast.makeText(this, "Left the destination beacon", Toast.LENGTH_SHORT).show()
                    Log.d(debugTAG, "Exit destination beacon")

                }
                .onContextChange {
                    //get all beacons
                    allDests ->
                    if (!allDests.isEmpty()) {
                        allDests.iterator().forEach { beacon ->
                            val destDeviceID = beacon.deviceId
                            val name = beacon.attachments["name"]!!
                            val coordinate = beacon.attachments["coordinate"]!!
                            val description = beacon.attachments["description"]!!

                            Log.d(debugTAG, "onContextChange - Device IDs: $destDeviceID")

                            if (destList.isNotEmpty()) {
                                for (i in destList) {
                                    if (i.deviceID.equals(destDeviceID, true)) {
                                        i.coordinate = Coordinate(coordinate.split(',')[0].trim().toInt(), coordinate.split(',')[1].trim().toInt())
                                        i.name = name
                                        i.description = description

                                        if (!displayList.contains(i.name)) {
                                            val tvDest = TextView(this)
                                            tvDest.gravity = Gravity.CENTER_HORIZONTAL
                                            tvDest.textSize = 24f
                                            tvDest.isClickable = false
                                            tvDest.text = i.name

                                            displayList.add(i.name)
                                            viewList.add(tvDest)
                                            sv_linear_layout.addView(tvDest)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                .build()

        destObsHandler = destProxObserver.startObserving(corridorZone)

        //After scanning for beacons for 5s
        Handler().postDelayed({
            if (viewList.isNotEmpty()) {
                destObsHandler.stop()
                readDestinations()
            } else {
                TTSCtrl.speakOut("No beacons found! Eye Guide is currently not usable. ")
                val homeIntent = Intent(Intent.ACTION_MAIN)
                homeIntent.addCategory(Intent.CATEGORY_HOME)
                homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

                destObsHandler.stop()
                shakeDetector.stop()
                compass.stop()
                mSpeechRecognizer.destroy()

                startActivity(homeIntent)
            }
        }, 5000)
    }

    override fun onDestroy() {
        destObsHandler.stop()
        shakeDetector.stop()
        compass.stop()
        mSpeechRecognizer.stopListening()
        mSpeechRecognizer.destroy()
        Log.d(debugTAG, "MainDestinationsActivity destroyed")
        super.onDestroy()
    }

    private fun readDestinations() {
        var index = 1

        // // var doneSpeech = false

        for (i in viewList) {
            destOutputString.append(". $index. ${i.text}. ")
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

    private fun getOrientation(azimuth: Float): Orientation {
        return if (azimuth > 315 || azimuth < 45) {
            Orientation.NORTH
        } else if (azimuth > 45 || azimuth < 135) {
            Orientation.EAST
        } else if (azimuth > 135 || azimuth < 225) {
            Orientation.SOUTH
        } else {
            Orientation.WEST
        }
    }

    private fun startNavigation(destBeacon: DestinationBeacon, output: String) {
        destObsHandler.stop()
        shakeDetector.stop()
        compass.stop()
        mSpeechRecognizer.destroy()

//        val nav = Navigator(findUserPos(2), destBeacon.coordinate, Orientation.NORTH)
//        nav.executeFastestPath()

        val intent = Intent(applicationContext, MainNavigationActivity::class.java)
        intent.putExtra("destination", destBeacon)
        intent.putExtra("currentPos", Navigator().findUserPos(destList))
        intent.putExtra("currentOrientation", userOrientation)
        intent.putExtra("TTSOutput", output)
        startActivity(intent)
    }

    private fun initializeListeners() {
        var azimuthCount = 0
        var tempAzimuth: Float = 0.toFloat()
        compass = Compass(this)

        val cl = object: CompassListener{
            override fun onNewAzimuth(azimuth: Float) {
                //Do something each time azimuth changes
                azimuthCount++
                tempAzimuth += azimuth
                if (azimuthCount == 200) {
                    tempAzimuth /= azimuthCount
                    userOrientation = getOrientation(tempAzimuth)
                    azimuthCount = 0
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

        gestureDetector = GestureDetectorCompat(this, gl)

        val dtl = object: GestureDetector.OnDoubleTapListener {
            override fun onDoubleTap(p0: MotionEvent?): Boolean {
                Log.d(debugTAG, "DOUBLE TAP DETECTED")
                return true
            }

            override fun onDoubleTapEvent(p0: MotionEvent?): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onSingleTapConfirmed(p0: MotionEvent?): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
        gestureDetector.setOnDoubleTapListener(dtl)

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
        val intMap: HashMap<Int, String> = hashMapOf(1 to "one", 2 to "two", 3 to "three", 4 to "four", 5 to "five", 6 to "six", 7 to "7", 8 to "eight")

        override fun onBeginningOfSpeech() {
        }

        override fun onBufferReceived(buffer: ByteArray) {
        }

        override fun onEndOfSpeech() {
        }

        override fun onError(error: Int) {
            // var doneSpeech = false

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
            val count = destList.size
            var choice: Int = 0
            // var doneSpeech = false

            Log.d(debugTAG, "STT results: " + matches.toString())

            if (matches.isNotEmpty()) {
                for (i in 1..count) {
                    val value = intMap.getValue(i)

                    if (matches.contains(value) || matches.contains(i.toString())) {
                        choice = i
                        break
                    }
                }

                if (choice != 0) {
                    var destBeacon = DestinationBeacon("Beacon", 0.0)
                    val dest = displayList[choice-1]

                    for (i in destList) {
                        if (i.name.equals(dest, true)) {
                            destBeacon = i
                            break
                        }
                    }

                    //Start navigation to choice
                    if (destBeacon.deviceID != "null") {
                        val output = "You have chosen to go to choice $choice, $dest ..."

                        startNavigation(destBeacon, output)
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
}