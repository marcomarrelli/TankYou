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
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.LocationOn
import androidx.compose.material.icons.twotone.Person
import androidx.compose.material.icons.twotone.Remove
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
                MapScreenWithFABs(
                    onNavigateToProfile = { currentScreen = Screen.PROFILE }
                )
            }

            Screen.PROFILE -> {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Profilo") },
                            navigationIcon = {
                                IconButton(onClick = { currentScreen = Screen.MAP }) {
                                    Icon(Icons.TwoTone.LocationOn, contentDescription = "Mappa")
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

    @Composable
    private fun MapScreenWithFABs(
        onNavigateToProfile: () -> Unit
    ) {
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
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 32.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallFloatingActionButton(
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

                    SmallFloatingActionButton(
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
                }
            }

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
                    SmallFloatingActionButton(
                        onClick = {
                            println("Search clicked")
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Search,
                            contentDescription = LocalContext.current.getString(R.string.search_icon_description)
                        )
                    }

                    SmallFloatingActionButton(
                        onClick = onNavigateToProfile,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Person,
                            contentDescription = LocalContext.current.getString(R.string.account_icon_description)
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