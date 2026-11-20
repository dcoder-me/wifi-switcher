package com.me.wifiswitcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val GOAL = "No Wi-Fi • data ON • hotspot ON"
private const val ON_WIFI = "On Wi-Fi • data OFF • hotspot OFF"
private const val BUSY_STATUS = "Checking for Wi-Fi…"
private const val FAILED_STATUS = "Couldn't switch: x"
private const val SU_ERROR = "ERROR: Cannot run program \"su\": error=2, No such file or directory"
// Real output from Galaxy A20 (Android 11) running stop-softap as the shell user
private const val SECURITY_ERROR = "Exception while executing WifiShellCommand: \n" +
    "java.lang.SecurityException: WifiService: Neither user 2000 nor current process has " +
    "android.permission.MAINLINE_NETWORK_STACK.\n\tat android.app.ContextImpl.enforce"

class StatusTextTest {
    @Test fun emptyOutputIsSuccess() = assertFalse(commandFailed(""))

    @Test fun successMessageIsSuccess() = assertFalse(commandFailed("Soft AP started successfully"))

    @Test fun suMissingIsFailure() = assertTrue(commandFailed(SU_ERROR))

    @Test fun securityExceptionIsFailure() = assertTrue(commandFailed(SECURITY_ERROR))

    @Test fun allCommandsWorkedShowsGoal() = assertEquals(GOAL, statusFor(GOAL, listOf("", "")))

    // The bug: status claimed "hotspot ON" while every command had failed
    @Test fun failedCommandsNeverShowGoal() {
        val status = statusFor(GOAL, listOf(SU_ERROR, SU_ERROR))
        assertFalse(status.contains("hotspot ON"))
        assertEquals("Couldn't switch: no permission. Open Shizuku and start it", status)
    }

    @Test fun otherFailureShowsFirstLineOfError() = assertEquals(
        "Couldn't switch: Exception while executing WifiShellCommand: ",
        statusFor(GOAL, listOf("", SECURITY_ERROR)),
    )

    @Test fun stoppedIsIdle() = assertEquals(Tone.IDLE, toneFor(ON_WIFI, running = false))

    @Test fun stoppedBeatsErrorRule() = assertEquals(Tone.IDLE, toneFor(FAILED_STATUS, running = false))

    @Test fun couldntPrefixIsError() = assertEquals(Tone.ERROR, toneFor(FAILED_STATUS, running = true))

    @Test fun ellipsisIsBusy() = assertEquals(Tone.BUSY, toneFor(BUSY_STATUS, running = true))

    @Test fun plainStatusIsOk() = assertEquals(Tone.OK, toneFor(ON_WIFI, running = true))
}
