package com.me.wifiswitcher

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.*
import android.os.Build
import android.os.IBinder
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private const val START_SOFTAP_CMD = "cmd wifi start-softap"
private const val STOP_SOFTAP_CMD = "cmd wifi stop-softap"
private const val HIDDEN_API_PREFIX = "L"

/**
 * Rules:
 *  - Connected to Wi-Fi      -> hotspot OFF, mobile data OFF
 *  - No Wi-Fi                -> mobile data ON, hotspot ON
 *  - While hotspot is on the Wi-Fi radio is busy, so every N minutes the
 *    hotspot is paused briefly to check whether a saved Wi-Fi is in range.
 */
class SwitcherService : Service() {
    companion object {
        const val CH = "switcher"
        var running = false
        var status = "Stopped"
    }

    private val wake = LinkedBlockingQueue<Int>()
    private var worker: Thread? = null
    private var hotspotMode = false
    private var onWifiHandled = false
    private lateinit var cm: ConnectivityManager

    private val wifiCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { wake.offer(1) }
        override fun onLost(network: Network) { wake.offer(1) }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        // === NOTE: === Lets reflection reach the hidden tethering classes on Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) HiddenApiBypass.addHiddenApiExemptions(HIDDEN_API_PREFIX)
        cm = getSystemService(ConnectivityManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CH, "WiFi Switcher", NotificationManager.IMPORTANCE_LOW))
        }
        startForeground(1, notif("Starting…"))
        cm.registerNetworkCallback(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), wifiCallback)
        running = true
        worker = Thread { loop() }.also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        running = false
        worker?.interrupt()
        try { cm.unregisterNetworkCallback(wifiCallback) } catch (_: Exception) {}
        setStatus("Stopped")
        super.onDestroy()
    }

    private fun prefs() = getSharedPreferences("cfg", Context.MODE_PRIVATE)

    private fun loop() {
        try {
            while (running) {
                tick()
                val checkMin = prefs().getInt("interval", 10).coerceAtLeast(2)
                val waitMs = if (hotspotMode) checkMin * 60_000L else 60_000L
                wake.clear()
                val woke = wake.poll(waitMs, TimeUnit.MILLISECONDS)
                if (woke != null) Thread.sleep(8_000) // let the network settle
            }
        } catch (_: InterruptedException) {}
    }

    private fun wifiConnected(): Boolean {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun waitForWifi(seconds: Int): Boolean {
        repeat(seconds) {
            if (wifiConnected()) return true
            Thread.sleep(1000)
        }
        return wifiConnected()
    }

    private fun tick() {
        if (wifiConnected()) {
            if (!onWifiHandled || hotspotMode) goWifi()
            return
        }
        onWifiHandled = false
        // Not on Wi-Fi: try Wi-Fi first (Android auto-joins saved networks)
        setStatus(if (hotspotMode) "Checking for Wi-Fi…" else "Looking for Wi-Fi…")
        if (hotspotMode) stopHotspot()
        Shell.run("svc wifi enable")
        if (waitForWifi(25)) goWifi() else goHotspot()
    }

    private fun goWifi() {
        val outputs = listOf(stopHotspot(), Shell.run("svc data disable"))
        hotspotMode = false
        onWifiHandled = true
        setStatus(statusFor("On Wi-Fi • data OFF • hotspot OFF", outputs))
    }

    private fun goHotspot() {
        val p = prefs()
        val ssid = p.getString("ssid", "MyHotspot")!!
        val pass = p.getString("pass", "12345678")!!
        val dataOut = Shell.run("svc data enable")
        Thread.sleep(3000)
        val hotspotOut = startHotspot(ssid, pass)
        hotspotMode = true
        setStatus(statusFor("No Wi-Fi • data ON • hotspot ON", listOf(dataOut, hotspotOut)))
    }

    /**
     * Turns the hotspot on: tethering service via Shizuku, or `cmd wifi` via root.
     * @param ssid hotspot name, used by the root path only
     * @param pass hotspot password, used by the root path only
     * @returns "" on success, otherwise an error line
     */
    private fun startHotspot(ssid: String, pass: String): String =
        if (Shell.shizukuReady()) startWifiTethering(shizukuSystemBinder(TETHERING_SERVICE))
        else Shell.run("$START_SOFTAP_CMD ${Shell.q(ssid)} wpa2 ${Shell.q(pass)}")

    /**
     * Turns the hotspot off: tethering service via Shizuku, or `cmd wifi` via root.
     * @returns "" on success, otherwise an error line
     */
    private fun stopHotspot(): String =
        if (Shell.shizukuReady()) stopWifiTethering(shizukuSystemBinder(TETHERING_SERVICE))
        else Shell.run(STOP_SOFTAP_CMD)

    private fun notif(text: String): Notification {
        val pi = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CH) else Notification.Builder(this)
        return b.setContentTitle("WiFi Switcher").setContentText(text)
            .setSmallIcon(R.drawable.ic_stat_wifi)
            .setContentIntent(pi).setOngoing(true).build()
    }

    private fun setStatus(s: String) {
        status = s
        if (running) getSystemService(NotificationManager::class.java).notify(1, notif(s))
    }
}
