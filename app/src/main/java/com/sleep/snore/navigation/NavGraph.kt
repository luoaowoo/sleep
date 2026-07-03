package com.sleep.snore.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.sleep.snore.ui.screen.home.HomeScreen
import com.sleep.snore.ui.screen.export.ExportScreen
import com.sleep.snore.ui.screen.playback.PlaybackScreen
import com.sleep.snore.ui.screen.recording.RecordingScreen
import com.sleep.snore.ui.screen.result.ResultScreen
import com.sleep.snore.ui.screen.risk.RiskAssessmentScreen
import com.sleep.snore.ui.screen.settings.SettingsScreen

@Composable
fun SleepNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Route.Home.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
        },
        exitTransition = {
            fadeOut(tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
        },
        popEnterTransition = {
            fadeIn(tween(300)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
        },
        popExitTransition = {
            fadeOut(tween(300)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
        }
    ) {
        composable(Route.Home.route) { HomeScreen(navController = navController) }
        composable(Route.Playback.route) { PlaybackScreen(navController = navController) }
        composable(Route.Settings.route) { SettingsScreen(navController = navController) }
        composable(Route.Recording.route) { RecordingScreen(navController = navController) }
        composable(Route.Export.route) { ExportScreen(navController = navController) }
        composable(
            route = Route.Result.TEMPLATE,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: -1L
            ResultScreen(navController = navController, recordId = recordId)
        }
        composable(Route.RiskAssessment.route) {
            RiskAssessmentScreen(navController = navController)
        }
    }
}
