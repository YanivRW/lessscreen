package com.yanivrw.lessscreen.data.models

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.LocalTime

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

// CRC: crc-BlockSchedule.md | R1, R2, R3, R4, R5, R6, R8
@Serializable
data class BlockSchedule(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("package_name") val packageName: String = "",
    @SerialName("is_all_day") val isAllDay: Boolean = false,
    @SerialName("start_time") val startTime: String? = null,  // "HH:mm" or "HH:mm:ss"
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("recurrence_days") val recurrenceDays: List<Int> = emptyList(), // 1=Mon..7=Sun; empty=every day
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("locked_by_user_id") val lockedByUserId: String? = null, // R8: reserved for friend-lock
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

    private fun parseHm(value: String?): LocalTime? =
        value?.take(5)?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
}
