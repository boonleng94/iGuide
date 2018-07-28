package boonleng94.iguide

import android.os.Bundle

import android.support.v7.app.AppCompatActivity
import java.util.*

import android.util.Log
import android.view.Gravity
import android.widget.TextView
import com.estimote.coresdk.common.requirements.SystemRequirementsChecker

import com.estimote.coresdk.service.BeaconManager
import com.estimote.coresdk.observation.region.beacon.BeaconRegion
import com.estimote.coresdk.cloud.api.CloudCallback
import com.estimote.coresdk.cloud.api.EstimoteCloud
import com.estimote.coresdk.cloud.model.BeaconInfo
import com.estimote.coresdk.common.config.EstimoteSDK
import com.estimote.coresdk.common.exception.EstimoteCloudException
import com.estimote.coresdk.observation.region.RegionUtils
import kotlinx.android.synthetic.main.activity_dest.*

class MainBeaconActivity : AppCompatActivity(){
    private lateinit var beaconManager: BeaconManager
    private lateinit var region: BeaconRegion
    private var beaconList: HashMap<String, Destination> = HashMap()
    private var beaconAcc: HashMap<String, Double> = HashMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dest)
        SystemRequirementsChecker.checkWithDefaultDialogs(this)

        EstimoteSDK.initialize(applicationContext, "iguide-msf", "f2cefefb18398ad56b51b6884e521727")

        beaconManager = BeaconManager(this)

        region = BeaconRegion("ranged region", UUID.fromString("b9407f30-f5f8-466e-aff9-25556b57fe6d"), null, null)

        //EST_LOCATION packet listener
        beaconManager.setLocationListener(BeaconManager.LocationListener{list ->
            if (!list.isEmpty()) {
                // TODO: update the UI here
                for (item in list) {
                    Log.d("ESTIMOTE LOC", item.toString())
                    Log.d("ESTIMOTE LOC", item.packetType.toString())
                    EstimoteCloud.getInstance().fetchBeaconDetails(item.id, object : CloudCallback<BeaconInfo> {
                        override fun success(beaconInfo: BeaconInfo) {
                            Log.d("ESTIMOTE LOC", beaconInfo.name + " accuracy : " + RegionUtils.computeAccuracy(item))
                        }

                        override fun failure(e: EstimoteCloudException) {
                            Log.d("ESTIMOTE LOC", "BEACON INFO ERROR: $e")
                        }
                    })
                }
            }
        })

        //EST_CONFIGURATION packet listener
        beaconManager.setConfigurableDevicesListener(BeaconManager.ConfigurableDevicesListener{list ->
            if (!list.isEmpty()) {
                Log.d("ESTIMOTE CON", list.size.toString())
                // TODO: update the UI here
                for (item in list) {
                    //Log.d("ESTIMOTE CON", item.deviceId.toString() + " accuracy : " + (Math.pow(10.0, ((item.txPower - item.rssi)/20.0)))/1000)
                    EstimoteCloud.getInstance().fetchBeaconDetails(item.deviceId, object : CloudCallback<BeaconInfo> {
                        override fun success(beaconInfo: BeaconInfo) {
                            Log.d("ESTIMOTE CON", (DestinationList.getDestinations()[beaconInfo.name] as Destination).name)

                            var acc = Math.round((Math.pow(10.0, ((item.txPower - item.rssi)/20.0)))/1000 * 1000.0) / 1000.0

                            Log.d("ESTIMOTE CON", beaconInfo.name + " accuracy : " + acc + "m")

                            beaconAcc.put(beaconInfo.name, acc)

                            // creating TextView programmatically, add to view
                            val tvDest = TextView(this@MainBeaconActivity)
                            tvDest.gravity = Gravity.CENTER_HORIZONTAL
                            tvDest.textSize = 24f
                            tvDest.text = (DestinationList.getDestinations()[beaconInfo.name] as Destination).name
                            sv_linear_layout.addView(tvDest)

                            //generateDestinations()
                        }
                        override fun failure(e: EstimoteCloudException) {
                            Log.d("ESTIMOTE CON", "BEACON INFO ERROR: $e")
                        }
                    })
                }
                beaconManager.stopConfigurableDevicesDiscovery()

            }
        })

        //EDDYSTONE packet listener
        beaconManager.setEddystoneListener(BeaconManager.EddystoneListener { list ->
            if (!list.isEmpty()) {
                // TODO: update the UI here
                for (item in list) {
                    Log.d("ESTIMOTE EDDY", item.instance + " accuracy : " + RegionUtils.computeAccuracy(item))
                }
            }
        })

        //iBeacon packet listener
        beaconManager.setRangingListener(BeaconManager.BeaconRangingListener{ _, list ->
            if (!list.isEmpty()) {
                // TODO: update the UI here
                for (item in list) {
                    Log.d("ESTIMOTE RAN", item.toString())
                    EstimoteCloud.getInstance().fetchBeaconDetails(item.proximityUUID, item.major, item.minor, object : CloudCallback<BeaconInfo> {
                        override fun success(beaconInfo: BeaconInfo) {
                            Log.d("ESTIMOTE RAN", beaconInfo.name + " accuracy : " + RegionUtils.computeAccuracy(item))
                        }

                        override fun failure(e: EstimoteCloudException) {
                            Log.d("ESTIMOTE RAN", "BEACON INFO ERROR: $e")
                        }
                    })
                }
            }
        })
    }

//    fun generateDestinations() {
//        // creating TextView programmatically, add to view
//        val tvDest = TextView(this@MainBeaconActivity)
//        tvDest.gravity = Gravity.CENTER_HORIZONTAL
//        tvDest.textSize = 24f
//        tvDest.text = (DestinationList.getDestinations()[beaconInfo.name] as Destination).name
//        sv_linear_layout.addView(tvDest)
//    }

    override fun onResume() {
        super.onResume()

        SystemRequirementsChecker.checkWithDefaultDialogs(this)

        beaconManager.connect {
            //beaconManager.startRanging(region)
            //beaconManager.startLocationDiscovery()
            beaconManager.startConfigurableDevicesDiscovery()
            //beaconManager.startEddystoneDiscovery()
        }
    }

    override fun onPause() {
        //beaconManager.stopRanging(region)
        //beaconManager.stopLocationDiscovery()
        beaconManager.stopConfigurableDevicesDiscovery()
        //beaconManager.stopEddystoneDiscovery()
        super.onPause()
    }
}