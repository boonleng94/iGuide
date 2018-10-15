package boonleng94.iguide

import java.io.Serializable

data class Beacon(val deviceID: String): Serializable, Cloneable {
    var measuredPower = -1
    var distance = -1.0
    var coordinate = Coordinate(-1.0, -1.0)
    var name = "Beacon Name"
    var description = "Beacon Description"

    override fun equals(o: Any?): Boolean {
        if (o is Beacon) {
            val p = o as Beacon?
            return this.deviceID == (p!!.deviceID)
        } else
            return false
    }

    public override fun clone(): Beacon{
        try {
            super.clone()
        } catch (e: CloneNotSupportedException ) {
            System.out.println("Cloning not allowed.");
        }

        return this
    }
}

data class Map(val mapID: String, val beaconList: ArrayList<Beacon>)