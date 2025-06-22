package project.unibo.tankyou.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import project.unibo.tankyou.BuildConfig
import project.unibo.tankyou.R
import project.unibo.tankyou.ui.theme.ThemeManager
import project.unibo.tankyou.ui.theme.ThemeMode
import project.unibo.tankyou.utils.Constants.AppLanguage
import project.unibo.tankyou.utils.SettingsManager
import project.unibo.tankyou.utils.getResourceString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val currentTheme by ThemeManager.themeMode
    val currentLanguage by SettingsManager.currentLanguage
    val showMyLocationOnMap by SettingsManager.showMyLocationOnMapFlow.collectAsState()
    val currentMapTint by SettingsManager.mapTintFlow.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(ThemeManager.palette.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection(
            title = getResourceString(R.string.appearance),
            icon = Icons.Default.ColorLens
        ) {
            ThemeSelectionCard(currentTheme = currentTheme)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = ThemeManager.palette.border
            )

            LanguageSelectionCard(
                currentLanguage = currentLanguage,
                onLanguageChange = { language ->
                    SettingsManager.setLanguage(language, context)
                }
            )
        }

        SettingsSection(
            title = getResourceString(R.string.settings_map_setting_title),
            icon = Icons.Default.Map
        ) {
            SettingsToggleItem(
                title = getResourceString(R.string.settings_own_position_title),
                description = getResourceString(R.string.settings_own_position_desc),
                checked = showMyLocationOnMap,
                onCheckedChange = {
                    SettingsManager.setShowMyLocationOnMap(it)
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = ThemeManager.palette.border
            )

            MapTintSelectionCard(
                currentTint = currentMapTint,
                onTintChange = { tint ->
                    SettingsManager.setMapTint(tint)
                }
            )
        }

        SettingsSection(
            title = "Actions",
            icon = Icons.Default.Refresh
        ) {
            SettingsActionItem(
                title = "Reset to Defaults",
                description = "Reset all settings to their default values",
                icon = Icons.Default.Refresh,
                onClick = {
                    coroutineScope.launch {
                        SettingsManager.resetToDefaults()
                        Toast.makeText(context, "Done!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = ThemeManager.palette.border
            )

            SettingsActionItem(
                title = "Clear Cache",
                description = "Clear application cache and temporary files",
                icon = Icons.Default.CleaningServices,
                onClick = {
                    coroutineScope.launch {
                        try {
                            context.cacheDir.deleteRecursively()
                            context.externalCacheDir?.deleteRecursively()
                            Toast.makeText(context, "Done!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Error occurred", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }


        SettingsSection(
            title = getResourceString(R.string.information),
            icon = Icons.Default.Info
        ) {
            SettingsInfoItem(
                title = getResourceString(R.string.app_version),
                value = BuildConfig.VERSION_NAME
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = ThemeManager.palette.border
            )

            SettingsInfoItem(
                title = getResourceString(R.string.developer),
                value = BuildConfig.AUTHORS.joinToString(", ")
            )
        }
    }
}

@Composable
private fun ThemeSelectionCard(currentTheme: ThemeMode) {
    Column(
        modifier = Modifier.selectableGroup()
    ) {
        Text(
            text = getResourceString(R.string.select_theme_mode),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp),
            color = ThemeManager.palette.text
        )

        OptionRow(
            text = getResourceString(R.string.light_mode),
            selected = currentTheme == ThemeMode.LIGHT,
            onClick = { ThemeManager.setTheme(ThemeMode.LIGHT) }
        )

        OptionRow(
            text = getResourceString(R.string.dark_mode),
            selected = currentTheme == ThemeMode.DARK,
            onClick = { ThemeManager.setTheme(ThemeMode.DARK) }
        )

        OptionRow(
            text = getResourceString(R.string.system_default_mode),
            selected = currentTheme == ThemeMode.SYSTEM,
            onClick = { ThemeManager.setTheme(ThemeMode.SYSTEM) }
        )
    }
}

@Composable
private fun LanguageSelectionCard(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup()
    ) {
        Text(
            text = getResourceString(R.string.select_language),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp),
            color = ThemeManager.palette.text
        )

        AppLanguage.entries.forEach { language ->
            OptionRow(
                text = language.getDisplayName(),
                selected = currentLanguage == language,
                onClick = { onLanguageChange(language) }
            )
        }
    }
}

@Composable
private fun MapTintSelectionCard(
    currentTint: SettingsManager.MapTint,
    onTintChange: (SettingsManager.MapTint) -> Unit
) {
    Column(
        modifier = Modifier.selectableGroup()
    ) {
        Text(
            text = "Map Tint",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp),
            color = ThemeManager.palette.text
        )

        SettingsManager.MapTint.entries.forEach { tint ->
            MapTintOptionRow(
                tint = tint,
                selected = currentTint == tint,
                onClick = { onTintChange(tint) }
            )
        }
    }
}

@Composable
private fun MapTintOptionRow(
    tint: SettingsManager.MapTint,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = ThemeManager.palette.accent,
                unselectedColor = ThemeManager.palette.text
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        if (tint != SettingsManager.MapTint.NONE) {
            Card(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                colors = CardDefaults.cardColors(
                    containerColor = Color(tint.colorValue)
                )
            ) {}

            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = tint.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = ThemeManager.palette.text
        )

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = getResourceString(R.string.selected),
                tint = ThemeManager.palette.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun OptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = ThemeManager.palette.accent,
                unselectedColor = ThemeManager.palette.text
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = ThemeManager.palette.text
        )

        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = getResourceString(R.string.selected),
                tint = ThemeManager.palette.accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ThemeManager.palette.title,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ThemeManager.palette.title
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ThemeManager.palette.primary.copy(alpha = 0.7f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = ThemeManager.palette.title
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ThemeManager.palette.text
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = ThemeManager.palette.primary.copy(alpha = 0.7f),
                checkedTrackColor = ThemeManager.palette.accent,
                uncheckedThumbColor = ThemeManager.palette.disabledText,
                uncheckedTrackColor = ThemeManager.palette.disabledBackground,
                uncheckedBorderColor = ThemeManager.palette.disabledBorder,
                checkedBorderColor = ThemeManager.palette.accent
            )
        )
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ThemeManager.palette.accent,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = ThemeManager.palette.title
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = ThemeManager.palette.text
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = ThemeManager.palette.text,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SettingsInfoItem(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = ThemeManager.palette.title
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = ThemeManager.palette.text
        )
    }
}