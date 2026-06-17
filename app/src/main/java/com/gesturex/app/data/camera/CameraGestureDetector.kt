package com.gesturex.app.data.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import com.gesturex.app.data.models.GestureEvent
import com.gesturex.app.data.models.GestureSource
import com.gesturex.app.data.models.GestureType
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraGestureDetector @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "CameraGestureDetector"
        private const val HAND_LANDMARKER_MODEL = "hand_landmarker.task"
        private const val GESTURE_RECOGNIZER_MODEL = "gesture_recognizer.task"
        private const val MIN_DETECTION_CONFIDENCE = 0.7f
        private const val MIN_TRACKING_CONFIDENCE = 0.7f
        private const val GESTURE_COOLDOWN_MS = 500L
    }

    private var handLandmarker: HandLandmarker? = null
    private var gestureRecognizer: GestureRecognizer? = null

    private val _gestureFlow = MutableSharedFlow<GestureEvent>(extraBufferCapacity = 10)
    val gestureFlow: SharedFlow<GestureEvent> = _gestureFlow

    private var lastGestureTime = 0L
    private var lastLandmarkResult: HandLandmarkerResult? = null
    private var previousWristY = 0f
    private var previousWristX = 0f

    // ===== Initialize MediaPipe =====
    fun initialize() {
        try {
            initHandLandmarker()
            initGestureRecognizer()
            Log.d(TAG, "MediaPipe initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "MediaPipe initialization failed: ${e.message}")
        }
    }

    private fun initHandLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(HAND_LANDMARKER_MODEL)
            .setDelegate(Delegate.GPU) // GPU for performance; fallback to CPU
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setNumHands(1)
            .setMinHandDetectionConfidence(MIN_DETECTION_CONFIDENCE)
            .setMinHandPresenceConfidence(MIN_DETECTION_CONFIDENCE)
            .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                processLandmarkerResult(result)
            }
            .setErrorListener { error ->
                Log.e(TAG, "HandLandmarker error: ${error.message}")
            }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    private fun initGestureRecognizer() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(GESTURE_RECOGNIZER_MODEL)
            .setDelegate(Delegate.GPU)
            .build()

        val options = GestureRecognizer.GestureRecognizerOptions.builder()
            .setBaseOptions(baseOptions)
            .setNumHands(1)
            .setMinHandDetectionConfidence(MIN_DETECTION_CONFIDENCE)
            .setMinTrackingConfidence(MIN_TRACKING_CONFIDENCE)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener { result, _ ->
                processGestureResult(result)
            }
            .setErrorListener { error ->
                Log.e(TAG, "GestureRecognizer error: ${error.message}")
            }
            .build()

        gestureRecognizer = GestureRecognizer.createFromOptions(context, options)
    }

    // ===== Process Camera Frame =====
    fun processFrame(bitmap: Bitmap, frameTime: Long) {
        val mpImage = BitmapImageBuilder(bitmap).build()

        // Run both detectors
        handLandmarker?.detectAsync(mpImage, frameTime)
        gestureRecognizer?.recognizeAsync(mpImage, frameTime)
    }

    // ===== Process Landmark Result (for swipe detection) =====
    private fun processLandmarkerResult(result: HandLandmarkerResult) {
        if (result.landmarks().isEmpty()) {
            previousWristX = 0f
            previousWristY = 0f
            return
        }

        val landmarks = result.landmarks()[0]
        // Wrist landmark is index 0
        val wrist = landmarks[0]
        val currentX = wrist.x()
        val currentY = wrist.y()

        // Detect swipe direction based on wrist movement
        if (previousWristX != 0f && previousWristY != 0f) {
            val deltaX = currentX - previousWristX
            val deltaY = currentY - previousWristY
            val threshold = 0.08f // 8% screen movement to trigger swipe

            when {
                deltaY < -threshold -> emitGesture(GestureType.HAND_SWIPE_UP)
                deltaY > threshold  -> emitGesture(GestureType.HAND_SWIPE_DOWN)
                deltaX < -threshold -> emitGesture(GestureType.HAND_SWIPE_LEFT)
                deltaX > threshold  -> emitGesture(GestureType.HAND_SWIPE_RIGHT)
            }

            // Pinch detection: thumb tip (4) and index tip (8) distance
            val thumbTip = landmarks[4]
            val indexTip = landmarks[8]
            val pinchDistance = Math.sqrt(
                ((thumbTip.x() - indexTip.x()) * (thumbTip.x() - indexTip.x()) +
                (thumbTip.y() - indexTip.y()) * (thumbTip.y() - indexTip.y())).toDouble()
            ).toFloat()

            // Detect fist: all fingers folded (all tips below their MCP joints)
            val isFingerFolded = { tipIdx: Int, mcpIdx: Int ->
                landmarks[tipIdx].y() > landmarks[mcpIdx].y()
            }
            val isFist = isFingerFolded(8, 5) && isFingerFolded(12, 9) &&
                         isFingerFolded(16, 13) && isFingerFolded(20, 17)
            if (isFist) emitGesture(GestureType.FIST)
        }

        previousWristX = currentX
        previousWristY = currentY
        lastLandmarkResult = result
    }

    // ===== Process Gesture Result (built-in gestures) =====
    private fun processGestureResult(result: GestureRecognizerResult) {
        if (result.gestures().isEmpty()) return

        val gesture = result.gestures()[0]
        if (gesture.isEmpty()) return

        val topGesture = gesture[0]
        val confidence = topGesture.score()

        if (confidence < MIN_DETECTION_CONFIDENCE) return

        // Map MediaPipe built-in gestures to our GestureType
        val gestureType = when (topGesture.categoryName()) {
            "Open_Palm"      -> GestureType.OPEN_PALM
            "Thumb_Up"       -> GestureType.THUMBS_UP
            "Victory"        -> GestureType.TWO_FINGERS_UP  // ✌️ = two fingers
            "Closed_Fist"    -> GestureType.FIST
            "Pointing_Up"    -> GestureType.POINTING_UP
            else             -> GestureType.NONE
        }

        if (gestureType != GestureType.NONE) {
            emitGesture(gestureType, confidence)
        }
    }

    // ===== Emit Gesture with Cooldown =====
    private fun emitGesture(type: GestureType, confidence: Float = 0.9f) {
        val now = System.currentTimeMillis()
        if (now - lastGestureTime < GESTURE_COOLDOWN_MS) return

        lastGestureTime = now
        _gestureFlow.tryEmit(
            GestureEvent(
                type = type,
                confidence = confidence,
                source = GestureSource.CAMERA
            )
        )
        Log.d(TAG, "Gesture detected: $type (confidence: $confidence)")
    }

    fun release() {
        handLandmarker?.close()
        gestureRecognizer?.close()
        handLandmarker = null
        gestureRecognizer = null
        Log.d(TAG, "CameraGestureDetector released")
    }
}
