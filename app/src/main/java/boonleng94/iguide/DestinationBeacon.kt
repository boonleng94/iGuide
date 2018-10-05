package boonleng94.iguide

import java.io.Serializable

data class DestinationBeacon(val deviceID: String, var measuredPower: Int): Serializable, Cloneable{
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

    public override fun clone(): DestinationBeacon{
        try {
            super.clone()
        } catch (e: CloneNotSupportedException ) {
            System.out.println("Cloning not allowed.");
        }

        return this
    }
}