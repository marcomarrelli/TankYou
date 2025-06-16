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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.MyLocation
import androidx.compose.material.icons.twotone.Remove
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
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
import project.unibo.tankyou.utils.SettingsManager
import project.unibo.tankyou.utils.TankYouVocabulary
import project.unibo.tankyou.utils.getResourceString

/**
 * Main activity for the TankYou application.
 * Handles navigation between different screens and manages permissions.
 */
class MainActivity : AppCompatActivity() {

    /** Reference to the [MapComponent] for managing map interactions. */
    private lateinit var mapComponent: MapComponent

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in [onSaveInstanceState]. Oth[...]
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        ThemeManager.initialize(this)
        SettingsManager.initialize(this)

        // Request necessary permissions for the app.
        requestPermissions()

        // Set the content view to the main app composable.
        setContent {
            TankYouTheme {
                TankYouVocabulary {
                    MainApp()
                }
            }
        }
    }

    /**
     * Composable function for the main application UI.
     * Manages the current screen based on authentication state and user navigation.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainApp() {
        val authViewModel: AuthViewModel = viewModel()
        val authState by authViewModel.authState.collectAsState()
        var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

        // Observe authentication state changes and navigate accordingly.
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

        // Display login/register screens or the main app content based on the current screen.
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
                contentWindowInsets = WindowInsets(0), // Rimuove gli insets automatici
                bottomBar = {
                    BottomNavigationBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { screen -> currentScreen = screen }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .background(ThemeManager.palette.background)
                        .imePadding()
                ) {
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

    /**
     * Composable function for the bottom navigation bar.
     *
     * @param currentScreen The currently selected screen.
     * @param onScreenSelected Callback function when a screen is selected.
     */
    @Composable
    private fun BottomNavigationBar(
        currentScreen: Screen,
        onScreenSelected: (Screen) -> Unit
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
                selected = currentScreen == Screen.MAP,
                onClick = { onScreenSelected(Screen.MAP) },
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
                selected = currentScreen == Screen.PROFILE,
                onClick = { onScreenSelected(Screen.PROFILE) },
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
                selected = currentScreen == Screen.SETTINGS,
                onClick = { onScreenSelected(Screen.SETTINGS) },
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

    /**
     * Composable function for the map screen with Floating Action Buttons (FABs).
     * Manages the visibility of FABs and handles their click events.
     */
    @Composable
    private fun MapScreenWithFABs() {
        var fabsVisible by remember { mutableStateOf(true) }
        var searchBarVisible by remember { mutableStateOf(false) }
        var searchText by remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        // val focusManager = LocalFocusManager.current

        // Request focus when search bar becomes visible
        LaunchedEffect(searchBarVisible) {
            if (searchBarVisible) {
                focusRequester.requestFocus()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
            //.statusBarsPadding()
        ) {
            // Embed the AndroidView containing the map with blur effect when search is active
            AndroidView(
                factory = { context ->
                    RelativeLayout(context).apply {
                        mapComponent = MapComponent(
                            context = this@MainActivity,
                            mapContainer = this,
                            onMapClick = {
                                if (!searchBarVisible) {
                                    fabsVisible = !fabsVisible
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (searchBarVisible) {
                            Modifier.blur(radius = 8.dp)
                        } else {
                            Modifier
                        }
                    )
            )

            // FABs (zoom e location) - nascosti quando search è attiva
            AnimatedVisibility(
                visible = fabsVisible && !searchBarVisible,
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
                        containerColor = ThemeManager.palette.background,
                        contentColor = ThemeManager.palette.text
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Add,
                            contentDescription = getResourceString(R.string.zoom_in_icon_description)
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            if (::mapComponent.isInitialized) {
                                mapComponent.zoomOut()
                            }
                        },
                        containerColor = ThemeManager.palette.background,
                        contentColor = ThemeManager.palette.text
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.Remove,
                            contentDescription = getResourceString(R.string.zoom_out_icon_description)
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            if (::mapComponent.isInitialized) {
                                mapComponent.centerOnMyLocation()
                            }
                        },
                        containerColor = ThemeManager.palette.background,
                        contentColor = ThemeManager.palette.text
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.MyLocation,
                            contentDescription = getResourceString(R.string.show_my_location_desc)
                        )
                    }
                }
            }

            // FAB for search - sempre visibile in alto a sinistra
            AnimatedVisibility(
                visible = !searchBarVisible && fabsVisible,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        searchBarVisible = true
                    },
                    containerColor = ThemeManager.palette.background,
                    contentColor = ThemeManager.palette.text
                ) {
                    Icon(
                        Icons.TwoTone.Search,
                        contentDescription = getResourceString(R.string.search_icon_description)
                    )
                }
            }

            AnimatedVisibility(
                visible = searchBarVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            searchText = ""
                            searchBarVisible = false
                        }
                )
            }

            AnimatedVisibility(
                visible = searchBarVisible,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp, vertical = 48.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ThemeManager.palette.background,
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(4.dp)
                        .imePadding()
                ) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        maxLines = 1,
                        placeholder = {
                            Text(
                                getResourceString(R.string.search_hint),
                                color = ThemeManager.palette.text.copy(alpha = 0.6f)
                            )
                        },
                        leadingIcon = {
                            if (searchText.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.TwoTone.Search,
                                    contentDescription = null,
                                    tint = ThemeManager.palette.text.copy(alpha = 0.6f)
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        searchText = ""
                                        searchBarVisible = false
                                        if (::mapComponent.isInitialized) {
                                            mapComponent.clearSearch()
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Close search",
                                        tint = ThemeManager.palette.text
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchText = ""
                                        if (::mapComponent.isInitialized) {
                                            mapComponent.clearSearch()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.TwoTone.Close,
                                        contentDescription = "Clear text",
                                        tint = ThemeManager.palette.text.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = ThemeManager.palette.text,
                            unfocusedTextColor = ThemeManager.palette.text,
                            cursorColor = ThemeManager.palette.primary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchText.isNotBlank()) {
                                    if (::mapComponent.isInitialized) {
                                        mapComponent.searchGasStations(searchText)
                                    }
                                } else {
                                    if (::mapComponent.isInitialized) {
                                        mapComponent.clearSearch()
                                    }
                                }
                                searchBarVisible = false
                            }
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                }
            }
        }
    }

    /**
     * Requests necessary permissions for the application.
     * Filters out already granted permissions and requests the missing ones.
     */
    private fun requestPermissions() {
        val permissions = Constants.App.PERMISSIONS

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
        }
    }

    /**
     * Called when the activity will start interacting with the user.
     */
    override fun onResume() {
        super.onResume()
        if (::mapComponent.isInitialized) {
            mapComponent.onResume()
        }
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     */
    override fun onPause() {
        super.onPause()
        if (::mapComponent.isInitialized) {
            mapComponent.onPause()
        }
    }
}