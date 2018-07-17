package boonleng94.iguide

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

import kotlin.math.sqrt

class ShakeDetector(context: Context) : SensorEventListener {
    lateinit var shakeListener: ShakeListener

    private var mShakeTimestamp: Long = 0
    private var mShakeCount: Int = 0

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accSensor: Sensor

    companion object {
        /*
         * The gForce that is necessary to register as shake.
         * Must be greater than 1G (one earth gravity unit).
         * You can install "G-Force", by Blake La Pierre
         * from the Google Play Store and run it to see how
         *  many G's it takes to register a shake
         */
        private val SHAKE_THRESHOLD_GRAVITY = 1.7f
        private val SHAKE_SLOP_TIME_MS = 500
        private val SHAKE_COUNT_RESET_TIME_MS = 2000
    }

    init {
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun start() {
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (shakeListener != null) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            // gForce will be close to 1 when there is no movement.
            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

            if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                val now = System.currentTimeMillis()
                // ignore shake events too close to each other (500ms)
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return
                }

                // reset the shake count after 3 seconds of no shakes
                if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0
                }

                mShakeTimestamp = now
                mShakeCount++

                shakeListener.onShake(mShakeCount)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // ignore
    }

}