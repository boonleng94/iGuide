//package boonleng94.iguide
//
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.os.Bundle
//import android.os.Handler
//import android.support.v7.app.AppCompatActivity
//import android.widget.Toast
//
//import android.speech.tts.TextToSpeech
//import android.support.v4.app.NotificationCompat
//import android.util.Log
//import java.util.Locale
//
//import com.estimote.indoorsdk.IndoorLocationManagerBuilder
//import com.estimote.indoorsdk_module.algorithm.OnPositionUpdateListener
//import com.estimote.indoorsdk_module.algorithm.ScanningIndoorLocationManager
//import com.estimote.indoorsdk_module.cloud.Location
//import com.estimote.indoorsdk_module.cloud.LocationPosition
//import com.estimote.indoorsdk_module.view.IndoorLocationView
//import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
//import kotlin.math.absoluteValue
//
///**
// * Main view for indoor location
// */
//
//class MainIndoorActivity : AppCompatActivity(), TTSListener {
//    private lateinit var indoorLocationView: IndoorLocationView
//    private lateinit var indoorLocationManager: ScanningIndoorLocationManager
//    private lateinit var location: Location
//    private lateinit var notification: Notification
//    private lateinit var i: TTSController
//    private var flag = false
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        indoorLocationView = findViewById(R.id.indoor_view)
//
//        createNotificationChannel()
//
//        // Get location id from intent and get location object from list of locations
//        setupLocation()
//
////        val handler = Handler()
////        val runnable = object : Runnable {
////            override fun run() {
////                handler.postDelayed(this, 100)
////            }
////        }
//
//        //Start
//        //handler.postDelayed(runnable, 100)
//
//        // Create IndoorManager object.
//        // Long story short - it takes list of scanned beacons, does the magic and returns estimated position (x,y)
//        // You need to setup it with your app context,  location data object,
//        // and your cloud credentials that you declared in CloudController
//        // we are using .withScannerInForegroundService(notification)
//        // this will allow for scanning in background and will ensure that the system won't kill the scanning.
//        // You can also use .withSimpleScanner() that will be handled without service.
//        indoorLocationManager = IndoorLocationManagerBuilder(this, location, (application as CloudController).cloudCredentials)
//                .withScannerInForegroundService(notification)
//                .build()
//
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
//        // Check if bluetooth is enabled, location permissions are granted, etc.
//        RequirementsWizardFactory.createEstimoteRequirementsWizard()
//                .fulfillRequirements(this,
//                        onRequirementsFulfilled = {
//                            Toast.makeText(applicationContext, "Scanning for beacons now", Toast.LENGTH_SHORT).show()
//                            indoorLocationManager.startPositioning()
//                        },
//                        onRequirementsMissing = {
//                            Toast.makeText(applicationContext, "Unable to scan for beacons. Requirements missing: ${it.joinToString()}", Toast.LENGTH_SHORT).show()
//                        },
//                        onError = {
//                            Toast.makeText(applicationContext, "Unable to scan for beacons. Error: ${it.message}", Toast.LENGTH_SHORT).show()
//                        })
//
//        //My codes
//        i = TTSController(this.applicationContext, Locale.US, this)
//    }
//
//    private fun setupLocation() {
//        // get id of location to show from intent
//        val locationId = "home-h28"
//        // get object of location. If something went wrong, we build empty location with no data.
//        // This is map for holding all locations from your account.
//        // You can move it somewhere else, but for sake of simplicity we put it in here.
//        location = (application as CloudController).locationsById[locationId] ?: buildEmptyLocation()
//        // Set the Activity title to you location name
//        title = location.name
//
//        // Give location object to your view to draw it on your screen
//        indoorLocationView.setLocation(location)
//    }
//
//    private fun buildEmptyLocation(): Location {
//        return Location("", "", true, "", 0.0, emptyList(), emptyList(), emptyList())
//    }
//
////    private fun createNotificationChannel() {
////        // Create the NotificationChannel, but only on API 26+ because
////        // the NotificationChannel class is new and not in the support library
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
////            val name = getString(R.string.app_name)
////            val description = getString(R.string.app_desc)
////            val importance = NotificationManager.IMPORTANCE_DEFAULT
////            val channel = NotificationChannel(APP_CODE.toString(), name, importance)
////            channel.description = description
////            // Register the channel with the system; you can't change the importance
////            // or other notification behaviors after this
////            val notificationManager = getSystemService(NotificationManager::class.java)
////            notificationManager!!.createNotificationChannel(channel)
////        }
////    }
//
//    override fun onSuccess(tts: TextToSpeech) {
//        flag = true
//
//        //continue using TTS/calling methods and other shits
//        i.speakOut(resources.getString(R.string.welcome))
//    }
//
//    override fun onFailure(tts: TextToSpeech) {
//        flag = false
//        finish()
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
