# GestureX 🤚
### Control your phone without touching the screen

---

## 📱 ANDROID 10+ SUPPORT (API 29+)

## ✅ Features
- **Camera Gestures** – MediaPipe hand tracking (front camera)
- **Sensor Gestures** – Accelerometer + Gyroscope
- **Accessibility Service** – Works across ALL apps (Instagram, YouTube, Spotify, etc.)
- **Foreground Service** – Runs continuously in background
- **Dark UI** – Beautiful purple/cyan theme

---

## 🛠️ SETUP INSTRUCTIONS (Android Studio)

### Step 1: Open Project
```
File → Open → Select GestureX folder
```

### Step 2: Download MediaPipe Models
Ye 2 files download karo aur `app/src/main/assets/` folder mein daalo:

| Model | Download Link |
|-------|--------------|
| `hand_landmarker.task` | https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/latest/hand_landmarker.task |
| `gesture_recognizer.task` | https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/latest/gesture_recognizer.task |

### Step 3: Sync Gradle
```
File → Sync Project with Gradle Files
```

### Step 4: Build & Run
```
Run → Run 'app'
```

---

## 📋 PERMISSIONS TO GRANT ON FIRST LAUNCH

1. **Camera** – Auto-requested on launch
2. **Notification** – Auto-requested on launch (Android 13+)
3. **Accessibility Service** – Go to:
   `Settings → Accessibility → Installed Services → GestureX → Enable`
4. **Draw Over Other Apps** – App will redirect automatically

---

## 🤌 GESTURE GUIDE

### Camera Gestures (Front Camera)
| Gesture | Action |
|---------|--------|
| ✋ Open Palm | Play / Pause |
| 👆 Swipe Up | Scroll / Next Reel |
| 👇 Swipe Down | Previous |
| 👈 Swipe Left | Go Back |
| 👉 Swipe Right | Forward |
| ✊ Fist | Lock Screen |
| 🤏 Pinch In | Volume Down |
| 🤏 Pinch Out | Volume Up |
| ✌️ Two Fingers | Screenshot |
| 👍 Thumbs Up | Like (Instagram) |

### Sensor Gestures (No Camera Needed)
| Gesture | Action |
|---------|--------|
| 📱 Shake phone | Go Home |
| ↩️ Tilt Left | Go Back |
| ➡️ Tilt Right | Next |
| 🔄 Flip face down | Mute Toggle |
| 👆 Double tap back | Custom Action |

---

## 🏗️ PROJECT STRUCTURE
```
GestureX/
├── app/src/main/
│   ├── assets/
│   │   ├── hand_landmarker.task      ← Download this!
│   │   └── gesture_recognizer.task   ← Download this!
│   ├── java/com/gesturex/app/
│   │   ├── data/
│   │   │   ├── camera/CameraGestureDetector.kt
│   │   │   ├── sensors/SensorGestureDetector.kt
│   │   │   └── models/GestureEvent.kt
│   │   ├── service/
│   │   │   ├── GestureForegroundService.kt
│   │   │   └── GestureAccessibilityService.kt
│   │   ├── ui/
│   │   │   ├── home/HomeScreen.kt
│   │   │   └── theme/
│   │   ├── viewmodel/GestureViewModel.kt
│   │   ├── di/AppModule.kt
│   │   ├── MainActivity.kt
│   │   └── GestureXApp.kt
│   └── res/xml/accessibility_service_config.xml
```

---

## ⚠️ IMPORTANT NOTES

1. **MediaPipe models** ko `assets/` folder mein manually add karna hai (links upar hain)
2. **Physical device** pe test karo — emulator mein camera kaam nahi karega
3. **Android 10+** (API 29+) required hai
4. First launch mein saari permissions do — bina unke app kaam nahi karega

---

## 🧰 TECH STACK
- Kotlin 2.1.0
- Jetpack Compose (BOM 2025.05.01)
- MediaPipe Tasks Vision 0.10.22
- CameraX 1.4.2
- Hilt 2.56.2
- Android 10+ (minSdk 29)
