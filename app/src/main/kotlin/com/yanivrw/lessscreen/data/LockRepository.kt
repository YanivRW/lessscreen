package com.yanivrw.lessscreen.data

import com.yanivrw.lessscreen.data.models.LockInfo
import com.yanivrw.lessscreen.supabase
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// CRC: crc-LockRepository.md | Seq: seq-friend-lock-setup.md, seq-friend-lock-overlay.md
// R25, R26, R27, R28, R29, R32, R33, R35, R36, R37, R38, R39, R40
object LockRepository {

    // Seq: seq-friend-lock-setup.md#1.4 | R25
    suspend fun setLockPartner(scheduleId: String, friendUserId: String) {
        supabase.from("blocking_schedules").update(
            mapOf("locked_by_user_id" to friendUserId)
        ) { filter { eq("id", scheduleId) } }
    }

    // Seq: seq-friend-lock-setup.md#3.3 | R37
    suspend fun removeLock(scheduleId: String) {
        supabase.postgrest.rpc("remove_lock", buildJsonObject {
            put("p_schedule_id", scheduleId)
        })
    }

    // Seq: seq-friend-lock-setup.md#2.9 | R27, R29
    suspend fun setPin(scheduleId: String, rawPin: String) {
        supabase.postgrest.rpc("set_lock_pin", buildJsonObject {
            put("p_schedule_id", scheduleId)
            put("p_raw_pin", rawPin)
        })
    }

    // Seq: seq-friend-lock-overlay.md#1.11 | R28
    // Verify-only — no state change. Used to check PIN before duration is chosen.
    suspend fun checkPin(scheduleId: String, rawPin: String): Boolean {
        return runCatching {
            supabase.postgrest.rpc("check_lock_pin", buildJsonObject {
                put("p_schedule_id", scheduleId)
                put("p_raw_pin", rawPin)
            }).data.trim('"', ' ') == "true"
        }.getOrDefault(false)
    }

    // Seq: seq-friend-lock-overlay.md#1.11 | R28, R32
    // Verify PIN and write unlocked_until atomically.
    // durationMinutes: -1 = indefinite ("9999" sentinel).
    suspend fun verifyPinAndUnlock(scheduleId: String, rawPin: String, durationMinutes: Int): Boolean {
        return runCatching {
            supabase.postgrest.rpc("verify_lock_pin", buildJsonObject {
                put("p_schedule_id", scheduleId)
                put("p_raw_pin", rawPin)
                put("p_duration_mins", durationMinutes)
            }).data.trim('"', ' ') == "true"
        }.getOrDefault(false)
    }

    // Seq: seq-friend-lock-overlay.md#2.3 | R36
    suspend fun relock(scheduleId: String) {
        supabase.from("blocking_schedules").update(
            mapOf("unlocked_until" to null, "updated_at" to "now()")
        ) { filter { eq("id", scheduleId) } }
    }

    // Seq: seq-friend-lock-setup.md#2.3 | R35
    suspend fun loadManagedLocks(): List<LockInfo> {
        val myId = AuthRepository.currentUserId() ?: return emptyList()

        @Serializable
        data class Row(
            val id: String,
            @SerialName("package_name") val packageName: String,
            @SerialName("unlocked_until") val unlockedUntil: String? = null,
            @SerialName("user_id") val userId: String,
        )

        @Serializable
        data class ProfileRow(
            val id: String,
            @SerialName("display_name") val displayName: String? = null,
            val email: String,
        )

        val schedules = supabase.from("blocking_schedules")
            .select { filter { eq("locked_by_user_id", myId) } }
            .decodeList<Row>()

        if (schedules.isEmpty()) return emptyList()

        val ownerIds = schedules.map { it.userId }.distinct()
        val profiles = supabase.from("profiles")
            .select { filter { isIn("id", ownerIds) } }
            .decodeList<ProfileRow>()
            .associateBy { it.id }

        // Check which schedules have a PIN set via block_lock_pins
        // (we can query schedule ids we manage since we own the lock partner role;
        //  we never get the hash — just the presence of a row)
        @Serializable
        data class PinRow(@SerialName("schedule_id") val scheduleId: String)
        val pinnedIds = runCatching {
            supabase.from("block_lock_pins")
                .select { filter { isIn("schedule_id", schedules.map { it.id }) } }
                .decodeList<PinRow>()
                .map { it.scheduleId }
                .toSet()
        }.getOrDefault(emptySet())

        return schedules.map { row ->
            val profile = profiles[row.userId]
            LockInfo(
                scheduleId = row.id,
                lockedUserDisplayName = profile?.displayName ?: profile?.email ?: "Unknown",
                packageName = row.packageName,
                hasPinSet = row.id in pinnedIds,
                unlockedUntil = row.unlockedUntil,
            )
        }
    }

    // Seq: seq-friend-lock-overlay.md#1.8 | R38
    // Subscribes to Realtime changes on blocking_schedules for the given schedule;
    // calls onChanged with the new unlocked_until value whenever it changes.
    fun subscribeUnlockState(
        scheduleId: String,
        scope: CoroutineScope,
        onChanged: (unlockedUntil: String?) -> Unit,
    ) {
        val channel = supabase.realtime.channel("lock-$scheduleId")
        channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
            table = "blocking_schedules"
        }.onEach { action ->
            val rowId = action.record["id"]?.toString()?.trim('"')
            if (rowId == scheduleId) {
                val until = action.record["unlocked_until"]?.toString()?.trim('"')
                onChanged(until)
            }
        }.launchIn(scope)
        scope.launch { supabase.realtime.connect() }
    }
}
