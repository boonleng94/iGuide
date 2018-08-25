package boonleng94.iguide

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import com.estimote.indoorsdk_module.cloud.Location
import java.util.*

/**
 * START YOUR JOURNEY HERE!
 * Main app class
 */
class MainApp : Application() {
    // !!! ULTRA IMPORTANT !!!
    // Change your credentials below to have access to locations from your account.
    // Make sure you have any locations created in cloud!
    // If you don't have your Estimote Cloud Account - go to https://cloud.estimote.com/ and create one :)
    val cloudCredentials = com.estimote.indoorsdk.EstimoteCloudCredentials("iguide-msf", "f2cefefb18398ad56b51b6884e521727")
    val proximityCloudCredentials = com.estimote.proximity_sdk.api.EstimoteCloudCredentials("iguide-msf", "f2cefefb18398ad56b51b6884e521727")

    // This is map for holding Indoor Location from your account.
    // You can move it somewhere else, but for sake of simplicity we put it in here.
    var location = Location()
    var speech = TTSController()
}