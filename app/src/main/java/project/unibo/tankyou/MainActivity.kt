package project.unibo.tankyou

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.MyLocation
import androidx.compose.material.icons.twotone.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import project.unibo.tankyou.components.MapComponent
import project.unibo.tankyou.data.database.auth.AuthState
import project.unibo.tankyou.data.database.auth.AuthViewModel
import project.unibo.tankyou.ui.screens.LoginScreen
import project.unibo.tankyou.ui.screens.ProfileScreen
import project.unibo.tankyou.ui.screens.RegisterScreen
import project.unibo.tankyou.ui.screens.Screen
import project.unibo.tankyou.ui.screens.SettingsScreen
import project.unibo.tankyou.ui.theme.TankYouTheme
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants

class MainActivity : AppCompatActivity() {

    private lateinit var mapComponent: MapComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        ThemeManager.initialize(this)

        requestPermissions()

        setContent {
            TankYouTheme {
                MainApp()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainApp() {
        val authViewModel: AuthViewModel = viewModel()
        val authState by authViewModel.authState.collectAsState()
        var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

        LaunchedEffect(authState) {
            when (authState) {
                is AuthState.Authenticated -> {
                    if (currentScreen == Screen.LOGIN || currentScreen == Screen.REGISTER) {
                        currentScreen = Screen.MAP
                    }
                }

                is AuthState.Unauthenticated -> {
                    currentScreen = Screen.LOGIN
                }

                else -> {}
            }
        }

        if (currentScreen == Screen.LOGIN || currentScreen == Screen.REGISTER) {
            when (currentScreen) {
                Screen.LOGIN -> {
                    LoginScreen(
                        onNavigateToRegister = { currentScreen = Screen.REGISTER },
                        onLoginSuccess = { currentScreen = Screen.MAP },
                        onContinueAsGuest = { currentScreen = Screen.MAP },
                        authViewModel = authViewModel
                    )
                }

                Screen.REGISTER -> {
                    RegisterScreen(
                        onNavigateToLogin = { currentScreen = Screen.LOGIN },
                        onRegisterSuccess = { currentScreen = Screen.MAP },
                        authViewModel = authViewModel
                    )
                }

                else -> {}
            }
        } else {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { screen -> currentScreen = screen }
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (currentScreen) {
                        Screen.MAP -> {
                            MapScreenWithFABs()
                        }

                        Screen.PROFILE -> {
                            ProfileScreen(
                                onLogout = { currentScreen = Screen.LOGIN },
                                authViewModel = authViewModel
                            )
                        }

                        Screen.SETTINGS -> {
                            SettingsScreen()
                        }

                        else -> {}
                    }
                }
            }
        }
    }

    @Composable
    private fun BottomNavigationBar(
        currentScreen: Screen,
        onScreenSelected: (Screen) -> Unit
    ) {
        NavigationBar {
            NavigationBarItem(
                icon = { Icon(Icons.Default.Map, contentDescription = "Mappa") },
                label = { Text("Map") },
                selected = currentScreen == Screen.MAP,
                onClick = { onScreenSelected(Screen.MAP) }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Person, contentDescription = "Profilo") },
                label = { Text("Profilo") },
                selected = currentScreen == Screen.PROFILE,
                onClick = { onScreenSelected(Screen.PROFILE) }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Impostazioni") },
                label = { Text("Impostazioni") },
                selected = currentScreen == Screen.SETTINGS,
                onClick = { onScreenSelected(Screen.SETTINGS) }
            )
        }
    }

    @Composable
    private fun MapScreenWithFABs() {
        var fabsVisible by remember { mutableStateOf(true) }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    RelativeLayout(context).apply {
                        mapComponent = MapComponent(
                            context = this@MainActivity,
                            mapContainer = this,
                            onMapClick = {
                                fabsVisible = !fabsVisible
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            AnimatedVisibility(
                visible = fabsVisible,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 32.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (::mapComponent.isInitialized) {
                                mapComponent.zoomIn()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Add,
                            contentDescription = LocalContext.current.getString(R.string.zoom_in_icon_description)
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            if (::mapComponent.isInitialized) {
                                mapComponent.zoomOut()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Remove,
                            contentDescription = LocalContext.current.getString(R.string.zoom_out_icon_description)
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            if (::mapComponent.isInitialized) {
                                mapComponent.centerOnMyLocation()
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.MyLocation,
                            contentDescription = "Centra sulla mia posizione"
                        )
                    }
                }
            }
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

    override fun onResume() {
        super.onResume()
        if (::mapComponent.isInitialized) {
            mapComponent.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mapComponent.isInitialized) {
            mapComponent.onPause()
        }
    }
}