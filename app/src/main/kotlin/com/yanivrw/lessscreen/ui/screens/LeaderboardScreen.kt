package com.yanivrw.lessscreen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanivrw.lessscreen.data.FriendsRepository
import com.yanivrw.lessscreen.data.models.LeaderboardEntry

@Composable
fun LeaderboardScreen() {
    var rows by remember { mutableStateOf<List<LeaderboardEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        runCatching { FriendsRepository.leaderboardToday() }
            .onSuccess { rows = it; error = null }
            .onFailure { error = it.message }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Scoreboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Today — lowest wins", color = Color(0xFFB0B0B0), fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))

        if (loading) {
            Text("Loading...", color = Color(0xFF888888))
        } else if (error != null) {
            Text(error!!, color = Color(0xFFFF6B6B))
        } else if (rows.isEmpty()) {
            Text("Add some friends first.", color = Color(0xFF888888))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(rows) { index, row -> RankRow(index + 1, row) }
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
            Text("$rank", color = Color(0xFF888888), fontSize = 18.sp,
                modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(12.dp))
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
