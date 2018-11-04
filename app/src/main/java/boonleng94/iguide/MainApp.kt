package boonleng94.iguide

import android.app.Application

import java.util.*

class MainApp : Application() {
    val proximityCloudCredentials = com.estimote.proximity_sdk.api.EstimoteCloudCredentials("iguide-msf", "f2cefefb18398ad56b51b6884e521727")

    // Shared variables
    var speech = TTSController()
    var channelID = "iGuide"
    var beaconList = ArrayList<Beacon>()
    var startPos = Coordinate(-1.0,-1.0)
    var idealQueue = ArrayList<Beacon>()
    var pathTaken = ArrayList<Coordinate>()

}