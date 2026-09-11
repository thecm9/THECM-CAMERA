package com.example.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class HorizonOrientation(
  val rollDegrees: Float = 0f,
  val pitchDegrees: Float = 0f,
  val isLevel: Boolean = true,
)

/**
 * Monitors the device tilt and roll in real-time for cinematic horizon leveling.
 */
class DeviceOrientationSensor(context: Context) {
  private val sensorManager =
    context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val sensor =
    sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
      ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

  fun getOrientationFlow(): Flow<HorizonOrientation> = callbackFlow {
    if (sensorManager == null || sensor == null) {
      trySend(HorizonOrientation(0f, 0f, true))
      awaitClose { }
      return@callbackFlow
    }

    val listener = object : SensorEventListener {
      override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate roll in degrees
        val roll = Math.toDegrees(atan2(x.toDouble(), y.toDouble())).toFloat()
        val pitch = Math.toDegrees(atan2(z.toDouble(), sqrt((x * x + y * y).toDouble()))).toFloat()

        // Normalize roll around portrait orientation (0 is level, positive is tilted left, negative right)
        val normalizedRoll = when {
          roll > 90f -> 180f - roll
          roll < -90f -> -180f - roll
          else -> -roll
        }

        // Within +/- 1.0 degree is considered level
        val isLevel = kotlin.math.abs(normalizedRoll) < 1.0f

        trySend(
          HorizonOrientation(
            rollDegrees = (normalizedRoll * 10f).roundToInt() / 10f,
            pitchDegrees = (pitch * 10f).roundToInt() / 10f,
            isLevel = isLevel,
          )
        )
      }

      override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)

    awaitClose {
      sensorManager.unregisterListener(listener)
    }
  }
}
