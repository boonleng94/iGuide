package boonleng94.iguide

import android.app.Application
import boonleng94.iguide.Controller.Coordinate
import boonleng94.iguide.Controller.TTSController
import boonleng94.iguide.Model.Beacon

import java.util.*

class MainApp : Application() {
    // iGuide cloud credentials
    val proximityCloudCredentials = com.estimote.proximity_sdk.api.EstimoteCloudCredentials("app id here", "app token here")

    // App shared variables
    var speech = TTSController()
    var channelID = "iGuide"
    var beaconList = ArrayList<Beacon>()
    var startPos = Coordinate(-1.0, -1.0)
    var idealQueue = ArrayList<Beacon>()
    var pathTaken = ArrayList<Coordinate>()

}