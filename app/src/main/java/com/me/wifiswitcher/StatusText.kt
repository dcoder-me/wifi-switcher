package com.me.wifiswitcher

private val FAILURE_MARKERS = listOf("error", "exception", "fail")
private const val SU_MISSING = "\"su\""
private const val NO_PERMISSION = "no permission. Open Shizuku and start it"
private const val MAX_REASON_CHARS = 120
private const val FAILURE_PREFIX = "Couldn't"
private const val BUSY_MARK = "…"

/** Colour family of the status icon on the main screen. */
enum class Tone { OK, BUSY, ERROR, IDLE }

/**
 * Tells whether a [Shell.run] output means the command did not work.
 * @param out text printed by the command (stdout + stderr)
 * @returns true when the output carries an error marker
 */
fun commandFailed(out: String): Boolean =
    FAILURE_MARKERS.any { marker -> out.contains(marker, ignoreCase = true) }

/**
 * Builds the status line from what actually happened, not what was attempted.
 * @param goal status to show when every command worked, e.g. "On Wi-Fi • data OFF"
 * @param outputs outputs of the commands that make the goal true
 * @returns [goal] on success, otherwise a "Couldn't switch" line naming the first failure
 */
fun statusFor(goal: String, outputs: List<String>): String {
    val firstError = outputs.firstOrNull(::commandFailed) ?: return goal
    val reason = if (firstError.contains(SU_MISSING)) NO_PERMISSION
        else firstError.lineSequence().first().take(MAX_REASON_CHARS)
    return "Couldn't switch: $reason"
}

/**
 * Picks the status icon colour from the status line. Rules run in order.
 * @param status status line shown on screen
 * @param running whether the switcher service is running
 * @returns IDLE when stopped, ERROR for a failure line, BUSY while a step is in progress, else OK
 */
fun toneFor(status: String, running: Boolean): Tone = when {
    !running -> Tone.IDLE
    status.startsWith(FAILURE_PREFIX) -> Tone.ERROR
    status.contains(BUSY_MARK) -> Tone.BUSY
    else -> Tone.OK
}
