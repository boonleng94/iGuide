package boonleng94.iguide

import android.os.Bundle

import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import java.util.*

class MainMapActivity : AppCompatActivity(){
    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)

//        val startPos = (application as MainApp).startPos
//        val beaconList = intent.getSerializableExtra("beaconList") as ArrayList<Beacon>
//        val idealQueue = intent.getSerializableExtra("idealQueue") as ArrayList<Beacon>
//        val pathTaken = intent.getSerializableExtra("pathTaken") as ArrayList<Coordinate>

        var beaconList = ArrayList<Beacon>()

        val cf1 = Beacon("08521b848f630526cdf23fe40044913d")
        cf1.name = "Placeholder cf1"
        cf1.coordinate = Coordinate(40.0,0.0)

        val cf2 = Beacon("bf96d1619008c20716ddafbf69747424")
        cf2.name = "Placeholder cf2"
        cf2.coordinate = Coordinate(40.0,2.0)

        val cf3 = Beacon("d137249e154e746b32fe0f25c89cfa05")
        cf3.name = "Placeholder cf3"
        cf3.coordinate = Coordinate(0.0,2.0)

        val cf4 = Beacon("21406f9c93238e88db98ec7fca351d20")
        cf4.name = "Entrance of Block B"
        cf4.coordinate = Coordinate(0.0,0.0)

        val lt1 = Beacon("c99857c5c5a01cdc348e02bb878c3b1d")
        lt1.name = "N4-02c-88"
        lt1.coordinate = Coordinate(35.0,2.0)

        val lt2 = Beacon("c449d2e64acc028dc214a74d53087827")
        lt2.name = "N4-02c-86"
        lt2.coordinate = Coordinate(31.0,2.0)

        val lt3 = Beacon("9176b3b9b95e4e6d69212c56fa21fe20")
        lt3.name = "N4-02c-84"
        lt3.coordinate = Coordinate(27.0,2.0)

        val lt4 = Beacon("881c05b08a25c096cbd4deaedfc6c70f")
        lt4.name = "N4-02c-82"
        lt4.coordinate = Coordinate(23.0,2.0)

        val br1 = Beacon("7d682761535f52f943994e8c8ef57613")
        br1.name = "N4-02c-80"
        br1.coordinate = Coordinate(19.0,2.0)

        val br2 = Beacon("99d3734cf5f35999cddc66e499b0f51e")
        br2.name = "N4-02c-78"
        br2.coordinate = Coordinate(15.0,2.0)

        val br3 = Beacon("4698eda62f2def1aef341553fa41b51c")
        br3.name = "N4-02c-76"
        br3.coordinate = Coordinate(11.0,2.0)

        val br4 = Beacon("e65c0c815eb675f11cad88bef67e1335")
        br4.name = "N4-02c-74"
        br4.coordinate = Coordinate(7.0,2.0)

        beaconList.add(cf1)
        beaconList.add(cf2)
        beaconList.add(cf3)
        beaconList.add(cf4)
        beaconList.add(lt1)
        beaconList.add(lt2)
        beaconList.add(lt3)
        beaconList.add(lt4)
        beaconList.add(br1)
        beaconList.add(br2)
        beaconList.add(br3)
        beaconList.add(br4)

        val startPos =  Coordinate (10.0, 10.0)

        val cor1 = Coordinate(0.0, 2.0)
        val cor2 = Coordinate(10.0, 2.0)
        var pathTaken = ArrayList<Coordinate>()
        pathTaken.add(cor1)
        pathTaken.add(cor2)

        val test = beaconList.iterator()

        while (test.hasNext()) {
            val i = test.next()
            val tempX = i.coordinate.x
            val tempY = i.coordinate.y
            i.coordinate.x = tempY
            i.coordinate.y = tempX
        }

        var idealQueue = ArrayList<Beacon>()
        val iq1 = Beacon("7d682761535f52f943994e8c8ef57613")
        iq1.name = "N4-02c-80"
        iq1.coordinate = Coordinate(19.0,2.0)

        val iq2 = Beacon("99d3734cf5f35999cddc66e499b0f51e")
        iq2.name = "N4-02c-78"
        iq2.coordinate = Coordinate(15.0,2.0)

        val iq3 = Beacon("4698eda62f2def1aef341553fa41b51c")
        iq3.name = "N4-02c-76"
        iq3.coordinate = Coordinate(11.0,2.0)

        val iq4 = Beacon("e65c0c815eb675f11cad88bef67e1335")
        iq4.name = "N4-02c-74"
        iq4.coordinate = Coordinate(7.0,2.0)
        idealQueue.add(iq1)
        idealQueue.add(iq2)
        idealQueue.add(iq3)
        idealQueue.add(iq4)

        val test2 = idealQueue.iterator()
        while (test2.hasNext()) {
            val i = test2.next()
            val tempX = i.coordinate.x
            val tempY = i.coordinate.y
            i.coordinate.x = tempY
            i.coordinate.y = tempX
        }

        for (i in pathTaken) {
            var tempX = i.x
            var tempY = i.y
            i.x = tempY
            i.y = tempX
        }

        map.updateMap(beaconList, startPos, idealQueue, pathTaken)
    }
}