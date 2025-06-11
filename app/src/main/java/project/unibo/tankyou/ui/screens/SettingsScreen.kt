package project.unibo.tankyou.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    val locationEnabled by SettingsManager.locationEnabled
    val showMyLocationOnMap by SettingsManager.showMyLocationOnMapFlow.collectAsState()
    
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(ThemeManager.palette.background)
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 16.dp),
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
                    println("Switch changed: $it")
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = ThemeManager.palette.border
            )
        }

        SettingsSection(
            title = getResourceString(R.string.location_access),
            icon = Icons.Default.LocationOn
        ) {
            SettingsToggleItem(
                title = getResourceString(R.string.location_access),
                description = getResourceString(R.string.location_access_desc),
                checked = locationEnabled,
                onCheckedChange = { SettingsManager.setLocationEnabled(it) }
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = ThemeManager.palette.border
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
                text = language.getDisplayName(LocalContext.current),
                selected = currentLanguage == language,
                onClick = { onLanguageChange(language) }
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            colors = RadioButtonDefaults.colors(
                selectedColor = ThemeManager.palette.accent,
                unselectedColor = ThemeManager.palette.primary,
                disabledSelectedColor = ThemeManager.palette.disabledText,
                disabledUnselectedColor = ThemeManager.palette.disabledBorder,
            ),
            onClick = null
        )

        Spacer(modifier = Modifier.width(12.dp))

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
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ThemeManager.palette.title
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = ThemeManager.palette.background
            )
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
                checkedThumbColor = ThemeManager.palette.white,
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