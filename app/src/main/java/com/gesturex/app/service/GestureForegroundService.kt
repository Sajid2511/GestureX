package com.gesturex.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.gesturex.app.MainActivity
import com.gesturex.app.R
import com.gesturex.app.data.camera.CameraGestureDetector
import com.gesturex.app.data.models.DefaultGestureMappings
import com.gesturex.app.data.models.GestureAction
import com.gesturex.app.data.models.GestureEvent
import com.gesturex.app.data.models.GestureType
import com.gesturex.app.data.sensors.SensorGestureDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

@AndroidEntryPoint
class GestureForegroundService : LifecycleService() {

    companion object {
        private const val TAG = "GestureFgService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "gesturex_channel"

        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE_CAMERA = "ACTION_TOGGLE_CAMERA"
        const val ACTION_TOGGLE_SENSOR = "ACTION_TOGGLE_SENSOR"

        var isRunning = false
        var isCameraActive = false
        var isSensorActive = false
    }

    @Inject lateinit var cameraGestureDetector: CameraGestureDetector
    @Inject lateinit var sensorGestureDetector: SensorGestureDetector

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    // ===== SERVICE LIFECYCLE =====

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START         -> startService()
            ACTION_STOP          -> stopService()
            ACTION_TOGGLE_CAMERA -> toggleCamera()
            ACTION_TOGGLE_SENSOR -> toggleSensor()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        stopAllDetection()
        cameraExecutor.shutdown()
        isRunning = false
        super.onDestroy()
    }

    // ===== START / STOP =====

    private fun startService() {
        if (isRunning) return
        isRunning = true

        startForeground(NOTIFICATION_ID, buildNotification("Gesture detection active"))

        // Initialize MediaPipe
        cameraGestureDetector.initialize()

        // Start collecting gestures
        collectGestures()

        // Start sensor by default
        startSensorDetection()

        Log.d(TAG, "GestureX Service Started")
    }

    private fun stopService() {
        stopAllDetection()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopAllDetection() {
        stopCameraDetection()
        stopSensorDetection()
        cameraGestureDetector.release()
    }

    // ===== CAMERA DETECTION =====

    fun startCameraDetection() {
        if (isCameraActive) return
        isCameraActive = true

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            setupCamera()
        }, ContextCompat.getMainExecutor(this))

        Log.d(TAG, "Camera detection started")
    }

    private fun setupCamera() {
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        val imageAnalyzer = ImageAnalysis.Builder()
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                this,
                cameraSelector,
                imageAnalyzer
            )
            Log.d(TAG, "Camera bound to lifecycle")
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed: ${e.message}")
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        // Mirror the bitmap for front camera
        val matrix = Matrix()
        matrix.preScale(-1f, 1f)
        val mirroredBitmap = Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, false
        )
        cameraGestureDetector.processFrame(mirroredBitmap, imageProxy.imageInfo.timestamp)
        imageProxy.close()
    }

    private fun stopCameraDetection() {
        isCameraActive = false
        cameraProvider?.unbindAll()
        Log.d(TAG, "Camera detection stopped")
    }

    private fun toggleCamera() {
        if (isCameraActive) stopCameraDetection() else startCameraDetection()
        updateNotification()
    }

    // ===== SENSOR DETECTION =====

    private fun startSensorDetection() {
        if (isSensorActive) return
        isSensorActive = true
        sensorGestureDetector.startListening()
        Log.d(TAG, "Sensor detection started")
    }

    private fun stopSensorDetection() {
        isSensorActive = false
        sensorGestureDetector.stopListening()
        Log.d(TAG, "Sensor detection stopped")
    }

    private fun toggleSensor() {
        if (isSensorActive) stopSensorDetection() else startSensorDetection()
        updateNotification()
    }

    // ===== COLLECT GESTURES & MAP TO ACTIONS =====

    private fun collectGestures() {
        // Collect camera gestures
        serviceScope.launch {
            cameraGestureDetector.gestureFlow.collect { event ->
                handleGestureEvent(event)
            }
        }

        // Collect sensor gestures
        serviceScope.launch {
            sensorGestureDetector.gestureFlow.collect { event ->
                handleGestureEvent(event)
            }
        }
    }

    private fun handleGestureEvent(event: GestureEvent) {
        Log.d(TAG, "Handling gesture: ${event.type} from ${event.source}")

        // Find the action for this gesture
        val action = findActionForGesture(event.type)

        if (action != GestureAction.NONE) {
            // Perform via Accessibility Service
            GestureAccessibilityService.instance?.performAction(action)
                ?: Log.w(TAG, "Accessibility service not connected!")
        }
    }

    private fun findActionForGesture(gestureType: GestureType): GestureAction {
        // First check app-specific mapping
        val currentApp = GestureAccessibilityService.currentForegroundApp
        val appSpecific = DefaultGestureMappings.mappings.find {
            it.gestureType == gestureType && it.appPackage == currentApp
        }

        // Fallback to default mapping
        return appSpecific?.action
            ?: DefaultGestureMappings.mappings.find {
                it.gestureType == gestureType && it.appPackage == "default"
            }?.action
            ?: GestureAction.NONE
    }

    // ===== NOTIFICATION =====

    private fun buildNotification(text: String) = run {
        createNotificationChannel()

        val openIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val stopIntent = Intent(this, GestureForegroundService::class.java).apply {
            action = ACTION_STOP
        }.let {
            PendingIntent.getService(this, 1, it, PendingIntent.FLAG_IMMUTABLE)
        }

        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GestureX")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val status = buildString {
            append("Camera: ${if (isCameraActive) "ON" else "OFF"} | ")
            append("Sensor: ${if (isSensorActive) "ON" else "OFF"}")
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(status))
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GestureX Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "GestureX gesture detection running"
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }
}