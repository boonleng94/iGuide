package boonleng94.iguide

import android.os.Bundle
import android.support.v4.view.GestureDetectorCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import kotlin.math.absoluteValue

class MainActivity: AppCompatActivity(), GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private lateinit var i: TTSController

    private lateinit var compass: Compass

    private lateinit var directionIv: ImageView
    private lateinit var destTv: TextView
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var gDetector: GestureDetectorCompat

    private var currentAzimuth: Float = 0.toFloat()
    private var tempAzimuth: Float = 0.toFloat()
    private var azimuthCount: Int = 0
    private var targetAzimuth: Float = 270.toFloat()
    private var destination: String = "None"

    private var originalAzimuth: Float = 0.toFloat()
    private var firstSpeech: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
        directionIv = findViewById(R.id.iv_direction)
        destTv = findViewById(R.id.destination_placeholder)
        setupCompass()
        setupShakeDetector()
        setupDoubleTapGesture()
        //i = TTSController(this.applicationContext, Locale.US, this)
    }

    override fun onStart() {
        compass.start()
        shakeDetector.start()
        super.onStart()
    }

    override fun onPause() {
        compass.stop()
        shakeDetector.stop()
        super.onPause()
    }

    override fun onResume() {
        compass.start()
        shakeDetector.start()
        super.onResume()
    }

    override fun onStop() {
        compass.stop()
        shakeDetector.stop()
        super.onStop()
    }

    override fun onDestroy() {
        compass.stop()
        shakeDetector.stop()
        super.onDestroy()
    }

    private fun setupCompass() {
        compass = Compass(this)
        val cl = object: CompassListener{
            override fun onNewAzimuth(azimuth: Float) {
                //Do something each time azimuth changes
                azimuthCount++
                tempAzimuth += azimuth
                if (azimuthCount == 200) {
                    tempAzimuth /= azimuthCount
                    checkOrientation(tempAzimuth)
                    azimuthCount = 0
                }
            }
        }
        compass.compassListener = cl
    }

    private fun checkOrientation(azimuth: Float) {
        currentAzimuth = azimuth

        Log.d("AZI", "CURRENT AZI $currentAzimuth")

        if (!firstSpeech) {
            if ((originalAzimuth - currentAzimuth).absoluteValue > 90) {
                firstSpeech = true
                Log.d("ABS VALUE", "ABS VALUE: " + (originalAzimuth - currentAzimuth))
            }
        }

        //Current azimuth to be within +/-45 of target azimuth
        if (destination != "T") {
            if (currentAzimuth > targetAzimuth - 45 && currentAzimuth < targetAzimuth + 45) {
                //Move forward
                i.speakOut("STOP and move forward")
            }
            else {
                destTv.setText(R.string.astray)

                if ((currentAzimuth - targetAzimuth) > 45) {
                    //Turn left until target azimuth
                    Log.d("LEFT", "TURN LEFT BRO")
                    directionIv.setImageDrawable(resources.getDrawable(R.drawable.move_forward, theme))
                    if (firstSpeech) {
                        originalAzimuth = currentAzimuth
                        i.speakOut("Turn left until I say stop")
                        firstSpeech = false
                    }
                } else if ((currentAzimuth - targetAzimuth) < -45) {
                    //Turn right until target azimuth
                    Log.d("RIGHT", "TURN RIGHT BRO")
                    directionIv.setImageDrawable(resources.getDrawable(R.drawable.move_forward, theme))
                    if (firstSpeech) {
                        originalAzimuth = currentAzimuth
                        i.speakOut("Turn right until I say stop")
                        firstSpeech = false
                    }
                }
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

    private fun getOrientation(userPrevCoordinate: Coordinate, userCurrentOrientation: Orientation, userCurrentCoordinate: Coordinate): Orientation {
        return if (userPrevCoordinate!!.x - userCurrentCoordinate.x > 0) {
            Orientation.WEST
        } else if (userCurrentCoordinate.x - userPrevCoordinate.x > 0) {
            Orientation.EAST
        } else {
            if (userPrevCoordinate.y - userCurrentCoordinate.y > 0) {
                Orientation.SOUTH
            } else if (userCurrentCoordinate.y - userPrevCoordinate.y > 0) {
                Orientation.NORTH
            } else {
                userCurrentOrientation
            }
        }
    }

    private fun setupShakeDetector() {
        shakeDetector = ShakeDetector(this)
        val sl = object: ShakeListener {
            override fun onShake(count: Int) {
                //Do shake event here
                if (count > 2) {
                    Log.d("SHAKE", "Shake detected: " + count)
                }
            }
        }
        shakeDetector.shakeListener = sl
    }

    private fun setupDoubleTapGesture() {
        gDetector = GestureDetectorCompat(this, this)
        gDetector.setOnDoubleTapListener(this)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
        Log.d("DOUBLE TAP", "DOUBLE TAP DETECTED")
        return true
    }

    override fun onDown(event: MotionEvent): Boolean {
        return true
    }

    override fun onFling(event1: MotionEvent, event2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return true
    }

    override fun onLongPress(event: MotionEvent) {
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return true
    }

    override fun onShowPress(event: MotionEvent) {
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        return true
    }

    override fun onDoubleTapEvent(event: MotionEvent): Boolean {
        return true
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        return true
    }
}