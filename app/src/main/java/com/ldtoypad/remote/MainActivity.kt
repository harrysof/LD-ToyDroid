package com.ldtoypad.remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ldtoypad.remote.feature.connection.ConnectionScreen
import com.ldtoypad.remote.feature.connection.ConnectionViewModel
import com.ldtoypad.remote.feature.library.LibraryScreen
import com.ldtoypad.remote.feature.library.LibraryViewModel
import com.ldtoypad.remote.feature.pad.ToyPadScreen
import com.ldtoypad.remote.feature.pad.ToyPadViewModel
import com.ldtoypad.remote.theme.LDToyPadRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)

        val appContainer = (application as App).container

        setContent {
            LDToyPadRemoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(appContainer)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(container: AppContainer) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "connection") {
        composable("connection") {
            val viewModel = remember {
                ConnectionViewModel(container.connectionRepository, container.connectToServer)
            }
            ConnectionScreen(
                viewModel = viewModel,
                onConnected = {
                    navController.navigate("pad") {
                        popUpTo("connection") { inclusive = true }
                    }
                }
            )
        }

        composable("pad") {
            val viewModel = remember {
                ToyPadViewModel(
                    container.toyPadRepository,
                    container.catalogueRepository,
                    container.placeToken,
                    container.removeToken,
                    container.moveToken
                )
            }
            ToyPadScreen(
                viewModel = viewModel,
                onNavigateToLibrary = { slotIndex ->
                    navController.navigate("library/$slotIndex")
                }
            )
        }

        composable("library/{slotIndex}") { backStackEntry ->
            val slotIndex = backStackEntry.arguments?.getString("slotIndex")?.toIntOrNull() ?: 1
            val viewModel = remember {
                LibraryViewModel(container.catalogueRepository, container.createToken)
            }
            LibraryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
