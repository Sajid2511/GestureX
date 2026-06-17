package com.gesturex.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gesturex.app.service.GestureForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            // Auto-start service after reboot
            val serviceIntent = Intent(context, GestureForegroundService::class.java).apply {
                action = GestureForegroundService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
