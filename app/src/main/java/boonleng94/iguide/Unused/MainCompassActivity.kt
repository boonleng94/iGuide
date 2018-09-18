//package boonleng94.iguide.Unused
//
//import android.os.Bundle
//import android.support.v4.view.GestureDetectorCompat
//import android.support.v7.app.AppCompatActivity
//import android.util.Log
//import android.view.GestureDetector
//import android.view.MotionEvent
//import android.widget.ImageView
//import android.widget.TextView
//import boonleng94.iguide.*
//import com.lemmingapex.trilateration.LinearLeastSquaresSolver
//import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver
//import com.lemmingapex.trilateration.TrilaterationFunction
//import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
//import org.apache.commons.math3.linear.SingularMatrixException
//import kotlin.math.absoluteValue
//
//class MainCompassActivity: AppCompatActivity(), GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
//    private val TAG = "CompassActivity"
//    private lateinit var i: TTSController
//
//    private lateinit var compass: Compass
//
//    private lateinit var directionIv: ImageView
//    private lateinit var destTv: TextView
//    private lateinit var shakeDetector: ShakeDetector
//    private lateinit var gDetector: GestureDetectorCompat
//
//    private var currentAzimuth: Float = 0.toFloat()
//    private var tempAzimuth: Float = 0.toFloat()
//    private var azimuthCount: Int = 0
//    private var targetAzimuth: Float = 270.toFloat()
//    private var destination: String = "None"
//
//    private var originalAzimuth: Float = 0.toFloat()
//    private var firstSpeech: Boolean = true
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_nav)
//        directionIv = findViewById(R.id.iv_direction)
//        destTv = findViewById(R.id.destination_placeholder)
//        setupCompass()
//        setupShakeDetector()
//        setupDoubleTapGesture()
//        //i = TTSController(this.applicationContext, Locale.US, this)
//
//        val positions = arrayOf(doubleArrayOf(0.0, 0.0), doubleArrayOf(0.0, 5.0), doubleArrayOf(3.0, 2.0))
//        val distances = doubleArrayOf(0.9, 1.0, 2.0)
//        val expectedPosition = doubleArrayOf(0.2, 2.4)
//        val acceptedDelta = 0.1
//
//        val trilaterationFunction = TrilaterationFunction(positions, distances)
//        val lSolver = LinearLeastSquaresSolver(trilaterationFunction)
//        val nlSolver = NonLinearLeastSquaresSolver(trilaterationFunction, LevenbergMarquardtOptimizer())
//
//        val linearCalculatedPosition = lSolver.solve()
//        val nonLinearOptimum = nlSolver.solve()
//
//        val res1 = linearCalculatedPosition.toArray()
//        val res2 = nonLinearOptimum.point.toArray()
//
//        Log.d("iGuide", "expectedPosition: #expectedPosition")
//        Log.d("iGuide", "linear calculatedPosition: $res1")
//        Log.d("iGuide", "non-linear calculatedPosition: $res2")
//
//        Log.d("iGuide", "number of iterations: " + nonLinearOptimum.iterations)
//        Log.d("iGuide", "number of evaluations: "+ nonLinearOptimum.evaluations)
//
//        try {
//            val standardDeviation = nonLinearOptimum.getSigma(0.0)
//            Log.d("iGuide", "standard deviation: " + standardDeviation.toArray())
//            Log.d("iGuide", "norm of deviation: " + standardDeviation.norm)
//            val covarianceMatrix = nonLinearOptimum.getCovariances(0.0)
//            Log.d("iGuide", "covariane matrix: $covarianceMatrix")
//        } catch (e: SingularMatrixException) {
//            Log.d("iGuide", e.message)
//        }
//    }
//
//    override fun onStart() {
//        compass.start()
//        shakeDetector.start()
//        super.onStart()
//    }
//
//    override fun onPause() {
//        compass.stop()
//        shakeDetector.stop()
//        super.onPause()
//    }
//
//    override fun onResume() {
//        compass.start()
//        shakeDetector.start()
//        super.onResume()
//    }
//
//    override fun onStop() {
//        compass.stop()
//        shakeDetector.stop()
//        super.onStop()
//    }
//
//    override fun onDestroy() {
//        compass.stop()
//        shakeDetector.stop()
//        super.onDestroy()
//    }
//
//    private fun setupCompass() {
//        compass = Compass(this)
//        val cl = object: CompassListener {
//            override fun onNewAzimuth(azimuth: Float) {
//                //Do something each time azimuth changes
//                azimuthCount++
//                tempAzimuth += azimuth
//                if (azimuthCount == 200) {
//                    tempAzimuth /= azimuthCount
//                    checkOrientation(tempAzimuth)
//                    azimuthCount = 0
//                }
//            }
//        }
//        compass.compassListener = cl
//    }
//
//    private fun checkOrientation(azimuth: Float) {
//        currentAzimuth = azimuth
//
//        Log.d("AZI", "CURRENT AZI $currentAzimuth")
//
//        if (!firstSpeech) {
//            if ((originalAzimuth - currentAzimuth).absoluteValue > 90) {
//                firstSpeech = true
//                Log.d("ABS VALUE", "ABS VALUE: " + (originalAzimuth - currentAzimuth))
//            }
//        }
//
//        //Current azimuth to be within +/-45 of target azimuth
//        if (destination != "T") {
//            if (currentAzimuth > targetAzimuth - 45 && currentAzimuth < targetAzimuth + 45) {
//                //Move forward
//                i.speakOut("STOP and move forward")
//            }
//            else {
//                destTv.setText(R.string.astray)
//
//                if ((currentAzimuth - targetAzimuth) > 45) {
//                    //Turn left until target azimuth
//                    Log.d("LEFT", "TURN LEFT BRO")
//                    directionIv.setImageDrawable(resources.getDrawable(R.drawable.move_forward, theme))
//                    if (firstSpeech) {
//                        originalAzimuth = currentAzimuth
//                        i.speakOut("Turn left until I say stop")
//                        firstSpeech = false
//                    }
//                } else if ((currentAzimuth - targetAzimuth) < -45) {
//                    //Turn right until target azimuth
//                    Log.d("RIGHT", "TURN RIGHT BRO")
//                    directionIv.setImageDrawable(resources.getDrawable(R.drawable.move_forward, theme))
//                    if (firstSpeech) {
//                        originalAzimuth = currentAzimuth
//                        i.speakOut("Turn right until I say stop")
//                        firstSpeech = false
//                    }
//                }
//            }
//        }
//    }
//
//    private fun getOrientation(azimuth: Float): Orientation {
//        return if (azimuth > 315 || azimuth < 45) {
//            Orientation.NORTH
//        } else if (azimuth > 45 || azimuth < 135) {
//            Orientation.EAST
//        } else if (azimuth > 135 || azimuth < 225) {
//            Orientation.SOUTH
//        } else {
//            Orientation.WEST
//        }
//    }
//
//    private fun getOrientation(userPrevCoordinate: Coordinate, userCurrentOrientation: Orientation, userCurrentCoordinate: Coordinate): Orientation {
//        return if (userPrevCoordinate!!.x - userCurrentCoordinate.x > 0) {
//            Orientation.WEST
//        } else if (userCurrentCoordinate.x - userPrevCoordinate.x > 0) {
//            Orientation.EAST
//        } else {
//            if (userPrevCoordinate.y - userCurrentCoordinate.y > 0) {
//                Orientation.SOUTH
//            } else if (userCurrentCoordinate.y - userPrevCoordinate.y > 0) {
//                Orientation.NORTH
//            } else {
//                userCurrentOrientation
//            }
//        }
//    }
//
//    private fun setupShakeDetector() {
//        shakeDetector = ShakeDetector(this)
//        val sl = object: ShakeListener {
//            override fun onShake(count: Int) {
//                //Do shake event here
//                if (count > 2) {
//                    Log.d("SHAKE", "Shake detected: " + count)
//                }
//            }
//        }
//        shakeDetector.shakeListener = sl
//    }
//
//    private fun setupDoubleTapGesture() {
//        gDetector = GestureDetectorCompat(this, this)
//        gDetector.setOnDoubleTapListener(this)
//    }
//
//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        gDetector.onTouchEvent(event)
//        return super.onTouchEvent(event)
//    }
//
//    override fun onDoubleTap(event: MotionEvent): Boolean {
//        Log.d("DOUBLE TAP", "DOUBLE TAP DETECTED")
//        return true
//    }
//
//    override fun onDown(event: MotionEvent): Boolean {
//        return true
//    }
//
//    override fun onFling(event1: MotionEvent, event2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
//        return true
//    }
//
//    override fun onLongPress(event: MotionEvent) {
//    }
//
//    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
//        return true
//    }
//
//    override fun onShowPress(event: MotionEvent) {
//    }
//
//    override fun onSingleTapUp(event: MotionEvent): Boolean {
//        return true
//    }
//
//    override fun onDoubleTapEvent(event: MotionEvent): Boolean {
//        return true
//    }
//
//    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
//        return true
//    }
//}