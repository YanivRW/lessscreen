package com.yanivrw.lessscreen.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanivrw.lessscreen.data.AuthRepository
import com.yanivrw.lessscreen.data.FriendsRepository
import com.yanivrw.lessscreen.data.LockRepository
import com.yanivrw.lessscreen.data.TRACKED_PACKAGES
import com.yanivrw.lessscreen.data.models.LockInfo
import com.yanivrw.lessscreen.data.models.Profile
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Seq: seq-friend-lock-setup.md#2.1, seq-friend-lock-setup.md#3.1 | R35, R36, R37
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var me by remember { mutableStateOf<Profile?>(null) }
    var friends by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var managedLocks by remember { mutableStateOf<List<LockInfo>>(emptyList()) }
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var setPinFor by remember { mutableStateOf<LockInfo?>(null) }

    suspend fun reload() {
        me = AuthRepository.myProfile()
        friends = FriendsRepository.listFriends()
        managedLocks = LockRepository.loadManagedLocks()
    }

    LaunchedEffect(Unit) { runCatching { reload() } }

    val appLabels = TRACKED_PACKAGES.toMap()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            Text("Friends", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
        }

        // Invite code card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Your invite code", color = Color(0xFFB0B0B0), fontSize = 14.sp)
                    Text(
                        me?.inviteCode ?: "...",
                        color = Color(0xFF7CFF6B),
                        fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { me?.inviteCode?.let { copyToClipboard(context, it) } }) {
                            Text("Copy")
                        }
                        TextButton(onClick = { me?.inviteCode?.let { shareCode(context, it) } }) {
                            Text("Share")
                        }
                        TextButton(onClick = { scope.launch { AuthRepository.signOut() } }) {
                            Text("Sign out", color = Color(0xFFFF6B6B))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Add friend by code
        item {
            Text("Add a friend by code", color = Color(0xFFB0B0B0), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase().take(8); message = null },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            val res = FriendsRepository.addFriendByCode(code)
                            message = res.fold(
                                onSuccess = { "Added ${it.displayName ?: it.email}" },
                                onFailure = { it.message ?: "Failed" },
                            )
                            if (res.isSuccess) {
                                code = ""
                                runCatching { reload() }
                            }
                        }
                    },
                    enabled = code.length >= 4,
                ) { Text("Add") }
            }
            if (message != null) {
                Spacer(Modifier.height(8.dp))
                Text(message!!, color = Color(0xFFB0B0B0))
            }
            Spacer(Modifier.height(20.dp))
        }

        // Friends list
        item {
            Text("Your friends", color = Color(0xFFB0B0B0), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
        }
        if (friends.isEmpty()) {
            item { Text("No friends yet.", color = Color(0xFF888888)) }
        } else {
            items(friends) { f ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                ) {
                    Text(
                        f.displayName ?: f.email,
                        color = Color.White, fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        // Locks I manage — Seq: seq-friend-lock-setup.md#2.6 | R35, R36, R37
        if (managedLocks.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("Locks I manage", color = Color(0xFFB0B0B0), fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
            }
            items(managedLocks, key = { it.scheduleId }) { lock ->
                ManagedLockCard(
                    lock = lock,
                    appLabel = appLabels[lock.packageName] ?: lock.packageName,
                    onSetPin = { setPinFor = lock },
                    onRelock = {
                        scope.launch {
                            runCatching { LockRepository.relock(lock.scheduleId) }
                            runCatching { reload() }
                        }
                    },
                    onRemove = {
                        scope.launch {
                            runCatching { LockRepository.removeLock(lock.scheduleId) }
                            runCatching { reload() }
                        }
                    },
                )
            }
        }
    }

    val currentLock = setPinFor
    if (currentLock != null) {
        SetPinSheet(
            lock = currentLock,
            appLabel = appLabels[currentLock.packageName] ?: currentLock.packageName,
            onDismiss = { setPinFor = null },
            onSave = { rawPin ->
                scope.launch {
                    runCatching { LockRepository.setPin(currentLock.scheduleId, rawPin) }
                    setPinFor = null
                    runCatching { reload() }
                }
            },
        )
    }
}

// Seq: seq-friend-lock-setup.md#2.6 | R35, R36, R37
@Composable
private fun ManagedLockCard(
    lock: LockInfo,
    appLabel: String,
    onSetPin: () -> Unit,
    onRelock: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔒", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "${lock.lockedUserDisplayName} → $appLabel",
                        color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        if (lock.hasPinSet) lockStatusText(lock.unlockedUntil) else "Waiting for you to set PIN",
                        color = Color(0xFF888888), fontSize = 13.sp,
                    )
                }
            }
            if (!lock.hasPinSet) {
                // Seq: seq-friend-lock-setup.md#2.7 | R27
                Button(
                    onClick = onSetPin,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                ) { Text("Set PIN", color = Color.White) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Seq: seq-friend-lock-overlay.md#2.3 | R36
                    Button(
                        onClick = onRelock,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                    ) { Text("Re-lock", color = Color.White) }
                    // Seq: seq-friend-lock-setup.md#3.1 | R37
                    Button(
                        onClick = onRemove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A1A1A)),
                    ) { Text("Remove lock", color = Color(0xFFFF6B6B)) }
                }
            }
        }
    }
}

// Seq: seq-friend-lock-setup.md#2.8 | R27, R29
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetPinSheet(
    lock: LockInfo,
    appLabel: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color(0xFF1C1C1F)) {
        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Set unlock code for ${lock.lockedUserDisplayName}'s $appLabel",
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            )
            Text(
                "Choose a 6-digit PIN that ${lock.lockedUserDisplayName} will need to enter to unlock the app.",
                color = Color(0xFF888888), fontSize = 14.sp,
            )
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pin = it },
                singleLine = true,
                label = { Text("6-digit PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSave(pin) },
                enabled = pin.length == 6,
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30D158)),
            ) { Text("Confirm", color = Color.White) }
        }
    }
}

private fun lockStatusText(unlockedUntil: String?): String {
    if (unlockedUntil == null) return "Locked"
    if (unlockedUntil.startsWith("9999")) return "Unlocked indefinitely"
    return runCatching {
        val instant = Instant.parse(unlockedUntil)
        if (instant.isAfter(Instant.now())) {
            val dt = instant.atZone(ZoneId.systemDefault())
            "Unlocked until ${dt.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        } else "Locked"
    }.getOrDefault("Locked")
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("invite", text))
}

private fun shareCode(context: Context, code: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "Join me on LessScreen! Download: https://github.com/YanivRW/lessscreen/releases — " +
                "use my invite code: $code",
        )
    }
    context.startActivity(Intent.createChooser(intent, "Share invite"))
}
