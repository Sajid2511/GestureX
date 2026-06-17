package com.gesturex.app.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gesturex.app.service.GestureForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GestureUiState(
    val isServiceRunning: Boolean = false,
    val isCameraActive: Boolean = false,
    val isSensorActive: Boolean = false,
    val lastGesture: String = "None",
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false
)

@HiltViewModel
class GestureViewModel @Inject constructor(
    private val app: Application
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(GestureUiState())
    val uiState: StateFlow<GestureUiState> = _uiState.asStateFlow()

    fun startService() {
        val intent = Intent(app, GestureForegroundService::class.java).apply {
            action = GestureForegroundService.ACTION_START
        }
        app.startForegroundService(intent)
        _uiState.value = _uiState.value.copy(isServiceRunning = true)
    }

    fun stopService() {
        val intent = Intent(app, GestureForegroundService::class.java).apply {
            action = GestureForegroundService.ACTION_STOP
        }
        app.startService(intent)
        _uiState.value = _uiState.value.copy(
            isServiceRunning = false,
            isCameraActive = false,
            isSensorActive = false
        )
    }

    fun toggleCamera() {
        val intent = Intent(app, GestureForegroundService::class.java).apply {
            action = GestureForegroundService.ACTION_TOGGLE_CAMERA
        }
        app.startService(intent)
        _uiState.value = _uiState.value.copy(
            isCameraActive = !_uiState.value.isCameraActive
        )
    }

    fun toggleSensor() {
        val intent = Intent(app, GestureForegroundService::class.java).apply {
            action = GestureForegroundService.ACTION_TOGGLE_SENSOR
        }
        app.startService(intent)
        _uiState.value = _uiState.value.copy(
            isSensorActive = !_uiState.value.isSensorActive
        )
    }

    fun updatePermissionStatus(accessibility: Boolean, overlay: Boolean) {
        _uiState.value = _uiState.value.copy(
            isAccessibilityEnabled = accessibility,
            isOverlayPermissionGranted = overlay
        )
    }

    fun syncServiceState() {
        _uiState.value = _uiState.value.copy(
            isServiceRunning = GestureForegroundService.isRunning,
            isCameraActive = GestureForegroundService.isCameraActive,
            isSensorActive = GestureForegroundService.isSensorActive
        )
    }
}
