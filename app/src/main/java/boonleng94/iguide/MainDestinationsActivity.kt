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
import android.view.View
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

    private var destList = ArrayList<DestinationBeacon>()

    //private var destinationsUpdated = false

    private var userNorth = 0.toFloat()

    //private var userPos = Coordinate(0, 0)
    private var userOrienFound = false

    var destOutputString = StringBuilder("Where would you like to go? ")

    private lateinit var mSpeechRecognizer: SpeechRecognizer
    private lateinit var mSpeechRecognizerIntent: Intent
    private lateinit var mSpeechRecognitionListener: SpeechRecognitionListener

    private var listenDestChoice: Boolean = false

    companion object {
        const val channelID = "iGuide"
        val tagToObserve = arrayOf("corridor", "entry")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dest)

        createNotificationChannel()
        initialize()

        val notif = NotificationCompat.Builder(this, channelID)
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

        //Build A ProximityZone to find Beacons with tag 'corridoor' within 20m distance
        //2. Get beacons in the corridor, get the coordinate of the beacon and put into destList of beacons that match the deviceID
        val corridorZone = ProximityZoneBuilder()
                .forTag(tagToObserve[0])
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
                                            //destinationsUpdated = true
                                        }
                                    }
                                }

                                //boolean used to find out if orientation found before - if found, no need to trilat again
                                if (!userOrienFound) {
                                    //3. Once destList is not empty - beacons are around, do trilateration of user, no need for proximity ranging anymore
                                    val oldPos = findUserPos()
                                    //destObsHandler.stop()

                                    //4. Make user walk 3-5 steps forward, do trilateration again to find new coordinate
                                    TTSCtrl.speakOut("Please walk 3 to 5 steps forward ahead of you for me to find out your orientation")
                                    Thread.sleep(3000)

                                    val newPos = findUserPos()

                                    //5. Find orientation of user
                                    val orienAngle = findUserOrientation(oldPos, newPos)

                                    TTSCtrl.speakOut("I am now going to orientate you to face North")

                                    //6. Make him face north
                                    makeUserFaceNorth(orienAngle)

                                    //7. User is now facing North
                                    userOrienFound = true
                                } else if (userOrienFound) {
                                    destObsHandler.stop()
                                    readDestinations()
                                }
                            }
                        }
                    }
                }
                .build()

//        entryZone = ProximityZoneBuilder()
//                .forTag(tagToObserve[1])
//                .inCustomRange(0.5)
//                .onEnter{
//                    //only nearest beacon
//                    dest ->
//                    val destination = dest.attachments["destination"]
//                    val description = dest.attachments["description"]
//                    Toast.makeText(this, "Entered entry $destination, description: $description", Toast.LENGTH_SHORT).show()
//                    Log.d("iGuide", "Entered entry $destination, description: $description")
//                }
//                .onExit{
//                    Toast.makeText(this, "Left the entry", Toast.LENGTH_SHORT).show()
//                    Log.d("iGuide", "Exit tag")
//
//                }
//                .onContextChange{
//                    //get all beacons
//                    allDests ->
//                    if (!allDests.isEmpty()) {
//                        val dest = allDests.iterator().next()
//
//                        val destDeviceID = dest.deviceId
//
//                        for (i in destList) {
//                            if (i.deviceID == destDeviceID) {
//                                val tvDest = TextView(this)
//                                tvDest.gravity = Gravity.CENTER_HORIZONTAL
//                                tvDest.textSize = 24f
//                                tvDest.text = i.deviceID
//                                sv_linear_layout.addView(tvDest)
//                            }
//                        }
//                    }
//                }
//                .build()

        destObsHandler = destProxObserver.startObserving(corridorZone)

        readDestinations()
    }

    override fun onDestroy() {
        destObsHandler.stop()
        scanHandle.stop()
        Log.d(channelID, "Corridor prox obs destroyed")
        super.onDestroy()
    }

//    private fun readDestinations() {
//        Handler().apply {
//            val TTSRunnable = object : Runnable {
//                override fun run() {
//                    if (destinationsUpdated) {
//                        val destCount = destList.size
//                        postDelayed(this, 5000)
//                        if (destCount == destList.size) {
//                            //wait for further dest updates for 5s, if not read out destinations
//                            var index = 1
//                            for (i in viewList) {
//                                outputString.append(". $index. ${i.text}. ")
//                                index++
//                            }
//
//                            TTSCtrl.speakOut(outputString.toString())
//                            destinationsUpdated = false
//                        }
//                    }
//                }
//            }
//            postDelayed(TTSRunnable, 5000)
//        }
//    }

    private fun readDestinations() {
        var index = 1
        for (i in viewList) {
            destOutputString.append(". $index. ${i.text}. ")
            index++
        }

        TTSCtrl.speakOut(destOutputString.toString())

        listenDestChoice = true

        if (listenDestChoice)
        {
            mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
        }
    }

    private fun findUserOrientation(previousPos: Coordinate, currentPos: Coordinate): Float {
        return Math.atan2((currentPos.y - previousPos.y).toDouble(), (currentPos.x - previousPos.x).toDouble()).toFloat()
    }

    private fun makeUserFaceNorth(angle: Float) {
        var angleToTurn: Float = 0.toFloat()
        var direction = Direction.FORWARD

        var oldAzimuth: Float = 0.toFloat()
        var oldAzimuthFound = false


        //NORTH = 90degrees
        //+-10degrees
        if (angle > 79 && angle < 101) {
            //Already facing north
        } else if (angle < 90) {
            angleToTurn = 90 - angle
            direction = Direction.LEFT
        } else if (270 > angle && angle > 90) {
            angleToTurn = angle - 90
            direction = Direction.RIGHT
        } else if (angle > 270) {
            angleToTurn = 360 - angle + 90
            direction = Direction.LEFT
        }

        TTSCtrl.speakOut("Turn $direction until I say stop")

        val cl = object: CompassListener{
            override fun onNewAzimuth(azimuth: Float) {
                //Do something each time azimuth changes
                if (!oldAzimuthFound) {
                    oldAzimuth = azimuth
                    oldAzimuthFound = true
                } else {
                    var targetAzimuth = oldAzimuth
                    userNorth = targetAzimuth

                    if (direction == Direction.LEFT) {
                        targetAzimuth -= angleToTurn
                    } else if (direction == Direction.RIGHT) {
                        targetAzimuth += angleToTurn
                    }

                    //Find deliberate turn
                    if ((oldAzimuth - azimuth).absoluteValue > 11) {
                        var currentAzimuth = azimuth

                        if ((currentAzimuth - targetAzimuth).absoluteValue < 11) {
                            //User is at north already
                            TTSCtrl.speakOut("Please stop. You are now facing North")
                            compass.stop()
                        }
                    }
                }
            }
        }
        compass.compassListener = cl

        compass.start()
    }

    private fun findUserPos(): Coordinate {
        // use only 3 nearest beacon coordinates/distances, can be extended to more
        var count = 2
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

//        val positions = arrayOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(0.0, 5.0), doubleArrayOf(3.0, 2.0))
//        val distances = doubleArrayOf(0.9, 1.0, 2.0)
//        val trilaterationFunction = TrilaterationFunction(positions, distances)

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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelID, name, importance)
            channel.description = getString(R.string.app_desc)

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    private fun initialize() {
        TTSCtrl = (application as MainApp).speech

        compass = Compass(this)

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
                if (count > 2) {
                    //Repeat audio
                    Log.d("iGuide", "Shake detected: " + count)
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
                    var destBeacon: DestinationBeacon = DestinationBeacon("null", 0.0)
                    val dest = viewList[choice].text.toString()
                    TTSCtrl.speakOut("You have chosen to go to $dest")

                    for (i in destList) {
                        if (i.name == dest) {
                            destBeacon = i
                            break
                        }
                    }
                    //Start navigation to choice
                    if (destBeacon.deviceID != "null") {
                        val nav = Navigator(findUserPos(), destBeacon.coordinate, Orientation.NORTH)
                        nav.executeFastestPath()
                    }
                } else {
                    TTSCtrl.speakOut(destOutputString.toString())
                }
            }
        }

        override fun onRmsChanged(rmsdB: Float) {}
    }
}