package fr.gillesray.chronomuscu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.gillesray.chronomuscu.ui.ChronoTheme
import fr.gillesray.chronomuscu.ui.SettingsScreen
import fr.gillesray.chronomuscu.ui.TimerScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ChronoTheme {
                ChronoApp()
            }
        }
    }
}

@Composable
private fun ChronoApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { SettingsRepository(context) }
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val timer by TimerService.state.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    // Android 13+ : la notification du chrono demande une autorisation. Un refus ne bloque rien.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        SettingsScreen(
            settings = settings,
            onBack = { showSettings = false },
            onPresetChange = { index, seconds -> scope.launch { repository.setPreset(index, seconds) } },
            onSoundChange = { scope.launch { repository.setSound(it) } },
            onPreviewSound = { scope.launch { EndAlert.preview(context, it) } },
            onVibrateChange = { scope.launch { repository.setVibrate(it) } },
            onDuckChange = { scope.launch { repository.setDuck(it) } },
        )
    } else {
        TimerScreen(
            settings = settings,
            timer = timer,
            onPreset = { seconds -> TimerService.start(context, seconds) },
            onStop = { TimerService.stop(context) },
            onSeries = { delta -> scope.launch { repository.adjustSeries(delta) } },
            onResetSeries = { scope.launch { repository.resetSeries() } },
            onOpenSettings = { showSettings = true },
        )
    }
}
