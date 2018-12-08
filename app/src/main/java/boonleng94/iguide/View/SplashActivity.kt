package boonleng94.iguide.View

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import boonleng94.iguide.*
import boonleng94.iguide.Controller.Navigator
import boonleng94.iguide.Controller.RSSIFilter
import boonleng94.iguide.Controller.TTSController
import boonleng94.iguide.Model.Beacon

import com.estimote.internal_plugins_api.scanning.ScanHandler
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
import com.estimote.scanning_plugin.api.EstimoteBluetoothScannerFactory

import java.util.*

//Splash Screen Activity for the application
class SplashActivity : AppCompatActivity(){
    private val debugTAG = "SplashActivity"

    private lateinit var TTSCtrl: TTSController
    private lateinit var scanHandle: ScanHandler

    private var beaconList = ArrayList<Beacon>()
    private var allRssiList = ArrayList<ArrayList<Int>>()
    private var rssiCount = 0

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
                                if (rssiCount == 500) {
                                    scanHandle.stop()

                                    //do filtering here
                                    val filter = RSSIFilter()

                                    for ((index, i) in allRssiList.withIndex()) {
                                        val filteredRssiList = filter.eliminateOutliers(i, 1.8f)
                                        val filteredRssi = filter.getMode(filteredRssiList)
                                        beaconList[index].distance = Navigator().computeDistance(filteredRssi, beaconList[index].measuredPower)
                                    }

                                    val intent = Intent(applicationContext, MainDestinationsActivity::class.java)
                                    intent.putExtra("detectedBeaconsList", beaconList)
                                    finish()
                                    startActivity(intent)
                                }

                                val beacon = Beacon(packet.deviceId)
                                beacon.measuredPower = packet.measuredPower
                                //Log.d(debugTAG, "For logging - Beacon DeviceID: " + beacon.deviceID + ", RSSI: " + packet.rssi)

                                if (!beaconList.contains(beacon)) {
                                    //new beacon detected
                                    beaconList.add(beacon)
                                    var beaconRSSIList = ArrayList<Int>()
                                    beaconRSSIList.add(packet.rssi)
                                    allRssiList.add(beaconRSSIList)
                                } else if (beaconList.contains(beacon)) {
                                    val index = beaconList.indexOf(beacon)
                                    allRssiList[index].add(packet.rssi)
                                    rssiCount++
                                }
                            }
                            .withOnScanErrorAction {
                                Log.e(debugTAG, "Bluetooth Scan failed: $it")
                            }
                            .start()

                    Handler().postDelayed({
                        if (beaconList.isEmpty()) {
                            TTSCtrl.speakOut("No beacons found! Eye Guide is currently not usable. ")
                            val homeIntent = Intent(Intent.ACTION_MAIN)
                            homeIntent.addCategory(Intent.CATEGORY_HOME)
                            homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            scanHandle.stop()
                            startActivity(homeIntent)
                        }
                    }, 30000)
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

    override fun onDestroy() {
        scanHandle.stop()
        Log.d(debugTAG, "$debugTAG destroyed")
        super.onDestroy()
    }
}