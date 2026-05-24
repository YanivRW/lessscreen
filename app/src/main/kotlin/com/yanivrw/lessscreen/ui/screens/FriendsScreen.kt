package com.yanivrw.lessscreen.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanivrw.lessscreen.data.AuthRepository
import com.yanivrw.lessscreen.data.FriendsRepository
import com.yanivrw.lessscreen.data.models.Profile
import kotlinx.coroutines.launch

@Composable
fun FriendsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var me by remember { mutableStateOf<Profile?>(null) }
    var friends by remember { mutableStateOf<List<Profile>>(emptyList()) }
    var code by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        me = AuthRepository.myProfile()
        friends = FriendsRepository.listFriends()
    }

    LaunchedEffect(Unit) { runCatching { reload() } }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Friends", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))

        // My invite code
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
                    TextButton(onClick = {
                        me?.inviteCode?.let { copyToClipboard(context, it) }
                    }) { Text("Copy") }
                    TextButton(onClick = {
                        me?.inviteCode?.let { shareCode(context, it) }
                    }) { Text("Share") }
                    TextButton(onClick = {
                        scope.launch {
                            AuthRepository.signOut()
                        }
                    }) { Text("Sign out", color = Color(0xFFFF6B6B)) }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
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
        Text("Your friends", color = Color(0xFFB0B0B0), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        if (friends.isEmpty()) {
            Text("No friends yet.", color = Color(0xFF888888))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(friends) { f ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1F)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            f.displayName ?: f.email,
                            color = Color.White, fontSize = 16.sp,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
    }
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
