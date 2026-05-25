package com.yanivrw.lessscreen.data.models

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
