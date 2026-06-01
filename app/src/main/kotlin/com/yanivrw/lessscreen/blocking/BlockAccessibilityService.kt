package com.yanivrw.lessscreen.blocking

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.yanivrw.lessscreen.data.BlockRepository
import com.yanivrw.lessscreen.data.LockRepository
import com.yanivrw.lessscreen.data.TRACKED_PACKAGES
import com.yanivrw.lessscreen.data.models.BlockSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// CRC: crc-BlockService.md | Seq: seq-block-detection.md, seq-friend-lock-overlay.md
// R10, R11, R12, R23, R30, R33, R34, R38
class BlockAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val overlay by lazy { BlockOverlay(this) }

    // Tracks which locked schedules we already subscribed to for Realtime
    private val subscribedScheduleIds = mutableSetOf<String>()

    // Seq: seq-block-detection.md#1.1
    override fun onServiceConnected() {
        scope.launch {
            BlockRepository.loadSchedules()
            // Subscribe Realtime for any locked schedules already in cache
            BlockRepository.schedules.collectLatest { schedules ->
                schedules.filter { it.isFriendLocked }.forEach { subscribeIfNeeded(it) }
            }
        }
    }

    // Seq: seq-block-detection.md#1.4
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Our own overlay took focus — keep showing, don't hide
        if (pkg == packageName) return
        val tracked = TRACKED_PACKAGES.find { it.first == pkg }
        if (tracked == null) {
            overlay.hide()
            return
        }
        // Seq: seq-block-detection.md#1.6
        val schedule = BlockRepository.schedules.value
            .firstOrNull { it.packageName == pkg && it.isActiveNow() }
            ?: run { overlay.hide(); return }

        // Seq: seq-friend-lock-overlay.md#1.3 | R33, R34
        if (schedule.isFriendLocked && !schedule.isCurrentlyUnlocked()) {
            subscribeIfNeeded(schedule)
            // Seq: seq-friend-lock-overlay.md#1.5 | R30
            overlay.showLocked(
                appLabel = tracked.second,
                lockPartnerName = resolvePartnerName(schedule),
                scope = scope,
                onGoHome = { performGlobalAction(GLOBAL_ACTION_HOME) },
                // Seq: seq-friend-lock-overlay.md#1.11 — check-only, no state change
                onVerifyPin = { pin -> LockRepository.checkPin(schedule.id, pin) },
                // Seq: seq-friend-lock-overlay.md#1.12 — atomic verify + write unlocked_until
                onUnlock = { pin, durationMinutes ->
                    LockRepository.verifyPinAndUnlock(schedule.id, pin, durationMinutes)
                    BlockRepository.loadSchedules()
                    overlay.hide()
                },
            )
        } else {
            // Seq: seq-block-detection.md#1.7.1 | R12
            overlay.show(tracked.second) { performGlobalAction(GLOBAL_ACTION_HOME) }
        }
    }

    override fun onInterrupt() = overlay.hide()

    override fun onUnbind(intent: Intent?): Boolean {
        overlay.hide()
        scope.cancel()
        return super.onUnbind(intent)
    }

    // Seq: seq-friend-lock-overlay.md#1.8 | R38
    private fun subscribeIfNeeded(schedule: BlockSchedule) {
        if (schedule.id in subscribedScheduleIds) return
        subscribedScheduleIds.add(schedule.id)
        LockRepository.subscribeUnlockState(schedule.id, scope) { unlockedUntil ->
            // Update in-memory cache and re-evaluate overlay
            BlockRepository.updateUnlockedUntil(schedule.id, unlockedUntil)
        }
    }

    private fun resolvePartnerName(schedule: BlockSchedule): String {
        // Partner display name not in schedule row — show generic label until
        // FriendsRepository is wired. Future: store display_name in cache.
        return "your friend"
    }
}
