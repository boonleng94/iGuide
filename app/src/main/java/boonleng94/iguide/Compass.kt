package boonleng94.iguide

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

interface CompassListener {
    fun onNewAzimuth(azimuth: Float)
}

class Compass(context: Context): SensorEventListener {
    lateinit var compassListener: CompassListener

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accSensor: Sensor
    private val magnSensor: Sensor
    private val gravSensor: Sensor
    private val rotSensor: Sensor

    private var haveAcc = false
    private var haveMagn = false
    private var haveGrav = false

    private val mGravity = FloatArray(3)
    private val mGeomagnetic = FloatArray(3)
    private val R = FloatArray(9)
    private val I = FloatArray(9)
    private val orien = FloatArray(3)


    private var azimuth: Float = 0.toFloat()
    private var azimuthFix: Float = 0.toFloat()

    init {
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        gravSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        rotSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    fun start() {
        sensorManager.registerListener(this, rotSensor, SensorManager.SENSOR_DELAY_GAME)
        haveGrav = sensorManager.registerListener(this, gravSensor, SensorManager.SENSOR_DELAY_GAME)
        if (!haveGrav) {
            haveAcc = sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        haveMagn = sensorManager.registerListener(this, magnSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    fun resetAzimuthFix() {
        azimuthFix = 0f
    }

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f

        synchronized(this) {
//            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
//                // calculate the rotation matrix
//                SensorManager.getRotationMatrixFromVector(R, event.values );
//                // get the azimuth value (orientation[0]) in degree
//                var azimuth = ((Math.toDegrees(SensorManager.getOrientation(R, orien)[0].toDouble()).toFloat()) + 360f) % 360
//                }
//            }

            if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0]
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1]
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2]
            }

            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * event.values[0]
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * event.values[1]
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * event.values[2]
            }

            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0]
                mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1]
                mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2]
            }

            val success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(R, orientation)
                azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat() // orientation
                azimuth = (azimuth + azimuthFix + 360f) % 360
                compassListener.onNewAzimuth(azimuth)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }
}