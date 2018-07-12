package boonleng94.iguide

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.estimote.proximity_sdk.proximity.ProximityObserver
import com.estimote.proximity_sdk.proximity.ProximityObserverBuilder

class MainProximityActivity : AppCompatActivity() {
    lateinit var observationHandler: ProximityObserver.Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val proximityObserver = ProximityObserverBuilder(applicationContext, (application as CloudController).proximityCloudCredentials)
                .withBalancedPowerMode()
                .withOnErrorAction { /* handle errors here */ }
                .build()

        val venueZone = proximityObserver.zoneBuilder()
                .forTag("venue")
                .inNearRange()
                .withOnEnterAction{
                    /* do something here */
                    Toast.makeText(this@MainProximityActivity, "Enter house", Toast.LENGTH_SHORT).show()
                }
                .withOnExitAction{
                    /* do something here */
                    Toast.makeText(this@MainProximityActivity, "Exit house", Toast.LENGTH_SHORT).show()
                }
                .withOnChangeAction{
                    /* do something here */
                }
                .create()

        val observationHandler = proximityObserver
                .addProximityZone(venueZone)
                .start()
    }

    override fun onDestroy() {
        observationHandler.stop()
        super.onDestroy()
    }
}