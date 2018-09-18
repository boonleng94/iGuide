//package boonleng94.iguide.Unused
//
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.os.Build
//import android.os.Bundle
//import android.support.v7.app.AppCompatActivity
//
//import android.support.v4.app.NotificationCompat
//import android.util.Log
//import boonleng94.iguide.MainApp
//import boonleng94.iguide.R
//import boonleng94.iguide.TTSController
//
//import com.estimote.indoorsdk.IndoorLocationManagerBuilder
//import com.estimote.indoorsdk_module.algorithm.OnPositionUpdateListener
//import com.estimote.indoorsdk_module.algorithm.ScanningIndoorLocationManager
//import com.estimote.indoorsdk_module.view.IndoorLocationView
//import com.estimote.indoorsdk_module.cloud.Location
//import com.estimote.indoorsdk_module.cloud.LocationPosition
//
//import com.estimote.indoorsdk_module.common.extensions.distanceTo
//
///**
// * Main view for indoor location
// */
//
//class MainIndoorActivity : AppCompatActivity() {
//    private lateinit var indoorLocationView: IndoorLocationView
//    private lateinit var indoorLocationManager: ScanningIndoorLocationManager
//    private lateinit var location: Location
//    private lateinit var notification: Notification
//    private lateinit var i: TTSController
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        indoorLocationView = findViewById(R.id.indoor_view)
//
//        //My codes
//        i = (application as MainApp).speech
//        i.speakOut("Main Indoor Activity Launched")
//        location = (application as MainApp).location
//        indoorLocationView.setLocation(location)
//
//        createNotificationChannel()
//
////        val handler = Handler()
////        val runnable = object : Runnable {
////            override fun run() {
////                handler.postDelayed(this, 100)
////            }
////        }
////
////        //Start
////        handler.postDelayed(runnable, 100)
//
//        // Create IndoorManager object.
//        // Long story short - it takes list of scanned beacons, does the magic and returns estimated position (x,y)
//        // You need to setup it with your app context,  location data object,
//        // and your cloud credentials that you declared in MainApp
//        // we are using .withScannerInForegroundService(notification)
//        // this will allow for scanning in background and will ensure that the system won't kill the scanning.
//        // You can also use .withSimpleScanner() that will be handled without service.
//        indoorLocationManager = IndoorLocationManagerBuilder(this, location, (application as MainApp).cloudCredentials)
//                .withScannerInForegroundService(notification)
//                .build()
////
////        var x = 0.0
////        var y = 0.0
////        var count = 0
//
//        // Hook the listener for position update events
//        indoorLocationManager.setOnPositionUpdateListener(object : OnPositionUpdateListener {
//            override fun onPositionOutsideLocation() {
//                indoorLocationView.hidePosition()
//            }
//
//            override fun onPositionUpdate(locationPosition: LocationPosition) {
//                Log.d("TEST", locationPosition.toString())
//                indoorLocationView.updatePosition(locationPosition)
//                val coffeeMachine = LocationPosition(3.1, 7.2, 0.0)
//                if (locationPosition.distanceTo(coffeeMachine) > 5) {
//                    // Start brewing coffee
//                }
////                x += locationPosition.x
////                y += locationPosition.y
////                count++
////                if (count == 5) {
////                    var avg = LocationPosition(x/5, y/5)
////                    Log.d("TEST", avg.toString())
////                    indoorLocationView.updatePosition(locationPosition)
////                    count = 0
////                    x= 0.0
////                    y = 0.0
////                }
//            }
//        })
//
//        indoorLocationManager.startPositioning()
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = getString(R.string.app_name)
//            val description = getString(R.string.app_desc)
//            val importance = NotificationManager.IMPORTANCE_HIGH
//            val channel = NotificationChannel("iGuide", name, importance)
//            channel.description = description
//            val notificationManager = getSystemService(NotificationManager::class.java)
//            notificationManager!!.createNotificationChannel(channel)
//        }
//
//        notification = NotificationCompat.Builder(this, "iGuide")
//                .setContentTitle("iGuide")
//                .setContentText("Foreground scanning")
//                .setSmallIcon(R.drawable.iguide_logo)
//                .build()
//    }
//}
