package boonleng94.iguide

import java.io.Serializable

data class DestinationBeacon(val deviceID: String, var distance: Double): Serializable{
    var coordinate = Coordinate(-1,-1)
    var name = "Beacon"
    var description = "Description"

    override fun equals(o: Any?): Boolean {
        if (o is DestinationBeacon) {
            val p = o as DestinationBeacon?
            return this.deviceID == (p!!.deviceID)
        } else
            return false
    }
}