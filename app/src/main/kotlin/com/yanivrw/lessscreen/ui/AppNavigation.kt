package com.yanivrw.lessscreen.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yanivrw.lessscreen.data.AuthRepository
import kotlinx.coroutines.launch
import com.yanivrw.lessscreen.ui.screens.FriendsScreen
import com.yanivrw.lessscreen.ui.screens.LeaderboardScreen
import com.yanivrw.lessscreen.ui.screens.ResultsScreen
import com.yanivrw.lessscreen.ui.screens.SignInScreen
import io.github.jan.supabase.auth.status.SessionStatus

private object Routes {
    const val RESULTS = "results"
    const val FRIENDS = "friends"
    const val LEADERBOARD = "leaderboard"
}

@Composable
fun AppRoot() {
    val status by AuthRepository.sessionStatus.collectAsState(initial = SessionStatus.Initializing)
    when (status) {
        is SessionStatus.Authenticated -> SignedInApp()
        is SessionStatus.NotAuthenticated -> SignInScreen()
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignedInApp() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: Routes.RESULTS
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        Routes.RESULTS to "Today",
        Routes.LEADERBOARD to "Scoreboard",
        Routes.FRIENDS to "Friends",
    )

    Scaffold(
        containerColor = Color(0xFF0E0E10),
        topBar = {
            TopAppBar(
                title = { Text("LessScreen", color = Color.White) },
                actions = {
                    TextButton(onClick = { scope.launch { AuthRepository.signOut() } }) {
                        Text("Sign out", color = Color(0xFFFF6B6B))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0E0E10)),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1C1C1F)) {
                tabs.forEach { (route, label) ->
                    NavigationBarItem(
                        selected = current == route,
                        onClick = {
                            nav.navigate(route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Text(
                                label,
                                color = if (current == route) Color.White else Color(0xFF888888),
                            )
                        },
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.RESULTS,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.RESULTS) { ResultsScreen() }
            composable(Routes.LEADERBOARD) { LeaderboardScreen() }
            composable(Routes.FRIENDS) { FriendsScreen() }
        }
    }
}
