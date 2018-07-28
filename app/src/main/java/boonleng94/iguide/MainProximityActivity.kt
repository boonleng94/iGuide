package boonleng94.iguide

import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import com.estimote.proximity_sdk.api.ProximityObserver
import com.estimote.proximity_sdk.api.ProximityObserverBuilder
import com.estimote.proximity_sdk.api.ProximityZone
import com.estimote.proximity_sdk.api.ProximityZoneBuilder


class MainProximityActivity : AppCompatActivity() {
    lateinit var observationHandler: ProximityObserver.Handler
    lateinit var observationHandler2: ProximityObserver.Handler
    lateinit var proximityObserver: ProximityObserver
    lateinit var venueZone: ProximityZone
    lateinit var venueFarZone: ProximityZone

    companion object {
        val channelID = "426"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val name = getString(R.string.app_name)
//            val description = getString(R.string.app_desc)
//            val importance = NotificationManager.IMPORTANCE_DEFAULT
//            val channel = NotificationChannel(channelID, name, importance)
//            channel.description = description
//            val notificationManager = getSystemService(NotificationManager::class.java)
//            notificationManager!!.createNotificationChannel(channel)
//        }
//
//        val notif = NotificationCompat.Builder(this, channelID)
//                .setContentTitle(getString(R.string.app_name))
//                .setContentText("Scanning for beacons")
//                .setSmallIcon(R.drawable.iguide_logo)
//                .build()

        proximityObserver = ProximityObserverBuilder(applicationContext, (application as CloudController).proximityCloudCredentials)
                .withLowLatencyPowerMode()
                //.withScannerInForegroundService(notif)
                .onError { /* handle errors here */
                    throwable ->
                    Log.d("iGuide", "error msg: $throwable")
                }
                .build()

        venueZone = ProximityZoneBuilder()
                .forTag("home")
                .inCustomRange(0.5)
                .onEnter{
                    /* do something here */
                    proximityContext ->
                    val title = proximityContext.attachments["title"]
                    val description = proximityContext.attachments["description"]
                    val imageUrl = proximityContext.attachments["image_url"]
                    Toast.makeText(this@MainProximityActivity, "Enter house", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Enter house")
                }
                .onExit{
                    /* do something here */
                    Toast.makeText(this@MainProximityActivity, "Exit house", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Exit house")

                }
                .onContextChange{
                    /* do something here */
                    Toast.makeText(this@MainProximityActivity, "Change house", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Change house")

                }
                .build()

        venueFarZone = ProximityZoneBuilder()
                .forTag("home")
                .inFarRange()
                .onEnter{
                    /* do something here */
                    Toast.makeText(this@MainProximityActivity, "Enter FAR house", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Enter FAR house")

                }
                .onExit{
                    /* do something here */
                    Toast.makeText(this@MainProximityActivity, "Exit FAR house", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Exit FAR house")

                }
                .onContextChange{
                    /* do something here */
                    Toast.makeText(this@MainProximityActivity, "Change FAR house", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Change FAR house")

                }
                .build()

//        observationHandler = proximityObserver.startObserving(venueZone)
//        observationHandler2 = proximityObserver.startObserving(venueFarZone)

        RequirementsWizardFactory.createEstimoteRequirementsWizard().fulfillRequirements(this,
                // onRequirementsFulfilled
                {
                    observationHandler = proximityObserver.startObserving(venueZone)
                    observationHandler2 = proximityObserver.startObserving(venueFarZone)
                    Log.d("iGuide", "Requirements fulfilled, observation started")
                },
                // onRequirementsMissing
                { requirements ->
                    Log.d("iGuide", "requirements missing: $requirements")
                }
                // onError
        ) { throwable ->
            Log.d("iGuide", "requirements error: $throwable")
        }
    }

    override fun onPause() {
//        observationHandler.stop()
//        observationHandler2.stop()
        Log.d("iGuide", "KENA PAUSE LEH")
        super.onPause()
    }

    override fun onDestroy() {
        observationHandler.stop()
        observationHandler2.stop()
        Log.d("iGuide", "KENA STOP LEH")
        super.onDestroy()
    }

    override fun onResume() {
//        observationHandler = proximityObserver.startObserving(venueZone)
//        observationHandler2 = proximityObserver.startObserving(venueFarZone)
        Log.d("iGuide", "RESUME?!")
        super.onResume()
    }
}