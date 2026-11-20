package com.me.wifiswitcher

import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalTest {
    @Test fun savedChoiceIsSelected() = assertEquals(INTERVAL_CHOICES.indexOf(10), intervalIndex(10))

    // Older builds let users type any number; pick the closest choice instead of resetting
    @Test fun typedValueMapsToClosestChoice() = assertEquals(INTERVAL_CHOICES.indexOf(5), intervalIndex(6))

    @Test fun tooSmallMapsToFirst() = assertEquals(0, intervalIndex(0))

    @Test fun tooLargeMapsToLast() = assertEquals(INTERVAL_CHOICES.lastIndex, intervalIndex(999))

    @Test fun wholeHourShowsAsHours() = assertEquals(Pair(true, 1), intervalShortValue(60))

    @Test fun shortIntervalShowsAsMinutes() = assertEquals(Pair(false, 15), intervalShortValue(15))

    @Test fun twoMinutesShowsAsMinutes() = assertEquals(Pair(false, 2), intervalShortValue(2))
}
