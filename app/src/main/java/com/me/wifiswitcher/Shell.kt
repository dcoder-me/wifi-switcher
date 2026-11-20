package com.me.wifiswitcher

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/** Runs shell commands with ADB-level rights via Shizuku, or via root (su) as a fallback. */
object Shell {
    fun shizukuReady(): Boolean = try {
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Throwable) { false }

    fun run(cmd: String): String {
        return try {
            val p: Process = if (shizukuReady()) {
                val m = Shizuku::class.java.getDeclaredMethod(
                    "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                m.isAccessible = true
                m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
            } else {
                Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            }
            val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
            p.waitFor()
            out.trim()
        } catch (e: Throwable) {
            "ERROR: ${e.message}"
        }
    }

    /** Wrap a value in single quotes safely for sh. */
    fun q(s: String) = "'" + s.replace("'", "'\\''") + "'"
}
