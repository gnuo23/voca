package com.voca.mobile.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.voca.mobile.ui.components.LoadingState
import com.voca.mobile.ui.screens.AuthScreen
import com.voca.mobile.ui.screens.ClassesScreen
import com.voca.mobile.ui.screens.DashboardScreen
import com.voca.mobile.ui.screens.DeckDetailScreen
import com.voca.mobile.ui.screens.DecksScreen
import com.voca.mobile.ui.screens.DifficultScreen
import com.voca.mobile.ui.screens.LearnScreen
import com.voca.mobile.ui.screens.ProfileScreen
import com.voca.mobile.ui.screens.ReviewScreen
import com.voca.mobile.ui.screens.WordDetailScreen

object Routes {
    const val HOME = "home"
    const val DECKS = "decks"
    const val REVIEW = "review"
    const val DIFFICULT = "difficult"
    const val CLASSES = "classes"
    const val PROFILE = "profile"
    const val DECK_DETAIL = "deck/{deckId}"
    const val WORD_DETAIL = "word/{vocabId}"
    const val LEARN = "learn/{deckId}"
    const val REVIEW_DECK = "review/deck/{deckId}"

    fun deckDetail(deckId: Long) = "deck/$deckId"
    fun wordDetail(vocabId: Long) = "word/$vocabId"
    fun learn(deckId: Long) = "learn/$deckId"
    fun reviewDeck(deckId: Long) = "review/deck/$deckId"
}

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val tabs = listOf(
    TabItem(Routes.HOME, "Home", Icons.Filled.Home),
    TabItem(Routes.DECKS, "Bộ thẻ", Icons.Filled.Style),
    TabItem(Routes.REVIEW, "Ôn tập", Icons.Filled.Autorenew),
    TabItem(Routes.DIFFICULT, "Từ khó", Icons.Filled.LocalFireDepartment),
    TabItem(Routes.CLASSES, "Lớp", Icons.Filled.Groups),
    TabItem(Routes.PROFILE, "Tôi", Icons.Filled.Person),
)

@Composable
fun VocaApp(app: AppViewModel) {
    val authed by app.authed.collectAsStateWithLifecycle()
    when (authed) {
        null -> LoadingState()
        false -> AuthScreen(app = app, onAuthed = { app.onAuthSuccess(it) })
        true -> MainShell(app)
    }
}

@Composable
private fun MainShell(app: AppViewModel) {
    val navController = rememberNavController()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomBar(navController) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) { DashboardScreen(app, navController) }
            composable(Routes.DECKS) { DecksScreen(app, navController) }
            composable(Routes.REVIEW) { ReviewScreen(app, navController) }
            composable(Routes.DIFFICULT) { DifficultScreen(app, navController) }
            composable(Routes.CLASSES) { ClassesScreen(app, navController) }
            composable(Routes.PROFILE) { ProfileScreen(app, navController) }
            composable(Routes.DECK_DETAIL) { entry ->
                val deckId = entry.arguments?.getString("deckId")?.toLongOrNull() ?: return@composable
                DeckDetailScreen(app, navController, deckId)
            }
            composable(Routes.WORD_DETAIL) { entry ->
                val vocabId = entry.arguments?.getString("vocabId")?.toLongOrNull() ?: return@composable
                WordDetailScreen(app, navController, vocabId)
            }
            composable(Routes.LEARN) { entry ->
                val deckId = entry.arguments?.getString("deckId")?.toLongOrNull() ?: return@composable
                LearnScreen(app, navController, deckId)
            }
            composable(Routes.REVIEW_DECK) { entry ->
                val deckId = entry.arguments?.getString("deckId")?.toLongOrNull() ?: return@composable
                ReviewScreen(app, navController, deckId)
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        tabs.forEach { tab ->
            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = {
                    Text(
                        tab.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
