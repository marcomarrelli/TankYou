package project.unibo.tankyou.ui.screens

import android.widget.RelativeLayout
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.twotone.Add
import androidx.compose.material.icons.twotone.Close
import androidx.compose.material.icons.twotone.MyLocation
import androidx.compose.material.icons.twotone.Remove
import androidx.compose.material.icons.twotone.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import project.unibo.tankyou.data.database.entities.FuelType
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.GasStationFlag
import project.unibo.tankyou.data.database.entities.toFuelTypeName
import project.unibo.tankyou.data.repositories.AuthRepository
import project.unibo.tankyou.data.repositories.SearchFilters
import project.unibo.tankyou.ui.components.GasStationCard
import project.unibo.tankyou.ui.components.MapComponent
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.SettingsManager
import project.unibo.tankyou.utils.getResourceString

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val activity = context as AppCompatActivity
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

    var showSavedOnly by remember { mutableStateOf(false) }

    val hasActiveFilters by remember {
        derivedStateOf {
            selectedFlags.isNotEmpty() ||
                    selectedFuelTypes.isNotEmpty() ||
                    selectedServiceTypes.isNotEmpty() ||
                    showSavedOnly
        }
    }

    val activeFilterCount by remember {
        derivedStateOf {
            selectedFlags.size + selectedFuelTypes.size + selectedServiceTypes.size +
                    (if (showSavedOnly) 1 else 0)
        }
    }

    var mapComponent: MapComponent? by remember { mutableStateOf(null) }

    LaunchedEffect(selectedGasStation) {
        fabsVisible = (selectedGasStation == null)
    }

    LaunchedEffect(searchBarVisible) {
        if (searchBarVisible) {
            focusRequester.requestFocus()
        }
    }

    fun performSearch(
        query: String,
        flags: Set<GasStationFlag>,
        fuelTypes: Set<FuelType>,
        serviceTypes: Set<Boolean>,
        showSavedOnly: Boolean = false
    ) {
        mapComponent?.let { map ->
            val filters = SearchFilters(
                flags = flags.toList(),
                fuelTypes = fuelTypes.toList(),
                serviceTypes = serviceTypes.toList()
            )

            if (query.isNotBlank() || flags.isNotEmpty() || fuelTypes.isNotEmpty() || serviceTypes.isNotEmpty() || showSavedOnly) {
                if (showSavedOnly) {
                    map.searchSavedGasStationsWithFilters(query, filters)
                } else {
                    map.searchGasStationsWithFilters(query, filters)
                }
            } else {
                map.clearSearch()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { _ ->
                RelativeLayout(activity).apply {
                    mapComponent = MapComponent(
                        context = activity,
                        mapContainer = this,
                        onMapClick = {
                            if (selectedGasStation == null && !searchBarVisible) {
                                fabsVisible = !fabsVisible
                            }
                        },
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
                    if (searchBarVisible) Modifier.blur(8.dp) else Modifier
                )
                .then(
                    if (selectedGasStation != null) Modifier.blur(2.dp) else Modifier
                ),
            update = { }
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
                        color = ThemeManager.palette.white,
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
                    onClick = { mapComponent?.zoomIn() },
                    containerColor = ThemeManager.palette.background,
                    contentColor = ThemeManager.palette.text
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Add,
                        contentDescription = getResourceString(project.unibo.tankyou.R.string.zoom_in_icon_description)
                    )
                }

                FloatingActionButton(
                    onClick = { mapComponent?.zoomOut() },
                    containerColor = ThemeManager.palette.background,
                    contentColor = ThemeManager.palette.text
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Remove,
                        contentDescription = getResourceString(project.unibo.tankyou.R.string.zoom_out_icon_description)
                    )
                }

                FloatingActionButton(
                    onClick = {
                        if (isLocationOverlayAvailable && showMyLocationOnMap) {
                            mapComponent?.centerOnMyLocation()
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
                        contentDescription = getResourceString(project.unibo.tankyou.R.string.show_my_location_desc)
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
                // @FIXME .windowInsetsPadding(WindowInsets.statusBars)
                .zIndex(5f)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = Constants.App.STATUS_BAR_PADDING,
                    bottom = 16.dp
                )
        ) {
            Box {
                FloatingActionButton(
                    onClick = { searchBarVisible = true },
                    containerColor = if (hasActiveFilters) {
                        ThemeManager.palette.primary
                    } else {
                        ThemeManager.palette.background
                    },
                    contentColor = ThemeManager.palette.text
                ) {
                    Icon(
                        Icons.TwoTone.Search,
                        contentDescription = getResourceString(project.unibo.tankyou.R.string.search_icon_description)
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
                            mapComponent?.clearSearch()
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
                                getResourceString(project.unibo.tankyou.R.string.search_hint),
                                color = ThemeManager.palette.text.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchText = ""
                                        mapComponent?.clearSearch()
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
                                        selectedServiceTypes,
                                        showSavedOnly
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
                            imageVector = if (showAdvancedFilters) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
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
                                    selectedServiceTypes,
                                    showSavedOnly
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
                                color = ThemeManager.palette.white,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.TwoTone.Search,
                                contentDescription = null,
                                tint = ThemeManager.palette.white
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Search", color = ThemeManager.palette.white)
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
                                    label = {
                                        Text(
                                            text = flag.name,
                                            color = ThemeManager.palette.text
                                        )
                                    },
                                    selected = selectedFlags.contains(flag),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ThemeManager.palette.primary,
                                        selectedLabelColor = ThemeManager.palette.white
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
                                    label = {
                                        Text(
                                            text = fuelType.id.toFuelTypeName(),
                                            color = ThemeManager.palette.text
                                        )
                                    },
                                    selected = selectedFuelTypes.contains(fuelType),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ThemeManager.palette.primary,
                                        selectedLabelColor = ThemeManager.palette.white
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (AuthRepository.getInstance().isUserLoggedIn()) {
                            Text(
                                text = "Saved Stations",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = ThemeManager.palette.text
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FilterChip(
                                onClick = {
                                    showSavedOnly = !showSavedOnly
                                },
                                label = {
                                    Text(
                                        text = "Show Only Saved",
                                        color = ThemeManager.palette.text
                                    )
                                },
                                selected = showSavedOnly,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ThemeManager.palette.primary,
                                    selectedLabelColor = ThemeManager.palette.white
                                ),
                                leadingIcon = if (showSavedOnly) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }
}