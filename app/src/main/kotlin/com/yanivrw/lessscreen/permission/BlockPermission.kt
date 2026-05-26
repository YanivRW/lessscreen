package com.yanivrw.lessscreen.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.yanivrw.lessscreen.blocking.BlockAccessibilityService

// CRC: crc-PermissionGuide.md | Seq: seq-block-detection.md#2.2 | R16, R17
object BlockPermission {

    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun hasAccessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val target = "${context.packageName}/${BlockAccessibilityService::class.java.canonicalName}"
        return enabled.split(":").any { it.equals(target, ignoreCase = true) }
    }

    fun openOverlaySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
