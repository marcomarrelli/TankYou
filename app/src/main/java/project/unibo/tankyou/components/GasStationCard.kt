package project.unibo.tankyou.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import project.unibo.tankyou.R
import project.unibo.tankyou.data.database.entities.Fuel
import project.unibo.tankyou.data.database.entities.FuelType
import project.unibo.tankyou.data.database.entities.GasStation
import project.unibo.tankyou.data.database.entities.toLocalizedDateFormat
import project.unibo.tankyou.data.repositories.AppRepository
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.getResourceString

@Composable
fun GasStationCard(
    gasStation: GasStation,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    var fuelPrices by remember { mutableStateOf<List<Fuel>>(emptyList()) }
    var fuelTypeNames by remember { mutableStateOf<List<FuelType>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val repository = remember { AppRepository.getInstance() }

    LaunchedEffect(gasStation) {
        isVisible = true
        isLoading = true

        try {
            val prices = repository.getFuelPricesForStation(gasStation.id)
            fuelTypeNames = Constants.FUEL_TYPES
            fuelPrices = prices
        } catch (e: Exception) {
            e.printStackTrace()
            fuelPrices = emptyList()
            fuelTypeNames = emptyList()
        } finally {
            isLoading = false
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
                animationSpec = tween(300)
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
                    .padding(16.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp
                        )
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = ThemeManager.palette.background
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = gasStation.name ?: "Gas Station",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = ThemeManager.palette.text
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${gasStation.address ?: getResourceString(R.string.not_available)}, ${
                                    gasStation.city ?: getResourceString(
                                        R.string.not_available
                                    )
                                }",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ThemeManager.palette.text
                            )
                        }

                        IconButton(
                            onClick = onDismiss
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = ThemeManager.palette.text
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = ThemeManager.palette.secondary.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Owner",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemeManager.palette.text,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = gasStation.owner ?: "Not specified",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ThemeManager.palette.text
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Brand",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ThemeManager.palette.text,
                                        fontWeight = FontWeight.Medium
                                    )
                                    val flagName = Constants.getGasStationFlagName(gasStation.flag)
                                    Text(
                                        text = flagName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ThemeManager.palette.text
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Fuel Prices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ThemeManager.palette.text
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = ThemeManager.palette.primary
                            )
                        }
                    } else if (fuelPrices.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = ThemeManager.palette.secondary.copy(alpha = 0.1f)
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No fuel prices available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ThemeManager.palette.text
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(fuelPrices) { fuel ->
                                FuelPriceItem(
                                    fuel = fuel,
                                    fuelTypeNames = fuelTypeNames
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
private fun FuelPriceItem(
    fuel: Fuel,
    fuelTypeNames: List<FuelType>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ThemeManager.palette.secondary.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val fuelTypeName: String = Constants.getFuelTypeName(fuel.type)

                Text(
                    text = fuelTypeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = ThemeManager.palette.text
                )

                Text(
                    text = if (fuel.self) "Self Service" else "Full Service",
                    style = MaterialTheme.typography.bodySmall,
                    color = ThemeManager.palette.text
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format("€%.3f", fuel.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ThemeManager.palette.primary,
                    fontSize = 18.sp
                )

                val date = fuel.date.toLocalizedDateFormat()
                Text(
                    text = "Updated: $date",
                    style = MaterialTheme.typography.labelSmall,
                    color = ThemeManager.palette.text
                )
            }
        }
    }
}