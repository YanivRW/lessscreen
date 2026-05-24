package com.yanivrw.lessscreen.data

import android.app.usage.UsageStatsManager
import android.content.Context
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

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
