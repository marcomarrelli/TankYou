package project.unibo.tankyou

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import project.unibo.tankyou.components.MapComponent
import project.unibo.tankyou.data.database.models.AuthState
import project.unibo.tankyou.components.Screen
import project.unibo.tankyou.ui.TankYouTheme
import project.unibo.tankyou.ui.ThemeManager
import project.unibo.tankyou.ui.LoginScreen
import project.unibo.tankyou.ui.RegisterScreen
import project.unibo.tankyou.ui.ProfileScreen
import project.unibo.tankyou.components.AuthViewModel
import project.unibo.tankyou.utils.Constants

class MainActivity : AppCompatActivity() {

    private lateinit var mapComponent: MapComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                else -> { }
            }
        }

        when (currentScreen) {
            Screen.LOGIN -> {
                LoginScreen(
                    onNavigateToRegister = { currentScreen = Screen.REGISTER },
                    onLoginSuccess = { currentScreen = Screen.MAP },
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
        val permissions = Constants.APP_PERMISSIONS

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