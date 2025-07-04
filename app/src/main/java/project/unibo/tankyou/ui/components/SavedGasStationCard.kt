package project.unibo.tankyou.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import project.unibo.tankyou.R
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.UserSavedGasStation
import project.unibo.tankyou.data.database.entities.toFlagLabel
import project.unibo.tankyou.data.database.entities.toLocalizedDateFormat
import project.unibo.tankyou.data.database.model.SavedGasStationsModel
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.getResourceString

/**
 * A composable card that displays user's saved gas stations with expand/collapse functionality.
 *
 * @param onStationClick Callback invoked when a gas station item is clicked
 * @param savedGasStationsModel ViewModel that manages saved gas stations data
 */
@Composable
fun SavedGasStationCard(
    onStationClick: (GasStation) -> Unit = {},
    savedGasStationsModel: SavedGasStationsModel = viewModel()
) {
    // Collect state from the ViewModel
    val savedStations: List<UserSavedGasStation> by savedGasStationsModel.savedStations.collectAsState()
    val isLoading: Boolean by savedGasStationsModel.isLoading.collectAsState()

    // State to hold stations with their complete details
    var stationsWithDetails: List<Pair<UserSavedGasStation, GasStation?>> by remember {
        mutableStateOf<List<Pair<UserSavedGasStation, GasStation?>>>(
            emptyList()
        )
    }
    var detailsLoading: Boolean by remember { mutableStateOf(false) }
    var isExpanded: Boolean by remember { mutableStateOf(false) }

    // Effect to fetch detailed information for each saved station when savedStations changes
    LaunchedEffect(savedStations) {
        if (savedStations.isNotEmpty()) {
            Log.d(
                Constants.App.LOG_TAG,
                "Fetching details for ${savedStations.size} saved stations"
            )
            detailsLoading = true
            try {
                // Map each saved station to its complete GasStation details
                val stationsWithDetailsTemp: List<Pair<UserSavedGasStation, GasStation?>> =
                    savedStations.map { savedStation ->
                        val gasStation: GasStation? =
                            Constants.App.APP_REPOSITORY.getStationById(savedStation.stationId.toString())
                        savedStation to gasStation
                    }
                stationsWithDetails = stationsWithDetailsTemp
                Log.d(
                    Constants.App.LOG_TAG,
                    "Successfully loaded details for ${stationsWithDetailsTemp.size} stations"
                )
            } catch (e: Exception) {
                Log.e(Constants.App.LOG_TAG, "Error fetching station details: ${e.message}", e)
                e.printStackTrace()
            } finally {
                detailsLoading = false
            }
        } else {
            stationsWithDetails = emptyList()
            Log.d(Constants.App.LOG_TAG, "No saved stations to display")
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeManager.palette.primary
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with expand/collapse button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = ThemeManager.palette.accent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getResourceString(R.string.saved_gas_station),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ThemeManager.palette.title
                    )
                    if (savedStations.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${savedStations.size})",
                            fontSize = 16.sp,
                            color = ThemeManager.palette.text.copy(alpha = 0.7f)
                        )
                    }
                }

                // Show expand/collapse button only if there are saved stations
                if (savedStations.isNotEmpty()) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) getResourceString(R.string.collapse) else getResourceString(
                                R.string.expand
                            ),
                            tint = ThemeManager.palette.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display content based on loading state and data availability
            when {
                isLoading || detailsLoading -> {
                    Log.d(Constants.App.LOG_TAG, "Showing loading indicator for saved stations")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ThemeManager.palette.accent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                stationsWithDetails.isEmpty() -> {
                    Log.d(
                        Constants.App.LOG_TAG,
                        "No saved stations to display - showing empty state"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = getResourceString(R.string.no_saved_gas_stations),
                            color = ThemeManager.palette.text.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                else -> {
                    // Determine which stations to display based on expand state
                    val displayStations: List<Pair<UserSavedGasStation, GasStation?>> =
                        if (isExpanded) {
                            stationsWithDetails
                        } else {
                            stationsWithDetails.take(3)
                        }

                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = if (isExpanded) 300.dp else 200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayStations) { (savedStation, gasStation) ->
                                if (gasStation != null) {
                                    SavedStationItem(
                                        savedStation = savedStation,
                                        gasStation = gasStation,
                                        onStationClick = { onStationClick(gasStation) }
                                    )
                                } else {
                                    Log.w(
                                        Constants.App.LOG_TAG,
                                        "GasStation is null for saved station ID: ${savedStation.stationId}"
                                    )
                                }
                            }
                        }
                    }

                    // Show "Show more" indicator when collapsed and there are more items
                    if (!isExpanded && stationsWithDetails.size > 3) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "..." + getResourceString(R.string.and) + " " +
                                    (stationsWithDetails.size - 3).toString() + " " +
                                    getResourceString(R.string.show_more),
                            color = ThemeManager.palette.text.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A composable item that displays individual saved gas station information.
 *
 * @param savedStation The saved gas station data containing user-specific information
 * @param gasStation The complete gas station information
 * @param onStationClick Callback invoked when the station item is clicked
 */
@Composable
private fun SavedStationItem(
    savedStation: UserSavedGasStation,
    gasStation: GasStation,
    onStationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStationClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemeManager.palette.background
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
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
                        style = MaterialTheme.typography.titleMedium,
                        color = ThemeManager.palette.title,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${gasStation.address ?: ""}, ${gasStation.province ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThemeManager.palette.text.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = gasStation.flag.toFlagLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemeManager.palette.accent,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Format and display save date
                    Text(
                        text = savedStation.savedAt.toLocalizedDateFormat(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ThemeManager.palette.text.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }

            // Display user notes if available
            if (!savedStation.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = savedStation.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeManager.palette.text,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}