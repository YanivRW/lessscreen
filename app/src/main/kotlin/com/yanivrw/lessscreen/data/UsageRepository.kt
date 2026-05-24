package com.yanivrw.lessscreen.data

import android.app.usage.UsageStatsManager
import android.content.Context
import com.yanivrw.lessscreen.supabase
import com.yanivrw.lessscreen.data.models.DailyUsageRow
import io.github.jan.supabase.postgrest.from
import java.time.LocalDate
import java.util.Calendar
import java.util.concurrent.TimeUnit

class UsageRepository(context: Context) {

    private val usm =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun usageToday(): List<AppUsage> {
        val now = System.currentTimeMillis()
        val stats = usm.queryAndAggregateUsageStats(startOfToday(), now)
        return TRACKED_PACKAGES.map { (pkg, label) ->
            val ms = stats[pkg]?.totalTimeInForeground ?: 0L
            AppUsage(pkg, label, TimeUnit.MILLISECONDS.toMinutes(ms))
        }
    }

    /** Push today's total to Supabase. No-op if not signed in. */
    suspend fun uploadToday() {
        val userId = AuthRepository.currentUserId() ?: return
        val total = usageToday().sumOf { it.minutes }.toInt()
        supabase.from("daily_usage").upsert(
            DailyUsageRow(
                userId = userId,
                date = LocalDate.now().toString(),
                totalMinutes = total,
            )
        )
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
