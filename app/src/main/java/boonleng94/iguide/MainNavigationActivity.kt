package boonleng94.iguide

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import boonleng94.iguide.Unused.NavigatorBackup

import com.estimote.proximity_sdk.api.ProximityObserver
import com.estimote.proximity_sdk.api.ProximityObserverBuilder
import com.estimote.proximity_sdk.api.ProximityZoneBuilder
import com.estimote.scanning_plugin.api.EstimoteBluetoothScannerFactory

import java.util.*
import kotlin.collections.ArrayList

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class MainNavigationActivity: AppCompatActivity(){
    private val debugTAG = "MainNavigationActivity"

    private lateinit var proxObsHandler: ProximityObserver.Handler
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var TTSCtrl: TTSController
    private lateinit var compass: Compass
    private lateinit var currentOrientation: Orientation

    private lateinit var directionIv: ImageView
    private lateinit var destTv: TextView
    private lateinit var passbyTv: TextView

    private lateinit var nextBeacon: DestinationBeacon
    private lateinit var destination: DestinationBeacon
    private lateinit var currentPos: Coordinate

    private var inBeacon = false

    private var TTSOutput = "Eye Guide"

    private var doubleTap = false

    private val nav = Navigator()
    private lateinit var nextOrientation: Orientation
    private var handler = Handler()

    private lateinit var idealQueue: Queue<DestinationBeacon>
    private lateinit var pathTaken: Queue<DestinationBeacon>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        directionIv = findViewById(R.id.iv_direction)
        destTv = findViewById(R.id.tv_destination)
        passbyTv = findViewById(R.id.passby_alert_placeholder)

        destination = intent.getSerializableExtra("destination") as DestinationBeacon
        currentPos = intent.getSerializableExtra("currentPos") as Coordinate
        currentOrientation = intent.getSerializableExtra("currentOrientation") as Orientation
        TTSCtrl = (application as MainApp).speech

        destTv.text = destination.name

        TTSOutput = "Navigating you to " + destination.name
        TTSCtrl.speakOut(TTSOutput)

        initializeListeners()

        val notif = NotificationCompat.Builder(this, (application as MainApp).channelID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Doing navigation...")
                .setSmallIcon(R.drawable.iguide_logo)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .build()

        val destList = (application as MainApp).destList

        //testing
        Log.d(debugTAG, "currentPos: $currentPos")
        var vb = NavigatorBackup(currentPos, currentOrientation, destination.coordinate, destList)
        vb.initialize()
        val test = vb.executeFastestPath()
        Log.d(debugTAG, "test: $test")

        //before ranging, navigate to nextBeacon (very first destination)
        nextBeacon = nav.findNextNearest(currentPos, destList)

        idealQueue = nav.findShortestPath(currentPos, destination.coordinate, destList, LinkedList<DestinationBeacon>())

        val threadForBeaconNav = object: Runnable {
            override fun run() {
                nextOrientation = nav.getNextOrientation(currentPos, currentOrientation, nextBeacon.coordinate)
                Log.d(debugTAG, "currentPos: $currentPos")
                Log.d(debugTAG, "currentOrientation: $currentOrientation")
                Log.d(debugTAG, "nextBeacon Coords: " + nextBeacon.coordinate)
                Log.d(debugTAG, "nextOrientation: $nextOrientation")

                if (currentOrientation != nextOrientation) {
                    var dir = nav.getDirectionToTurn(currentOrientation, nextOrientation)
                    Log.d(debugTAG, "direction: $dir")

                    if (dir == Direction.LEFT) {
                        directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_left, null))
                        TTSOutput = "Please turn to your left..."
                        TTSCtrl.speakOut(TTSOutput)
                    } else if (dir == Direction.RIGHT) {
                        directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_right, null))
                        TTSOutput = "Please turn to your right..."
                        TTSCtrl.speakOut(TTSOutput)
                    } else if (dir == Direction.BACK) {
                        TTSOutput = "Please turn around to your back..."
                        TTSCtrl.speakOut(TTSOutput)
                        directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_around, null))
                    }

                    handler.postDelayed(this, 5000)
                } else {
                    var xSteps = 0
                    var ySteps = 0
                    var delayX: Long = 1500
                    var delayY: Long = 1500

                    if (currentPos.x - nextBeacon.coordinate.x != 0.0) {
                        xSteps = (((currentPos.x - nextBeacon.coordinate.x).absoluteValue)/2*3).roundToInt()
                        delayX *= xSteps
                    }

                    if (currentPos.y - nextBeacon.coordinate.y != 0.0) {
                        ySteps = (((currentPos.y - nextBeacon.coordinate.y).absoluteValue) / 2 * 3).roundToInt()
                        delayY *= ySteps
                    }

                    if (xSteps != 0) {
                        TTSOutput = "Please move $xSteps steps forward"
                        TTSCtrl.speakOut(TTSOutput)

                        Handler().postDelayed({
                            xSteps = 0
                            //after delay, reach x steps
                            currentOrientation = nextOrientation
                            currentPos = Coordinate(nextBeacon.coordinate.x, currentPos.y)
                            nextOrientation = nav.getNextOrientation(currentPos, currentOrientation, nextBeacon.coordinate)

                            handler.postDelayed(this, 5000)
                        }, delayX)
                    } else if (ySteps != 0) {
                        TTSOutput = "Please move $ySteps steps forward"
                        TTSCtrl.speakOut(TTSOutput)

                        Handler().postDelayed({
                            //after delay, reach y steps
                            ySteps = 0

                            if (!inBeacon) {
                                //still not in beacon
                                //reNavigate()
                            } else {
                            }
                        }, delayY)
                    }
                }
            }
        }

        handler.post(threadForBeaconNav)

        //Build a ProximityObserver for ranging Beacons
        val proxObserver = ProximityObserverBuilder(applicationContext, (application as MainApp).proximityCloudCredentials)
                .withLowLatencyPowerMode()
                .withScannerInForegroundService(notif)
                .onError { /* handle errors here */
                    throwable ->
                    Log.d(debugTAG, "error msg: $throwable")
                }
                .build()

        //Build A ProximityZone to find Beacons with tag 'corridor' within 1m distance
        //proximity zone observers, reach the first beacon at queue (.element), dequeue (.remove), give next instructions
        //think of how to do instructions
        //find orientation, walk
        //enter zone, remove, give next instruction
        //find orientation, walk repeat

        //After leaving zone, est. 10+steps enter next zone, if don't have, trilat and re orientate.
        //If skipped zone from B to D (skipped C), drop all till E, go to E.

        val destinationZone = ProximityZoneBuilder()
                .forTag("home")
                .inCustomRange(1.0)
                .onEnter {
                    //only nearest beacon, triggers once upon entry
                    beacon ->
                    val name = beacon.attachments["name"]
                    val coordinate = beacon.attachments["coordinate"]!!
                    val description = beacon.attachments["description"]

                    inBeacon = true

                    //dest reached
                    if (beacon.deviceId.equals(destination.deviceID, true)) {
                        TTSOutput = "You have reached your destination!"
                        TTSCtrl.speakOut(TTSOutput)

                        proxObsHandler.stop()
                        shakeDetector.stop()
                        compass.stop()

                        startActivity(Intent(applicationContext, MainDestinationsActivity::class.java))

                        Toast.makeText(this, "Entered destination $name, description: $description", Toast.LENGTH_SHORT).show()
                        Log.d(debugTAG, "Entered destination $name, description: $description")
                    } else {
                        currentPos = Coordinate(coordinate.split(',')[0].trim().toDouble(), coordinate.split(',')[1].trim().toDouble())

                        if (beacon.deviceId.equals(nextBeacon.deviceID, true)) {
                            //nextBeacon reached, idealQueue taken
                            val iter = destList.iterator()
                            while (iter.hasNext()) {
                                val i = iter.next()
                                if (i.coordinate == currentPos) {
                                    pathTaken.add(i)
                                    iter.remove()
                                }
                            }
                            nextBeacon = Navigator().findNextNearest(currentPos, destList)
                            handler.post(threadForBeaconNav)
                        }
                    }
                }
                .onExit {
                    beacon ->
                    val dest = beacon.attachments["name"]
                    TTSOutput = "You have just walked pass $dest"
                    TTSCtrl.speakOut(TTSOutput)
                    passbyTv.text = TTSOutput

                    inBeacon = false

                    if (doubleTap) {
                        TTSOutput = "Destination changed to $dest"
                        TTSCtrl.speakOut(TTSOutput)

                        destination = DestinationBeacon(beacon.deviceId, 1)

                        //exit is in 1m range, so turn back and walk 1m to go back
                        TTSOutput = "Please turn around and move 3 steps forward"
                        TTSCtrl.speakOut(TTSOutput)
                    }

                    Toast.makeText(this, "Left the beacon", Toast.LENGTH_SHORT).show()
                    Log.d(debugTAG, "Exit tag")
                }
                .onContextChange {
                }
                .build()

        proxObsHandler = proxObserver.startObserving(destinationZone)
    }

    override fun onDestroy() {
        proxObsHandler.stop()
        shakeDetector.stop()
        compass.stop()
        Log.d(debugTAG, "MainNavigationActivity destroyed")
        super.onDestroy()
    }

    private fun reNavigate() {
        val beaconList = ArrayList<DestinationBeacon>()

        var scanHandle = EstimoteBluetoothScannerFactory(applicationContext)
                .getSimpleScanner()
                .estimoteLocationScan()
                .withLowLatencyPowerMode()
                .withOnPacketFoundAction { packet ->
                    val beacon = DestinationBeacon(packet.deviceId, packet.measuredPower)
                    beacon.distance = nav.computeDistance(packet.rssi, packet.measuredPower)

                    if (!beaconList.contains(beacon)) {
                        beaconList.add(beacon)
                        Log.d(debugTAG, "new beaconlist beacon: " + beacon.deviceID + ", " + beacon.distance)
                    } else if (beaconList.contains(beacon)) {
                        val index = beaconList.indexOf(beacon)

                        //MUST DO SOMETHING TO GET MORE ACCURATE DISTANCES
//                                if ((beaconList[index].distance - beacon.distance).absoluteValue > 1.0) {
//                                    beaconList[index].distance = beacon.distance
//                                    Log.d(debugTAG, "new updated destlist beacon: " + beacon.deviceID + ", " + beacon.distance)
//                                }
                    }

                    if (beaconList.isNotEmpty()) {
                        beaconList.sortBy {
                            it.distance
                        }
                    }
                }
                .withOnScanErrorAction {
                    Log.e(debugTAG, "Scan failed: $it")
                }
                .start()

        Handler().postDelayed({
            if (beaconList.isNotEmpty()) {
                scanHandle.stop()
            }
        }, 5000)

        val currentPos = nav.findUserPos(beaconList)

        Log.d(debugTAG, "currentOrientation: $currentOrientation")

        val nextOrientation = nav.getNextOrientation(currentPos, currentOrientation, nextBeacon.coordinate)

        while (currentOrientation != nextOrientation) {
            val dir = nav.getDirectionToTurn(currentOrientation, nextOrientation)

            if (dir == Direction.LEFT) {
                directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_left, null))
                TTSOutput = "Please turn to your left..."
                TTSCtrl.speakOut(TTSOutput)
            } else if (dir == Direction.RIGHT) {
                directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_right, null))
                TTSOutput = "Please turn to your right..."
                TTSCtrl.speakOut(TTSOutput)
            } else if (dir == Direction.BACK) {
                TTSOutput = "You are moving astray from your destination... Please turn around or double tap to change destination..."
                TTSCtrl.speakOut(TTSOutput)
                directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_around, null))
            }

            //Wait for user to turn
            Handler().postDelayed({
            }, 2000)
        }

        //1m = 2 coordinate units = 3 steps
        if (currentOrientation == nextOrientation) {
            var xSteps = 0
            var ySteps = 0
            if (currentPos.x - nextBeacon.coordinate.x != 0.0) {
                xSteps = (((currentPos.x - nextBeacon.coordinate.x).absoluteValue)/2*3).roundToInt()
            } else if (currentPos.y - nextBeacon.coordinate.y != 0.0 ) {
                ySteps = (((currentPos.y - nextBeacon.coordinate.y).absoluteValue)/2*3).roundToInt()
            }

            if (xSteps != 0) {
                TTSOutput = "Please move $xSteps steps forward"
                TTSCtrl.speakOut(TTSOutput)
                Handler().postDelayed({
                }, 5000)
            } else if (ySteps !=0 ) {
                val nextOrientation = nav.getNextOrientation(currentPos, currentOrientation, nextBeacon.coordinate)
                val dir = nav.getDirectionToTurn(currentOrientation, nextOrientation)

                if (dir == Direction.LEFT) {
                    directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_left, null))
                    TTSOutput = "Please turn to your left..."
                    TTSCtrl.speakOut(TTSOutput)
                } else if (dir == Direction.RIGHT) {
                    directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_right, null))
                    TTSOutput = "Please turn to your right..."
                    TTSCtrl.speakOut(TTSOutput)
                } else if (dir == Direction.BACK) {
                    TTSOutput = "You are moving astray from your destination... Please turn around or double tap to change destination..."
                    TTSCtrl.speakOut(TTSOutput)
                    directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_around, null))
                }

                TTSOutput = "Please move $ySteps steps forward"
                TTSCtrl.speakOut(TTSOutput)

                Handler().postDelayed({
                }, 5000)
            }

            if (!inBeacon) {
                reNavigate()
            }
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
                    currentOrientation = compass.getOrientation(tempAzimuth)
                    azimuthCount = 0
                    Log.d(debugTAG, "Azimuth: " + tempAzimuth)
                }
            }
        }

        compass.compassListener = cl
        compass.start()

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
                doubleTap = true
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
}