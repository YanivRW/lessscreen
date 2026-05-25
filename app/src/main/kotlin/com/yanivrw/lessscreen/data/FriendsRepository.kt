package com.yanivrw.lessscreen.data

import androidx.compose.ui.graphics.Color
import com.yanivrw.lessscreen.supabase
import com.yanivrw.lessscreen.data.models.DailyUsageRow
import com.yanivrw.lessscreen.data.models.FriendRow
import com.yanivrw.lessscreen.data.models.LeaderboardEntry
import com.yanivrw.lessscreen.data.models.Profile
import com.yanivrw.lessscreen.data.models.UserHistory
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate

// Chart colours — index 0 is always "me"
val chartColors = listOf(
    Color(0xFF7CFF6B), // green  — me
    Color(0xFF6BB8FF), // blue
    Color(0xFFFF6B6B), // red
    Color(0xFFFFD700), // gold
    Color(0xFFB06BFF), // purple
    Color(0xFFFF9F6B), // orange
    Color(0xFF6BFFEA), // teal
)

object FriendsRepository {

    suspend fun addFriendByCode(code: String): Result<Profile> = runCatching {
        val me = AuthRepository.currentUserId() ?: error("Not signed in")
        val friend = supabase.from("profiles")
            .select { filter { eq("invite_code", code.trim().uppercase()) } }
            .decodeSingleOrNull<Profile>() ?: error("No user with that invite code")
        if (friend.id == me) error("That's your own code")
        supabase.from("friends").upsert(
            listOf(
                FriendRow(userId = me, friendId = friend.id),
                FriendRow(userId = friend.id, friendId = me),
            )
        )
        friend
    }

    suspend fun listFriends(): List<Profile> {
        val me = AuthRepository.currentUserId() ?: return emptyList()
        val rows = supabase.from("friends")
            .select { filter { eq("user_id", me) } }
            .decodeList<FriendRow>()
        if (rows.isEmpty()) return emptyList()
        val ids = rows.map { it.friendId }
        return supabase.from("profiles")
            .select { filter { isIn("id", ids) } }
            .decodeList()
    }

    /** Today's leaderboard: me + friends, ascending by minutes (lowest wins). */
    suspend fun leaderboardToday(): List<LeaderboardEntry> {
        val me = AuthRepository.currentUserId() ?: return emptyList()
        val friends = listFriends()
        val allIds = (friends.map { it.id } + me).distinct()
        val today = LocalDate.now().toString()
        val usage = supabase.from("daily_usage")
            .select { filter { isIn("user_id", allIds); eq("date", today) } }
            .decodeList<DailyUsageRow>()
            .associateBy { it.userId }
        val myProfile = AuthRepository.myProfile()
        val all = friends + listOfNotNull(myProfile)
        return all.map { p ->
            LeaderboardEntry(
                userId = p.id,
                displayName = p.displayName ?: p.email.substringBefore('@'),
                minutesToday = usage[p.id]?.totalMinutes ?: 0,
                isMe = (p.id == me),
            )
        }.sortedBy { it.minutesToday }
    }

    /**
     * Returns per-user history for the last 7 days (including today),
     * ready to be drawn as chart lines.
     */
    suspend fun usageHistory7Days(): List<UserHistory> {
        val me = AuthRepository.currentUserId() ?: return emptyList()
        val friends = listFriends()
        val allIds = (friends.map { it.id } + me).distinct()

        val today = LocalDate.now()
        val sevenDaysAgo = today.minusDays(6).toString()
        val dateRange = (0..6).map { today.minusDays((6 - it).toLong()).toString() }

        val rows = supabase.from("daily_usage")
            .select {
                filter {
                    isIn("user_id", allIds)
                    gte("date", sevenDaysAgo)
                }
            }
            .decodeList<DailyUsageRow>()

        // Map: userId -> (date -> minutes)
        val byUser = rows.groupBy { it.userId }
            .mapValues { (_, v) -> v.associateBy { it.date } }

        val myProfile = AuthRepository.myProfile()
        val all = listOfNotNull(myProfile) + friends   // me first → green

        return all.mapIndexed { idx, profile ->
            UserHistory(
                userId = profile.id,
                displayName = profile.displayName ?: profile.email.substringBefore('@'),
                isMe = profile.id == me,
                dailyMinutes = dateRange.map { date ->
                    byUser[profile.id]?.get(date)?.totalMinutes ?: 0
                },
                color = chartColors[idx.coerceAtMost(chartColors.lastIndex)],
                dateLabels = dateRange.map { it.substring(8) }, // DD
            )
        }
    }
}
