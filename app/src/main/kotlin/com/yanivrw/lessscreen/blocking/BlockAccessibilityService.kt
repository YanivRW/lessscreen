package com.yanivrw.lessscreen.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.yanivrw.lessscreen.data.BlockRepository
import com.yanivrw.lessscreen.data.TRACKED_PACKAGES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// CRC: crc-BlockService.md | Seq: seq-block-detection.md | R10, R11, R12, R23
class BlockAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val overlay by lazy { BlockOverlay(this) }

    // Seq: seq-block-detection.md#1.1
    override fun onServiceConnected() {
        scope.launch { BlockRepository.loadSchedules() }
    }

    // Seq: seq-block-detection.md#1.4
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Our own overlay stole focus — keep it showing, don't hide.
        if (pkg == packageName) return
        val tracked = TRACKED_PACKAGES.find { it.first == pkg }
        if (tracked == null) {
            overlay.hide()
            return
        }
        // Seq: seq-block-detection.md#1.6
        val blocked = BlockRepository.schedules.value.any { it.packageName == pkg && it.isActiveNow() }
        // Seq: seq-block-detection.md#1.7.1
        if (blocked) overlay.show(tracked.second) { performGlobalAction(GLOBAL_ACTION_HOME) }
        else overlay.hide()
    }

    override fun onInterrupt() = overlay.hide()

    override fun onUnbind(intent: Intent?): Boolean {
        overlay.hide()
        scope.cancel()
        return super.onUnbind(intent)
    }
}
