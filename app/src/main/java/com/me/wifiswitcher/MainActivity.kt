package com.me.wifiswitcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Animatable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

private const val TAG = "MainActivity"
private const val PREFS_NAME = "cfg"
private const val KEY_SSID = "ssid"
private const val KEY_PASS = "pass"
private const val KEY_INTERVAL = "interval"
private const val KEY_ENABLED = "enabled"
private const val DEFAULT_SSID = "MyHotspot"
private const val DEFAULT_PASS = "12345678"
private const val DEFAULT_INTERVAL = 10
private const val MIN_PASS_CHARS = 8
private const val RENDER_INTERVAL_MS = 2000L
private const val SWITCH_SETTLE_MS = 3000L
private const val SHIZUKU_REQUEST_CODE = 1
private const val SEGMENT_SLIDE_MS = 220L
private const val SEGMENT_TEXT_SP = 14f

/**
 * Settings and status screen. Shows the switcher's live state and lets the user turn it on or off.
 */
class MainActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var prefs: SharedPreferences
    private lateinit var ssidInput: EditText
    private lateinit var passInput: EditText
    private lateinit var segmentGroup: View
    private lateinit var segmentIndicator: View
    private lateinit var segmentRow: LinearLayout
    private var selectedInterval = 0
    private lateinit var rootGroup: View
    private lateinit var statusIcon: ImageView
    private lateinit var statusLabel: TextView
    private lateinit var statusText: TextView
    private lateinit var permissionText: TextView
    private lateinit var powerSwitch: Switch
    private lateinit var grantButton: TextView
    private var lastToggleAt = 0L
    private val onSwitchChanged = CompoundButton.OnCheckedChangeListener { _, checked -> onSwitchToggled(checked) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        bindViews()
        loadSettings()
        playSplash(findViewById(R.id.splash), findViewById(R.id.splash_ripple), findViewById(R.id.splash_tile),
            findViewById(R.id.splash_glyph), savedInstanceState == null)
        powerSwitch.setOnCheckedChangeListener(onSwitchChanged)
        grantButton.setOnClickListener { onGrantClicked() }
        startRenderLoop()
    }

    override fun onPause() {
        saveSettings()
        super.onPause()
    }

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun bindViews() {
        ssidInput = findViewById(R.id.ssid_input)
        passInput = findViewById(R.id.pass_input)
        segmentGroup = findViewById(R.id.interval_segments)
        segmentIndicator = findViewById(R.id.segment_indicator)
        segmentRow = findViewById(R.id.segment_row)
        buildSegments()
        rootGroup = findViewById(R.id.root_group)
        statusIcon = findViewById(R.id.status_icon)
        statusLabel = findViewById(R.id.status_label)
        statusText = findViewById(R.id.status_text)
        permissionText = findViewById(R.id.permission_text)
        powerSwitch = findViewById(R.id.power_switch)
        grantButton = findViewById(R.id.grant_button)
    }

    /**
     * Adds one label per interval choice to the segment row. Selection later only restyles these labels.
     */
    private fun buildSegments() {
        for ((index, minutes) in INTERVAL_CHOICES.withIndex()) {
            segmentRow.addView(buildSegment(index, minutes))
        }
    }

    /**
     * Creates one tappable segment label.
     * @param index position of this choice in [INTERVAL_CHOICES]
     * @param minutes interval this segment stands for
     * @return label that selects [index] when tapped
     */
    private fun buildSegment(index: Int, minutes: Int): TextView = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        gravity = Gravity.CENTER
        textSize = SEGMENT_TEXT_SP
        text = shortLabel(minutes)
        contentDescription = getString(R.string.interval_choice_desc, minutes)
        isClickable = true
        isFocusable = true
        setOnClickListener { selectInterval(index, animate = true) }
    }

    /**
     * Builds the compact label for an interval, such as "10m" or "1h".
     * @param minutes interval to label
     * @return localized short label
     */
    private fun shortLabel(minutes: Int): String {
        val (isHours, value) = intervalShortValue(minutes)
        return if (isHours) getString(R.string.interval_hours_short, value)
        else getString(R.string.interval_minutes_short, value)
    }

    private fun loadSettings() {
        ssidInput.setText(prefs.getString(KEY_SSID, DEFAULT_SSID))
        passInput.setText(prefs.getString(KEY_PASS, DEFAULT_PASS))
        selectedInterval = intervalIndex(prefs.getInt(KEY_INTERVAL, DEFAULT_INTERVAL))
        // === NOTE: === The row has no width until layout runs, so the thumb can only be placed after it
        segmentGroup.post { selectInterval(selectedInterval, animate = false) }
    }

    private fun saveSettings() {
        prefs.edit()
            .putString(KEY_SSID, ssidInput.text.toString())
            .putString(KEY_PASS, passInput.text.toString())
            .putInt(KEY_INTERVAL, INTERVAL_CHOICES[selectedInterval])
            .apply()
    }

    /**
     * Selects one interval segment: moves the thumb, restyles the labels, and saves when the user chose it.
     * @param index position in [INTERVAL_CHOICES] to select
     * @param animate true to slide the thumb and save; false to place it at once
     */
    private fun selectInterval(index: Int, animate: Boolean) {
        selectedInterval = index
        val segmentWidth = segmentRow.width / INTERVAL_CHOICES.size
        segmentIndicator.layoutParams.width = segmentWidth
        segmentIndicator.requestLayout()
        val target = (index * segmentWidth).toFloat()
        if (animate) slideIndicator(target) else segmentIndicator.translationX = target
        restyleSegments(index)
        if (animate) saveSettings()
    }

    /**
     * Slides the thumb to a segment position.
     * @param target horizontal offset in pixels
     */
    private fun slideIndicator(target: Float) {
        segmentIndicator.animate()
            .translationX(target)
            .setDuration(SEGMENT_SLIDE_MS)
            .setInterpolator(AnimationUtils.loadInterpolator(this, android.R.interpolator.fast_out_slow_in))
            .start()
    }

    /**
     * Marks one label as selected (bold, primary colour) and the rest as normal.
     * @param selectedIndex position of the selected label
     */
    private fun restyleSegments(selectedIndex: Int) {
        for (position in 0 until segmentRow.childCount) {
            val label = segmentRow.getChildAt(position) as TextView
            val selected = position == selectedIndex
            label.setTextColor(getColor(if (selected) R.color.text_primary else R.color.text_secondary))
            label.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)
            label.isSelected = selected
        }
    }

    /**
     * Handles a user flip of the power switch: saves settings, then starts or stops the switcher.
     * @param checked switch position the user just chose
     */
    private fun onSwitchToggled(checked: Boolean) {
        lastToggleAt = SystemClock.elapsedRealtime()
        saveSettings()
        when {
            !checked -> stopSwitcher()
            isPasswordTooShort() -> {
                toast(R.string.password_short)
                setSwitchQuietly(false)
            }
            else -> startSwitcher()
        }
    }

    /**
     * Sets the switch position without firing the change listener, so code updates do not loop back.
     * @param checked position to show on the switch
     */
    private fun setSwitchQuietly(checked: Boolean) {
        powerSwitch.setOnCheckedChangeListener(null)
        powerSwitch.isChecked = checked
        powerSwitch.setOnCheckedChangeListener(onSwitchChanged)
    }

    // === NOTE: === Only the root path writes the hotspot password, so only it needs 8+ characters
    private fun isPasswordTooShort(): Boolean =
        rootGroup.visibility == View.VISIBLE && passInput.text.length < MIN_PASS_CHARS

    private fun startSwitcher() {
        prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        startForegroundService(Intent(this, SwitcherService::class.java))
    }

    private fun stopSwitcher() {
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        stopService(Intent(this, SwitcherService::class.java))
    }

    private fun onGrantClicked() {
        try {
            when {
                !Shizuku.pingBinder() -> toast(R.string.shizuku_not_running)
                Shell.shizukuReady() -> toast(R.string.already_granted)
                else -> Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Shizuku check failed", e)
            toast(R.string.shizuku_missing)
        }
    }

    private fun startRenderLoop() {
        render()
        ui.postDelayed({ startRenderLoop() }, RENDER_INTERVAL_MS)
    }

    private fun render() {
        val running = SwitcherService.running
        renderStatus(running, SwitcherService.status)
        renderSwitch(running)
        renderPermission(Shell.shizukuReady())
    }

    /**
     * Keeps the switch in step with the service, but only once the settle window has passed.
     * @param running whether the switcher service is running
     */
    private fun renderSwitch(running: Boolean) {
        // === NOTE: === The service starts asynchronously; syncing right after a tap would flip the switch back
        val settled = SystemClock.elapsedRealtime() - lastToggleAt > SWITCH_SETTLE_MS
        if (settled && powerSwitch.isChecked != running) setSwitchQuietly(running)
    }

    private fun renderStatus(running: Boolean, status: String) {
        statusLabel.setText(if (running) R.string.running else R.string.stopped)
        // === NOTE: === The label already says "Stopped"; repeating it adds nothing, so show the next step instead
        statusText.text = if (running) status else getString(R.string.tap_start)
        val tone = toneFor(status, running)
        statusIcon.imageTintList = ColorStateList.valueOf(getColor(toneColorRes(tone)))
        renderPulse(running && tone != Tone.ERROR)
    }

    /**
     * Starts or stops the Wi-Fi pulse on the status icon.
     * @param animate true while the switcher runs without an error
     */
    private fun renderPulse(animate: Boolean) {
        val pulse = statusIcon.drawable as? Animatable ?: return
        if (animate && !pulse.isRunning) pulse.start()
        if (!animate && pulse.isRunning) pulse.stop()
    }

    private fun renderPermission(ready: Boolean) {
        permissionText.setText(if (ready) R.string.perm_ready else R.string.perm_missing)
        val fallbackVisibility = if (ready) View.GONE else View.VISIBLE
        grantButton.visibility = fallbackVisibility
        rootGroup.visibility = fallbackVisibility
    }

    private fun toast(messageRes: Int) = Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
}

/**
 * Maps a status tone to its colour resource.
 * @param tone colour family from [toneFor]
 * @return colour resource id used to tint the status icon
 */
private fun toneColorRes(tone: Tone): Int = when (tone) {
    Tone.OK -> R.color.tone_ok
    Tone.BUSY -> R.color.tone_busy
    Tone.ERROR -> R.color.tone_error
    Tone.IDLE -> R.color.tone_idle
}
