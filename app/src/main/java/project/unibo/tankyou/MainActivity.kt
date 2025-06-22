package project.unibo.tankyou

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import project.unibo.tankyou.data.database.auth.AuthState
import project.unibo.tankyou.data.database.auth.AuthViewModel
import project.unibo.tankyou.ui.navigation.NavigationHost
import project.unibo.tankyou.ui.screens.Screen
import project.unibo.tankyou.ui.theme.TankYouTheme
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.SettingsManager
import project.unibo.tankyou.utils.TankYouVocabulary
import project.unibo.tankyou.utils.getResourceString

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        ThemeManager.initialize(this)
        SettingsManager.initialize(this)

        Constants.initializeConstantLists()

        requestPermissions()

        setContent {
            TankYouTheme {
                TankYouVocabulary {
                    MainApp()
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainApp() {
        val authViewModel: AuthViewModel = viewModel()
        val authState by authViewModel.authState.collectAsState()
        val isGuestMode by authViewModel.isGuestMode.collectAsState()
        val navController = rememberNavController()
        val currentBackStackEntry by navController.currentBackStackEntryAsState()

        val showBottomBar = when {
            authState is AuthState.Authenticated && !isGuestMode -> true
            authState is AuthState.Unauthenticated && isGuestMode -> true
            else -> false
        }

        if (showBottomBar) {
            Scaffold(
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    BottomNavigationBar(
                        navController = navController,
                        currentDestination = currentBackStackEntry?.destination?.route
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = androidx.compose.ui.Modifier
                        .padding(paddingValues)
                        .background(ThemeManager.palette.background)
                        .imePadding()
                ) {
                    NavigationHost(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }
            }
        } else {
            NavigationHost(
                navController = navController,
                authViewModel = authViewModel
            )
        }
    }

    @Composable
    private fun BottomNavigationBar(
        navController: androidx.navigation.NavHostController,
        currentDestination: String?
    ) {
        NavigationBar(
            containerColor = ThemeManager.palette.primary
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                label = {
                    Text(
                        getResourceString(R.string.map_page_title)
                    )
                },
                selected = currentDestination == Screen.Map::class.qualifiedName,
                onClick = {
                    navController.navigate(Screen.Map) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemColors(
                    selectedIconColor = ThemeManager.palette.secondary,
                    selectedTextColor = ThemeManager.palette.secondary,
                    selectedIndicatorColor = ThemeManager.palette.primary,
                    unselectedIconColor = ThemeManager.palette.text,
                    unselectedTextColor = ThemeManager.palette.text,
                    disabledIconColor = ThemeManager.palette.disabledText,
                    disabledTextColor = ThemeManager.palette.disabledText
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                label = { Text(getResourceString(R.string.profile_page_title)) },
                selected = currentDestination == Screen.Profile::class.qualifiedName,
                onClick = {
                    navController.navigate(Screen.Profile) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemColors(
                    selectedIconColor = ThemeManager.palette.secondary,
                    selectedTextColor = ThemeManager.palette.secondary,
                    selectedIndicatorColor = ThemeManager.palette.primary,
                    unselectedIconColor = ThemeManager.palette.text,
                    unselectedTextColor = ThemeManager.palette.text,
                    disabledIconColor = ThemeManager.palette.disabledText,
                    disabledTextColor = ThemeManager.palette.disabledText
                )
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text(getResourceString(R.string.settings_page_title)) },
                selected = currentDestination == Screen.Settings::class.qualifiedName,
                onClick = {
                    navController.navigate(Screen.Settings) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemColors(
                    selectedIconColor = ThemeManager.palette.secondary,
                    selectedTextColor = ThemeManager.palette.secondary,
                    selectedIndicatorColor = ThemeManager.palette.primary,
                    unselectedIconColor = ThemeManager.palette.text,
                    unselectedTextColor = ThemeManager.palette.text,
                    disabledIconColor = ThemeManager.palette.disabledText,
                    disabledTextColor = ThemeManager.palette.disabledText
                )
            )
        }
    }

    private fun requestPermissions() {
        val permissions = Constants.App.PERMISSIONS

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
        }
    }
}