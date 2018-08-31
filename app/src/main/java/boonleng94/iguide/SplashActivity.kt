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
    private lateinit var TTSCtrl: TTSController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make actionbar invisible.
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.hide()
        setContentView(R.layout.activity_splash)

        TTSCtrl = TTSController()
        TTSCtrl.initialize(applicationContext, Locale.US)
        (application as MainApp).speech = TTSCtrl

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