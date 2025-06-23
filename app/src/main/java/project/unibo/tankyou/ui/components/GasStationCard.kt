package project.unibo.tankyou.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import project.unibo.tankyou.R
import project.unibo.tankyou.data.database.entities.Fuel
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.toFlagLabel
import project.unibo.tankyou.data.database.entities.toFuelTypeName
import project.unibo.tankyou.data.database.entities.toLabel
import project.unibo.tankyou.data.database.entities.toLocalizedDateFormat
import project.unibo.tankyou.data.database.entities.toTypeLabel
import project.unibo.tankyou.data.repositories.AppRepository
import project.unibo.tankyou.data.repositories.AuthRepository
import project.unibo.tankyou.data.repositories.UserRepository
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.getResourceString
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A composable that displays a detailed gas station card with fuel prices and favorite functionality
 *
 * @param gasStation The gas station data to display
 * @param onDismiss Callback function to handle card dismissal
 * @param modifier Optional modifier for customizing the appearance
 */
@Composable
fun GasStationCard(
    gasStation: GasStation,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // State variables for UI management
    var isVisible: Boolean by remember { mutableStateOf(false) }
    var fuelPrices: List<Fuel> by remember { mutableStateOf(emptyList()) }
    var isLoading: Boolean by remember { mutableStateOf(true) }
    var isFavorite: Boolean by remember { mutableStateOf(false) }
    var offsetY: Float by remember { mutableFloatStateOf(0f) }
    var showSaveDialog: Boolean by remember { mutableStateOf(false) }
    var isSaving: Boolean by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val repository: AppRepository = remember { AppRepository.getInstance() }
    val userRepository: UserRepository = remember { UserRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    // Initialize card data when gas station changes
    LaunchedEffect(gasStation) {
        isVisible = true
        isLoading = true

        try {
            Log.d(Constants.App.LOG_TAG, "Loading fuel prices for gas station: ${gasStation.id}")
            val prices: List<Fuel> = repository.getFuelPricesForStation(gasStation.id)
            fuelPrices = prices

            // Check if the station is already saved as favorite
            Log.d(
                Constants.App.LOG_TAG,
                "Checking favorite status for gas station: ${gasStation.id}"
            )
            isFavorite = userRepository.isGasStationSaved(gasStation.id)
        } catch (e: Exception) {
            Log.e(
                Constants.App.LOG_TAG,
                "Error loading gas station data for station: ${gasStation.id}",
                e
            )
            fuelPrices = emptyList()
        } finally {
            isLoading = false
            Log.d(Constants.App.LOG_TAG, "Finished loading data for gas station: ${gasStation.id}")
        }
    }

    /**
     * Refreshes the favorite state from the database
     */
    fun refreshFavoriteState() {
        coroutineScope.launch {
            try {
                Log.d(
                    Constants.App.LOG_TAG,
                    "Refreshing favorite state for gas station: ${gasStation.id}"
                )
                isFavorite = userRepository.isGasStationSaved(gasStation.id)
            } catch (e: Exception) {
                Log.e(
                    Constants.App.LOG_TAG,
                    "Error refreshing favorite state for station: ${gasStation.id}",
                    e
                )
            }
        }
    }

    /**
     * Handles the favorite button click logic
     * Either removes from favorites or shows the save dialog
     */
    fun handleFavoriteClick() {
        // Prevent multiple clicks during operation
        if (isSaving) {
            Log.w(
                Constants.App.LOG_TAG,
                "Favorite click ignored - save operation in progress for station: ${gasStation.id}"
            )
            return
        }

        coroutineScope.launch {
            isSaving = true
            try {
                Log.d(
                    Constants.App.LOG_TAG,
                    "Processing favorite click for gas station: ${gasStation.id}"
                )

                // Re-check current state before proceeding to avoid race conditions
                val currentlyFavorite: Boolean = userRepository.isGasStationSaved(gasStation.id)

                if (currentlyFavorite) {
                    // Remove from favorites
                    Log.d(
                        Constants.App.LOG_TAG,
                        "Attempting to remove gas station ${gasStation.id} from favorites"
                    )
                    val success: Boolean = userRepository.removeSavedGasStation(gasStation.id)
                    if (success) {
                        isFavorite = false
                        Log.d(
                            Constants.App.LOG_TAG,
                            "Successfully removed gas station ${gasStation.id} from favorites"
                        )
                    } else {
                        Log.e(
                            Constants.App.LOG_TAG,
                            "Failed to remove gas station ${gasStation.id} from favorites"
                        )
                        // Refresh state in case of error to maintain consistency
                        refreshFavoriteState()
                    }
                } else {
                    // Show save dialog to add to favorites
                    Log.d(
                        Constants.App.LOG_TAG,
                        "Showing save dialog for gas station: ${gasStation.id}"
                    )
                    showSaveDialog = true
                }
            } catch (e: Exception) {
                Log.e(
                    Constants.App.LOG_TAG,
                    "Error handling favorite click for station: ${gasStation.id}",
                    e
                )
                // Refresh state in case of error to maintain consistency
                refreshFavoriteState()
            } finally {
                isSaving = false
            }
        }
    }

    /**
     * Handles saving a gas station to favorites with optional notes
     *
     * @param notes Optional notes to save with the gas station
     */
    fun handleSaveStation(notes: String) {
        coroutineScope.launch {
            isSaving = true
            try {
                Log.d(
                    Constants.App.LOG_TAG,
                    "Attempting to save gas station ${gasStation.id} with notes: ${notes.isNotBlank()}"
                )

                // Double-check that the station isn't already saved to prevent duplicates
                val alreadySaved: Boolean = userRepository.isGasStationSaved(gasStation.id)
                if (alreadySaved) {
                    Log.i(
                        Constants.App.LOG_TAG,
                        "Gas station ${gasStation.id} is already saved, updating UI state"
                    )
                    isFavorite = true
                    showSaveDialog = false
                    return@launch
                }

                val success: Boolean = userRepository.saveGasStation(
                    gasStation.id,
                    notes.ifBlank { null }
                )

                if (success) {
                    Log.d(Constants.App.LOG_TAG, "Gas station ${gasStation.id} saved successfully")
                    isFavorite = true
                    showSaveDialog = false
                } else {
                    Log.e(Constants.App.LOG_TAG, "Failed to save gas station ${gasStation.id}")
                    // Refresh state to ensure consistency
                    refreshFavoriteState()
                }
            } catch (e: Exception) {
                Log.e(Constants.App.LOG_TAG, "Error saving gas station ${gasStation.id}", e)
                // Refresh state to ensure consistency
                refreshFavoriteState()
            } finally {
                isSaving = false
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Transparent overlay to capture background clicks
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { },
            color = Color.Transparent
        ) {}

        // Animated card that slides in from bottom
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        // Handle drag gestures for dismissing the card
                        detectDragGestures(
                            onDragEnd = {
                                val threshold: Float = with(density) { 150.dp.toPx() }
                                if (offsetY > threshold) {
                                    Log.d(Constants.App.LOG_TAG, "Card dismissed via drag gesture")
                                    onDismiss()
                                } else {
                                    // Reset position if drag threshold not met
                                    offsetY = 0f
                                }
                            }
                        ) { _, dragAmount ->
                            // Update card position during drag, only allow downward movement
                            val newOffset: Float = offsetY + dragAmount.y
                            offsetY = if (newOffset > 0) newOffset else 0f
                        }
                    }
                    .clip(
                        RoundedCornerShape(
                            topStart = 32.dp,
                            topEnd = 32.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = ThemeManager.palette.background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Drag handle indicator at the top
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Surface(
                            color = ThemeManager.palette.text.copy(alpha = 0.3f),
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Header section with station name, address and action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        // Station name and address information
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = gasStation.name ?: getResourceString(R.string.not_available),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = ThemeManager.palette.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Address section with location icon
                            Row(
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = ThemeManager.palette.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    gasStation.address?.let { address: String ->
                                        Text(
                                            text = address,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ThemeManager.palette.text,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    // Display city and province if available
                                    if (gasStation.city != null || gasStation.province != null) {
                                        val locationText: String = buildString {
                                            gasStation.city?.let { append(it) }
                                            gasStation.province?.let {
                                                if (gasStation.city != null) append(" ")
                                                append("($it)")
                                            }
                                            if (isEmpty()) {
                                                append(getResourceString(R.string.not_available))
                                            }
                                        }
                                        Text(
                                            text = locationText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThemeManager.palette.text.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Action buttons (favorite and close)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Show favorite button only for logged in users
                            if (AuthRepository.getInstance().isUserLoggedIn()) {
                                IconButton(
                                    onClick = { handleFavoriteClick() },
                                    enabled = !isSaving,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(40.dp)
                                ) {
                                    if (isSaving) {
                                        // Show loading indicator during save operation
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = ThemeManager.palette.primary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        // Show filled or outlined heart based on favorite status
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                            tint = if (isFavorite) ThemeManager.palette.alert else ThemeManager.palette.text,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            // Close button
                            IconButton(
                                onClick = {
                                    Log.d(Constants.App.LOG_TAG, "Gas station card closed by user")
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = ThemeManager.palette.text,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(
                        color = ThemeManager.palette.text.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Station information card (brand, type, owner)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ThemeManager.palette.secondary.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Brand and type information in a row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StationInfoItem(
                                    icon = Icons.Default.Flag,
                                    label = getResourceString(R.string.card_brand_title),
                                    value = gasStation.flag.toFlagLabel(),
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                StationInfoItem(
                                    icon = Icons.Default.LocalGasStation,
                                    label = getResourceString(R.string.card_type_title),
                                    value = gasStation.type.toTypeLabel(),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Owner information (if available)
                            gasStation.owner?.let { owner: String ->
                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider(
                                    color = ThemeManager.palette.text.copy(alpha = 0.1f),
                                    thickness = 1.dp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                StationInfoItem(
                                    icon = Icons.Default.Person,
                                    label = getResourceString(R.string.card_owner_title),
                                    value = owner,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    HorizontalDivider(
                        color = ThemeManager.palette.text.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Fuel prices section header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = ThemeManager.palette.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getResourceString(R.string.fuel_prices_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ThemeManager.palette.title
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Fuel prices content with loading, empty or data states
                    if (isLoading) {
                        // Loading state
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = ThemeManager.palette.disabledBackground
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = ThemeManager.palette.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    } else if (fuelPrices.isEmpty()) {
                        // Empty state when no fuel prices are available
                        Log.w(
                            Constants.App.LOG_TAG,
                            "No fuel prices available for gas station: ${gasStation.id}"
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = ThemeManager.palette.disabledBackground
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getResourceString(R.string.no_fuel_prices),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeManager.palette.disabledText
                                )
                            }
                        }
                    } else {
                        // Data state - display fuel prices in a scrollable list
                        Log.d(
                            Constants.App.LOG_TAG,
                            "Displaying ${fuelPrices.size} fuel prices for gas station: ${gasStation.id}"
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(fuelPrices) { fuel: Fuel ->
                                FuelPriceItem(fuel = fuel)
                            }
                        }
                    }
                }
            }
        }
    }

    // Save station dialog
    SaveStationDialog(
        isVisible = showSaveDialog,
        stationName = gasStation.name ?: "Gas Station",
        onDismiss = {
            Log.d(
                Constants.App.LOG_TAG,
                "Save station dialog dismissed for gas station: ${gasStation.id}"
            )
            showSaveDialog = false
            isSaving = false // Reset saving state when dialog is dismissed
        },
        onSave = { notes: String -> handleSaveStation(notes) }
    )
}

/**
 * A composable that displays a single station information item with an icon, label and value
 *
 * @param icon The icon to display
 * @param label The label text
 * @param value The value text
 * @param modifier Optional modifier for customizing the appearance
 */
@Composable
private fun StationInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ThemeManager.palette.accent,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = ThemeManager.palette.text,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeManager.palette.title,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A composable that displays a fuel price item with fuel type, service type and price information
 *
 * @param fuel The fuel data to display
 */
@Composable
private fun FuelPriceItem(fuel: Fuel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThemeManager.palette.secondary.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fuel type and service type information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fuel.type.toFuelTypeName(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ThemeManager.palette.title
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = fuel.self.toLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeManager.palette.text
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Price and last update information
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Format price with 3 decimal places
                val formattedPrice: String = String.format(Locale.getDefault(), "€%.3f", fuel.price)
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ThemeManager.palette.accent,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(1.dp))

                // Last update date
                val lastUpdateText: String =
                    getResourceString(R.string.fuel_prices_last_update_title) + " " + fuel.date.toLocalizedDateFormat()
                Text(
                    text = lastUpdateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeManager.palette.text
                )
            }
        }
    }
}