package com.yanivrw.lessscreen.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.yanivrw.lessscreen.data.AppUsage
import com.yanivrw.lessscreen.data.UsageRepository
import com.yanivrw.lessscreen.permission.hasUsageAccess
import com.yanivrw.lessscreen.permission.openUsageAccessSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ResultsScreen() {
    val context = LocalContext.current
    val repo = remember { UsageRepository(context) }
    val scope = rememberCoroutineScope()

    var hasPermission by remember { mutableStateOf(hasUsageAccess(context)) }
    var usage by remember { mutableStateOf<List<AppUsage>>(emptyList()) }

    // Re-check permission + load on every resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = hasUsageAccess(context)
                if (hasPermission) {
                    usage = repo.usageToday()
                    scope.launch { runCatching { repo.uploadToday() } }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Auto-refresh every 60 seconds while the screen is visible.
    // usageToday() already queries from start of current calendar day,
    // so it resets automatically at midnight with no extra logic needed.
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            while (true) {
                delay(60_000)
                usage = repo.usageToday()
                scope.launch { runCatching { repo.uploadToday() } }
            }
        }
    }

    if (!hasPermission) {
        PermissionPrompt(onGrant = { openUsageAccessSettings(context) })
    } else {
        UsageList(usage)
    }
}

@Composable
private fun PermissionPrompt(onGrant: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "LessScreen needs Usage Access",
            fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Android requires you to enable this in Settings so the app " +
                "can read your screen time. Find LessScreen in the list and toggle it on.",
            color = Color(0xFFB0B0B0),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) { Text("Open Settings") }
    }
}

@Composable
private fun UsageList(usage: List<AppUsage>) {
    val total = usage.sumOf { it.minutes }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Today", color = Color(0xFFB0B0B0), fontSize = 14.sp)
        Text("$total min", color = Color.White, fontSize = 56.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(usage) { app ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(app.label, color = Color.White, fontSize = 18.sp)
                        Text(
                            "${app.minutes} min",
                            color = Color(0xFF7CFF6B),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}
