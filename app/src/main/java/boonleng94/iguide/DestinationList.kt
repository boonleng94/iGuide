package boonleng94.iguide

import java.util.*

object DestinationList {
    fun getDestinations() : HashMap<String, Destination> {
        var destList: HashMap<String, Destination> = HashMap()

        var dest1 = Destination("Destination LT1", "Destination LT1 Description")
        var dest2 = Destination("Destination LT2", "Destination LT2 Description")
        var dest3 = Destination("Destination LT3", "Destination LT3 Description")
        var dest4 = Destination("Destination LT4", "Destination LT4 Description")
        var dest5 = Destination("Destination BR1", "Destination BR1 Description")
        var dest6 = Destination("Destination BR2", "Destination BR2 Description")
        var dest7 = Destination("Destination BR3", "Destination BR3 Description")
        var dest8 = Destination("Destination BR4", "Destination BR4 Description")
        var dest9 = Destination("Destination CF1", "Destination CF1 Description")
        var dest10 = Destination("Destination CF2", "Destination CF2 Description")
        var dest11 = Destination("Destination CF3", "Destination CF3 Description")
        var dest12 = Destination("Destination CF4", "Destination CF4 Description")

        destList.put("LT1", dest1)
        destList.put("LT2", dest2)
        destList.put("LT3", dest3)
        destList.put("LT4", dest4)
        destList.put("BR1", dest5)
        destList.put("BR2", dest6)
        destList.put("BR3", dest7)
        destList.put("BR4", dest8)
        destList.put("CF1", dest9)
        destList.put("CF2", dest10)
        destList.put("CF3", dest11)
        destList.put("CF4", dest12)

        return destList
    }
}

class Destination(var name: String, var desc: String)

