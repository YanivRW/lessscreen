package com.yanivrw.lessscreen.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanivrw.lessscreen.data.FriendsRepository
import com.yanivrw.lessscreen.data.models.LeaderboardEntry
import com.yanivrw.lessscreen.data.models.UserHistory

@Composable
fun LeaderboardScreen() {
    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var history by remember { mutableStateOf<List<UserHistory>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        runCatching {
            leaderboard = FriendsRepository.leaderboardToday()
            history = FriendsRepository.usageHistory7Days()
        }.onFailure { error = it.message }
        loading = false
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Scoreboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Lowest wins · 7-day trend", color = Color(0xFFB0B0B0), fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
        }

        // Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("7 Days", color = Color(0xFFB0B0B0), fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    if (history.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().height(160.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (loading) "Loading..." else "No data yet",
                                color = Color(0xFF555555),
                            )
                        }
                    } else {
                        UsageLineChart(history = history)
                        Spacer(Modifier.height(12.dp))
                        ChartLegend(history = history)
                    }
                }
            }
        }

        // Today's ranking
        item {
            Text("Today", color = Color(0xFFB0B0B0), fontSize = 14.sp)
        }

        if (loading) {
            item { Text("Loading...", color = Color(0xFF888888)) }
        } else if (error != null) {
            item { Text(error!!, color = Color(0xFFFF6B6B)) }
        } else if (leaderboard.isEmpty()) {
            item { Text("Add friends to see the scoreboard.", color = Color(0xFF888888)) }
        } else {
            itemsIndexed(leaderboard) { index, row -> RankRow(index + 1, row) }
        }
    }
}

@Composable
private fun UsageLineChart(history: List<UserHistory>) {
    val maxMinutes = history.flatMap { it.dailyMinutes }.maxOrNull()?.coerceAtLeast(10) ?: 60

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val chartW = size.width
        val chartH = size.height - 24.dp.toPx()  // leave room for day labels
        val days = 7
        val xStep = chartW / (days - 1).toFloat()
        val yScale = chartH / maxMinutes.toFloat()

        // Horizontal grid lines
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = chartH - (i * chartH / gridCount)
            drawLine(
                color = Color(0xFF2A2A2E),
                start = Offset(0f, y),
                end = Offset(chartW, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // Day labels on X axis
        val labels = history.firstOrNull()?.dateLabels ?: return@Canvas
        labels.forEachIndexed { i, label ->
            val x = i * xStep
            drawContext.canvas.nativeCanvas.drawText(
                label,
                x,
                size.height,
                android.graphics.Paint().apply {
                    color = Color(0xFF666666).toArgb()
                    textSize = 10.dp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                },
            )
        }

        // Line per user
        history.forEach { user ->
            val path = Path()
            user.dailyMinutes.forEachIndexed { i, minutes ->
                val x = i * xStep
                val y = chartH - (minutes * yScale)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = user.color,
                style = Stroke(
                    width = if (user.isMe) 3.dp.toPx() else 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
            // Dot on today (last point)
            val lastX = (days - 1) * xStep
            val lastY = chartH - (user.dailyMinutes.last() * yScale)
            drawCircle(
                color = user.color,
                radius = if (user.isMe) 5.dp.toPx() else 4.dp.toPx(),
                center = Offset(lastX, lastY),
            )
        }
    }
}

@Composable
private fun ChartLegend(history: List<UserHistory>) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        history.forEach { user ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(user.color)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    user.displayName + if (user.isMe) " (you)" else "",
                    color = Color(0xFFB0B0B0),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun RankRow(rank: Int, entry: LeaderboardEntry) {
    val bg = if (entry.isMe) Color(0xFF243024) else Color(0xFF1C1C1F)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "#$rank" },
                color = Color(0xFF888888), fontSize = 18.sp,
                modifier = Modifier.width(36.dp),
            )
            Text(
                entry.displayName + if (entry.isMe) " (you)" else "",
                color = Color.White, fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${entry.minutesToday} min",
                color = Color(0xFF7CFF6B),
                fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
