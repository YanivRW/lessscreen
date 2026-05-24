package com.yanivrw.lessscreen.data

data class AppUsage(
    val packageName: String,
    val label: String,
    val minutes: Long,
)

val TRACKED_PACKAGES: List<Pair<String, String>> = listOf(
    "com.instagram.android"      to "Instagram",
    "com.zhiliaoapp.musically"   to "TikTok",
    "com.google.android.youtube" to "YouTube",
    "com.facebook.katana"        to "Facebook",
    "com.twitter.android"        to "X",
    "com.linkedin.android"       to "LinkedIn",
)
