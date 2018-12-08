package boonleng94.iguide.Model

import boonleng94.iguide.Controller.Coordinate
import java.io.Serializable

//Data class to store beacon objects with default variables
data class Beacon(val deviceID: String): Serializable, Cloneable {
    var measuredPower = -1
    var distance = -1.0
    var coordinate = Coordinate(-1.0, -1.0)
    var name = "Original Location"
    var description = "Beacon Description"

    override fun equals(o: Any?): Boolean {
        if (o is Beacon) {
            val p = o as Beacon?
            return this.deviceID == (p!!.deviceID)
        } else
            return false
    }

    public override fun clone(): Beacon {
        try {
            super.clone()
        } catch (e: CloneNotSupportedException ) {
            System.out.println("Cloning not allowed.");
        }

        return this
    }
}

//Data class to store the virtual map
data class Map(val mapID: String, val beaconList: ArrayList<Beacon>)