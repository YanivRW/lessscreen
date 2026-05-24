package com.yanivrw.lessscreen.data

import com.yanivrw.lessscreen.supabase
import com.yanivrw.lessscreen.data.models.DailyUsageRow
import com.yanivrw.lessscreen.data.models.FriendRow
import com.yanivrw.lessscreen.data.models.LeaderboardEntry
import com.yanivrw.lessscreen.data.models.Profile
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate

object FriendsRepository {

    /** Add a friend by their invite code. Creates rows in both directions so it's mutual. */
    suspend fun addFriendByCode(code: String): Result<Profile> = runCatching {
        val me = AuthRepository.currentUserId()
            ?: error("Not signed in")
        val friend = supabase.from("profiles")
            .select { filter { eq("invite_code", code.trim().uppercase()) } }
            .decodeSingleOrNull<Profile>()
            ?: error("No user with that invite code")

        if (friend.id == me) error("That's your own code")

        // Upsert both directions to make it mutual + idempotent.
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

    /** Today's leaderboard: me + my friends, ascending by minutes. */
    suspend fun leaderboardToday(): List<LeaderboardEntry> {
        val me = AuthRepository.currentUserId() ?: return emptyList()
        val friends = listFriends()
        val allIds = (friends.map { it.id } + me).distinct()
        val today = LocalDate.now().toString()

        val usage = supabase.from("daily_usage")
            .select {
                filter {
                    isIn("user_id", allIds)
                    eq("date", today)
                }
            }
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
}
