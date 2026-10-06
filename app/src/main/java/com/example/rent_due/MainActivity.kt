package com.example.rent_due

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.rent_due.ui.components.InterstitialAdManager
import com.example.rent_due.ui.screens.HomeScreen
import com.example.rent_due.ui.screens.RecordScreen
import com.example.rent_due.ui.screens.SplashScreen
import com.example.rent_due.ui.screens.StoriesScreen
import com.example.rent_due.ui.theme.RentDueTheme
import com.example.rent_due.ui.viewmodel.RentViewModel
import com.example.rent_due.ui.viewmodel.RentViewModelFactory

class MainActivity : ComponentActivity() {

    private val rentViewModel: RentViewModel by viewModels {
        RentViewModelFactory((application as RentApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        InterstitialAdManager.loadAd(this)
        setContent {
            RentDueTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RentAppNavGraph(viewModel = rentViewModel)
                }
            }
        }
    }
}

@Composable
fun RentAppNavGraph(viewModel: RentViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onHouseClick = { houseId ->
                    navController.navigate("stories/$houseId")
                }
            )
        }

        composable(
            route = "stories/{houseId}",
            arguments = listOf(
                navArgument("houseId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
            StoriesScreen(
                houseId = houseId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onRoomClick = { roomId ->
                    navController.navigate("record/$houseId/$roomId")
                }
            )
        }

        composable(
            route = "record/{houseId}/{roomId}",
            arguments = listOf(
                navArgument("houseId") { type = NavType.StringType },
                navArgument("roomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val houseId = backStackEntry.arguments?.getString("houseId") ?: ""
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            RecordScreen(
                houseId = houseId,
                roomId = roomId,
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
