package fr.gillesray.chronomuscu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.gillesray.chronomuscu.EndSound
import fr.gillesray.chronomuscu.Presets
import fr.gillesray.chronomuscu.Settings
import fr.gillesray.chronomuscu.formatPreset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onBack: () -> Unit,
    onPresetChange: (index: Int, seconds: Int) -> Unit,
    onSoundChange: (EndSound) -> Unit,
    onPreviewSound: (EndSound) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onDuckChange: (Boolean) -> Unit,
) {
    var editing by rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle("Chronos")
            settings.presets.forEachIndexed { index, seconds ->
                ListItem(
                    headlineContent = { Text("Chrono ${index + 1}") },
                    trailingContent = {
                        Text(formatPreset(seconds), style = MaterialTheme.typography.titleMedium)
                    },
                    modifier = Modifier.clickable { editing = index },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Son de fin")
            EndSound.entries.forEach { sound ->
                ListItem(
                    headlineContent = { Text(sound.label) },
                    leadingContent = { RadioButton(selected = settings.sound == sound, onClick = null) },
                    trailingContent = {
                        if (sound != EndSound.AUCUN) {
                            TextButton(onClick = { onPreviewSound(sound) }) { Text("Écouter") }
                        }
                    },
                    modifier = Modifier.selectable(
                        selected = settings.sound == sound,
                        role = Role.RadioButton,
                        onClick = { onSoundChange(sound) },
                    ),
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("Comportement")
            SwitchRow(
                title = "Faire vibrer",
                subtitle = "Vibre à la fin du décompte.",
                checked = settings.vibrate,
                onChange = onVibrateChange,
            )
            SwitchRow(
                title = "Baisser la musique",
                subtitle = "Baisse les autres sons pendant que le chrono sonne.",
                checked = settings.duck,
                onChange = onDuckChange,
            )
        }
    }

    editing?.let { index ->
        DurationDialog(
            title = "Chrono ${index + 1}",
            initialSeconds = settings.presets[index],
            onDismiss = { editing = null },
            onConfirm = { seconds ->
                onPresetChange(index, seconds)
                editing = null
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.selectable(
            selected = checked,
            role = Role.Switch,
            onClick = { onChange(!checked) },
        ),
    )
}

@Composable
private fun DurationDialog(
    title: String,
    initialSeconds: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var minutes by rememberSaveable { mutableIntStateOf(initialSeconds / 60) }
    var seconds by rememberSaveable { mutableIntStateOf(initialSeconds % 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stepper(
                    label = "Minutes",
                    value = minutes,
                    onMinus = { minutes = (minutes - 1).coerceAtLeast(0) },
                    onPlus = { minutes = (minutes + 1).coerceAtMost(99) },
                )
                Stepper(
                    label = "Secondes",
                    value = seconds,
                    onMinus = { seconds = if (seconds < 5) 55 else (seconds - 5) / 5 * 5 },
                    onPlus = { seconds = if (seconds >= 55) 0 else (seconds / 5 + 1) * 5 },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(Presets.clamp(minutes * 60 + seconds)) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        },
    )
}

@Composable
private fun Stepper(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        FilledTonalIconButton(onClick = onPlus) { Text("+", fontSize = 22.sp) }
        Text(
            "%02d".format(value),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(72.dp),
        )
        FilledTonalIconButton(onClick = onMinus) { Text("-", fontSize = 22.sp) }
    }
}
