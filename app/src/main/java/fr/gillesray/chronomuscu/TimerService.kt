package fr.gillesray.chronomuscu

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TimerState(
    val running: Boolean = false,
    /** Fin du décompte, en base [SystemClock.elapsedRealtime]. */
    val endAt: Long = 0L,
    val totalMs: Long = 0L,
)

/**
 * Tient le décompte en service de premier plan : le chrono sonne à l'heure
 * même écran éteint ou application fermée.
 */
class TimerService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> begin(intent.getIntExtra(EXTRA_SECONDS, 0))
            else -> finishService()
        }
        return START_NOT_STICKY
    }

    private fun begin(seconds: Int) {
        if (seconds <= 0) {
            finishService()
            return
        }
        job?.cancel()

        val totalMs = seconds * 1000L
        val endAt = SystemClock.elapsedRealtime() + totalMs
        _state.value = TimerState(running = true, endAt = endAt, totalMs = totalMs)

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(totalMs),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        holdWakeLock(totalMs + 15_000)

        job = scope.launch {
            while (true) {
                val left = remainingMs(endAt, SystemClock.elapsedRealtime())
                if (left <= 0) break
                delay(minOf(left, 1_000))
            }
            _state.value = TimerState(running = false, endAt = endAt, totalMs = totalMs)
            val settings = SettingsRepository(this@TimerService).current()
            EndAlert.play(this@TimerService, settings.sound, settings.vibrate, settings.duck)
            finishService()
        }
    }

    private fun finishService() {
        job?.cancel()
        job = null
        if (_state.value.running) _state.value = _state.value.copy(running = false)
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (_state.value.running) _state.value = _state.value.copy(running = false)
        releaseWakeLock()
        scope.cancel()
        super.onDestroy()
    }

    private fun holdWakeLock(timeoutMs: Long) {
        releaseWakeLock()
        val power = getSystemService(PowerManager::class.java)
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ChronoMuscu:timer").apply {
            setReferenceCounted(false)
            acquire(timeoutMs)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun buildNotification(totalMs: Long): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Chrono en cours", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Temps de repos restant"
                setShowBadge(false)
            }
        )

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Repos en cours")
            .setContentText("Durée : " + formatClock(totalMs))
            .setWhen(System.currentTimeMillis() + totalMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp)
            .addAction(0, "Arrêter", stop)
            .build()
    }

    companion object {
        private const val ACTION_START = "fr.gillesray.chronomuscu.START"
        private const val ACTION_STOP = "fr.gillesray.chronomuscu.STOP"
        private const val EXTRA_SECONDS = "seconds"
        private const val CHANNEL_ID = "timer"
        private const val NOTIFICATION_ID = 1

        private val _state = MutableStateFlow(TimerState())
        val state: StateFlow<TimerState> = _state.asStateFlow()

        fun start(context: Context, seconds: Int) {
            val intent = Intent(context, TimerService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_SECONDS, seconds)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            if (!_state.value.running) return
            context.startService(Intent(context, TimerService::class.java).setAction(ACTION_STOP))
        }
    }
}
