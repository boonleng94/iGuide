package boonleng94.iguide

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast

import com.google.android.gms.location.ActivityRecognitionClient
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task


class BGMvmtDetectionService : Service() {
    companion object {
        private val debugTAG = "BGMvmtDetectionService"
    }

    private var mIntentService: Intent? = null
    private var mPendingIntent: PendingIntent? = null
    private var mActivityRecognitionClient: ActivityRecognitionClient? = null

//    var mBinder: IBinder = BGMvmtDetectionService().LocalBinder()
//
//    inner class LocalBinder : Binder() {
//        val serverInstance: BGMvmtDetectionService
//            get() = this@BGMvmtDetectionService
//    }

    override fun onCreate() {
        super.onCreate()
        Log.d(debugTAG, "BGMVMT created")
        mActivityRecognitionClient = ActivityRecognitionClient(this)
        mIntentService = Intent(this, MovementDetectionService::class.java)
        mPendingIntent = PendingIntent.getService(this, 1, mIntentService!!, PendingIntent.FLAG_UPDATE_CURRENT)
        requestActivityUpdatesButtonHandler()
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(debugTAG, "BGMVMT binding")
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(debugTAG, "BGMVMT started")
        return Service.START_STICKY
    }

    private fun requestActivityUpdatesButtonHandler() {
        val task = mActivityRecognitionClient!!.requestActivityUpdates(30 * 1000, mPendingIntent)

        task.addOnSuccessListener {
            Toast.makeText(applicationContext, "Successfully requested activity updates", Toast.LENGTH_SHORT).show()
        }

        task.addOnFailureListener {
            Toast.makeText(applicationContext, "Requesting activity updates failed to start", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeActivityUpdatesButtonHandler() {
        val task = mActivityRecognitionClient!!.removeActivityUpdates(mPendingIntent)
        task.addOnSuccessListener {
            Toast.makeText(applicationContext, "Removed activity updates successfully!", Toast.LENGTH_SHORT).show()
        }

        task.addOnFailureListener {
            Toast.makeText(applicationContext, "Failed to remove activity updates!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeActivityUpdatesButtonHandler()
    }
}