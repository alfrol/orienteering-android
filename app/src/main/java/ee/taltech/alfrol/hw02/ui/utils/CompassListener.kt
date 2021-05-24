package ee.taltech.alfrol.hw02.ui.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class CompassListener(
    context: Context,
    private val host: OnCompassUpdateCallback
) : SensorEventListener {

    /**
     * An interface that must be implemented by the hosting class
     * which needs to receive the sensor updates.
     */
    interface OnCompassUpdateCallback {

        /**
         * Receive the new compass orientation angle.
         */
        fun onCompassUpdate(angle: Float)
    }

    private val sensorManager: SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    override fun onSensorChanged(event: SensorEvent?) {
        val sensorType = event?.sensor?.type ?: return
        val values = event.values

        when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> lowPass(values, accelerometerReading)
            Sensor.TYPE_MAGNETIC_FIELD -> lowPass(values, magnetometerReading)
        }

        updateOrientationAngles()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    /**
     * Get updates from the most recent accelerometer and magnetometer sensor readings.
     */
    fun startListening() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.also { accelerometer ->
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_DELAY_UI
            )
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.also { magneticField ->
            sensorManager.registerListener(
                this,
                magneticField,
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    /**
     * Stop listening to the accelerometer and magnetometer sensors.
     */
    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Compute the three orientation angles based on the readings
     * from device's accelerometer and magnetometer.
     */
    private fun updateOrientationAngles() {
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val degrees = -(Math.toDegrees(orientationAngles[0].toDouble()) + 360).toFloat() % 360
        host.onCompassUpdate(degrees)
    }

    /**
     * Filter out the readings from the accelerometer and magnetometer.
     */
    private fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.05f

        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }
}