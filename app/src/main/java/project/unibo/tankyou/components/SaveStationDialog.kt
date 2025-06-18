package project.unibo.tankyou.components

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
import project.unibo.tankyou.utils.getResourceString

@Composable
fun SaveStationDialog(
    isVisible: Boolean,
    stationName: String,
    initialNotes: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    if (isVisible) {
        var notes by remember { mutableStateOf(initialNotes) }

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
                    Text(
                        text = "Notes", //getResourceString(R.string.save_station_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ThemeManager.palette.title
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stationName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ThemeManager.palette.text
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { if (it.length <= 255) notes = it },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = ThemeManager.palette.text
                            )
                        ) {
                            Text(getResourceString(R.string.cancel_button))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onSave(notes) },
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
    }
}