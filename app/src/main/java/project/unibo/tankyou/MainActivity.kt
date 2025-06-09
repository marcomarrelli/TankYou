package project.unibo.tankyou

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

            Screen.MAP -> {
                AuthenticatedContent(
                    onNavigateToProfile = { currentScreen = Screen.PROFILE },
                    onLogout = { currentScreen = Screen.LOGIN }
                )
            }

            Screen.PROFILE -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Profilo") },
                            navigationIcon = {
                                IconButton(onClick = { currentScreen = Screen.MAP }) {
                                    Icon(Icons.Default.LocationOn, contentDescription = "Mappa")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        ProfileScreen(
                            onLogout = { currentScreen = Screen.LOGIN },
                            authViewModel = authViewModel
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AuthenticatedContent(
        onNavigateToProfile: () -> Unit,
        onLogout: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("TankYou") },
                    actions = {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Profilo")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                AndroidView(
                    factory = { context ->
                        RelativeLayout(context).apply {
                            mapComponent = MapComponent(this@MainActivity, this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
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