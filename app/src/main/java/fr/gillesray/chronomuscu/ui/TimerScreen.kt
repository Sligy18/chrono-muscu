package fr.gillesray.chronomuscu.ui

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.gillesray.chronomuscu.Settings
import fr.gillesray.chronomuscu.TimerState
import fr.gillesray.chronomuscu.formatClock
import fr.gillesray.chronomuscu.formatPreset
import fr.gillesray.chronomuscu.remainingMs
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    settings: Settings,
    timer: TimerState,
    onPreset: (Int) -> Unit,
    onStop: () -> Unit,
    onSeries: (Int) -> Unit,
    onResetSeries: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val now by produceState(SystemClock.elapsedRealtime(), timer) {
        while (timer.running) {
            value = SystemClock.elapsedRealtime()
            delay(50)
        }
        value = SystemClock.elapsedRealtime()
    }
    val remaining = if (timer.running) remainingMs(timer.endAt, now) else 0L
    val fraction = if (timer.totalMs > 0) remaining.toFloat() / timer.totalMs else 0f
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chrono Muscu") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Réglages")
                    }
                },
            )
        },
    ) { padding ->
        if (landscape) {
            Row(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Dial(remaining, fraction, timer.running, onStop, Modifier.weight(1f).fillMaxSize())
                Controls(settings, timer, onPreset, onSeries, onResetSeries, Modifier.weight(1.3f))
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Dial(remaining, fraction, timer.running, onStop, Modifier.weight(1f).fillMaxWidth())
                Controls(settings, timer, onPreset, onSeries, onResetSeries, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun Controls(
    settings: Settings,
    timer: TimerState,
    onPreset: (Int) -> Unit,
    onSeries: (Int) -> Unit,
    onResetSeries: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PresetGrid(settings.presets, timer, columns = 3, onPreset = onPreset)
        SeriesCard(settings.series, onSeries, onResetSeries)
    }
}

@Composable
private fun Dial(
    remaining: Long,
    fraction: Float,
    running: Boolean,
    onStop: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                .clip(CircleShape)
                .clickable(enabled = running, onClickLabel = "Arrêter le chrono", onClick = onStop),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxSize().padding(8.dp),
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    formatClock(remaining),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                )
                Text(
                    if (running) "Toucher pour arrêter" else "Choisis ton repos",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PresetGrid(
    presets: List<Int>,
    timer: TimerState,
    columns: Int,
    onPreset: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        presets.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { seconds ->
                    val active = timer.running && timer.totalMs == seconds * 1000L
                    val label = @Composable {
                        Text(formatPreset(seconds), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    }
                    val modifier = Modifier.weight(1f).height(64.dp)
                    if (active) {
                        Button(onClick = { onPreset(seconds) }, modifier) { label() }
                    } else {
                        FilledTonalButton(onClick = { onPreset(seconds) }, modifier) { label() }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesCard(series: Int, onSeries: (Int) -> Unit, onReset: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Séries", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onReset, enabled = series > 0) { Text("Remettre à zéro") }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SeriesButton("-5") { onSeries(-5) }
                SeriesButton("-1") { onSeries(-1) }
                Text(
                    series.toString(),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                SeriesButton("+1") { onSeries(1) }
                SeriesButton("+5") { onSeries(5) }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SeriesButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 10.dp)) {
        Text(label, fontSize = 18.sp)
    }
}
