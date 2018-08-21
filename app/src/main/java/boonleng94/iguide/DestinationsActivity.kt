package boonleng94.iguide

import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.os.Bundle
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

import kotlin.math.roundToInt

class DestinationsActivity : AppCompatActivity(){
    private lateinit var destObsHandler: ProximityObserver.Handler
    private lateinit var scanHandle: ScanHandler
    private lateinit var shakeDetector: ShakeDetector
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var viewList: ArrayList<TextView>

    private var destList= ArrayList<DestinationBeacon>()

    companion object {
        const val channelID = "iGuide"
        val tagToObserve = arrayOf("corridor", "entry")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dest)

        createNotificationChannel()
        setupListeners()

        val notif = NotificationCompat.Builder(this, channelID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Fetching destinations...")
                .setSmallIcon(R.drawable.iguide_logo)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .build()

        val destProxObserver = ProximityObserverBuilder(applicationContext, (application as MainApp).proximityCloudCredentials)
                .withLowLatencyPowerMode()
                .withScannerInForegroundService(notif)
                .onError { /* handle errors here */
                    throwable ->
                    Log.d("iGuide", "error msg: $throwable")
                }
                .build()

        val corridorZone = ProximityZoneBuilder()
                .forTag(tagToObserve[0])
                .inCustomRange(20.0)
                .onEnter{
                    //only nearest beacon
                    dest ->
                    val destination = dest.attachments["destination"]
                    val description = dest.attachments["description"]
                    Toast.makeText(this, "Entered corridor $destination, description: $description", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Entered corridor $destination, description: $description")
                }
                .onExit{
                    Toast.makeText(this, "Left the corridor", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Exit tag")

                }
                .onContextChange{
                    //get all beacons
                    allDests ->
                    if (!allDests.isEmpty()) {
                        allDests.iterator().forEach {
                            dest ->
                            val destDeviceID = dest.deviceId
                            val destination = dest.attachments["destination"]
                            val coordinate = dest.attachments["coordinate"]!!
                            //val description = dest.attachments["description"]

                            for (i in destList) {
                                if (i.deviceID == destDeviceID) {
                                    i.coordinate = Coordinate(coordinate.split(',')[0].toInt(), coordinate.split(',')[1].toInt())

                                    val tvDest = TextView(this)
                                    tvDest.gravity = Gravity.CENTER_HORIZONTAL
                                    tvDest.textSize = 24f
                                    tvDest.text = destination

                                    if (!viewList.contains(tvDest)) {
                                        viewList.add(tvDest)
                                        sv_linear_layout.addView(tvDest)
                                    }
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

        scanHandle = EstimoteBluetoothScannerFactory(applicationContext)
                .getSimpleScanner()
                .estimoteLocationScan()
                .withLowLatencyPowerMode()
                .withOnPacketFoundAction {
                    packet ->
                    val beacon = DestinationBeacon (packet.deviceId, (Math.pow(10.0, ((packet.measuredPower - packet.rssi)/20.0)))/1000)

                    if (!destList.contains(beacon)) {
                        destList.add(beacon)
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

        destObsHandler = destProxObserver.startObserving(corridorZone)
    }

    override fun onDestroy() {
        destObsHandler.stop()
        scanHandle.stop()
        Log.d(channelID, "Corridor prox obs destroyed")
        super.onDestroy()
    }

    private fun findUserOrientation(previousPos: Coordinate, currentPos: Coordinate) {
        val angle = Math.atan2((currentPos.y - previousPos.y).toDouble(), (currentPos.x - previousPos.x).toDouble())
    }

    private fun findUserPos(): Coordinate {
        var posList = Array(destList.size) {_ -> doubleArrayOf(0.0, 0.0)}
        var distanceList = DoubleArray(destList.size)

        if (destList.isNotEmpty()) {
            destList.sortBy {
                it.distance
            }

            // use only 3 nearest beacon coordinates/distances
            var count = 2

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
//
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

    private fun setupListeners() {
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
                    Log.d("iGuide", "Shake detected: " + count)
                }
            }
        }
        shakeDetector.shakeListener = sl
    }
}