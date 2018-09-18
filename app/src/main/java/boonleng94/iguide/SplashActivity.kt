package boonleng94.iguide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window

import com.estimote.internal_plugins_api.scanning.ScanHandler
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
import com.estimote.scanning_plugin.api.EstimoteBluetoothScannerFactory

import java.util.*

class SplashActivity : AppCompatActivity(){
    private val debugTAG = "SplashActivity"

    private lateinit var TTSCtrl: TTSController
    private lateinit var scanHandle: ScanHandler

    private var beaconList = ArrayList<DestinationBeacon>()
    private var noUpdateCount = 0

    // Requesting permission to RECORD_AUDIO
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200
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
                    Log.d(debugTAG, "Requirements fulfilled, MainDestinationsActivity launched")

                    //1. Scan for Beacons with Estimote Location (Monitoring) packets, get distances, add to beaconList
                    scanHandle = EstimoteBluetoothScannerFactory(applicationContext)
                            .getSimpleScanner()
                            .estimoteLocationScan()
                            .withLowLatencyPowerMode()
                            .withOnPacketFoundAction { packet ->
                                if (noUpdateCount == 200) {
                                    if (beaconList.isNotEmpty()) {
                                        scanHandle.stop()
                                        (application as MainApp).destList = beaconList
                                        startActivity(Intent(applicationContext, MainDestinationsActivity::class.java))
                                    } else {
                                        scanHandle.stop()
                                        TTSCtrl.speakOut("No beacons found! Eye Guide is currently not usable. ")
                                        val homeIntent = Intent(Intent.ACTION_MAIN)
                                        homeIntent.addCategory(Intent.CATEGORY_HOME)
                                        homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        startActivity(homeIntent)
                                    }
                                }

                                val beacon = DestinationBeacon(packet.deviceId, Math.pow(10.0, ((packet.measuredPower - packet.rssi) / 20.0)))
                                //Math.pow(10.0, (packet.rssi - packet.measuredPower) / -20)

                                Log.d(debugTAG, "For logging - Beacon DeviceID: " + beacon.deviceID + ", Distance: " + beacon.distance)

                                if (!beaconList.contains(beacon)) {
                                    beaconList.add(beacon)
                                    Log.d(debugTAG, "Added to beaconList: " + beacon.deviceID + ", " + beacon.distance)
                                } else if (beaconList.contains(beacon)) {
                                    val index = beaconList.indexOf(beacon)

                                    //Always take lower bound reading above 1.0m
                                    //Reason is it will never be below 1.0m, as the beacons will be placed at above head level,
                                    //average distance from beacon to phone will definitely be more than 1.0m
                                    if (beaconList[index].distance > beacon.distance && beacon.distance > 1.0) {
                                        noUpdateCount = 0
                                        beaconList[index].distance = beacon.distance
                                        Log.d(debugTAG, "Updated beaconList beacon: " + beacon.deviceID + ", " + beacon.distance)
                                    } else {
                                        noUpdateCount++
                                    }
                                }

                                if (beaconList.isNotEmpty()) {
                                    beaconList.sortBy {
                                        it.distance
                                    }
                                }
                            }
                            .withOnScanErrorAction {
                                Log.e(debugTAG, "Bluetooth Scan failed: $it")
                            }
                            .start()
                },
                // onRequirementsMissing
                { requirements ->
                    Log.d(debugTAG, "Requirements not fulfilled: $requirements")
                }
                // onError
        ) { throwable ->
            Log.d(debugTAG, "Requirements error: $throwable")
        }
    }
}