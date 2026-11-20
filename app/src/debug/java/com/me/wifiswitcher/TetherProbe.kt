package com.me.wifiswitcher

// === NOTE: === Debug-only entry point, run as the shell user (same rights as Shizuku):
// adb shell CLASSPATH=<apk> app_process / com.me.wifiswitcher.TetherProbe <list|start|stop>
object TetherProbe {
    private const val CONNECTOR = "android.net.ITetheringConnector"
    private const val WIFI = "android.net.wifi.IWifiManager"

    @JvmStatic
    fun main(args: Array<String>) {
        when (args.firstOrNull()) {
            "start" -> println("start -> '${startWifiTethering(rawSystemBinder(TETHERING_SERVICE))}'")
            "stop" -> println("stop -> '${stopWifiTethering(rawSystemBinder(TETHERING_SERVICE))}'")
            else -> listMethods()
        }
        System.exit(0)
    }

    private fun listMethods() {
        Class.forName(CONNECTOR).declaredMethods.map { method -> method.toGenericString() }.sorted().forEach(::println)
        Class.forName(WIFI).declaredMethods.map { method -> method.toGenericString() }
            .filter { line -> line.contains("SoftAp", ignoreCase = true) }.sorted().forEach(::println)
    }
}
