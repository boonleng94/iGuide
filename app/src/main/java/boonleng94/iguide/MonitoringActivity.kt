//package boonleng94.iguide
//
//import android.Manifest
//import android.annotation.TargetApi
//import android.content.Context
//import android.content.pm.PackageManager
//import android.os.Build
//import android.os.Bundle
//import android.os.RemoteException
//import android.app.Activity
//import android.app.AlertDialog
//import android.content.DialogInterface
//import android.content.Intent
//import android.util.Log
//import android.view.View
//import android.widget.EditText
//
//import org.altbeacon.beacon.BeaconConsumer
//import org.altbeacon.beacon.BeaconManager
//import org.altbeacon.beacon.BeaconParser
//import org.altbeacon.beacon.MonitorNotifier
//import org.altbeacon.beacon.Region
//
///**
// *
// * @author dyoung
// * @author Matt Tyler
// */
//class MonitoringActivity : Activity() {
//
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        Log.d(TAG, "onCreate")
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_nav)
//        verifyBluetooth()
//        logToDisplay("Application just launched")
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            // Android M Permission check
//            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                val builder = AlertDialog.Builder(this)
//                builder.setTitle("This app needs location access")
//                builder.setMessage("Please grant location access so this app can detect beacons in the background.")
//                builder.setPositiveButton(android.R.string.ok, null)
//                builder.setOnDismissListener {
//                    requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
//                            PERMISSION_REQUEST_COARSE_LOCATION)
//                }
//                builder.show()
//            }
//        }
//    }
//
//    override fun onRequestPermissionsResult(requestCode: Int,
//                                            permissions: Array<String>, grantResults: IntArray) {
//        when (requestCode) {
//            PERMISSION_REQUEST_COARSE_LOCATION -> {
//                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    Log.d(TAG, "coarse location permission granted")
//                } else {
//                    val builder = AlertDialog.Builder(this)
//                    builder.setTitle("Functionality limited")
//                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.")
//                    builder.setPositiveButton(android.R.string.ok, null)
//                    builder.setOnDismissListener { }
//                    builder.show()
//                }
//                return
//            }
//        }
//    }
//
//    public override fun onResume() {
//        super.onResume()
//        //(this.applicationContext as BeaconReferenceApplication).setMonitoringActivity(this)
//    }
//
//    public override fun onPause() {
//        super.onPause()
//        //(this.applicationContext as BeaconReferenceApplication).setMonitoringActivity(null)
//    }
//
//    private fun verifyBluetooth() {
//
//        try {
//            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
//                val builder = AlertDialog.Builder(this)
//                builder.setTitle("Bluetooth not enabled")
//                builder.setMessage("Please enable bluetooth in settings and restart this application.")
//                builder.setPositiveButton(android.R.string.ok, null)
//                builder.setOnDismissListener {
//                    finish()
//                    System.exit(0)
//                }
//                builder.show()
//            }
//        } catch (e: RuntimeException) {
//            val builder = AlertDialog.Builder(this)
//            builder.setTitle("Bluetooth LE not available")
//            builder.setMessage("Sorry, this device does not support Bluetooth LE.")
//            builder.setPositiveButton(android.R.string.ok, null)
//            builder.setOnDismissListener {
//                finish()
//                System.exit(0)
//            }
//            builder.show()
//
//        }
//
//    }
//
//    fun logToDisplay(line: String) {
//        runOnUiThread {
//            //val editText = this@MonitoringActivity.findViewById(R.id.monitoringText) as EditText
//            //editText.append(line + "\n")
//            Log.d(TAG, "I see a beacon again $line" )
//
//        }
//    }
//
//    companion object {
//        protected val TAG = "MonitoringActivity"
//        private val PERMISSION_REQUEST_COARSE_LOCATION = 1
//    }
//
//}