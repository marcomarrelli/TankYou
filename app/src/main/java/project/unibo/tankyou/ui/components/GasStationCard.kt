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
import project.unibo.tankyou.utils.getResourceString
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun GasStationCard(
    gasStation: GasStation,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    var fuelPrices by remember { mutableStateOf<List<Fuel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isFavorite by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val repository = remember { AppRepository.getInstance() }
    val userRepository = remember { UserRepository.getInstance() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(gasStation) {
        isVisible = true
        isLoading = true

        try {
            val prices = repository.getFuelPricesForStation(gasStation.id)
            fuelPrices = prices

            // Always check the current state from database
            isFavorite = userRepository.isGasStationSaved(gasStation.id)
        } catch (e: Exception) {
            Log.e("GasStationCard", "Error loading gas station data", e)
            fuelPrices = emptyList()
        } finally {
            isLoading = false
        }
    }

    fun refreshFavoriteState() {
        coroutineScope.launch {
            try {
                isFavorite = userRepository.isGasStationSaved(gasStation.id)
            } catch (e: Exception) {
                Log.e("GasStationCard", "Error refreshing favorite state", e)
            }
        }
    }

    fun handleFavoriteClick() {
        if (isSaving) return // Prevent multiple clicks during operation

        coroutineScope.launch {
            isSaving = true
            try {
                // Re-check current state before proceeding
                val currentlyFavorite = userRepository.isGasStationSaved(gasStation.id)

                if (currentlyFavorite) {
                    // Remove from favorites
                    val success = userRepository.removeSavedGasStation(gasStation.id)
                    if (success) {
                        isFavorite = false
                        Log.d(
                            "GasStationCard",
                            "Successfully removed gas station ${gasStation.id} from favorites"
                        )
                    } else {
                        Log.e(
                            "GasStationCard",
                            "Failed to remove gas station ${gasStation.id} from favorites"
                        )
                        // Refresh state in case of error
                        refreshFavoriteState()
                    }
                } else {
                    // Show save dialog to add to favorites
                    showSaveDialog = true
                }
            } catch (e: Exception) {
                Log.e("GasStationCard", "Error handling favorite click", e)
                // Refresh state in case of error
                refreshFavoriteState()
            } finally {
                isSaving = false
            }
        }
    }

    fun handleSaveStation(notes: String) {
        coroutineScope.launch {
            isSaving = true
            try {
                // Double-check that the station isn't already saved
                val alreadySaved = userRepository.isGasStationSaved(gasStation.id)
                if (alreadySaved) {
                    Log.i("GasStationCard", "Gas station ${gasStation.id} is already saved")
                    isFavorite = true
                    showSaveDialog = false
                    return@launch
                }

                val success = userRepository.saveGasStation(
                    gasStation.id,
                    notes.ifBlank { null }
                )

                if (success) {
                    Log.d("GasStationCard", "Gas station ${gasStation.id} saved successfully")
                    isFavorite = true
                    showSaveDialog = false
                } else {
                    Log.e("GasStationCard", "Failed to save gas station ${gasStation.id}")
                    // Refresh state to ensure consistency
                    refreshFavoriteState()
                }
            } catch (e: Exception) {
                Log.e("GasStationCard", "Error saving gas station ${gasStation.id}", e)
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
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { },
            color = Color.Transparent
        ) {}

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
                        detectDragGestures(
                            onDragEnd = {
                                val threshold = with(density) { 150.dp.toPx() }
                                if (offsetY > threshold) {
                                    onDismiss()
                                } else {
                                    offsetY = 0f
                                }
                            }
                        ) { _, dragAmount ->
                            val newOffset = offsetY + dragAmount.y
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
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
                                    gasStation.address?.let { address ->
                                        Text(
                                            text = address,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = ThemeManager.palette.text,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (gasStation.city != null || gasStation.province != null) {
                                        Text(
                                            text = buildString {
                                                gasStation.city?.let { append(it) }
                                                gasStation.province?.let {
                                                    if (gasStation.city != null) append(" ")
                                                    append("($it)")
                                                }
                                                if (isEmpty()) {
                                                    append(getResourceString(R.string.not_available))
                                                }
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = ThemeManager.palette.text.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (AuthRepository.getInstance().isUserLoggedIn()) {
                                IconButton(
                                    onClick = { handleFavoriteClick() },
                                    enabled = !isSaving,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(40.dp)
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = ThemeManager.palette.primary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                            tint = if (isFavorite) ThemeManager.palette.alert else ThemeManager.palette.text,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
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

                            if (gasStation.owner != null) {
                                Spacer(modifier = Modifier.height(12.dp))

                                HorizontalDivider(
                                    color = ThemeManager.palette.text.copy(alpha = 0.1f),
                                    thickness = 1.dp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                StationInfoItem(
                                    icon = Icons.Default.Person,
                                    label = getResourceString(R.string.card_owner_title),
                                    value = gasStation.owner,
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

                    if (isLoading) {
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
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(fuelPrices) { fuel ->
                                FuelPriceItem(fuel = fuel)
                            }
                        }
                    }
                }
            }
        }
    }

    SaveStationDialog(
        isVisible = showSaveDialog,
        stationName = gasStation.name ?: "Gas Station",
        onDismiss = {
            showSaveDialog = false
            isSaving = false // Reset saving state when dialog is dismissed
        },
        onSave = { notes -> handleSaveStation(notes) }
    )
}

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

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "€%.3f", fuel.price),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ThemeManager.palette.accent,
                    fontSize = 20.sp
                )

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = getResourceString(R.string.fuel_prices_last_update_title) + " " + fuel.date.toLocalizedDateFormat(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeManager.palette.text
                )
            }
        }
    }
}