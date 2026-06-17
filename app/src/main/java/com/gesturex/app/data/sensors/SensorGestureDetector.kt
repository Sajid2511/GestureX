package com.gesturex.app.data.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.gesturex.app.data.models.GestureEvent
import com.gesturex.app.data.models.GestureSource
import com.gesturex.app.data.models.GestureType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

@Singleton
class SensorGestureDetector @Inject constructor(
    private val context: Context
) : SensorEventListener {

    companion object {
        private const val TAG = "SensorGestureDetector"
        private const val SHAKE_THRESHOLD = 15.0f
        private const val TILT_THRESHOLD = 6.0f
        private const val GESTURE_COOLDOWN_MS = 600L
        private const val DOUBLE_TAP_TIMEOUT_MS = 500L
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private val _gestureFlow = MutableSharedFlow<GestureEvent>(extraBufferCapacity = 10)
    val gestureFlow: SharedFlow<GestureEvent> = _gestureFlow

    private var lastGestureTime = 0L
    private var lastAccelTime = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    // Shake detection
    private var shakeCount = 0
    private var lastShakeTime = 0L

    // Double tap back detection (via accelerometer z-axis)
    private var backTapCount = 0
    private var lastBackTapTime = 0L

    // Gyroscope data for tilt
    private var gyroX = 0f
    private var gyroY = 0f

    // ===== Start Listening =====
    fun startListening() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Accelerometer registered")
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Gyroscope registered")
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Sensors unregistered")
    }

    // ===== Sensor Event Callback =====
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event)
            Sensor.TYPE_GYROSCOPE     -> handleGyroscope(event)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ===== Accelerometer Handler =====
    private fun handleAccelerometer(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val now = System.currentTimeMillis()

        // Rate limit
        if (now - lastAccelTime < 50) return
        lastAccelTime = now

        if (lastX == 0f) {
            lastX = x; lastY = y; lastZ = z
            return
        }

        val deltaX = x - lastX
        val deltaY = y - lastY
        val deltaZ = z - lastZ

        // ===== SHAKE DETECTION =====
        val acceleration = sqrt((deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ).toDouble()).toFloat()
        if (acceleration > SHAKE_THRESHOLD) {
            if (now - lastShakeTime > 1000) {
                emitGesture(GestureType.SHAKE)
                lastShakeTime = now
            }
        }

        // ===== FACE DOWN DETECTION (flip phone) =====
        // When phone is face down, z < -8 (gravity pulls up)
        if (z < -8.5f) {
            emitGesture(GestureType.FLIP_DOWN)
        }

        // ===== DOUBLE TAP BACK DETECTION =====
        // Back tap: quick z-axis spike when tapping phone back
        if (abs(deltaZ) > 8f && abs(deltaZ) < 20f) {
            if (now - lastBackTapTime < DOUBLE_TAP_TIMEOUT_MS) {
                backTapCount++
                if (backTapCount >= 2) {
                    emitGesture(GestureType.DOUBLE_TAP_BACK)
                    backTapCount = 0
                }
            } else {
                backTapCount = 1
            }
            lastBackTapTime = now
        }

        lastX = x
        lastY = y
        lastZ = z
    }

    // ===== Gyroscope Handler (for tilt detection) =====
    private fun handleGyroscope(event: SensorEvent) {
        gyroX = event.values[0]  // Pitch (tilt forward/back)
        gyroY = event.values[1]  // Roll  (tilt left/right)

        // ===== TILT LEFT / RIGHT =====
        when {
            gyroY > TILT_THRESHOLD  -> emitGesture(GestureType.TILT_LEFT)
            gyroY < -TILT_THRESHOLD -> emitGesture(GestureType.TILT_RIGHT)
        }
    }

    // ===== Emit Gesture with Cooldown =====
    private fun emitGesture(type: GestureType) {
        val now = System.currentTimeMillis()
        if (now - lastGestureTime < GESTURE_COOLDOWN_MS) return

        // Skip FLIP_DOWN repeat (only emit once)
        if (type == GestureType.FLIP_DOWN && now - lastGestureTime < 3000) return

        lastGestureTime = now
        _gestureFlow.tryEmit(
            GestureEvent(
                type = type,
                confidence = 1.0f,
                source = GestureSource.SENSOR
            )
        )
        Log.d(TAG, "Sensor gesture: $type")
    }
}
