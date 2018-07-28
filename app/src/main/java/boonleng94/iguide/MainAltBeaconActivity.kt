//package boonleng94.iguide
//
//import android.app.Notification
//import android.app.PendingIntent
//import android.content.Intent
//import android.os.Bundle
//import android.os.RemoteException
//import android.support.v4.app.FragmentActivity
//import android.support.v7.app.AppCompatActivity
//import android.util.Log
//import org.altbeacon.beacon.*
//import org.altbeacon.beacon.startup.BootstrapNotifier
//import org.altbeacon.beacon.startup.RegionBootstrap
//import android.content.Context.NOTIFICATION_SERVICE
//import android.app.NotificationManager
//import android.app.TaskStackBuilder
//import android.content.Context
//import android.support.v4.app.NotificationCompat
//
//
//class MainAltBeaconActivity : AppCompatActivity(), BeaconConsumer, BootstrapNotifier {
//    private lateinit var beaconManager: BeaconManager
//    private lateinit var regionBootstrap: RegionBootstrap
//    private var haveDetectedBeaconsSinceBoot = false
//    private var monitoringActivity: MonitoringActivity = MonitoringActivity()
//    private lateinit var region: Region
//
//    companion object {
//        protected val TAG = "MonitoringActivity"
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_nav)
//        beaconManager = BeaconManager.getInstanceForApplication(this)
//        // To detect proprietary beacons, you must add a line like below corresponding to your beacon
//        // type.  Do a web search for "setBeaconLayout" to get the proper expression.
//        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
//
//        // Uncomment the code below to use a foreground service to scan for beacons. This unlocks
//        // the ability to continually scan for long periods of time in the background on Andorid 8+
//        // in exchange for showing an icon at the top of the screen and a always-on notification to
//        // communicate to users that your app is using resources in the background.
////        var builder = Notification.Builder(this)
////        builder.setSmallIcon(R.drawable.iguide_logo);
////        builder.setContentTitle("Scanning for Beacons");
////        var intent = Intent(this, MonitoringActivity::class.java)
////        var pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
////        builder.setContentIntent(pendingIntent);
////        beaconManager.enableForegroundServiceScanning(builder.build(), 456);
//
//        // For the above foreground scanning service to be useful, you need to disable
//        // JobScheduler-based scans (used on Android 8+) and set a fast background scan
//        // cycle that would otherwise be disallowed by the operating system.
//        beaconManager.setEnableScheduledScanJobs(false)
//        beaconManager.backgroundBetweenScanPeriod =0
//        beaconManager.backgroundScanPeriod = 1100
//
//        //beaconManager.bind(this)
//
//        Log.d(TAG, "setting up background monitoring for beacons and power saving")
//        // wake up the app when a beacon is seen
//        region = Region("backgroundRegion", null, null, null)
//        regionBootstrap = RegionBootstrap(this, region)
//
//        // simply constructing this class and holding a reference to it in your custom Application
//        // class will automatically cause the BeaconLibrary to save battery whenever the application
//        // is not visible.  This reduces bluetooth power usage by about 60%
//        // backgroundPowerSaver = new BackgroundPowerSaver(this);
//
//        // If you wish to test beacon detection in the Android Emulator, you can use code like this:
//        // BeaconManager.setBeaconSimulator(new TimedBeaconSimulator() );
//        // ((TimedBeaconSimulator) BeaconManager.getBeaconSimulator()).createTimedSimulatedBeacons();
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        beaconManager.unbind(this)
//    }
//
//    override fun onBeaconServiceConnect() {
//        beaconManager.addMonitorNotifier(object : MonitorNotifier {
//            override fun didEnterRegion(region: Region) {
//                Log.i(TAG, "I just saw an beacon for the first time!")
//            }
//
//            override fun didExitRegion(region: Region) {
//                Log.i(TAG, "I no longer see an beacon")
//            }
//
//            override fun didDetermineStateForRegion(state: Int, region: Region) {
//                Log.i(TAG, "I have just switched from seeing/not seeing beacons: $state")
//            }
//        })
//
//        try {
//            beaconManager.startMonitoringBeaconsInRegion(region)
//        } catch (e: RemoteException) {
//        }
//
//    }
//
//    override fun didEnterRegion(arg0: Region) {
//        // In this example, this class sends a notification to the user whenever a Beacon
//        // matching a Region (defined above) are first seen.
//        Log.d(TAG, "did enter region.")
//        if (!haveDetectedBeaconsSinceBoot) {
//            Log.d(TAG, "auto launching MainActivity")
//
//            // The very first time since boot that we detect an beacon, we launch the MainActivity
//            var intent = Intent(this, MonitoringActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            // Important:  make sure to add android:launchMode="singleInstance" in the manifest
//            // to keep multiple copies of this activity from getting created if the user has
//            // already manually launched the app.
//            this.startActivity(intent)
//            haveDetectedBeaconsSinceBoot = true
//        } else {
//            if (monitoringActivity != null) {
//                // If the Monitoring Activity is visible, we log info about the beacons we have
//                // seen on its display
//                monitoringActivity.logToDisplay("I see a beacon again")
//            } else {
//                // If we have already seen beacons before, but the monitoring activity is not in
//                // the foreground, we send a notification to the user on subsequent detections.
//                Log.d(TAG, "Sending notification.")
//                sendNotification()
//            }
//        }
//    }
//
//    override fun didExitRegion(region: Region) {
//        if (monitoringActivity != null) {
//            monitoringActivity.logToDisplay("I no longer see a beacon.")
//        }
//    }
//
//    override fun didDetermineStateForRegion(state: Int, region: Region) {
//        if (monitoringActivity != null) {
//            monitoringActivity.logToDisplay("I have just switched from seeing/not seeing beacons: $state")
//        }
//    }
//
//    private fun sendNotification() {
//        val builder = NotificationCompat.Builder(this)
//                .setContentTitle("Beacon Reference Application")
//                .setContentText("An beacon is nearby.")
//                .setSmallIcon(R.drawable.iguide_logo)
//
//        val stackBuilder = TaskStackBuilder.create(this)
//        stackBuilder.addNextIntent(Intent(this, MonitoringActivity::class.java))
//        val resultPendingIntent = stackBuilder.getPendingIntent(
//                0,
//                PendingIntent.FLAG_UPDATE_CURRENT
//        )
//        builder.setContentIntent(resultPendingIntent)
//        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.notify(1, builder.build())
//    }
//
//    fun setMonitoringActivity(activity: MonitoringActivity) {
//        this.monitoringActivity = activity
//    }
//
//
//}