package project.unibo.tankyou

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.RelativeLayout
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.MyLocation
import androidx.compose.material.icons.twotone.Remove
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import project.unibo.tankyou.data.database.auth.AuthState
import project.unibo.tankyou.data.database.auth.AuthViewModel
import project.unibo.tankyou.data.database.entities.FuelType
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.GasStationFlag
import project.unibo.tankyou.data.database.entities.toFuelTypeName
import project.unibo.tankyou.data.repositories.SearchFilters
import project.unibo.tankyou.ui.components.GasStationCard
import project.unibo.tankyou.ui.components.MapComponent
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

class MainActivity : AppCompatActivity() {
    private lateinit var mapComponent: MapComponent

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
        var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

        LaunchedEffect(authState, isGuestMode) {
            when {
                authState is AuthState.Authenticated && !isGuestMode -> {
                    if (currentScreen == Screen.LOGIN || currentScreen == Screen.REGISTER) {
                        currentScreen = Screen.MAP
                    }
                }

                authState is AuthState.Unauthenticated && isGuestMode -> {
                    if (currentScreen == Screen.LOGIN || currentScreen == Screen.REGISTER) {
                        currentScreen = Screen.MAP
                    }
                }

                authState is AuthState.Unauthenticated && !isGuestMode -> {
                    currentScreen = Screen.LOGIN
                }
            }
        }

        if (currentScreen == Screen.LOGIN || currentScreen == Screen.REGISTER) {
            when (currentScreen) {
                Screen.LOGIN -> {
                    LoginScreen(
                        onNavigateToRegister = { currentScreen = Screen.REGISTER },
                        onLoginSuccess = { currentScreen = Screen.MAP },
                        onContinueAsGuest = {
                            authViewModel.continueAsGuest()
                            currentScreen = Screen.MAP
                        },
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
                contentWindowInsets = WindowInsets(0),
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
                                onLogout = {
                                    authViewModel.signOut()
                                    currentScreen = Screen.LOGIN
                                },
                                onNavigateToLogin = {
                                    authViewModel.signOut()
                                    currentScreen = Screen.LOGIN
                                },
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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun MapScreenWithFABs() {
        val coroutineScope = rememberCoroutineScope()
        var fabsVisible by remember { mutableStateOf(true) }
        var searchBarVisible by remember { mutableStateOf(false) }
        var searchText by remember { mutableStateOf("") }
        var selectedGasStation by remember { mutableStateOf<GasStation?>(null) }
        var isFollowModeActive by remember { mutableStateOf(false) }
        var isLocationOverlayAvailable by remember { mutableStateOf(false) }
        var showAdvancedFilters by remember { mutableStateOf(false) }
        var selectedFlags by remember { mutableStateOf(setOf<GasStationFlag>()) }
        var selectedFuelTypes by remember { mutableStateOf(setOf<FuelType>()) }
        var selectedServiceTypes by remember { mutableStateOf(setOf<Boolean>()) }
        var isSearching by remember { mutableStateOf(false) }
        val focusRequester = remember { FocusRequester() }

        val showMyLocationOnMap by SettingsManager.showMyLocationOnMapFlow.collectAsState()

        val availableFlags: List<GasStationFlag> = Constants.GAS_STATION_FLAGS
        val availableFuelTypes: List<FuelType> = Constants.FUEL_TYPES

        val hasActiveFilters by remember {
            derivedStateOf {
                selectedFlags.isNotEmpty() || selectedFuelTypes.isNotEmpty() || selectedServiceTypes.isNotEmpty()
            }
        }

        val activeFilterCount by remember {
            derivedStateOf {
                selectedFlags.size + selectedFuelTypes.size + selectedServiceTypes.size
            }
        }

        LaunchedEffect(selectedGasStation) {
            if (selectedGasStation != null) {
                fabsVisible = false
            } else {
                fabsVisible = true
            }
        }

        LaunchedEffect(searchBarVisible) {
            if (searchBarVisible) {
                focusRequester.requestFocus()
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                factory = { context ->
                    RelativeLayout(context).apply {
                        mapComponent = MapComponent(
                            context = this@MainActivity,
                            mapContainer = this,
                            onMapClick = {},
                            onGasStationClick = { gasStation ->
                                selectedGasStation = gasStation
                            },
                            onFollowModeChanged = { isActive ->
                                isFollowModeActive = isActive
                            },
                            onLocationOverlayAvailabilityChanged = { isAvailable ->
                                isLocationOverlayAvailable = isAvailable
                            },
                            onSearchStateChanged = { searching ->
                                isSearching = searching
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
                    .then(
                        if (selectedGasStation != null) {
                            Modifier.blur(radius = 2.dp)
                        } else {
                            Modifier
                        }
                    )
            )

            selectedGasStation?.let { gasStation ->
                GasStationCard(
                    gasStation = gasStation,
                    onDismiss = { selectedGasStation = null },
                    modifier = Modifier
                        .zIndex(10f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { }
                )
            }

            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .zIndex(15f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = ThemeManager.palette.primary,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Searching...",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = fabsVisible && !searchBarVisible && selectedGasStation == null,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 32.dp)
                    .zIndex(5f)
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
                            if (::mapComponent.isInitialized && isLocationOverlayAvailable && showMyLocationOnMap) {
                                mapComponent.centerOnMyLocation()
                            }
                        },
                        containerColor = if (isLocationOverlayAvailable && showMyLocationOnMap) {
                            ThemeManager.palette.background
                        } else {
                            ThemeManager.palette.disabledBackground
                        },
                        contentColor = if (isLocationOverlayAvailable && showMyLocationOnMap) {
                            if (isFollowModeActive) ThemeManager.palette.accent else ThemeManager.palette.text
                        } else {
                            ThemeManager.palette.disabledText
                        }
                    ) {
                        Icon(
                            imageVector = Icons.TwoTone.MyLocation,
                            contentDescription = getResourceString(R.string.show_my_location_desc)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !searchBarVisible && fabsVisible && selectedGasStation == null,
                enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 16.dp)
                    .zIndex(5f)
            ) {
                Box {
                    FloatingActionButton(
                        onClick = {
                            searchBarVisible = true
                        },
                        containerColor = if (hasActiveFilters) {
                            ThemeManager.palette.primary
                        } else {
                            ThemeManager.palette.background
                        },
                        contentColor = ThemeManager.palette.text
                    ) {
                        Icon(
                            Icons.TwoTone.Search,
                            contentDescription = getResourceString(R.string.search_icon_description)
                        )
                    }

                    if (hasActiveFilters) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = ThemeManager.palette.accent,
                                    shape = CircleShape
                                )
                                .align(Alignment.TopEnd)
                                .zIndex(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (activeFilterCount > 9) "9+" else activeFilterCount.toString(),
                                color = ThemeManager.palette.white,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
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
                            showAdvancedFilters = false
                            selectedFlags = emptySet()
                            selectedFuelTypes = emptySet()
                            selectedServiceTypes = emptySet()
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = ThemeManager.palette.background,
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(16.dp)
                        .imePadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                searchText = ""
                                searchBarVisible = false
                                showAdvancedFilters = false
                                selectedFlags = emptySet()
                                selectedFuelTypes = emptySet()
                                selectedServiceTypes = emptySet()
                                if (::mapComponent.isInitialized) {
                                    mapComponent.clearSearch()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Close search",
                                tint = ThemeManager.palette.text
                            )
                        }

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
                                .weight(1f)
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
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    coroutineScope.launch {
                                        performSearch(
                                            searchText,
                                            selectedFlags,
                                            selectedFuelTypes,
                                            selectedServiceTypes
                                        )
                                        searchBarVisible = false
                                    }
                                }
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showAdvancedFilters = !showAdvancedFilters }
                        ) {
                            Text(
                                text = "More Filters",
                                color = ThemeManager.palette.primary
                            )
                            if (hasActiveFilters) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            color = ThemeManager.palette.accent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (activeFilterCount > 9) "9+" else activeFilterCount.toString(),
                                        color = ThemeManager.palette.white,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showAdvancedFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = ThemeManager.palette.primary
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    performSearch(
                                        searchText,
                                        selectedFlags,
                                        selectedFuelTypes,
                                        selectedServiceTypes
                                    )
                                    searchBarVisible = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemeManager.palette.primary
                            ),
                            enabled = !isSearching
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.TwoTone.Search,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Search", color = Color.White)
                        }
                    }

                    AnimatedVisibility(
                        visible = showAdvancedFilters,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Flags",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = ThemeManager.palette.text
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                availableFlags.forEach { flag ->
                                    FilterChip(
                                        onClick = {
                                            selectedFlags = if (selectedFlags.contains(flag)) {
                                                selectedFlags - flag
                                            } else {
                                                selectedFlags + flag
                                            }
                                        },
                                        label = { Text(flag.name) },
                                        selected = selectedFlags.contains(flag),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ThemeManager.palette.primary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Fuel Types",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = ThemeManager.palette.text
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                availableFuelTypes.forEach { fuelType ->
                                    FilterChip(
                                        onClick = {
                                            selectedFuelTypes =
                                                if (selectedFuelTypes.contains(fuelType)) {
                                                    selectedFuelTypes - fuelType
                                                } else {
                                                    selectedFuelTypes + fuelType
                                                }
                                        },
                                        label = { Text(fuelType.id.toFuelTypeName()) },
                                        selected = selectedFuelTypes.contains(fuelType),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ThemeManager.palette.primary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun performSearch(
        query: String,
        flags: Set<GasStationFlag>,
        fuelTypes: Set<FuelType>,
        serviceTypes: Set<Boolean>
    ) {
        if (::mapComponent.isInitialized) {
            val filters = SearchFilters(
                flags = flags.toList(),
                fuelTypes = fuelTypes.toList(),
                serviceTypes = serviceTypes.toList()
            )

            if (query.isNotBlank() || flags.isNotEmpty() || fuelTypes.isNotEmpty() || serviceTypes.isNotEmpty()) {
                mapComponent.searchGasStationsWithFilters(query, filters)
            } else {
                mapComponent.clearSearch()
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