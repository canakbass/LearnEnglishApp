package com.app.wordlearn.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.app.wordlearn.presentation.analytics.AnalyticsScreen
import com.app.wordlearn.presentation.analytics.AnalyticsViewModel
import com.app.wordlearn.presentation.auth.AuthViewModel
import com.app.wordlearn.presentation.auth.EmailVerificationScreen
import com.app.wordlearn.presentation.auth.LoginScreen
import com.app.wordlearn.presentation.auth.RegisterScreen
import com.app.wordlearn.presentation.components.LoadingScreen
import com.app.wordlearn.presentation.home.HomeScreen
import com.app.wordlearn.presentation.quiz.QuizScreen
import com.app.wordlearn.presentation.quiz.QuizViewModel
import com.app.wordlearn.presentation.settings.SettingsScreen
import com.app.wordlearn.presentation.settings.SettingsViewModel
import com.app.wordlearn.presentation.wordchain.SavedStoriesScreen
import com.app.wordlearn.presentation.wordchain.WordChainScreen
import com.app.wordlearn.presentation.wordchain.WordChainViewModel
import com.app.wordlearn.presentation.wordle.WordleScreen
import com.app.wordlearn.presentation.wordle.WordleViewModel
import com.app.wordlearn.presentation.words.AddWordScreen
import com.app.wordlearn.presentation.words.WordListScreen
import com.app.wordlearn.presentation.words.WordListViewModel

/**
 * Tek NavController + iki nested sub-graph (auth / main) + auth durumuna bağlı
 * top-level overlay'ler (loading, email verification).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()

    // Erken çıkışlar: bunlar bütün ekranı kaplayan tam ekran state'ler.
    when {
        authState.isLoading -> {
            LoadingScreen(); return
        }
        authState.needsEmailVerification -> {
            EmailVerificationScreen(
                authViewModel = authViewModel,
                email = authState.verificationEmail,
                onBackToLogin = { /* authState değişimi recomposition tetikler */ }
            )
            return
        }
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val isMainTab = currentRoute.isBottomNavRoute()

    // Auth state değişince doğru graph'a yönlendir (login → main, logout → auth).
    LaunchedEffect(authState.isLoggedIn) {
        val target = if (authState.isLoggedIn) Screen.GRAPH_MAIN else Screen.GRAPH_AUTH
        if (navController.currentDestination?.parent?.route != target) {
            navController.navigate(target) {
                // Tüm back stack'i temizle — auth/main arası geri tuşuyla geçilemesin.
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                val detailTitle = titleForRoute(currentRoute)
                if (detailTitle.isNotEmpty()) {
                    TopAppBar(
                        title = { Text(detailTitle, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            },
            bottomBar = {
                if (isMainTab) MainBottomBar(navController, currentRoute)
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (authState.isLoggedIn) Screen.GRAPH_MAIN else Screen.GRAPH_AUTH,
                modifier = Modifier.padding(innerPadding)
            ) {
                authGraph(navController, authViewModel)
                mainGraph(navController, authViewModel, userName = authState.userName, isGuest = authState.isGuest)
            }
        }
    }
}

@Composable
private fun MainBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = navController.currentBackStackEntry
                ?.destination
                ?.hierarchy
                ?.any { it.route == item.screen.route } == true ||
                currentRoute == item.screen.route

            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.screen.title) },
                label = { Text(item.screen.title) },
                selected = selected,
                onClick = {
                    navController.navigate(item.screen.route) {
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

/** Auth alt-grafı: login + register. */
private fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    navigation(startDestination = Screen.Login.route, route = Screen.GRAPH_AUTH) {
        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = { /* authState.isLoggedIn değişimi LaunchedEffect'i tetikler */ },
                onGuestLogin = { authViewModel.loginAsGuest() }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = { /* authState değişimi LaunchedEffect'i tetikler */ }
            )
        }
    }
}

/** Main alt-grafı: bottom-nav sekmeleri + detay ekranları. */
private fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userName: String,
    isGuest: Boolean = false
) {
    navigation(startDestination = Screen.Home.route, route = Screen.GRAPH_MAIN) {
        composable(Screen.Home.route) {
            HomeScreen(
                userName = userName,
                onStartQuiz = { navController.navigate(Screen.Quiz.route) },
                onNavigateToWordle = { navController.navigate(Screen.Wordle.route) },
                onNavigateToWordChain = { navController.navigate(Screen.WordChain.route) }
            )
        }
        composable(Screen.Words.route) {
            val vm: WordListViewModel = hiltViewModel()
            WordListScreen(
                viewModel = vm,
                onAddWordClick = { navController.navigate(Screen.AddWord.route) }
            )
        }
        composable(Screen.Analytics.route) {
            val vm: AnalyticsViewModel = hiltViewModel()
            AnalyticsScreen(viewModel = vm)
        }
        composable(Screen.Settings.route) {
            val vm: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = vm,
                userName = userName,
                onLogout = { authViewModel.logout() },
                onUpdateDisplayName = { authViewModel.updateDisplayName(it) },
                onDeleteAccount = { authViewModel.deleteAccount() },
                isGuest = isGuest
            )
        }
        composable(Screen.Quiz.route) {
            val vm: QuizViewModel = hiltViewModel()
            QuizScreen(viewModel = vm)
        }
        composable(Screen.Wordle.route) {
            val vm: WordleViewModel = hiltViewModel()
            WordleScreen(viewModel = vm)
        }
        composable(Screen.WordChain.route) {
            val vm: WordChainViewModel = hiltViewModel()
            WordChainScreen(
                viewModel = vm,
                onNavigateToSavedStories = { navController.navigate(Screen.SavedStories.route) }
            )
        }
        composable(Screen.SavedStories.route) {
            SavedStoriesScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.AddWord.route) {
            val vm: WordListViewModel = hiltViewModel()
            AddWordScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
