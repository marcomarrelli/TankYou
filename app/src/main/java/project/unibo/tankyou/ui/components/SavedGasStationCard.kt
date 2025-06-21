package project.unibo.tankyou.ui.components

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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import project.unibo.tankyou.data.database.model.SavedGasStationsModel
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants

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
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Gas Stations",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ThemeManager.palette.title,
                    fontWeight = FontWeight.Medium
                )

                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = ThemeManager.palette.accent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                isLoading || detailsLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
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
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(stationsWithDetails) { (savedStation, gasStation) ->
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
                        style = MaterialTheme.typography.titleSmall,
                        color = ThemeManager.palette.title,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = ThemeManager.palette.accent
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${gasStation.city ?: ""}, ${gasStation.province ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeManager.palette.text.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = gasStation.flag.toFlagLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeManager.palette.accent,
                    fontSize = 10.sp
                )
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