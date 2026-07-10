package com.sleep.snore.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Playback : Route("playback") {
        const val RECORD_TEMPLATE = "playback/{recordId}"
        fun createRoute(recordId: Long) = "playback/$recordId"
    }
    data object Settings : Route("settings")
    data object Recording : Route("recording")
    data object Export : Route("export")
    data class Result(val recordId: Long = -1) : Route("result/{recordId}") {
        companion object {
            const val TEMPLATE = "result/{recordId}"
            fun createRoute(recordId: Long) = "result/$recordId"
        }
    }
    data object RiskAssessment : Route("risk_assessment")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Route.Home.route, "首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Route.Playback.route, "回放", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    BottomNavItem(Route.Settings.route, "设置", Icons.Filled.Settings, Icons.Outlined.Settings)
)
