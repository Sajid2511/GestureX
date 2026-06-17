package com.gesturex.app.data.models

/**
 * Sabhi detected gestures ka enum
 */
enum class GestureType {
    // Camera-based (Hand gestures)
    HAND_SWIPE_UP,
    HAND_SWIPE_DOWN,
    HAND_SWIPE_LEFT,
    HAND_SWIPE_RIGHT,
    OPEN_PALM,          // Pause/Play
    PINCH_IN,           // Volume down
    PINCH_OUT,          // Volume up
    TWO_FINGERS_UP,     // Screenshot
    FIST,               // Lock screen
    THUMBS_UP,          // Like
    POINTING_UP,        // Scroll to top

    // Sensor-based (Phone movement)
    TILT_LEFT,
    TILT_RIGHT,
    SHAKE,
    FLIP_DOWN,          // Face down = mute
    DOUBLE_TAP_BACK,

    NONE
}

/**
 * Ek gesture event ka data class
 */
data class GestureEvent(
    val type: GestureType,
    val confidence: Float = 1.0f,
    val source: GestureSource = GestureSource.CAMERA,
    val timestamp: Long = System.currentTimeMillis()
)

enum class GestureSource {
    CAMERA,
    SENSOR
}

/**
 * Gesture ko kaunsa action perform karna hai
 */
enum class GestureAction {
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
    TAP_CENTER,
    LONG_PRESS,
    VOLUME_UP,
    VOLUME_DOWN,
    SCREENSHOT,
    LOCK_SCREEN,
    MUTE_TOGGLE,
    PLAY_PAUSE,
    GO_BACK,
    GO_HOME,
    RECENT_APPS,
    NONE
}

/**
 * Gesture to Action mapping
 */
data class GestureMapping(
    val gestureType: GestureType,
    val action: GestureAction,
    val appPackage: String = "default"  // "default" = sabhi apps ke liye
)

/**
 * Default gesture mappings
 */
object DefaultGestureMappings {
    val mappings = listOf(
        GestureMapping(GestureType.HAND_SWIPE_UP, GestureAction.SWIPE_UP),
        GestureMapping(GestureType.HAND_SWIPE_DOWN, GestureAction.SWIPE_DOWN),
        GestureMapping(GestureType.HAND_SWIPE_LEFT, GestureAction.SWIPE_LEFT),
        GestureMapping(GestureType.HAND_SWIPE_RIGHT, GestureAction.SWIPE_RIGHT),
        GestureMapping(GestureType.OPEN_PALM, GestureAction.PLAY_PAUSE),
        GestureMapping(GestureType.PINCH_IN, GestureAction.VOLUME_DOWN),
        GestureMapping(GestureType.PINCH_OUT, GestureAction.VOLUME_UP),
        GestureMapping(GestureType.TWO_FINGERS_UP, GestureAction.SCREENSHOT),
        GestureMapping(GestureType.FIST, GestureAction.LOCK_SCREEN),
        GestureMapping(GestureType.TILT_LEFT, GestureAction.GO_BACK),
        GestureMapping(GestureType.TILT_RIGHT, GestureAction.SWIPE_RIGHT),
        GestureMapping(GestureType.SHAKE, GestureAction.GO_HOME),
        GestureMapping(GestureType.FLIP_DOWN, GestureAction.MUTE_TOGGLE),
    )
}
