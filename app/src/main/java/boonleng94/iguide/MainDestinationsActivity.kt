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

//proximity using scanning_plugin, legacy sdk using scanning_sdk
import com.estimote.internal_plugins_api.scanning.ScanHandler
import com.estimote.scanning_plugin.api.EstimoteBluetoothScannerFactory

import com.lemmingapex.trilateration.LinearLeastSquaresSolver
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
import com.lemmingapex.trilateration.TrilaterationFunction

import kotlinx.android.synthetic.main.activity_dest.*
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import org.apache.commons.math3.linear.SingularMatrixException
import kotlin.math.absoluteValue

import kotlin.math.roundToInt

class MainDestinationsActivity : AppCompatActivity() {
    private lateinit var destObsHandler: ProximityObserver.Handler
    private lateinit var scanHandle: ScanHandler
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var viewList: ArrayList<TextView>
    private lateinit var TTSCtrl: TTSController
    private lateinit var compass: Compass
    private lateinit var userOrientation: Orientation
    private lateinit var mSpeechRecognizer: SpeechRecognizer
    private lateinit var mSpeechRecognizerIntent: Intent
    private lateinit var mSpeechRecognitionListener: SpeechRecognitionListener

    private var destList = ArrayList<DestinationBeacon>()

    private var destOutputString = StringBuilder("Where would you like to go? ")

    private var TTSOutput = "iGuide"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dest)

        createNotificationChannel()

        TTSCtrl = (application as MainApp).speech
        TTSOutput = "Please wait as I find destinations"
        TTSCtrl.speakOut(TTSOutput)

        initializeListeners()

        val notif = NotificationCompat.Builder(this, (application as MainApp).channelID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Fetching destinations...")
                .setSmallIcon(R.drawable.iguide_logo)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .build()

        //1. Scan for Beacons with Estimote Location (Monitoring) packets, get distances, add to destList
        scanHandle = EstimoteBluetoothScannerFactory(applicationContext)
                .getSimpleScanner()
                .estimoteLocationScan()
                .withLowLatencyPowerMode()
                .withOnPacketFoundAction { packet ->
                    val beacon = DestinationBeacon(packet.deviceId, Math.pow(10.0, ((packet.measuredPower - packet.rssi) / 20.0)))

                    //Math.pow(10.0, (packet.rssi - packet.measuredPower) / -20)

                    if (!destList.contains(beacon)) {
                        destList.add(beacon)
                    } else if (destList.contains(beacon)) {
                        val index = destList.indexOf(beacon)

                        if ((destList[index].distance - beacon.distance).absoluteValue > 1.0) {
                            destList[index].distance = beacon.distance
                        }
                    }

                    if (destList.isNotEmpty()) {
                        destList.sortBy {
                            it.distance
                        }
                    }
                }
                .withOnScanErrorAction {
                    Log.e("iGuide", "Scan failed: $it")
                }
                .start()

        //Build a ProximityObserver for ranging Beacons
        val destProxObserver = ProximityObserverBuilder(applicationContext, (application as MainApp).proximityCloudCredentials)
                .withLowLatencyPowerMode()
                .withScannerInForegroundService(notif)
                .onError { /* handle errors here */
                    throwable ->
                    Log.d("iGuide", "error msg: $throwable")
                }
                .build()

        //Build A ProximityZone to find Beacons with tag 'corridor' within 20m distance
        //2. Get beacons in the corridor, get the coordinate of the beacon and put into destList of beacons that match the deviceID
        val corridorZone = ProximityZoneBuilder()
                .forTag("corridor")
                .inCustomRange(20.0)
                .onEnter {
                    //only nearest beacon
                    dest ->
                    val destination = dest.attachments["destination"]
                    val description = dest.attachments["description"]
                    Toast.makeText(this, "Entered corridor $destination, description: $description", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Entered corridor $destination, description: $description")
                }
                .onExit {
                    Toast.makeText(this, "Left the corridor", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Exit tag")

                }
                .onContextChange {
                    //get all beacons
                    allDests ->
                    if (!allDests.isEmpty()) {
                        allDests.iterator().forEach { dest ->
                            val destDeviceID = dest.deviceId
                            val destination = dest.attachments["destination"]!!
                            val coordinate = dest.attachments["coordinate"]!!
                            //val description = dest.attachments["description"]

                            if (destList.isNotEmpty()) {
                                for (i in destList) {
                                    if (i.deviceID == destDeviceID) {
                                        i.coordinate = Coordinate(coordinate.split(',')[0].toInt(), coordinate.split(',')[1].toInt())
                                        i.name = destination

                                        val tvDest = TextView(this)
                                        tvDest.gravity = Gravity.CENTER_HORIZONTAL
                                        tvDest.textSize = 24f
                                        tvDest.isClickable = false
                                        tvDest.text = i.name

                                        if (!viewList.contains(tvDest)) {
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

        //After scanning for beacons for 10s
        Handler().postDelayed({
            scanHandle.stop()
            destObsHandler.stop()
            readDestinations()
        }, 10000)
    }

    override fun onDestroy() {
        scanHandle.stop()
        destObsHandler.stop()
        shakeDetector.stop()
        Log.d((application as MainApp).channelID, "MainDestinationsActivity destroyed")
        super.onDestroy()
    }

    private fun readDestinations() {
        var index = 1
        for (i in viewList) {
            destOutputString.append(". $index. ${i.text}. ")
            index++
        }

        TTSOutput = destOutputString.toString()
        TTSCtrl.speakOut(TTSOutput)

        mSpeechRecognizer.startListening(mSpeechRecognizerIntent)
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

    private fun findUserPos(count: Int): Coordinate {
        // use only 3 nearest beacon coordinates/distances, can be extended to more
        var count = count
        var posList = Array(count) {_ -> doubleArrayOf(0.0, 0.0)}
        var distanceList = DoubleArray(count)

        if (destList.isNotEmpty()) {
            destList.sortBy {
                it.distance
            }

            for (i in destList) {
                var pos = doubleArrayOf(i.coordinate.x.toDouble(), i.coordinate.y.toDouble())

                distanceList[count] = i.distance
                posList[count] = pos

                count--
            }
        }

        val trilaterationFunction = TrilaterationFunction(posList, distanceList)
        val lSolver = LinearLeastSquaresSolver(trilaterationFunction)
        val nlSolver = NonLinearLeastSquaresSolver(trilaterationFunction, LevenbergMarquardtOptimizer())

        val linearCalculatedPosition = lSolver.solve()
        val nonLinearOptimum = nlSolver.solve()

        val res1 = linearCalculatedPosition.toArray()
        val res2 = nonLinearOptimum.point.toArray()

        Log.d("iGuide", "linear calculatedPosition: $res1")
        Log.d("iGuide", "non-linear calculatedPosition: $res2")

        Log.d("iGuide", "number of iterations: " + nonLinearOptimum.iterations)
        Log.d("iGuide", "number of evaluations: "+ nonLinearOptimum.evaluations)

        try {
            val standardDeviation = nonLinearOptimum.getSigma(0.0)
            Log.d("iGuide", "standard deviation: " + standardDeviation.toArray())
            Log.d("iGuide", "norm of deviation: " + standardDeviation.norm)
            val covarianceMatrix = nonLinearOptimum.getCovariances(0.0)
            Log.d("iGuide", "covariane matrix: $covarianceMatrix")
        } catch (e: SingularMatrixException) {
            Log.d("iGuide", e.message)
        }

        return Coordinate(res1[0].roundToInt(), res1[1].roundToInt())
    }

    private fun startNavigation(destBeacon: DestinationBeacon) {
        (application as MainApp).destList = destList

        TTSOutput = "Navigating you to " + destBeacon.name
        TTSCtrl.speakOut(TTSOutput)

        val nav = Navigator(findUserPos(2), destBeacon.coordinate, Orientation.NORTH)
        nav.executeFastestPath()
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
                Log.d("iGuide", "DOUBLE TAP DETECTED")
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
                    Log.d("iGuide", "Shake detected: " + count)
                    TTSCtrl.speakOut(TTSOutput)
                }
            }
        }
        shakeDetector.shakeListener = sl
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
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent)
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

            if (matches.isNotEmpty()) {
                for (i in 1..count) {
                    val value = intMap.getValue(i)

                    if (matches.contains(value)) {
                        choice = i
                        break
                    }
                }

                if (choice != 0) {
                    var destBeacon = DestinationBeacon("null", 0.0)
                    val dest = viewList[choice].text.toString()
                    TTSOutput = "You have chosen to go to $dest"
                    TTSCtrl.speakOut(TTSOutput)

                    for (i in destList) {
                        if (i.name == dest) {
                            destBeacon = i
                            break
                        }
                    }

                    //Start navigation to choice
                    if (destBeacon.deviceID != "null") {
                        startNavigation(destBeacon)
                    }
                } else {
                    TTSOutput = destOutputString.toString()
                    TTSCtrl.speakOut(TTSOutput)
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