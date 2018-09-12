package boonleng94.iguide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import com.estimote.internal_plugins_api.scanning.ScanHandler

import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
import com.estimote.scanning_plugin.api.EstimoteBluetoothScannerFactory

import java.util.*
import kotlin.math.absoluteValue

/**
 * Simple splash screen to load the data from cloud.
 * Make sure to initialize EstimoteSDK with your APP ID and APP TOKEN in {@link MainApp} class.
 * You can get those credentials from your Estimote Cloud account :)
 */
class SplashActivity : AppCompatActivity(){
    private lateinit var TTSCtrl: TTSController
    private lateinit var scanHandle: ScanHandler

    private var destList = ArrayList<DestinationBeacon>()

    private val debugTAG = "SplashActivity"

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onRequestPermissionsResult (requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make actionbar invisible.
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        TTSCtrl = TTSController()
        TTSCtrl.initialize(applicationContext, Locale.US)
        (application as MainApp).speech = TTSCtrl

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        RequirementsWizardFactory.createEstimoteRequirementsWizard().fulfillRequirements(this,
                // onRequirementsFulfilled
                {
                    //1. Scan for Beacons with Estimote Location (Monitoring) packets, get distances, add to destList
                    scanHandle = EstimoteBluetoothScannerFactory(applicationContext)
                            .getSimpleScanner()
                            .estimoteLocationScan()
                            .withLowLatencyPowerMode()
                            .withOnPacketFoundAction { packet ->
                                val beacon = DestinationBeacon(packet.deviceId, Math.pow(10.0, ((packet.measuredPower - packet.rssi) / 20.0)))
                                //Math.pow(10.0, (packet.rssi - packet.measuredPower) / -20)

                                if (!destList.contains(beacon)) {
                                    destList.add(beacon)
                                    Log.d(debugTAG, "new destlist beacon: " + beacon.deviceID + ", " + beacon.distance)
                                } else if (destList.contains(beacon)) {
                                    val index = destList.indexOf(beacon)

                                    //MUST DO SOMETHING TO GET MORE ACCURATE DISTANCES
                                    if ((destList[index].distance - beacon.distance).absoluteValue > 1.0) {
                                        destList[index].distance = beacon.distance
                                        Log.d(debugTAG, "new updated destlist beacon: " + beacon.deviceID + ", " + beacon.distance)
                                    }
                                }

                                if (destList.isNotEmpty()) {
                                    destList.sortBy {
                                        it.distance
                                    }
                                }
                            }
                            .withOnScanErrorAction {
                                Log.e(debugTAG, "Scan failed: $it")
                            }
                            .start()

                    Handler().postDelayed({
                        if (destList.isNotEmpty()) {
                            scanHandle.stop()
                            (application as MainApp).destList = destList
                            startActivity(Intent(applicationContext, MainDestinationsActivity::class.java))
                        } else {
                            TTSCtrl.speakOut("No beacons found! Eye Guide is currently not usable. ")
                            val homeIntent = Intent(Intent.ACTION_MAIN)
                            homeIntent.addCategory(Intent.CATEGORY_HOME)
                            homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(homeIntent)
                        }
                    }, 5000)

                    Log.d("iGuide", "Requirements fulfilled, MainDestinationsActivity launched")
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
}