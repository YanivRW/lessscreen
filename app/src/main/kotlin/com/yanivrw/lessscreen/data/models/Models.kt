package com.yanivrw.lessscreen.data.models

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

@Serializable
data class Profile(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("invite_code") val inviteCode: String,
)

@Serializable
data class DailyUsageRow(
    @SerialName("user_id") val userId: String,
    val date: String,                     // ISO yyyy-MM-dd
    @SerialName("total_minutes") val totalMinutes: Int,
)

@Serializable
data class FriendRow(
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String,
)

// Joined view (manually composed in repo) for leaderboard rows.
data class LeaderboardEntry(
    val userId: String,
    val displayName: String,
    val minutesToday: Int,
    val isMe: Boolean,
)

// One entry per user for the 7-day chart.
data class UserHistory(
    val userId: String,
    val displayName: String,
    val isMe: Boolean,
    val dailyMinutes: List<Int>,   // index 0 = 6 days ago … index 6 = today
    val color: Color,
    val dateLabels: List<String>,  // "DD" for each day
)

// CRC: crc-BlockSchedule.md | R1, R2, R3, R4, R5, R6, R8, R25, R32, R33, R39
@Serializable
data class BlockSchedule(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("package_name") val packageName: String = "",
    @SerialName("is_all_day") val isAllDay: Boolean = false,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("recurrence_days") val recurrenceDays: List<Int> = emptyList(),
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("locked_by_user_id") val lockedByUserId: String? = null,
    @SerialName("unlocked_until") val unlockedUntil: String? = null,   // R39
    @SerialName("unlock_requested_at") val unlockRequestedAt: String? = null, // R39
) {
    fun isActiveNow(): Boolean {
        if (!isEnabled) return false
        val now = LocalDateTime.now()
        val today = now.dayOfWeek.value
        if (recurrenceDays.isNotEmpty() && today !in recurrenceDays) return false
        if (isAllDay) return true
        val start = parseHm(startTime) ?: return false
        val end = parseHm(endTime) ?: return false
        val nowTime = now.toLocalTime()
        return if (start <= end) {
            nowTime >= start && nowTime < end
        } else {
            // overnight window (e.g. 22:00 → 06:00)
            nowTime >= start || nowTime < end
        }
    }

    // R33, R34: true when unlocked_until is in the future (or indefinite sentinel)
    fun isCurrentlyUnlocked(): Boolean {
        val until = unlockedUntil ?: return false
        if (until.startsWith("9999")) return true
        return runCatching {
            Instant.parse(until).isAfter(Instant.now())
        }.getOrDefault(false)
    }

    // R25: schedule has an active friend lock (partner assigned + PIN set implied)
    val isFriendLocked: Boolean get() = lockedByUserId != null

    private fun parseHm(value: String?): LocalTime? =
        value?.take(5)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
}

// CRC: crc-LockRepository.md | R35, R36, R37
data class LockInfo(
    val scheduleId: String,
    val lockedUserDisplayName: String,
    val packageName: String,
    val hasPinSet: Boolean,
    val unlockedUntil: String?,   // mirrors BlockSchedule.unlockedUntil
)
