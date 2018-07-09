package boonleng94.iguide


import android.app.Notification
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast

import android.speech.tts.TextToSpeech
import java.util.Locale

import com.estimote.indoorsdk.IndoorLocationManagerBuilder
import com.estimote.indoorsdk_module.algorithm.OnPositionUpdateListener
import com.estimote.indoorsdk_module.algorithm.ScanningIndoorLocationManager
import com.estimote.indoorsdk_module.cloud.Location
import com.estimote.indoorsdk_module.cloud.LocationPosition
import com.estimote.indoorsdk_module.view.IndoorLocationView
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory

/**
 * Main view for indoor location
 */

class MainIndoorActivity : AppCompatActivity(), TTSListener {
    private lateinit var indoorLocationView: IndoorLocationView
    private lateinit var indoorLocationManager: ScanningIndoorLocationManager
    private lateinit var location: Location
    private lateinit var notification: Notification
    private lateinit var i: TTSController
    private var flag = false

    companion object {
        const val intentKeyLocationId = "Testing"
        fun createIntent(context: Context, locationId: String): Intent {
            val intent = Intent(context, MainIndoorActivity::class.java)
            intent.putExtra(intentKeyLocationId, locationId)
            return intent
        }

        //My codes
        private const val APP_CODE = 220292
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Declare notification that will be displayed in user's notification bar.
        // You can modify it as you want/
        notification = Notification.Builder(this)
                .setSmallIcon(R.drawable.iguide_logo)
                .setContentTitle("Estimote Inc. \u00AE")
                .setContentText("Indoor location is running...")
                .setPriority(Notification.PRIORITY_HIGH)
                .build()

        // Get location id from intent and get location object from list of locations
        setupLocation()

        // Init indoor location view here
        indoorLocationView = findViewById(R.id.indoor_view)

        // Give location object to your view to draw it on your screen
        indoorLocationView.setLocation(location)

        // Create IndoorManager object.
        // Long story short - it takes list of scanned beacons, does the magic and returns estimated position (x,y)
        // You need to setup it with your app context,  location data object,
        // and your cloud credentials that you declared in CloudController
        // we are using .withScannerInForegroundService(notification)
        // this will allow for scanning in background and will ensure that the system won't kill the scanning.
        // You can also use .withSimpleScanner() that will be handled without service.
        indoorLocationManager = IndoorLocationManagerBuilder(this, location, (application as CloudController).cloudCredentials)
                .withPositionUpdateInterval(500)
                .withScannerInForegroundService(notification).build()

        // Hook the listener for position update events
        indoorLocationManager.setOnPositionUpdateListener(object : OnPositionUpdateListener {
            override fun onPositionOutsideLocation() {
                indoorLocationView.hidePosition()
            }

            override fun onPositionUpdate(locationPosition: LocationPosition) {
                indoorLocationView.updatePosition(locationPosition)

                Toast.makeText(applicationContext, locationPosition.toString(), Toast.LENGTH_SHORT).show()
            }
        })

        // Check if bluetooth is enabled, location permissions are granted, etc.
        RequirementsWizardFactory.createEstimoteRequirementsWizard()
                .fulfillRequirements(this,
                        onRequirementsFulfilled = {
                            indoorLocationManager.startPositioning()
                        },
                        onRequirementsMissing = {
                            Toast.makeText(applicationContext, "Unable to scan for beacons. Requirements missing: ${it.joinToString()}", Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            Toast.makeText(applicationContext, "Unable to scan for beacons. Error: ${it.message}", Toast.LENGTH_SHORT).show()
                        })

        //My codes
        i = TTSController(this.applicationContext, Locale.US, this)
    }

    override fun onDestroy() {
        indoorLocationManager.stopPositioning()
        super.onDestroy()
    }

    private fun setupLocation() {
        // get id of location to show from intent
        val locationId = intent.extras.getString(intentKeyLocationId)
        // get object of location. If something went wrong, we build empty location with no data.
        location = (application as CloudController).locationsById[locationId] ?: buildEmptyLocation()
        // Set the Activity title to you location name
        title = location.name
    }

    private fun buildEmptyLocation(): Location {
        return Location("", "", true, "", 0.0, emptyList(), emptyList(), emptyList())
    }

//    private fun createNotificationChannel() {
//        // Create the NotificationChannel, but only on API 26+ because
//        // the NotificationChannel class is new and not in the support library
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = getString(R.string.app_name)
//            val description = getString(R.string.app_desc)
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel(APP_CODE.toString(), name, importance)
//            channel.description = description
//            // Register the channel with the system; you can't change the importance
//            // or other notification behaviors after this
//            val notificationManager = getSystemService(NotificationManager::class.java)
//            notificationManager!!.createNotificationChannel(channel)
//        }
//    }

    override fun onSuccess(tts: TextToSpeech) {
        flag = true

        //continue using TTS/calling methods and other shits
        i.speakOut("TEST")
        startActivity(Intent(this, STTTest::class.java))
    }

    override fun onFailure(tts: TextToSpeech) {
        flag = false
        finish()
    }
}
