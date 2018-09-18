package boonleng94.iguide

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

interface CompassListener {
    fun onAzimuthChange(azimuth: Float)
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

    private val gravity = FloatArray(3)
    private val magnetic = FloatArray(3)
    private val constantR = FloatArray(9)
    private val constantI = FloatArray(9)

    private var azimuth: Float = 0.toFloat()

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

    override fun onSensorChanged(event: SensorEvent) {
        val alpha = 0.97f

        synchronized(this) {
//            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
//                // calculate the rotation matrix
//                SensorManager.getRotationMatrixFromVector(constantR, event.values );
//                // get the azimuth value (orientation[0]) in degree
//                var azimuth = ((Math.toDegrees(SensorManager.getOrientation(constantR, orien)[0].toDouble()).toFloat()) + 360f) % 360
//                }
//            }

            if (event.sensor.type == Sensor.TYPE_GRAVITY) {
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
            }

            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]
            }

            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                magnetic[0] = alpha * magnetic[0] + (1 - alpha) * event.values[0]
                magnetic[1] = alpha * magnetic[1] + (1 - alpha) * event.values[1]
                magnetic[2] = alpha * magnetic[2] + (1 - alpha) * event.values[2]
            }

            val success = SensorManager.getRotationMatrix(constantR, constantI, gravity, magnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(constantR, orientation)
                azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuth = (azimuth + 360f) % 360
                compassListener.onAzimuthChange(azimuth)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }
}