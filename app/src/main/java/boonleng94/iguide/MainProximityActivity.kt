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
import com.estimote.scanning_plugin.api.EstimoteBluetoothScannerFactory

class MainProximityActivity : AppCompatActivity() {
    lateinit var observationHandler: ProximityObserver.Handler
    lateinit var observationHandler2: ProximityObserver.Handler
    lateinit var proximityObserver: ProximityObserver
    lateinit var venueZone: ProximityZone
    lateinit var venueFarZone: ProximityZone

    companion object {
        val channelID = "iGuide"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)

        createNotificationChannel()

        val notif = NotificationCompat.Builder(this, channelID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Proximity Scanning")
                .setSmallIcon(R.drawable.iguide_logo)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .build()

        proximityObserver = ProximityObserverBuilder(applicationContext, (application as CloudController).proximityCloudCredentials)
                .withLowLatencyPowerMode()
                .withScannerInForegroundService(notif)
                .onError { /* handle errors here */
                    throwable ->
                    Log.d("iGuide", "error msg: $throwable")
                }
                .build()

//        var bluetoothScanner = EstimoteBluetoothScannerFactory(applicationContext).getSimpleScanner()
//        var connectivityScanHandler =
//                bluetoothScanner
//                        .estimoteConnectivityScan()
//                        .withOnPacketFoundAction {
//                            Log.d("Full Telemetry", "Got Full Telemetry packet: $it")
//                        }
//                        .withOnScanErrorAction {
//                            Log.e("Full Telemetry", "Full Telemetry scan failed: $it")
//                        }
//                        .start()

        venueZone = ProximityZoneBuilder()
                .forTag("venue")
                .inCustomRange(0.5)
                .onEnter{
                    /* do something here */
                    proximityContext ->
                    val destination = proximityContext.attachments["destination"]
                    val description = proximityContext.attachments["description"]
                    Toast.makeText(this@MainProximityActivity, "Entered $destination, description: $description", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Entered $destination, description: $description")
                }
                .onExit{
                    /* do something here */
                    Toast.makeText(this@MainProximityActivity, "Exit house", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Exit tag")

                }
                .onContextChange{
                    /* do something here */
                    proximityZoneContext ->
                    if (!proximityZoneContext.isEmpty()) {
                        val nearestBeacon = proximityZoneContext.iterator().next()
                        val nearestDest = nearestBeacon.attachments["destination"]
                        val nearestDesc = nearestBeacon.attachments["description"]
                        Toast.makeText(this@MainProximityActivity, "Nearest dest: $nearestDest, description: $nearestDesc", Toast.LENGTH_SHORT).show()
                        Log.d("iGuide", "Nearest dest: $nearestDest, description: $nearestDesc")
                    }
                }
                .build()

        venueFarZone = ProximityZoneBuilder()
                .forTag("home")
                .inCustomRange(3.0)
                .onEnter{
                    /* do something here */
                    Toast.makeText(this@MainProximityActivity, "Entered vicinity", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Entered vicinity")

                }
                .onExit{
                    /* do something here */
                    Toast.makeText(this@MainProximityActivity, "Exit vicinity", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Exit vicinity")

                }
                .onContextChange{
                    /* do something here */
                    proximityZoneContext ->
                    if (!proximityZoneContext.isEmpty()) {
                        proximityZoneContext.iterator().forEach {
                            nearestBeacon ->
                            val nearestDest = nearestBeacon.attachments["destination"]
                            val nearestDesc = nearestBeacon.attachments["description"]
                            Log.d("iGuide", "Nearest dest: $nearestDest, description: $nearestDesc")
                        }
                    }
                    Toast.makeText(this@MainProximityActivity, "Changed vicinity", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Changed vicinity")
                }
                .build()


        RequirementsWizardFactory.createEstimoteRequirementsWizard().fulfillRequirements(this,
                // onRequirementsFulfilled
                {
                    //observationHandler = proximityObserver.startObserving(venueZone)
                    //observationHandler2 = proximityObserver.startObserving(venueFarZone)
                    observationHandler = proximityObserver.startObserving(venueZone, venueFarZone)
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

    override fun onDestroy() {
        observationHandler.stop()
        //observationHandler2.stop()
        Log.d("iGuide", "KENA STOP LEH")
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val description = getString(R.string.app_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }
}