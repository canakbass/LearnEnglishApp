package com.app.wordlearn.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Uygulamanın tüm navigasyon hedefleri.
 *
 * Tek bir `NavController` üzerinden kullanılır; tip-güvenlik için route string'leri
 * burada toplanmıştır. Yeni bir ekran eklerken bu sınıfa eklemen yeterli.
 */
sealed class Screen(val route: String, val title: String) {

    // ---------- Auth alt-grafı ----------
    data object Login : Screen("login", "Giriş Yap")
    data object Register : Screen("register", "Kayıt Ol")

    // ---------- Bottom-nav (ana) ekranları ----------
    data object Home : Screen("home", "Ana Sayfa")
    data object Words : Screen("words", "Kelimeler")
    data object Analytics : Screen("analytics", "Analiz")
    data object Settings : Screen("settings", "Ayarlar")

    // ---------- Detay / oyun ekranları ----------
    data object Quiz : Screen("quiz", "Günlük Quiz")
    data object Wordle : Screen("wordle", "Wordle")
    data object WordChain : Screen("wordchain", "Word Chain")
    data object SavedStories : Screen("saved_stories", "Kayıtlı Hikayeler")
    data object AddWord : Screen("addword", "Kelime Ekle")

    companion object {
        const val GRAPH_AUTH = "graph_auth"
        const val GRAPH_MAIN = "graph_main"
    }
}

/** Bottom navigation barında görünecek sekmeler (icon dahil). */
data class BottomNavItem(val screen: Screen, val icon: ImageVector)

val bottomNavItems: List<BottomNavItem> = listOf(
    BottomNavItem(Screen.Home, Icons.Default.Home),
    BottomNavItem(Screen.Words, Icons.Default.MenuBook),
    BottomNavItem(Screen.Analytics, Icons.Default.BarChart),
    BottomNavItem(Screen.Settings, Icons.Default.Settings),
)

private val bottomNavRouteSet: Set<String> = bottomNavItems.map { it.screen.route }.toSet()

/** Verilen rota bottom-nav sekmesi mi? */
fun String?.isBottomNavRoute(): Boolean = this != null && this in bottomNavRouteSet

/** Verilen rota için ekran başlığını döner (yoksa boş string). */
fun titleForRoute(route: String?): String = when (route) {
    Screen.Quiz.route -> Screen.Quiz.title
    Screen.Wordle.route -> Screen.Wordle.title
    Screen.WordChain.route -> Screen.WordChain.title
    Screen.SavedStories.route -> Screen.SavedStories.title
    Screen.AddWord.route -> Screen.AddWord.title
    else -> ""
}
