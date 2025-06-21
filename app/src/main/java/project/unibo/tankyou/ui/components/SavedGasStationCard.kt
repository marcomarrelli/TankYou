package project.unibo.tankyou.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.UserSavedGasStation
import project.unibo.tankyou.data.database.entities.toFlagLabel
import project.unibo.tankyou.data.database.entities.toLocalizedDateFormat
import project.unibo.tankyou.data.database.model.SavedGasStationsModel
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SavedGasStationCard(
    onStationClick: (GasStation) -> Unit = {},
    savedGasStationsModel: SavedGasStationsModel = viewModel()
) {
    val savedStations by savedGasStationsModel.savedStations.collectAsState()
    val isLoading by savedGasStationsModel.isLoading.collectAsState()

    var stationsWithDetails by remember {
        mutableStateOf<List<Pair<UserSavedGasStation, GasStation?>>>(
            emptyList()
        )
    }
    var detailsLoading by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(savedStations) {
        if (savedStations.isNotEmpty()) {
            detailsLoading = true
            try {
                val stationsWithDetailsTemp = savedStations.map { savedStation ->
                    val gasStation =
                        Constants.App.REPOSITORY.getStationById(savedStation.stationId.toString())
                    savedStation to gasStation
                }
                stationsWithDetails = stationsWithDetailsTemp
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                detailsLoading = false
            }
        } else {
            stationsWithDetails = emptyList()
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
                        text = "Saved Gas Stations",
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

                if (savedStations.isNotEmpty()) {
                    IconButton(onClick = { isExpanded = !isExpanded }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = ThemeManager.palette.accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading || detailsLoading -> {
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No saved gas stations yet.\nStart exploring and save your favorites!",
                            color = ThemeManager.palette.text.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                else -> {
                    // Show first 3 stations when collapsed, all when expanded
                    val displayStations = if (isExpanded) {
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
                            modifier = Modifier.heightIn(max = if (isExpanded) 400.dp else 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayStations) { (savedStation, gasStation) ->
                                if (gasStation != null) {
                                    SavedStationItem(
                                        savedStation = savedStation,
                                        gasStation = gasStation,
                                        onStationClick = { onStationClick(gasStation) }
                                    )
                                }
                            }
                        }
                    }

                    // Show "Show more" indicator when collapsed and there are more items
                    if (!isExpanded && stationsWithDetails.size > 3) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "... and ${stationsWithDetails.size - 3} more stations",
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
                        text = gasStation.name ?: "Unknown Station",
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
                    gasStation.flag?.let { flag ->
                        Text(
                            text = flag.toFlagLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = ThemeManager.palette.accent,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }

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

            if (!savedStation.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = savedStation.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeManager.palette.text,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatSaveDate(savedAt: String): String {
    return try {
        val instant = Instant.parse(savedAt)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            .withLocale(Locale.getDefault())
            .withZone(ZoneId.systemDefault())
        "Saved: ${formatter.format(instant)}"
    } catch (e: Exception) {
        "Saved: Unknown"
    }
}