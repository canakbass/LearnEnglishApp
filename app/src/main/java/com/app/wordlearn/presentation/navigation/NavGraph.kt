package com.app.wordlearn.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.wordlearn.presentation.analytics.AnalyticsScreen
import com.app.wordlearn.presentation.analytics.AnalyticsViewModel
import com.app.wordlearn.presentation.auth.AuthViewModel
import com.app.wordlearn.presentation.auth.LoginScreen
import com.app.wordlearn.presentation.auth.RegisterScreen
import com.app.wordlearn.presentation.home.HomeScreen
import com.app.wordlearn.presentation.quiz.QuizScreen
import com.app.wordlearn.presentation.quiz.QuizViewModel
import com.app.wordlearn.presentation.settings.SettingsScreen
import com.app.wordlearn.presentation.settings.SettingsViewModel
import com.app.wordlearn.presentation.wordchain.WordChainScreen
import com.app.wordlearn.presentation.wordchain.WordChainViewModel
import com.app.wordlearn.presentation.wordle.WordleScreen
import com.app.wordlearn.presentation.wordle.WordleViewModel
import com.app.wordlearn.presentation.words.AddWordScreen
import com.app.wordlearn.presentation.words.WordListScreen
import com.app.wordlearn.presentation.words.WordListViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Ana Sayfa", Icons.Default.Home)
    data object Words : Screen("words", "Kelimeler", Icons.Default.MenuBook)
    data object Analytics : Screen("analytics", "Analiz", Icons.Default.BarChart)
    data object Settings : Screen("settings", "Ayarlar", Icons.Default.Settings)
}

val bottomNavItems = listOf(Screen.Home, Screen.Words, Screen.Analytics, Screen.Settings)

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    if (!authState.isLoggedIn) {
        // Auth Graph
        val authNavController = rememberNavController()
        NavHost(navController = authNavController, startDestination = "login") {
            composable("login") {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { authNavController.navigate("register") },
                    onLoginSuccess = { /* authState.isLoggedIn triggers recomposition */ }
                )
            }
            composable("register") {
                RegisterScreen(
                    authViewModel = authViewModel,
                    onNavigateToLogin = { authNavController.popBackStack() },
                    onRegisterSuccess = { /* authState.isLoggedIn triggers recomposition */ }
                )
            }
        }
    } else {
        // Main Graph with Bottom Navigation
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onStartQuiz = { navController.navigate("quiz") },
                        onNavigateToWordle = { navController.navigate("wordle") },
                        onNavigateToWordChain = { navController.navigate("wordchain") }
                    )
                }

                composable(Screen.Words.route) {
                    val viewModel: WordListViewModel = hiltViewModel()
                    WordListScreen(
                        viewModel = viewModel,
                        onAddWordClick = { navController.navigate("addword") }
                    )
                }

                composable(Screen.Analytics.route) {
                    val viewModel: AnalyticsViewModel = hiltViewModel()
                    AnalyticsScreen(viewModel = viewModel)
                }

                composable(Screen.Settings.route) {
                    val viewModel: SettingsViewModel = hiltViewModel()
                    SettingsScreen(viewModel = viewModel)
                }

                composable("quiz") {
                    val viewModel: QuizViewModel = hiltViewModel()
                    QuizScreen(viewModel = viewModel)
                }

                composable("wordle") {
                    val viewModel: WordleViewModel = hiltViewModel()
                    WordleScreen(viewModel = viewModel)
                }

                composable("wordchain") {
                    val viewModel: WordChainViewModel = hiltViewModel()
                    WordChainScreen(viewModel = viewModel)
                }

                composable("addword") {
                    val viewModel: WordListViewModel = hiltViewModel()
                    AddWordScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
