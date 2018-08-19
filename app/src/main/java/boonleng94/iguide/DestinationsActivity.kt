package boonleng94.iguide

import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast

import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
import com.estimote.proximity_sdk.api.ProximityObserver
import com.estimote.proximity_sdk.api.ProximityObserverBuilder
import com.estimote.proximity_sdk.api.ProximityZone
import com.estimote.proximity_sdk.api.ProximityZoneBuilder

//proximity using scanning_plugin, legacy sdk using scanning_sdk
import com.estimote.scanning_plugin.api.EstimoteBluetoothScannerFactory

import kotlinx.android.synthetic.main.activity_dest.*

class DestinationsActivity : AppCompatActivity() {
    lateinit var destObsHandler: ProximityObserver.Handler
    lateinit var destProxObserver: ProximityObserver
    lateinit var corridorZone: ProximityZone
    lateinit var entryZone: ProximityZone

    companion object {
        const val channelID = "iGuide"
        val tagToObserve = arrayOf("corridor", "entry")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dest)

        createNotificationChannel()

        val notif = NotificationCompat.Builder(this, channelID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Fetching destinations...")
                .setSmallIcon(R.drawable.iguide_logo)
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .build()

        val bluetoothScanner = EstimoteBluetoothScannerFactory(applicationContext)
                .getSimpleScanner()

        val scanHandle = bluetoothScanner
                .estimoteLocationScan()
                .withLowLatencyPowerMode()
                .withOnPacketFoundAction {
                    // HERE YOU ARE NOTIFIED ABOUT EVERY PACKET
                    //val rssi = it.rssi
                    val estimoteDeviceId = it.deviceId
                    val dist = (Math.pow(10.0, ((it.measuredPower - it.rssi)/20.0)))/1000
                }
                .withOnScanErrorAction {
                    Log.e("iGuide", "Scan failed: $it")
                }
                .start()
        scanHandle.stop()

        destProxObserver = ProximityObserverBuilder(applicationContext, (application as MainApp).proximityCloudCredentials)
                .withLowLatencyPowerMode()
                .withScannerInForegroundService(notif)
                .onError { /* handle errors here */
                    throwable ->
                    Log.d("iGuide", "error msg: $throwable")
                }
                .build()

        corridorZone = ProximityZoneBuilder()
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
                            val destName = dest.attachments["destination"]
                            val destDesc = dest.attachments["description"]
                            Toast.makeText(this, "Nearest dest: $destName, description: $destDesc", Toast.LENGTH_SHORT).show()
                            val tvDest = TextView(this)
                            tvDest.gravity = Gravity.CENTER_HORIZONTAL
                            tvDest.textSize = 24f
                            tvDest.text = destName
                            sv_linear_layout.addView(tvDest)

                            val scanHandle = bluetoothScanner
                                    .estimoteLocationScan()
                                    .withLowLatencyPowerMode()
                                    .withOnPacketFoundAction {
                                        // HERE YOU ARE NOTIFIED ABOUT EVERY PACKET
                                        //val rssi = it.rssi
                                        val estimoteDeviceId = it.deviceId
                                        val dist = (Math.pow(10.0, ((it.measuredPower - it.rssi) / 20.0))) / 1000
                                    }
                                    .withOnScanErrorAction {
                                        Log.e("iGuide", "Scan failed: $it")
                                    }
                                    .start()


                            scanHandle.stop()
                        }
                    }
                }
                .build()

        entryZone = ProximityZoneBuilder()
                .forTag(tagToObserve[0])
                .inCustomRange(0.5)
                .onEnter{
                    //only nearest beacon
                    dest ->
                    val destination = dest.attachments["destination"]
                    val description = dest.attachments["description"]
                    Toast.makeText(this, "Entered entry $destination, description: $description", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Entered entry $destination, description: $description")
                }
                .onExit{
                    Toast.makeText(this, "Left the entry", Toast.LENGTH_SHORT).show()
                    Log.d("iGuide", "Exit tag")

                }
                .onContextChange{
                    //get all beacons
                    allDests ->
                    if (!allDests.isEmpty()) {
                        val dest = allDests.iterator().next()
                        val destName = dest.attachments["destination"]
                        val destDesc = dest.attachments["description"]
                        Toast.makeText(this, "Nearest entry dest: $destName, description: $destDesc", Toast.LENGTH_SHORT).show()
                        val tvDest = TextView(this)
                        tvDest.gravity = Gravity.CENTER_HORIZONTAL
                        tvDest.textSize = 24f
                        tvDest.text = destName
                        sv_linear_layout.addView(tvDest)
                    }
                }
                .build()

        RequirementsWizardFactory.createEstimoteRequirementsWizard().fulfillRequirements(this,
                // onRequirementsFulfilled
                {
                    //observationHandler = proximityObserver.startObserving(venueZone)
                    destObsHandler = destProxObserver.startObserving(corridorZone, entryZone)
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
        destObsHandler.stop()
        Log.d(channelID, "Corridor prox obs destroyed")
        super.onDestroy()
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
}