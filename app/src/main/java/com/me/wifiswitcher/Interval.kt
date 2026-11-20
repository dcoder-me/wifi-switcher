package com.me.wifiswitcher

import kotlin.math.abs

private const val MINUTES_PER_HOUR = 60

// === NOTE: === Starts at 2 because the service never checks more often than every 2 minutes
val INTERVAL_CHOICES = listOf(2, 5, 10, 15, 30, 60)

/**
 * Finds which segment to show as selected for a saved interval.
 * @param savedMinutes interval stored in settings, possibly typed freely by an older build
 * @return index into [INTERVAL_CHOICES] of the closest choice
 */
fun intervalIndex(savedMinutes: Int): Int =
    INTERVAL_CHOICES.indices.minBy { index -> abs(INTERVAL_CHOICES[index] - savedMinutes) }

/**
 * Short label for an interval choice: whole hours show as hours, everything else as minutes.
 * @param minutes choice from [INTERVAL_CHOICES]
 * @return Pair(isHours, value), e.g. 60 gives (true, 1) and 15 gives (false, 15)
 */
fun intervalShortValue(minutes: Int): Pair<Boolean, Int> =
    if (minutes % MINUTES_PER_HOUR == 0) true to minutes / MINUTES_PER_HOUR else false to minutes
