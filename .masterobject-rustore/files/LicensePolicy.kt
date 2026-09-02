package ru.furniturecrm.app

import kotlin.math.max

/** Pure trial/licensing rules kept separate from Android and RuStore SDK for deterministic tests. */
object LicensePolicy {
    const val TRIAL_DAYS: Long = 3
    const val DAY_MS: Long = 24L * 60L * 60L * 1000L
    const val TRIAL_MS: Long = TRIAL_DAYS * DAY_MS

    fun trustedNow(wallClockNowMs: Long, maxSeenWallClockMs: Long): Long =
        max(wallClockNowMs, maxSeenWallClockMs)

    fun evaluate(firstLaunchMs: Long, nowMs: Long, purchased: Boolean): LicenseAccess {
        if (purchased) return LicenseAccess.Full
        val end = firstLaunchMs + TRIAL_MS
        val remaining = (end - nowMs).coerceAtLeast(0L)
        return if (remaining > 0L) LicenseAccess.Trial(end, remaining) else LicenseAccess.Locked(end)
    }
}

sealed interface LicenseAccess {
    data object Full : LicenseAccess
    data class Trial(val endsAtMs: Long, val remainingMs: Long) : LicenseAccess
    data class Locked(val endedAtMs: Long) : LicenseAccess
}
