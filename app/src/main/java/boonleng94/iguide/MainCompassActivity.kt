package boonleng94.iguide

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView

class MainCompassActivity: AppCompatActivity(), GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private val TAG = "CompassActivity"

    private lateinit var compass: Compass
    private lateinit var arrowView: ImageView

    private lateinit var shakeDetector: ShakeDetector

    private var currentAzimuth: Float = 0.toFloat()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compass)
        arrowView = findViewById(R.id.main_image_hands)
        setupCompass()
        setupShakeDetector()
    }

    override fun onStart() {
        Log.d(TAG, "start compass")
        compass.start()
        super.onStart()
    }

    override fun onPause() {
        compass.stop()
        super.onPause()
    }

    override fun onResume() {
        compass.start()
        super.onResume()
    }

    override fun onStop() {
        compass.stop()
        super.onStop()
        Log.d(TAG, "stop compass")
    }

    override fun onDestroy() {
        Log.d(TAG, "stop compass")
        compass.stop()
        super.onDestroy()
    }

    private fun setupCompass() {
        compass = Compass(this)
        val cl = object : CompassListener {
            override fun onNewAzimuth(azimuth: Float) {
                adjustArrow(azimuth)
            }
        }
        compass.compassListener = cl
    }

    private fun adjustArrow(azimuth: Float) {
        Log.d(TAG, "will set rotation from $currentAzimuth to $azimuth")

        val an = RotateAnimation(-currentAzimuth, -azimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
        currentAzimuth = azimuth
        an.duration = 500
        an.repeatCount = 0
        an.fillAfter = true

        arrowView.startAnimation(an)
    }

    private fun setupShakeDetector() {
        shakeDetector = ShakeDetector(this)
        val sl = object : ShakeListener {
            override fun onShake(count: Int) {
                //Do shake event here
            }
        }
        shakeDetector.shakeListener = sl
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
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
    }}