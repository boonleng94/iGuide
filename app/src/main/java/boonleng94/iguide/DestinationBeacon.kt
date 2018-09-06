package boonleng94.iguide

import java.io.Serializable

data class DestinationBeacon(val deviceID: String, var distance: Double): Serializable{
    lateinit var coordinate: Coordinate
    lateinit var name: String

    override fun equals(o: Any?): Boolean {
        if (o is DestinationBeacon) {
            val p = o as DestinationBeacon?
            return this.deviceID == (p!!.deviceID)
        } else
            return false
    }
}