package boonleng94.iguide

import java.io.Serializable

data class DestinationBeacon(val deviceID: String, var measuredPower: Int): Serializable{
    var distance = -1.0
    var coordinate = Coordinate(-1.0, -1.0)
    var name = "Beacon Name"
    var description = "Beacon Description"

    override fun equals(o: Any?): Boolean {
        if (o is DestinationBeacon) {
            val p = o as DestinationBeacon?
            return this.deviceID == (p!!.deviceID)
        } else
            return false
    }
}