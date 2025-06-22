package project.unibo.tankyou.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import project.unibo.tankyou.data.database.auth.AuthState
import project.unibo.tankyou.data.database.auth.AuthViewModel
import project.unibo.tankyou.ui.screens.LoginScreen
import project.unibo.tankyou.ui.screens.MapScreen
import project.unibo.tankyou.ui.screens.ProfileScreen
import project.unibo.tankyou.ui.screens.RegisterScreen
import project.unibo.tankyou.ui.screens.Screen
import project.unibo.tankyou.ui.screens.SettingsScreen

@Composable
fun NavigationHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val isGuestMode by authViewModel.isGuestMode.collectAsState()

    val startDestination = when {
        authState is AuthState.Authenticated && !isGuestMode -> Screen.Map
        authState is AuthState.Unauthenticated && isGuestMode -> Screen.Map
        else -> Screen.Login
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Screen.Login> {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Map) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onContinueAsGuest = {
                    authViewModel.continueAsGuest()
                    navController.navigate(Screen.Map) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable<Screen.Register> {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.navigateUp()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Map) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable<Screen.Map> {
            MapScreen()
        }

        composable<Screen.Profile> {
            ProfileScreen(
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }

        composable<Screen.Settings> {
            SettingsScreen()
        }
    }
}