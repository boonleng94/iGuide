package boonleng94.iguide

import android.app.IntentService
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

import java.util.ArrayList


class MovementDetectionService : IntentService(debugTAG) {

    companion object {
        protected val debugTAG = "MovementDetectionService"
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onHandleIntent(intent: Intent?) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)

            // Get the list of the probable activities associated with the current state of the
            // device. Each activity is associated with a confidence level, which is an int between
            // 0 and 100.
            val detectedActivities = result.probableActivities as ArrayList<DetectedActivity>

            for (activity in detectedActivities) {
                Log.e(debugTAG, "Detected activity: " + activity.getType() + ", " + activity.getConfidence())
                broadcastActivity(activity)
            }
        }
    }

    private fun broadcastActivity(activity: DetectedActivity) {
        val intent = Intent("iGuide_service")
        intent.putExtra("type", activity.type)
        intent.putExtra("confidence", activity.confidence)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}// Use the TAG to name the worker thread.