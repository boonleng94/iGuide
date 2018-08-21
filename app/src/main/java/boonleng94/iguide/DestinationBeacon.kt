package boonleng94.iguide

data class DestinationBeacon(val deviceID: String, var distance: Double) {
    lateinit var coordinate: Coordinate

    override fun equals(o: Any?): Boolean {
        if (o is DestinationBeacon) {
            val p = o as DestinationBeacon?
            return this.deviceID == (p!!.deviceID)
        } else
            return false
    }
}