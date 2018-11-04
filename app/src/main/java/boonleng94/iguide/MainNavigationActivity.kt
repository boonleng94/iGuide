package boonleng94.iguide

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import com.estimote.proximity_sdk.api.ProximityObserver
import com.estimote.proximity_sdk.api.ProximityObserverBuilder
import com.estimote.proximity_sdk.api.ProximityZoneBuilder

import java.util.*
import kotlin.collections.ArrayList

import kotlin.math.absoluteValue
import kotlin.math.roundToInt

import android.graphics.Color
import android.os.CountDownTimer
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation

class MainNavigationActivity: AppCompatActivity(){
    private val debugTAG = "MainNavigationActivity"

    private lateinit var proxObsHandler: ProximityObserver.Handler
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var gestureDetector: GestureDetector
    private lateinit var alphaAnim : AlphaAnimation

    private lateinit var TTSCtrl: TTSController
    private lateinit var compass: Compass
    private lateinit var currentOrientation: Orientation

    private lateinit var directionIv: ImageView
    private lateinit var destTv: TextView
    private lateinit var destTopTv: TextView
    private lateinit var passbyTv: TextView

    private lateinit var nextBeacon: Beacon
    private lateinit var destination: Beacon
    private lateinit var currentPos: Coordinate
    private lateinit var nextOrientation: Orientation

    private var inBeacon = false
    private var doubleTap = false

    private var handler = Handler()
    val nav = Navigator()
    private var idealQueue: Queue<Beacon> = LinkedList<Beacon>()
    private var pathTaken: Queue<Coordinate> = LinkedList<Coordinate>()

    private var TTSOutput = "Eye Guide"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)

        directionIv = findViewById(R.id.iv_direction)
        destTv = findViewById(R.id.tv_destination)
        destTopTv = findViewById(R.id.destination_placeholder)
        passbyTv = findViewById(R.id.passby_alert_placeholder)

        destination = intent.getSerializableExtra("destination") as Beacon
        currentPos = intent.getSerializableExtra("currentPos") as Coordinate
        currentOrientation = intent.getSerializableExtra("currentOrientation") as Orientation
        nextBeacon = intent.getSerializableExtra("nextBeacon") as Beacon
        var temp = intent.getSerializableExtra("idealQueue") as ArrayList<Beacon>
        val destList = intent.getSerializableExtra("beaconList") as ArrayList<Beacon>

        destTv.text = destination.name

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

        for (i in temp) {
            idealQueue.add(i)
        }

        //Checking
        for (i in idealQueue) {
            Log.d(debugTAG, "Queue: " + i.name + ", " + i.coordinate)
        }


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
                        destTopTv.setText(resources.getString(R.string.enroute))
                        destTopTv.setTextColor(Color.BLACK)
                        TTSOutput = "Please turn to your left..."
                        TTSCtrl.speakOut(TTSOutput)
                    } else if (dir == Direction.RIGHT) {
                        directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_right, null))
                        destTopTv.setText(resources.getString(R.string.enroute))
                        destTopTv.setTextColor(Color.BLACK)
                        TTSOutput = "Please turn to your right..."
                        TTSCtrl.speakOut(TTSOutput)
                    } else if (dir == Direction.BACK) {
                        directionIv.setImageDrawable(resources.getDrawable(R.drawable.turn_around, null))
                        destTopTv.setText(resources.getString(R.string.astray))
                        destTopTv.setTextColor(Color.RED)
                        TTSOutput = "Please turn around to your back..."
                        TTSCtrl.speakOut(TTSOutput)
                    }

                    handler.postDelayed(this, 5000)
                } else {
                    directionIv.setImageDrawable(resources.getDrawable(R.drawable.move_forward, null))

                    var xSteps = 0
                    var ySteps = 0
                    var delayX: Long = 1500
                    var delayY: Long = 1500

                    if (currentPos.x - nextBeacon.coordinate.x != 0.0) {
                        xSteps = (((currentPos.x - nextBeacon.coordinate.x).absoluteValue)*3).roundToInt()
                        delayX *= xSteps
                    }

                    if (currentPos.y - nextBeacon.coordinate.y != 0.0) {
                        ySteps = (((currentPos.y - nextBeacon.coordinate.y).absoluteValue)*3).roundToInt()
                        delayY *= ySteps
                    }

                    if (xSteps != 0) {
                        TTSOutput = "Please move $xSteps steps forward ..."
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
                        TTSOutput = "Please move $ySteps steps forward ..."
                        TTSCtrl.speakOut(TTSOutput)

                        Handler().postDelayed({
                            //after delay, reach y steps
                            ySteps = 0

                            if (!inBeacon) {
                                //still not in beacon
                                //reNavigate()
                            } else {
                                handler.removeCallbacksAndMessages(null)
                            }
                        }, delayY)
                    }
                }
            }
        }

        if (!inBeacon) {
            handler.post(threadForBeaconNav)
        }

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
        val destinationZone = ProximityZoneBuilder()
                .forTag("corridor")
                .inCustomRange(0.9)
                .onEnter {
                    //only nearest beacon, triggers once upon entry
                    beacon ->
                    val name = beacon.attachments["name"]
                    val coordinate = beacon.attachments["coordinate"]!!
                    val description = beacon.attachments["description"]

                    handler.removeCallbacksAndMessages(null)
                    inBeacon = true
                    doubleTap = false

                    pathTaken.add(currentPos)

                    //dest reached
                    if (beacon.deviceId.equals(destination.deviceID, true)) {
                        TTSOutput = "You have reached your destination!... "
                        TTSCtrl.speakOut(TTSOutput)

                        (application as MainApp).pathTaken = ArrayList(pathTaken)

                        //Go to destination reached activity
                        val intent = Intent(applicationContext, MainDestinationReachedActivity::class.java)
                        intent.putExtra("currentPos", destination)
                        finish()
                        startActivity(intent)

                        Toast.makeText(this, "Entered destination $name, description: $description", Toast.LENGTH_SHORT).show()
                    } else {
//                        TTSOutput = "Please stop. You are now at $name... $description... "
//                        TTSCtrl.speakOut(TTSOutput)
                        Toast.makeText(this, "Entered beacon $name, description: $description", Toast.LENGTH_SHORT).show()
                        Log.d(debugTAG, "Entered beacon $name, description: $description")

                        currentPos = Coordinate(coordinate.split(',')[0].trim().toDouble(), coordinate.split(',')[1].trim().toDouble())

                        //nextBeacon reached, idealQueue taken
                        if (beacon.deviceId.equals(nextBeacon.deviceID, true)) {
                            idealQueue.remove()
                            nextBeacon = idealQueue.element()
                        } else {
                            //beacon does not match nextBeacon, idealQueue not taken
                            //propose another fastest path from current beacon
                            nav.initialize(currentPos, currentOrientation, destination.coordinate, destList)
                            idealQueue = nav.executeFastestPath()
                            nextBeacon = idealQueue.element()
                            //Checking
                            for (i in idealQueue) {
                                Log.d(debugTAG, "Queue: " + i.name + ", " + i.coordinate)
                            }
                        }

                        handler.post(threadForBeaconNav)
                    }
                }
                .onExit {
                    beacon ->
                    val dest = beacon.attachments["name"]
                    TTSCtrl.speakOut("You have just walked pass $dest ...")
                    passbyTv.text = "You have just walked pass $dest"
                    passbyTv.startAnimation(alphaAnim)

                    inBeacon = false

                    if (doubleTap) {
                        TTSCtrl.speakOut("Destination changed to $dest")

                        destination = Beacon(beacon.deviceId)
                        destTv.text = destination.name

                        //exit is in 1m range, so turn back and walk 1m to go back
                        TTSOutput = "Please turn around and move 3 steps forward ..."
                        TTSCtrl.speakOut(TTSOutput)
                    }

                    Toast.makeText(this, "Left beacon $dest", Toast.LENGTH_SHORT).show()
                    Log.d(debugTAG, "Exit tag")
                }
                .onContextChange {
                }
                .build()

        proxObsHandler = proxObserver.startObserving(destinationZone)

//        val cdt = object : CountDownTimer(30 * 1000, 1000) {
//            override fun onTick(millisUntilFinished: Long) {
//                Log.i(debugTAG, "Seconds remaining: " + millisUntilFinished / 1000);
//            }
//            override fun onFinish() {
//                val TTSUPL = object: UtteranceProgressListener() {
//                    override fun onDone(utteranceId: String?) {
//                        if (inBeacon) {
//                            handler.post(threadForBeaconNav)
//                        }
//                    }
//                    override fun onError(utteranceId: String?) {
//                    }
//                    override fun onStart(utteranceId: String?) {
//                    }
//                }
//                TTSCtrl.talk.setOnUtteranceProgressListener(TTSUPL)
//
//                handler.removeCallbacksAndMessages(null)
//                if (inBeacon) {
//                    TTSCtrl.speakOut("You have stopped moving for 30 seconds . . . Please double tap if you wish to change destinations . . .Otherwise, you are currently enroute to your destination!")
//                }
//            }
//        }
//
//        broadcastReceiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                if (intent.action == "iGuide_service") {
//                    val type = intent.getIntExtra("type", -1)
//                    //val confidence = intent.getIntExtra("confidence", 0)
//
//                    Log.d(debugTAG, "SERVICE TYPE: $type")
//
//                    if (type == DetectedActivity.STILL) {
//                        cdt.start()
//                        Log.d(debugTAG, "Still")
//                    }
//                    else {
//                        cdt.cancel()
//                        Log.d(debugTAG, "NOT STILL")
//                    }
//                }
//            }
//        }
//
//        startTracking()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onDestroy() {
        proxObsHandler.stop()
        shakeDetector.stop()
        compass.stop()
        Log.d(debugTAG, "$debugTAG destroyed")
        super.onDestroy()
    }

    private fun initializeListeners() {
        var azimuthCount = 0
        var tempAzimuth: Float = 0.toFloat()
        compass = Compass(this)

        val cl = object : CompassListener {
            override fun onAzimuthChange(azimuth: Float) {
                //Do something each time azimuth changes
                azimuthCount++
                tempAzimuth += azimuth
                if (azimuthCount == 200) {
                    tempAzimuth /= azimuthCount
                    currentOrientation = compass.getOrientation(tempAzimuth)
                    azimuthCount = 0
                    //Log.d(debugTAG, "Azimuth: " + tempAzimuth)
                }
            }
        }

        compass.compassListener = cl
        compass.start()

        val sgl = object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(p0: MotionEvent?): Boolean {
                doubleTap = true
                Log.d(debugTAG, "DOUBLE TAP DETECTED")
                return true
            }
        }

        gestureDetector = GestureDetector(this, sgl)

        shakeDetector = ShakeDetector(this)
        val sl = object : ShakeListener {
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

        alphaAnim = AlphaAnimation(1.0f, 0.0f)
        alphaAnim.duration = 5000

        val al = object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                passbyTv.visibility = View.VISIBLE
            }

            override fun onAnimationRepeat(p0: Animation?) {
            }

            override fun onAnimationEnd(p0: Animation?) {
                passbyTv.visibility = View.GONE
            }
        }

        alphaAnim.setAnimationListener(al)
    }

//    override fun onResume() {
//        super.onResume()
//        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, IntentFilter("iGuide_service"))
//    }
//
//    override fun onPause() {
//        super.onPause()
//
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
//    }
//
//    private fun startTracking() {
//        Log.d(debugTAG, "Tracking started")
//        val intent1 = Intent(this, BGMvmtDetectionService::class.java)
//        startService(intent1)
//    }
//
//    private fun stopTracking() {
//        val intent = Intent(this, BGMvmtDetectionService::class.java)
//        stopService(intent)
//    }
}