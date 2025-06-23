package project.unibo.tankyou.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import project.unibo.tankyou.R
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.utils.Constants
import project.unibo.tankyou.utils.getResourceString

/**
 * A dialog component that allows users to save a gas station with optional notes.
 *
 * @param isVisible Whether the dialog should be displayed
 * @param stationName The name of the gas station to be saved
 * @param initialNotes Initial notes text to populate the input field
 * @param onDismiss Callback invoked when the dialog is dismissed without saving
 * @param onSave Callback invoked when the user saves the station with notes
 */
@Composable
fun SaveStationDialog(
    isVisible: Boolean,
    stationName: String,
    initialNotes: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // Only render dialog content when visible
    if (isVisible) {
        Log.d(Constants.App.LOG_TAG, "SaveStationDialog shown for station: $stationName")

        // State to hold the current notes text with initial value
        var notes: String by remember { mutableStateOf(initialNotes) }

        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ThemeManager.palette.background
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Dialog title
                    Text(
                        text = "Notes", //getResourceString(R.string.save_station_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemeManager.palette.title
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display the station name being saved
                    Text(
                        text = stationName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeManager.palette.text
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notes input field with character limit validation
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { newValue: String ->
                            // Enforce character limit of 255 characters
                            if (newValue.length <= 255) {
                                notes = newValue
                                Log.d(
                                    Constants.App.LOG_TAG,
                                    "Notes updated: ${newValue.length} characters"
                                )
                            } else {
                                Log.w(
                                    Constants.App.LOG_TAG,
                                    "Notes input exceeded 255 character limit"
                                )
                            }
                        },
                        label = {
                            Text(
                                text = "Notes", // getResourceString(R.string.notes_label),
                                color = ThemeManager.palette.text
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Notes", //getResourceString(R.string.notes_placeholder),
                                color = ThemeManager.palette.text.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ThemeManager.palette.accent,
                            unfocusedBorderColor = ThemeManager.palette.text.copy(alpha = 0.3f),
                            focusedTextColor = ThemeManager.palette.text,
                            unfocusedTextColor = ThemeManager.palette.text
                        ),
                        supportingText = {
                            Text(
                                text = "${notes.length}/255",
                                color = ThemeManager.palette.text.copy(alpha = 0.6f)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Cancel button
                        TextButton(
                            onClick = {
                                Log.d(Constants.App.LOG_TAG, "SaveStationDialog cancelled by user")
                                onDismiss()
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = ThemeManager.palette.text
                            )
                        ) {
                            Text(getResourceString(R.string.cancel_button))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Save button
                        Button(
                            onClick = {
                                Log.d(
                                    Constants.App.LOG_TAG,
                                    "Saving station '$stationName' with notes: '${notes.take(50)}${if (notes.length > 50) "..." else ""}'"
                                )
                                onSave(notes)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThemeManager.palette.accent,
                                contentColor = ThemeManager.palette.background
                            )
                        ) {
                            Text(getResourceString(R.string.save_button))
                        }
                    }
                }
            }
        }
    } else {
        Log.d(Constants.App.LOG_TAG, "SaveStationDialog not visible")
    }
}