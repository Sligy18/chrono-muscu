package fr.gillesray.chronomuscu

import java.util.Locale

/** Temps restant affiché, arrondi à la seconde supérieure : 90 000 ms -> "1:30". */
fun formatClock(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = (ms + 999) / 1000
    return "%d:%02d".format(Locale.ROOT, totalSeconds / 60, totalSeconds % 60)
}

/** Libellé d'un bouton de chrono : 25 -> 25", 90 -> 1'30". */
fun formatPreset(seconds: Int): String =
    if (seconds < 60) "$seconds\"" else "%d'%02d\"".format(Locale.ROOT, seconds / 60, seconds % 60)

fun remainingMs(endAt: Long, now: Long): Long = maxOf(0L, endAt - now)

object Series {
    fun adjust(current: Int, delta: Int): Int = maxOf(0, current + delta)
}

object Presets {
    const val COUNT = 6
    val DEFAULTS = listOf(25, 60, 90, 120, 180, 240)
    fun clamp(seconds: Int): Int = seconds.coerceIn(5, 5999)
}

enum class EndSound(val label: String) {
    BIP("Bip"),
    BOIS("Bois"),
    AUCUN("Aucun"),
}
