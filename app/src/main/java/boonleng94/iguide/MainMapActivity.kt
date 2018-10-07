package boonleng94.iguide

import android.os.Bundle

import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import java.util.ArrayList

class MainMapActivity : AppCompatActivity(){
    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)
//
//
//        val destList = (application as MainApp).destList
//        val startPos = (application as MainApp).startPos
//
//        val idealQueue = intent.getSerializableExtra("idealQueue") as ArrayList<DestinationBeacon>
//        val pathTaken = intent.getSerializableExtra("pathTaken") as ArrayList<Coordinate>
//
//        map.updateMap(destList, startPos, idealQueue, pathTaken)
    }
}