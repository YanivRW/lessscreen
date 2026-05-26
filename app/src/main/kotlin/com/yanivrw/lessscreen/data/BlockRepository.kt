package com.yanivrw.lessscreen.data

import android.content.Context
import android.content.SharedPreferences
import com.yanivrw.lessscreen.data.models.BlockSchedule
import com.yanivrw.lessscreen.supabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// CRC: crc-BlockRepository.md | Seq: seq-schedule-sync.md | R7, R9, R23
object BlockRepository {

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    private val _schedules = MutableStateFlow<List<BlockSchedule>>(emptyList())
    val schedules: StateFlow<List<BlockSchedule>> = _schedules.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("block_queue", Context.MODE_PRIVATE)
    }

    // Seq: seq-schedule-sync.md#1.3
    suspend fun loadSchedules() {
        val userId = AuthRepository.currentUserId() ?: run {
            _schedules.value = emptyList()
            return
        }
        flushQueue()
        _schedules.value = supabase.from("blocking_schedules")
            .select { filter { eq("user_id", userId) } }
            .decodeList<BlockSchedule>()
    }

    // Seq: seq-schedule-sync.md#1.3 (online) / seq-schedule-sync.md#2.5 (offline)
    suspend fun saveSchedule(schedule: BlockSchedule) {
        try {
            supabase.from("blocking_schedules").upsert(schedule)
            loadSchedules()
        } catch (_: Exception) {
            enqueue(PendingOp("upsert", json.encodeToString(schedule)))
            updateCache(schedule)
        }
    }

    // Seq: seq-schedule-sync.md#3.6
    suspend fun deleteSchedule(id: String) {
        runCatching {
            supabase.from("blocking_schedules").delete { filter { eq("id", id) } }
        }.onFailure {
            enqueue(PendingOp("delete", id))
        }
        _schedules.value = _schedules.value.filter { it.id != id }
    }

    // Seq: seq-schedule-sync.md#2.13
    suspend fun flushQueue() {
        val ops = dequeueAll()
        if (ops.isEmpty()) return
        for (op in ops) {
            runCatching {
                when (op.type) {
                    "upsert" -> supabase.from("blocking_schedules")
                        .upsert(json.decodeFromString<BlockSchedule>(op.data))
                    "delete" -> supabase.from("blocking_schedules")
                        .delete { filter { eq("id", op.data) } }
                }
            }.onFailure { return }
        }
        clearQueue()
    }

    private fun updateCache(schedule: BlockSchedule) {
        val list = _schedules.value.toMutableList()
        val idx = list.indexOfFirst { it.id == schedule.id }
        if (idx >= 0) list[idx] = schedule else list.add(schedule)
        _schedules.value = list
    }

    private fun enqueue(op: PendingOp) {
        if (!::prefs.isInitialized) return
        val ops = dequeueAll().toMutableList().also { it.add(op) }
        prefs.edit().putString("queue", json.encodeToString(ops)).apply()
    }

    private fun dequeueAll(): List<PendingOp> {
        if (!::prefs.isInitialized) return emptyList()
        return prefs.getString("queue", null)
            ?.let { runCatching { json.decodeFromString<List<PendingOp>>(it) }.getOrNull() }
            ?: emptyList()
    }

    private fun clearQueue() {
        if (!::prefs.isInitialized) return
        prefs.edit().remove("queue").apply()
    }

    @Serializable
    private data class PendingOp(val type: String, val data: String)
}
