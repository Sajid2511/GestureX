package com.gesturex.app.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.gesturex.app.ui.theme.*
import com.gesturex.app.viewmodel.GestureViewModel

@Composable
fun HomeScreen(
    viewModel: GestureViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Check permissions on resume
    LaunchedEffect(Unit) {
        val isAccessibility = isAccessibilityEnabled(context)
        val isOverlay = Settings.canDrawOverlays(context)
        viewModel.updatePermissionStatus(isAccessibility, isOverlay)
        viewModel.syncServiceState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBackground, DarkSurface2)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== HEADER =====
            HeaderSection()

            // ===== MAIN POWER BUTTON =====
            PowerButtonSection(
                isRunning = uiState.isServiceRunning,
                onToggle = {
                    if (uiState.isServiceRunning) viewModel.stopService()
                    else viewModel.startService()
                }
            )

            // ===== PERMISSIONS WARNING =====
            if (!uiState.isAccessibilityEnabled || !uiState.isOverlayPermissionGranted) {
                PermissionWarningCard(
                    accessibilityOk = uiState.isAccessibilityEnabled,
                    overlayOk = uiState.isOverlayPermissionGranted,
                    context = context
                )
            }

            // ===== DETECTION TOGGLES =====
            Text(
                "Detection Modes",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )

            DetectionToggleCard(
                icon = Icons.Default.CameraAlt,
                title = "Camera Gestures",
                subtitle = "Hand gestures via front camera",
                isActive = uiState.isCameraActive,
                onToggle = { viewModel.toggleCamera() },
                accentColor = CyanAccent
            )

            DetectionToggleCard(
                icon = Icons.Default.Sensors,
                title = "Sensor Gestures",
                subtitle = "Tilt, shake & double-tap",
                isActive = uiState.isSensorActive,
                onToggle = { viewModel.toggleSensor() },
                accentColor = PurpleLight
            )

            // ===== GESTURE GUIDE =====
            GestureGuideCard()

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ===== HEADER =====
@Composable
private fun HeaderSection() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(PurplePrimary, CyanAccent))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Gesture,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                "GestureX",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            )
            Text(
                "Control your phone with gestures",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

// ===== POWER BUTTON =====
@Composable
private fun PowerButtonSection(
    isRunning: Boolean,
    onToggle: () -> Unit
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isRunning) GreenSuccess else Color.White.copy(alpha = 0.15f),
        animationSpec = tween(400),
        label = "buttonColor"
    )
    val scale by animateFloatAsState(
        targetValue = if (isRunning) 1.05f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Glowing ring effect
            Box(contentAlignment = Alignment.Center) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(buttonColor.copy(alpha = 0.15f))
                )
                // Inner button
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier
                        .size(90.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(buttonColor)
                ) {
                    Icon(
                        if (isRunning) Icons.Default.Power else Icons.Default.PowerSettingsNew,
                        contentDescription = "Toggle Service",
                        tint = if (isRunning) Color.Black else Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                if (isRunning) "GestureX is ACTIVE" else "Tap to Activate",
                color = if (isRunning) GreenSuccess else Color.White.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                if (isRunning) "Gestures are being detected" else "Service is stopped",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 13.sp
            )
        }
    }
}

// ===== PERMISSION WARNING =====
@Composable
private fun PermissionWarningCard(
    accessibilityOk: Boolean,
    overlayOk: Boolean,
    context: android.content.Context
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = OrangeWarning.copy(alpha = 0.12f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = OrangeWarning, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Permissions Required", color = OrangeWarning, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            if (!accessibilityOk) {
                PermissionRow(
                    text = "Accessibility Service (for screen control)",
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                )
            }

            if (!overlayOk) {
                PermissionRow(
                    text = "Draw Over Other Apps (for overlay)",
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) {
            Text("Enable", color = OrangeWarning, fontSize = 12.sp)
        }
    }
}

// ===== DETECTION TOGGLE CARD =====
@Composable
private fun DetectionToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    onToggle: () -> Unit,
    accentColor: Color
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) accentColor.copy(alpha = 0.12f) else CardBackground,
        animationSpec = tween(300),
        label = "bgColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = if (isActive) 0.25f else 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = if (isActive) accentColor else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }

            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                )
            )
        }
    }
}

// ===== GESTURE GUIDE =====
@Composable
private fun GestureGuideCard() {
    val gestures = listOf(
        "✋" to "Open Palm → Play/Pause",
        "👆" to "Swipe Up → Next Reel",
        "👇" to "Swipe Down → Previous",
        "👈" to "Swipe Left → Go Back",
        "✊" to "Fist → Lock Screen",
        "🤏" to "Pinch → Volume",
        "✌️" to "Two Fingers → Screenshot",
        "📱" to "Shake → Go Home",
        "↩️" to "Tilt Left → Back"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Gesture Guide",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Divider(color = Color.White.copy(alpha = 0.08f))
            gestures.forEach { (emoji, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                }
            }
        }
    }
}

// ===== UTILS =====
private fun isAccessibilityEnabled(context: android.content.Context): Boolean {
    val service = "${context.packageName}/com.gesturex.app.service.GestureAccessibilityService"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.contains(service)
}