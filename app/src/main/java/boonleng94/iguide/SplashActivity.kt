package boonleng94.iguide

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Window
import com.estimote.indoorsdk_module.cloud.CloudCallback
import com.estimote.indoorsdk_module.cloud.EstimoteCloudException
import com.estimote.indoorsdk_module.cloud.IndoorCloudManagerFactory
import com.estimote.indoorsdk_module.cloud.Location
import com.estimote.mustard.rx_goodness.rx_requirements_wizard.RequirementsWizardFactory
import java.util.*

/**
 * Simple splash screen to load the data from cloud.
 * Make sure to initialize EstimoteSDK with your APP ID and APP TOKEN in {@link MainApp} class.
 * You can get those credentials from your Estimote Cloud account :)
 */
class SplashActivity : AppCompatActivity(){
    private lateinit var i: TTSController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make actionbar invisible.
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        i = TTSController()
        i.initialize(applicationContext, Locale.US)
        (application as MainApp).speech = i

        // Create object for communicating with Estimote cloud.
        // IMPORTANT - you need to put here your Estimote Cloud credentials.
        // We daclared them in MainApp.ktlass
//        val cloudManager = IndoorCloudManagerFactory().create(applicationContext, (application as MainApp).cloudCredentials)
//
//        // Launch request for all locations connected to your account.
//        // If you don't see any - check your cloud account - maybe you should create those locations first?
//        cloudManager.getLocation("home-h28", object: CloudCallback<Location> {
//            override fun success(location: Location) {
//                (application as MainApp).location = location
//
//                // If all is fine, go ahead and launch activity with list of your locations :)
//                //startActivity(Intent(applicationContext, MainIndoorActivity::class.java))
//            }
//
//            override fun failure(serverException: EstimoteCloudException) {
//                // For the sake of this demo, you need to make sure you have an internet connection and AppID/AppToken set :)
////                Toast.makeText(this@SplashActivity, "Unable to fetch location data from Estimote" +
////                        "Please check your internet connection", Toast.LENGTH_LONG).show()
////                startMainActivity()
//            }
//        })

        RequirementsWizardFactory.createEstimoteRequirementsWizard().fulfillRequirements(this,
                // onRequirementsFulfilled
                {
                    startActivity(Intent(applicationContext, MainCompassActivity::class.java))
                    Log.d("iGuide", "Requirements fulfilled, observation started")
                },
                // onRequirementsMissing
                { requirements ->
                    Log.d("iGuide", "requirements missing: $requirements")
                }
                // onError
        ) { throwable ->
            Log.d("iGuide", "requirements error: $throwable")
        }
    }
}