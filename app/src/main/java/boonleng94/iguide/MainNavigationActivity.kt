package boonleng94.iguide

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.support.v4.app.NotificationCompat
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.estimote.proximity_sdk.api.ProximityObserver
import com.estimote.proximity_sdk.api.ProximityObserverBuilder
import com.estimote.proximity_sdk.api.ProximityZoneBuilder
import kotlinx.android.synthetic.main.activity_dest.*
import java.util.*
import kotlin.math.absoluteValue

class MainNavigationActivity: AppCompatActivity(){
    private lateinit var proxObsHandler: ProximityObserver.Handler
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var TTSCtrl: TTSController
    private lateinit var compass: Compass
    private lateinit var userOrientation: Orientation

    private lateinit var directionIv: ImageView
    private lateinit var destTv: TextView

    private lateinit var nextBeacon: DestinationBeacon

    private var TTSOutput = "iGuide"

    private lateinit var destination: DestinationBeacon
    private lateinit var currentPos: Coordinate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        directionIv = findViewById(R.id.iv_direction)
        destTv = findViewById(R.id.destination_placeholder)

        destination = intent.getSerializableExtra("destination") as DestinationBeacon
        currentPos = intent.getSerializableExtra("currentPos") as Coordinate

        TTSCtrl = (application as MainApp).speech
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

        var queue: Queue<DestinationBeacon> = LinkedList<DestinationBeacon>()
        destList.sortBy {
            it.distance
        }

        for (i in destList) {
            queue.add(i)

            if (i == destination) {
                break
            }
        }

        nextBeacon = queue.element()

        //Build a ProximityObserver for ranging Beacons
        val proxObserver = ProximityObserverBuilder(applicationContext, (application as MainApp).proximityCloudCredentials)
                .withLowLatencyPowerMode()
                .withScannerInForegroundService(notif)
                .onError { /* handle errors here */
                    throwable ->
                    Log.d("iGuide", "error msg: $throwable")
                }
                .build()

        //Build A ProximityZone to find Beacons with tag 'obstacle' within 1m distance
        val obstacleZone = ProximityZoneBuilder()
                .forTag("obstacle")
                .inCustomRange(1.0)
                .onEnter {
                    //only nearest beacon
                    obstacle ->
                    val instructions = obstacle.attachments["instructions"]
                    Toast.makeText(this, "Encountered obstacle, instruction: $instructions", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Encountered obstacle, instruction: $instructions")
                }
                .onExit {
                    Toast.makeText(this, "Left the obstacle", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Exit obstacle")
                }
                .onContextChange {
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
                .forTag("corridor")
                .inCustomRange(1.0)
                .onEnter {
                    //only nearest beacon, triggers once upon entry
                    beacon ->
                    val dest = beacon.attachments["destination"]
                    val desc = beacon.attachments["description"]

                    //dest reached
                    if (beacon.deviceId == destination.deviceID) {
                        startActivity(Intent(applicationContext, MainDestinationsActivity::class.java))
                        Toast.makeText(this, "Entered destination $dest, description: $desc", Toast.LENGTH_SHORT).show()
                        Log.d("iGuide", "Entered destination $dest, description: $desc")
                    } else if (beacon.deviceId == nextBeacon.deviceID){
                        //nextBeacon reached
                        val currCoord = nextBeacon.coordinate
                        Toast.makeText(this, "Entered beacon $dest, description: $desc", Toast.LENGTH_SHORT).show()
                        Log.d("iGuide", "Entered beacon $dest, description: $desc")
                        queue.remove()
                        nextBeacon = queue.element()

                        var nextOrientation = getNextOrientation(currentPos, userOrientation, nextBeacon.coordinate)

                        while (userOrientation != nextOrientation) {
                            val dir = getDirectionToTurn(userOrientation, nextOrientation)

                            TTSOutput = "Please turn to your $dir"
                            TTSCtrl.speakOut(TTSOutput)

                            //Wait for user to turn
                            Handler().postDelayed({
                            }, 2000)
                        }

                        //1m = 2 coordinate units = 3 steps
                        if (userOrientation == nextOrientation) {
                            var steps = 0
                            if (currCoord.x - nextBeacon.coordinate.x !=0) {
                                steps = ((currCoord.x - nextBeacon.coordinate.x).absoluteValue)/2*3
                            } else if (currCoord.y - nextBeacon.coordinate.y !=0 ) {
                                steps = ((currCoord.y - nextBeacon.coordinate.y).absoluteValue)/2*3
                            }

                            TTSOutput = "Please walk $steps forward"
                            TTSCtrl.speakOut(TTSOutput)
                        }
                    } else if (beacon.deviceId != nextBeacon.deviceID ) {
                        //check if skipped beacon or wrong beacon
                        var tempQ = queue

                        //skipped zone
                        while (beacon.deviceId != nextBeacon.deviceID) {
                            tempQ.remove()
                            nextBeacon = queue.element()
                        }

                        //totally wrong beacon that was not in queue
                        if (queue.isEmpty()) {
                            //error occurred, go back MainDest
                            TTSOutput = "An error has occurred, please choose your destination again"
                            TTSCtrl.speakOut(TTSOutput)
                            startActivity(Intent(applicationContext, MainDestinationsActivity::class.java))
                        }
                    }
                }
                .onExit {
                    Toast.makeText(this, "Left the beacon", Toast.LENGTH_SHORT).show()
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

//                            if (destList.isNotEmpty()) {
//                                for (i in destList) {
//                                    if (i.deviceID == destDeviceID) {
//                                        i.coordinate = Coordinate(coordinate.split(',')[0].toInt(), coordinate.split(',')[1].toInt())
//                                        i.name = destination
//
//                                        val tvDest = TextView(this)
//                                        tvDest.gravity = Gravity.CENTER_HORIZONTAL
//                                        tvDest.textSize = 24f
//                                        tvDest.isClickable = false
//                                        tvDest.text = i.name
//
//                                        if (!viewList.contains(tvDest)) {
//                                            viewList.add(tvDest)
//                                            sv_linear_layout.addView(tvDest)
//                                        }
//
//                                    }
//                                }
//                            }
                        }
                    }
                }
                .build()

        proxObsHandler = proxObserver.startObserving(obstacleZone, destinationZone)
    }

    override fun onDestroy() {
        proxObsHandler.stop()
        shakeDetector.stop()
        compass.stop()
        Log.d((application as MainApp).channelID, "MainNavigationActivity destroyed")
        super.onDestroy()
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

    private fun getDirectionToTurn(originalOrientation: Orientation, nextOrientation: Orientation): Direction? {
        when (originalOrientation) {
            Orientation.NORTH -> when (nextOrientation) {
                Orientation.SOUTH -> return Direction.BACK
                Orientation.WEST -> return Direction.LEFT
                Orientation.EAST -> return Direction.RIGHT
            }
            Orientation.SOUTH -> when (nextOrientation) {
                Orientation.NORTH -> return Direction.BACK
                Orientation.WEST -> return Direction.RIGHT
                Orientation.EAST -> return Direction.LEFT
            }
            Orientation.EAST -> when (nextOrientation) {
                Orientation.NORTH -> return Direction.LEFT
                Orientation.SOUTH -> return Direction.RIGHT
                Orientation.WEST -> return Direction.BACK
            }
            Orientation.WEST -> when (nextOrientation) {
                Orientation.NORTH -> return Direction.RIGHT
                Orientation. SOUTH -> return Direction.LEFT
                Orientation.EAST -> return Direction.BACK
            }
        }
        return null
    }

    private fun getNextOrientation(userCurrentCoordinate: Coordinate?, userCurrentOrientation: Orientation, targetCoordinate: Coordinate): Orientation {
        return if (userCurrentCoordinate!!.x - targetCoordinate.x > 0) {
            Orientation.WEST
        } else if (targetCoordinate.x - userCurrentCoordinate.x > 0) {
            Orientation.EAST
        } else {
            if (userCurrentCoordinate.y - targetCoordinate.y > 0) {
                Orientation.SOUTH
            } else if (targetCoordinate.y - userCurrentCoordinate.y > 0) {
                Orientation.NORTH
            } else {
                userCurrentOrientation
            }
        }
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
                    Log.d("iGuide", "Shake detected: $count")
                    TTSCtrl.speakOut(TTSOutput)
                }
            }
        }
        shakeDetector.shakeListener = sl
    }
}