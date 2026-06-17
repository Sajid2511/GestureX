package com.gesturex.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.gesturex.app.data.models.GestureAction

/**
 * GestureX Accessibility Service
 * Ye service actual screen pe swipes, taps inject karti hai
 * Dusre apps (Instagram, YouTube, etc.) ko control karne ke liye
 */
class GestureAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "GestureA11yService"
        var instance: GestureAccessibilityService? = null
        var currentForegroundApp: String = ""
    }

    private lateinit var audioManager: AudioManager
    private var screenWidth = 1080
    private var screenHeight = 2340

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Get screen dimensions
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        Log.d(TAG, "Accessibility Service connected. Screen: ${screenWidth}x${screenHeight}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Track which app is in foreground
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.toString()?.let {
                currentForegroundApp = it
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // ===== PERFORM ACTION BASED ON GESTURE =====
    fun performAction(action: GestureAction) {
        Log.d(TAG, "Performing action: $action")
        when (action) {
            GestureAction.SWIPE_UP      -> performSwipeUp()
            GestureAction.SWIPE_DOWN    -> performSwipeDown()
            GestureAction.SWIPE_LEFT    -> performSwipeLeft()
            GestureAction.SWIPE_RIGHT   -> performSwipeRight()
            GestureAction.TAP_CENTER    -> performTap(screenWidth / 2f, screenHeight / 2f)
            GestureAction.LONG_PRESS    -> performLongPress(screenWidth / 2f, screenHeight / 2f)
            GestureAction.VOLUME_UP     -> changeVolume(AudioManager.ADJUST_RAISE)
            GestureAction.VOLUME_DOWN   -> changeVolume(AudioManager.ADJUST_LOWER)
            GestureAction.SCREENSHOT    -> performScreenshot()
            GestureAction.LOCK_SCREEN   -> performLockScreen()
            GestureAction.MUTE_TOGGLE   -> toggleMute()
            GestureAction.PLAY_PAUSE    -> sendMediaKey(android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            GestureAction.GO_BACK       -> performGlobalAction(GLOBAL_ACTION_BACK)
            GestureAction.GO_HOME       -> performGlobalAction(GLOBAL_ACTION_HOME)
            GestureAction.RECENT_APPS   -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            GestureAction.NONE          -> { /* No action */ }
        }
    }

    // ===== SWIPE GESTURES =====

    private fun performSwipeUp() {
        val startX = screenWidth / 2f
        val startY = screenHeight * 0.75f
        val endY = screenHeight * 0.25f
        dispatchSwipe(startX, startY, startX, endY, durationMs = 300)
    }

    private fun performSwipeDown() {
        val startX = screenWidth / 2f
        val startY = screenHeight * 0.25f
        val endY = screenHeight * 0.75f
        dispatchSwipe(startX, startY, startX, endY, durationMs = 300)
    }

    private fun performSwipeLeft() {
        val startX = screenWidth * 0.8f
        val startY = screenHeight / 2f
        val endX = screenWidth * 0.2f
        dispatchSwipe(startX, startY, endX, startY, durationMs = 250)
    }

    private fun performSwipeRight() {
        val startX = screenWidth * 0.2f
        val startY = screenHeight / 2f
        val endX = screenWidth * 0.8f
        dispatchSwipe(startX, startY, endX, startY, durationMs = 250)
    }

    // ===== TAP & LONG PRESS =====

    private fun performTap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun performLongPress(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 1500)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // ===== CORE DISPATCH SWIPE =====

    private fun dispatchSwipe(
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        durationMs: Long = 300
    ) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Swipe completed")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Swipe cancelled")
            }
        }, null)
    }

    // ===== VOLUME =====

    private fun changeVolume(direction: Int) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            direction,
            AudioManager.FLAG_SHOW_UI
        )
    }

    private fun toggleMute() {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_RING,
            AudioManager.ADJUST_TOGGLE_MUTE,
            AudioManager.FLAG_SHOW_UI
        )
    }

    // ===== MEDIA =====

    private fun sendMediaKey(keyCode: Int) {
        val downEvent = android.view.KeyEvent(
            android.view.KeyEvent.ACTION_DOWN, keyCode
        )
        audioManager.dispatchMediaKeyEvent(downEvent)
        val upEvent = android.view.KeyEvent(
            android.view.KeyEvent.ACTION_UP, keyCode
        )
        audioManager.dispatchMediaKeyEvent(upEvent)
    }

    // ===== SCREENSHOT =====

    private fun performScreenshot() {
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    // ===== LOCK SCREEN =====

    private fun performLockScreen() {
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
    }
}
