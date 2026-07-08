package com.englishworder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.englishworder.data.worker.ReviewReminderWorker
import com.englishworder.ui.games.LinkMatchGameScreen
import com.englishworder.ui.games.ListeningGameScreen
import com.englishworder.ui.games.QuizGameScreen
import com.englishworder.ui.games.SpellingGameScreen
import com.englishworder.ui.learn.LearnSessionScreen
import com.englishworder.ui.navigation.Routes
import com.englishworder.domain.model.StudyFilter
import com.englishworder.ui.progress.ProgressScreen
import com.englishworder.ui.review.GameListSelectScreen
import com.englishworder.ui.review.ReviewHubScreen
import com.englishworder.ui.splash.SplashScreen
import com.englishworder.ui.theme.EnglishWorderTheme
import com.englishworder.ui.wordlist.ImportScreen
import com.englishworder.ui.wordlist.WordDetailScreen
import com.englishworder.ui.wordlist.WordListScreen
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        scheduleReviewReminder()

        setContent {
            EnglishWorderTheme {
                EnglishWorderAppContent()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun scheduleReviewReminder() {
        val request = PeriodicWorkRequestBuilder<ReviewReminderWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReviewReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

private enum class BottomTab(val route: String, val label: String) {
    WORD_LISTS(Routes.WORD_LISTS, "词库"),
    PROGRESS(Routes.PROGRESS, "计划"),
    REVIEW(Routes.REVIEW, "复习")
}

@Composable
private fun EnglishWorderAppContent() {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    MainNavHost()
}

@Composable
private fun MainNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val showBottomBar = currentRoute in BottomTab.entries.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    when (tab) {
                                        BottomTab.WORD_LISTS -> Icons.Default.LibraryBooks
                                        BottomTab.PROGRESS -> Icons.Default.Assessment
                                        BottomTab.REVIEW -> Icons.Default.SportsEsports
                                    },
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.WORD_LISTS,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.WORD_LISTS) {
                WordListScreen(
                    onOpenList = { navController.navigate(Routes.wordListDetail(it)) },
                    onImport = { navController.navigate(Routes.importGlobal()) },
                    onStartLearn = { listId ->
                        navController.navigate(Routes.learnSession(listId))
                    }
                )
            }
            composable(
                route = Routes.WORD_LIST_DETAIL,
                arguments = listOf(navArgument("listId") { type = NavType.LongType })
            ) { entry ->
                val listId = entry.arguments?.getLong("listId") ?: return@composable
                WordDetailScreen(
                    listId = listId,
                    onBack = { navController.popBackStack() },
                    onImport = { navController.navigate(Routes.import(listId)) }
                )
            }
            composable(Routes.IMPORT) {
                ImportScreen(listId = 0, onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.IMPORT_TO_LIST,
                arguments = listOf(navArgument("listId") { type = NavType.LongType })
            ) { entry ->
                val listId = entry.arguments?.getLong("listId") ?: return@composable
                ImportScreen(
                    listId = listId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PROGRESS) {
                ProgressScreen(
                    onStartReview = { listId ->
                        navController.navigate(Routes.learnSession(listId, "plan"))
                    },
                    onStartLearn = { listId ->
                        navController.navigate(Routes.learnSession(listId))
                    }
                )
            }
            composable(
                route = Routes.LEARN_SESSION,
                arguments = listOf(
                    navArgument("listId") { type = NavType.LongType },
                    navArgument("filter") {
                        type = NavType.StringType
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { entry ->
                val listId = entry.arguments?.getLong("listId") ?: return@composable
                val filterArg = entry.arguments?.getString("filter")
                val filter = filterArg?.takeIf { it.isNotBlank() }?.let { StudyFilter.fromString(it) }
                LearnSessionScreen(
                    listId = listId,
                    initialFilter = filter,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.REVIEW) {
                ReviewHubScreen(
                    onSetupGame = { gameType ->
                        navController.navigate(Routes.gameListPick(gameType))
                    }
                )
            }
            composable(
                route = Routes.GAME_LIST_PICK,
                arguments = listOf(navArgument("gameType") { type = NavType.StringType })
            ) { entry ->
                val gameType = entry.arguments?.getString("gameType") ?: return@composable
                GameListSelectScreen(
                    gameType = gameType,
                    onStart = { listId ->
                        val route = when (gameType) {
                            "quiz" -> Routes.gameQuiz(listId)
                            "link" -> Routes.gameLink(listId)
                            "spelling" -> Routes.gameSpelling(listId)
                            "listening" -> Routes.gameListening(listId)
                            else -> return@GameListSelectScreen
                        }
                        navController.navigate(route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.GAME_QUIZ,
                arguments = listOf(navArgument("listId") { type = NavType.LongType })
            ) { entry ->
                QuizGameScreen(
                    listId = entry.arguments?.getLong("listId") ?: return@composable,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.GAME_LINK,
                arguments = listOf(navArgument("listId") { type = NavType.LongType })
            ) { entry ->
                LinkMatchGameScreen(
                    listId = entry.arguments?.getLong("listId") ?: return@composable,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.GAME_SPELLING,
                arguments = listOf(navArgument("listId") { type = NavType.LongType })
            ) { entry ->
                SpellingGameScreen(
                    listId = entry.arguments?.getLong("listId") ?: return@composable,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Routes.GAME_LISTENING,
                arguments = listOf(navArgument("listId") { type = NavType.LongType })
            ) { entry ->
                ListeningGameScreen(
                    listId = entry.arguments?.getLong("listId") ?: return@composable,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

