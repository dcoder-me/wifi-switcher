package com.me.wifiswitcher

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

// === NOTE: === Talks to Android's tethering service directly. `cmd wifi start-softap` needs
// MAINLINE_NETWORK_STACK, which the shell user (and so Shizuku) lacks; this service only
// needs TETHER_PRIVILEGED, which the shell user has.
const val TETHERING_SERVICE = "tethering"
private const val TETHERING_WIFI = 0
private const val TETHER_ERROR_NO_ERROR = 0
private const val NO_RESULT = -1
private const val RESULT_TIMEOUT_SEC = 15L
private const val SHELL_PACKAGE = "com.android.shell"
private const val CONNECTOR_STUB = "android.net.ITetheringConnector\$Stub"
private const val REQUEST_PARCEL = "android.net.TetheringRequestParcel"
private const val LISTENER = "android.net.IIntResultListener"
private const val LISTENER_STUB = "$LISTENER\$Stub"

/**
 * Looks up a system service binder by name, with this process's own rights.
 * @param name service name, e.g. [TETHERING_SERVICE]
 * @returns the raw binder
 */
fun rawSystemBinder(name: String): IBinder =
    Class.forName("android.os.ServiceManager").getMethod("getService", String::class.java)
        .invoke(null, name) as IBinder

/**
 * Looks up a system service binder through Shizuku, so the call runs with the shell user's rights.
 * @param name service name, e.g. [TETHERING_SERVICE]
 * @returns the binder wrapped so calls go through Shizuku
 */
fun shizukuSystemBinder(name: String): IBinder =
    ShizukuBinderWrapper(SystemServiceHelper.getSystemService(name))

/**
 * Turns the Wi-Fi hotspot on, using the phone's saved hotspot name and password.
 * @param binder tethering service binder (wrapped by Shizuku in the app)
 * @returns "" on success, otherwise a line starting with "ERROR:"
 */
fun startWifiTethering(binder: IBinder): String = runTetherCall(binder, "startTethering")

/**
 * Turns the Wi-Fi hotspot off.
 * @param binder tethering service binder (wrapped by Shizuku in the app)
 * @returns "" on success, otherwise a line starting with "ERROR:"
 */
fun stopWifiTethering(binder: IBinder): String = runTetherCall(binder, "stopTethering")

private fun runTetherCall(binder: IBinder, methodName: String): String = try {
    val result = AtomicInteger(NO_RESULT)
    val answered = CountDownLatch(1)
    val connector = Class.forName(CONNECTOR_STUB).getMethod("asInterface", IBinder::class.java).invoke(null, binder)!!
    val listener = Class.forName(LISTENER_STUB).getMethod("asInterface", IBinder::class.java)
        .invoke(null, resultBinder(result, answered))!!
    invokeMatching(connector, methodName, listener)
    when {
        !answered.await(RESULT_TIMEOUT_SEC, TimeUnit.SECONDS) -> "ERROR: tethering service did not answer"
        result.get() == TETHER_ERROR_NO_ERROR -> ""
        else -> "ERROR: tethering failed with code ${result.get()}"
    }
} catch (e: Throwable) {
    val cause = (e as? InvocationTargetException)?.targetException ?: e
    "ERROR: ${cause.javaClass.simpleName}: ${cause.message}"
}

// Fills each parameter by its type, so it works whether or not the phone's
// version adds a "calling feature id" string after the package name.
private fun invokeMatching(connector: Any, methodName: String, listener: Any) {
    val method = connector.javaClass.methods.first { method -> method.name == methodName }
    val firstString = method.parameterTypes.indexOfFirst { type -> type == String::class.java }
    val args = method.parameterTypes.mapIndexed { index, type ->
        when {
            type.name == REQUEST_PARCEL -> wifiRequest()
            type.name == LISTENER -> listener
            type == Int::class.javaPrimitiveType -> TETHERING_WIFI
            type == String::class.java && index == firstString -> SHELL_PACKAGE
            else -> null
        }
    }
    method.invoke(connector, *args.toTypedArray())
}

private fun wifiRequest(): Any {
    val request = Class.forName(REQUEST_PARCEL).getDeclaredConstructor().newInstance()
    request.javaClass.getField("tetheringType").setInt(request, TETHERING_WIFI)
    // === NOTE: === Must stay false: true makes Android 11 demand NETWORK_STACK (error 14),
    // which the shell user lacks; false lets TETHER_PRIVILEGED pass.
    request.javaClass.getField("exemptFromEntitlementCheck").setBoolean(request, false)
    request.javaClass.getField("showProvisioningUi").setBoolean(request, false)
    return request
}

private fun resultBinder(result: AtomicInteger, answered: CountDownLatch): IBinder = object : Binder() {
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (code != FIRST_CALL_TRANSACTION) return super.onTransact(code, data, reply, flags)
        data.enforceInterface(LISTENER)
        result.set(data.readInt())
        answered.countDown()
        return true
    }
}
