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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yanivrw.lessscreen.data.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun SignInScreen() {
    val context = LocalContext.current
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(Modifier.fillMaxSize(), color = Color(0xFF0E0E10)) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("LessScreen", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                if (isSignUp) "Create your account" else "Sign in to continue",
                color = Color(0xFFB0B0B0),
            )
            Spacer(Modifier.height(32.dp))

            // Google Sign-In button
            OutlinedButton(
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { AuthRepository.signInWithGoogle(context) }
                            .onFailure { error = it.message ?: "Google sign-in failed" }
                        busy = false
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text("Continue with Google", fontSize = 16.sp)
            }

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Divider(Modifier.weight(1f), color = Color(0xFF333333))
                Text("  or  ", color = Color(0xFF888888), fontSize = 12.sp)
                Divider(Modifier.weight(1f), color = Color(0xFF333333))
            }
            Spacer(Modifier.height(20.dp))

            // Email + password
            OutlinedTextField(
                value = email, onValueChange = { email = it; error = null },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it; error = null },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error!!, color = Color(0xFFFF6B6B), textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    busy = true; error = null
                    scope.launch {
                        runCatching {
                            if (isSignUp) AuthRepository.signUp(email.trim(), password)
                            else AuthRepository.signIn(email.trim(), password)
                        }.onFailure { error = it.message ?: "Failed" }
                        busy = false
                    }
                },
                enabled = !busy && email.isNotBlank() && password.length >= 6,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (isSignUp) "Sign up" else "Sign in")
                }
            }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { isSignUp = !isSignUp; error = null }) {
                Text(
                    if (isSignUp) "Already have an account? Sign in"
                    else "New here? Create an account",
                    color = Color(0xFF7CFF6B),
                )
            }
        }
    }
}
