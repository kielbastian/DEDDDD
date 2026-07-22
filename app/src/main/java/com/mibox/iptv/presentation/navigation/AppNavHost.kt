package com.mibox.iptv.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mibox.iptv.presentation.live.LiveTvScreen
import com.mibox.iptv.presentation.player.PlayerScreen

/** Trasy aplikacji. Prosty graf pod nawigację pilotem (D-Pad). */
object Routes {
    const val LIVE = "live"
    const val PLAYER = "player/{sourceId}/{categoryId}/{index}"
    fun player(sourceId: Long, categoryId: String, index: Int) =
        "player/$sourceId/$categoryId/$index"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LIVE) {

        composable(Routes.LIVE) {
            LiveTvScreen(
                onChannelSelected = { sourceId, categoryId, index ->
                    navController.navigate(Routes.player(sourceId, categoryId, index))
                }
            )
        }

        composable(Routes.PLAYER) { backStack ->
            val sourceId = backStack.arguments?.getString("sourceId")?.toLongOrNull() ?: 0L
            val categoryId = backStack.arguments?.getString("categoryId").orEmpty()
            val index = backStack.arguments?.getString("index")?.toIntOrNull() ?: 0
            PlayerScreen(
                sourceId = sourceId,
                categoryId = categoryId.takeIf { it != "all" },
                startIndex = index,
                onExit = { navController.popBackStack() },
            )
        }
    }
}
